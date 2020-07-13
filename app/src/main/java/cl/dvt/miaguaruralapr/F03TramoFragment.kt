package cl.dvt.miaguaruralapr

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_f03_cobros.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class F03TramoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_f03_cobros, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchTramo()


    }

    private fun fetchTramo(){
        val adapter = GroupAdapter<GroupieViewHolder>()
        tramo_recyclerView_tramo.adapter = adapter /* Cargando el ReclyclerView de esta Actividad*/

        val ref   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(currentApr!!.uidApr)
            .collection("userPrices")
            .orderBy("consumptionBase", Query.Direction.ASCENDING)
        ref
            .addSnapshotListener(MetadataChanges.INCLUDE){result, e ->
                if (e != null) {
                    Log.d("Consumption", "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (document in result?.documentChanges!!) {
                    when (document.type){
                        DocumentChange.Type.ADDED ->{
                            Log.d("Tramo", "Indice del tramo : ${document.newIndex}")
                            val tramo = document.document.toObject(TramoObject::class.java)
                            adapter.add(document.newIndex,TramoItemAdapter(tramo)) /* IMPORTANTE : cargando datos a los items del adaptador personalizado */
                            adapter.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */
                            val tramo = document.document.toObject(TramoObject::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,TramoItemAdapter(tramo))
                            adapter.notifyItemChanged(document.oldIndex)     /*actualizando datos a los items del adaptador personalizado*/
                            Log.d("Tramo", "id del documento modificado: ${document.document.id}")
                        }
                        DocumentChange.Type.REMOVED     ->{
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.notifyItemRemoved(document.oldIndex)
                            Log.d("Tramo", "id del consumo borrado: ${document.document.id}")
                        }
                    }
                }/* end for */
                /** cargar chart */
                builtChart(adapter)

            }/* end listener */

    }

    private fun builtChart(adapter: GroupAdapter<GroupieViewHolder>){
        /* dependency: https://github.com/PhilJay/MPAndroidChart */

        /** carga List con Entry(x,y) -> se crea el LineDataSet(List) -> se setea DataLine(dataSet) -> chart.data = DataLine */

        /* instando Arrays con objetos Entry/BarEntry */
        val chart = consumption_chart_tramo /* Instando layout */

        /* Poblando arrayList principal(lineas)*/
        val list = populateChartData(adapter)

        /* Data Set list */
        val dataSet = LineDataSet(list,"precio $")
        dataSet.color       = resources.getColor(R.color.purpleLight)
        dataSet.lineWidth      = 5f
        dataSet.valueTextSize  = 12f
        dataSet.circleRadius   = 10f
        dataSet.fillColor      = resources.getColor(R.color.colorAccent)

        /*Encapsulando */
        val dataLine = LineData(dataSet)

        /* configurando estilo del recuadro gráfico  */
        chart.animateXY(500,1000)
        chart.description.text           = "tramo m3"
        chart.legend.isWordWrapEnabled   = false
        chart.legend.verticalAlignment   = Legend.LegendVerticalAlignment.BOTTOM
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        chart.legend.orientation         = Legend.LegendOrientation.HORIZONTAL
        chart.legend.setDrawInside(false)

        /* seteando eje Y izquierdo */
        chart.axisLeft.setDrawGridLines(false)
        val yMargin = 50f
        chart.axisLeft.axisMinimum = dataLine.yMin-yMargin
        chart.axisLeft.axisMaximum = dataLine.yMax+yMargin
        /* desactivando Y derecho */
        chart.axisRight.isEnabled = false


        /* seteando eje X y etiquetas */
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        val xMargin = 5f
        chart.xAxis.axisMaximum = dataLine.xMax+xMargin
        chart.xAxis.axisMinimum = dataLine.xMin-xMargin
        chart.xAxis.granularity = 5f
        chart.isDragEnabled = false
        chart.isScaleXEnabled = false
        chart.isScaleYEnabled = false

        /* chagando data en el chart IMPORTANTE*/
        chart.data = dataLine

/*        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return xAxisLabelR.getOrNull(value.toInt()) ?: ""
            }
        }*/
        chart.axisLeft.valueFormatter = object : ValueFormatter(){
            private val format = DecimalFormat("$ #.###")
            override fun getPointLabel(entry: Entry?): String {
                return format.format(entry?.y)
            }
        }


    }
    private var xAxisLabelR = arrayListOf<String>()
    fun populateChartData(adapter: GroupAdapter<GroupieViewHolder>): ArrayList<Entry> {
        /* consumos individuales cargados en el gráfico de barras */
        val list = arrayListOf<Entry>()
        val xAxisLabel= arrayListOf<String>()

        if (adapter.itemCount>0){
            val dateLecture = Calendar.getInstance()

            for (index in 0 until adapter.itemCount){
                /* loading values x y bar chart */
                val tramo = adapter.getItem(index) as TramoItemAdapter
                list.add(
                    Entry(
                        tramo.tramo.consumptionBase.toFloat() ,
                        tramo.tramo.priceBase.toFloat()
                    )
                )
                Log.d("Tramo Chart", "cargado x:$index y:${tramo.tramo.priceBase.toString()} ")
                /* creating label of X axis */
                xAxisLabel.add(tramo.tramo.consumptionBase.toString())
            }
            xAxisLabelR = xAxisLabel /* array con etiquetas del eje x */

        }else{
            list.add(Entry(1f,3f))
            list.add(Entry(2f,2f))
            list.add(Entry(3f,1f))
        }
        return list
    }



}
