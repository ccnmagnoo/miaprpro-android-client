package cl.dvt.miaguaruralapr

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_f03_cobros.*
import kotlinx.android.synthetic.main.section_op_tramo.view.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

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

                //abrir dialogo de edición
                adapter.setOnItemClickListener{item, view ->
                    val tramoItem = item as TramoItemAdapter
                    updateTramoDialog(tramoItem.tramo,requireActivity(),adapter)
                }

                //
            }/* end listener */

    }

    private fun updateTramoDialog(tramo: TramoObject, context: Context,adapter: GroupAdapter<GroupieViewHolder>?) {
        //abriendo diálogo
        val mDialogview = LayoutInflater.from(context).inflate(R.layout.section_op_tramo,null)
        val mBuilder = AlertDialog.Builder(context).setView(mDialogview)
        val mDialogBuilder = mBuilder.show()

        //poblando datos del editText
        mDialogview.name_editText_tramoOp.hint          = tramo.name
        mDialogview.base_editText_tramoOp.hint          = (tramo.consumptionBase.toString())
        mDialogview.price_editText_tramoOp.hint         = (tramo.priceBase.toString())
        mDialogview.description_editText_tramoOp.hint   = (tramo.description)

        //configurando edible base
        if (!tramo.edible){
            mDialogview.base_editText_tramoOp.isEnabled = false
        }

        mDialogview.ok_button_tramoOp.setOnClickListener {
            //TODO:verificar datos y password
            if (!updateTramo(mDialogview,mDialogBuilder,tramo,context,adapter)) {
                return@setOnClickListener
            }else{
                //TODO:subir tramo actualizado
                mDialogBuilder.dismiss()
            }
        }

        mDialogview.cancel_button_tramoOp.setOnClickListener {
            mDialogBuilder.dismiss()
        }
    }

    private fun updateTramo(mDialogview: View,mDialogBuilder:AlertDialog,tramo: TramoObject, context: Context, adapter: GroupAdapter<GroupieViewHolder>?):Boolean {
        //fechting editText values
        val name  = if(mDialogview.name_editText_tramoOp.text.toString().isEmpty())   { tramo.name}   else{mDialogview.name_editText_tramoOp.text.toString()}
        val base= if(mDialogview.base_editText_tramoOp.text.toString().isEmpty())   { tramo.consumptionBase}   else{mDialogview.base_editText_tramoOp.text.toString().toDouble()}
        val price   = if(mDialogview.price_editText_tramoOp.text.toString().isEmpty())  { tramo.priceBase}   else{mDialogview.price_editText_tramoOp.text.toString().toInt()}
        val des   = if(mDialogview.description_editText_tramoOp.text.toString().isEmpty())   { tramo.description}   else{mDialogview.description_editText_tramoOp.text.toString()}

        //create list of Tramos
        val tramoList = arrayListOf<TramoObject>()
        if (adapter!=null){
            for (index in 0 until adapter.itemCount){
                val item = adapter.getItem(index) as TramoItemAdapter
                tramoList.add (item.tramo)
            }
        }

        //Gen sorted tramo list
        val tramoListSorted = tramoList.sortedBy { item -> item.consumptionBase }

        //index del tramo actualmente en edición
        val index = tramoListSorted.indexOf(tramo)
        val tramoDown:TramoObject? = tramoListSorted.elementAtOrNull(index-1)
        val tramoUp:TramoObject? = tramoListSorted.elementAtOrNull(index+1)

        //checking limits Range values

            //base limits range
        val baseRange = Pair(tramoDown?.consumptionBase?:0.0, tramoUp?.consumptionBase?:100.0)
        if(base in baseRange.first..baseRange.second){
            //TODO:continuar
        }else{
            mDialogview.base_editText_tramoOp.error = "sólo rango de ${baseRange.first} a ${baseRange.second} m³"
            Toast.makeText(context,"error",Toast.LENGTH_LONG).show()
            return false
        }

            //pricing limits Range values
        val priceRange = Pair(tramoDown?.priceBase?:0, tramoUp?.priceBase?:5000)
        if(price in priceRange.first..priceRange.second){
            //TODO:continuar
        }else{
            mDialogview.price_editText_tramoOp.error = "sólo rango de $${priceRange.first} a $${priceRange.second} clp"
            Toast.makeText(context,"error",Toast.LENGTH_LONG).show()
            return false
        }

        //validar password
        val pass = mDialogview.password_editText_tramoOp
        val email = currentApr?.userAprEmail

        when {
            pass.text.toString().isEmpty() -> {
                //sin password
                pass.error = "password vacío"
                return false
            }
            email == null -> {
                //sin email
                pass.error = "error en la cuenta"
                return false
            }
            else -> {
                //verificar cuenta
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass.text.toString())
                    .addOnSuccessListener {
                        Toast.makeText(context,"tramo actualizado",Toast.LENGTH_LONG).show()
                        mDialogBuilder.dismiss()

                    }
                    .addOnFailureListener {
                        pass.error = "password incorrecto"
                        pass.requestFocus()
                        return@addOnFailureListener
                    }
                return false
            }
        }


    }

    private fun builtChart(adapter: GroupAdapter<GroupieViewHolder>){
        /* dependency: https://github.com/PhilJay/MPAndroidChart */

        /** carga List con Entry(x,y) -> se crea el LineDataSet(List) -> se setea DataLine(dataSet) -> chart.data = DataLine */

        /* instando Arrays con objetos Entry/BarEntry */
        val chart = consumption_chart_tramo /* Instando layout */

        /* Poblando arrayList principal(lineas)*/
        val (listOfEntries,listOfLabels) = populateChartData(adapter)

        /* Data Set list */
        val dataSet = LineDataSet(listOfEntries,"precio $")
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
/*
       chart.xAxis.valueFormatter = object : ValueFormatter() {
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
    private fun populateChartData(adapter: GroupAdapter<GroupieViewHolder>): Pair<ArrayList<Entry>,ArrayList<String>> {
        /* consumos individuales cargados en el gráfico de barras */
        val listOfEntries = arrayListOf<Entry>()
        val xAxisLabel= arrayListOf<String>()

        if (adapter.itemCount>0){
            val dateLecture = Calendar.getInstance()

            for (index in 0 until adapter.itemCount){
                /* loading values x y bar chart */
                val tramo = adapter.getItem(index) as TramoItemAdapter
                listOfEntries.add(
                    Entry(
                        tramo.tramo.consumptionBase.toFloat() ,
                        tramo.tramo.priceBase.toFloat()
                    )
                )
                Log.d("Tramo Chart", "cargado x:$index y:${tramo.tramo.priceBase.toString()} ")
                /* creating label of X axis */
                xAxisLabel.add(tramo.tramo.consumptionBase.toString())
            }
        }else{
            listOfEntries.add(Entry(1f,3f))
            listOfEntries.add(Entry(2f,2f))
            listOfEntries.add(Entry(3f,1f))
        }
        return Pair(listOfEntries,xAxisLabel)
    }



}
