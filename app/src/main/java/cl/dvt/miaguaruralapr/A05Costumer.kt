package cl.dvt.miaguaruralapr

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import cl.dvt.miaguaruralapr.MainActivity.Companion.block_key
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_a05_costumer.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt


@Suppress("IMPLICIT_CAST_TO_ANY", "NAME_SHADOWING",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class A05Costumer : AppCompatActivity() {
    override fun onStart() {
        super.onStart()
        val costumer = intent.getParcelableExtra<CostumerObject>(F02CostumerFragment.COSTUMER_KEY)
        fetchCostumer(costumer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a05_costumer)
        /* Recibir intent */
        val costumer = intent.getParcelableExtra<CostumerObject>(F02CostumerFragment.COSTUMER_KEY)

        fetchConsumption(costumer)

        update_floatingButton_costumerActivity.setOnClickListener {
            updateCostumer(costumer)
        }

        /* acción del botón regresar */
        back_button_costumerActivity.setOnClickListener {
            if (!block_key){finish()}
        }
    }


    var debtOnCloud = 0.0
    private fun fetchCostumer(costumer: CostumerObject){

        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(costumer.uidApr)
            .collection("userCostumer")
            .document(costumer.medidorNumber.toString())
        ref
            .addSnapshotListener  { document, e ->
                if (e != null) {
                    Log.w("costumerActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }
                val source = if (document != null && document.metadata.hasPendingWrites()){"Local"}else{"Server"}

                if (document != null && document.exists()) {
                    val costumer = document.toObject(CostumerObject::class.java)
                    Log.d("costumerActivity", "$source data: $costumer")
                    /* formatos */
                    val formatCurrency      = DecimalFormat("$ #,###")
                    /* cargando datos no editables */
                    medidorNumber_textView_costumerActivity.text = costumer?.medidorNumber.toString()
                    debt_textView_costumerActivity.text = formatCurrency.format(costumer?.userCostumerDebt)
                    debtOnCloud = costumer?.userCostumerDebt?.toDouble() ?: 0.0

                    /* cargando datos  editables */
                    name_editText_consumptionActivity.setText(costumer?.userCostumerName)
                    email_editText_costumerActivity.setText(costumer?.userCostumerEmail)
                    phone_editText_costumerActivity.setText(costumer?.userCostumerPhone)
                    dir_editText_costumerActivity.setText(costumer?.userCostumerDir)


                } else {
                    Log.d("costumerActivity", "$source data: null")
                }

            }

    }

    private fun updateCostumer(costumer: CostumerObject){
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(costumer.uidApr)
            .collection("userCostumer")
            .document(costumer.medidorNumber.toString())

        /* get values to update */
        val userCostumerName    = name_editText_consumptionActivity.text.toString()
        val userCostumerEmail   = email_editText_costumerActivity.text.toString()
        val userCostumerPhone   = phone_editText_costumerActivity.text.toString()
        val userCostumerDir     = dir_editText_costumerActivity.text.toString()

        /*making hashMap of*/
        val updateValues = hashMapOf(
            "userCostumerName" to userCostumerName,
            "userCostumerEmail" to userCostumerEmail,
            "userCostumerPhone" to userCostumerPhone,
            "userCostumerDir" to userCostumerDir
        )

        /* checking input data */
        if(checkInput(updateValues)){
            /*update information on cloud*/
           ref.set(updateValues, SetOptions.merge())
               .addOnSuccessListener {
                   Log.d("updateCostumer", "DocumentSnapshot successfully written!")
                   updateStatus_textView_costumerActivity.visibility = View.VISIBLE
               }
               .addOnFailureListener {
                       e -> Log.w("updateCostumer", "Error writing document", e)
               }
        }else{
            return
        }

    }

    private fun checkInput(updateValues:HashMap<String,String>):Boolean{
        if (updateValues["userCostumerName"]?.length!! < 5){
            name_editText_consumptionActivity.error = "corto"
            name_editText_consumptionActivity.requestFocus()
            return false
        }
        if(updateValues["userCostumerEmail"]!!.isNotEmpty()){
            if(!updateValues["userCostumerEmail"]!!.isEmailValid()){
                email_editText_costumerActivity.error = "inválido"
                email_editText_costumerActivity.requestFocus()
                return false
            }
        }
        if (updateValues["userCostumerPhone"]!!.isNotEmpty()){
            when (updateValues["userCostumerPhone"]?.length!!){
                12-> {
                    return true
                }
                9 -> {
                    phone_editText_costumerActivity.error = "incluya el +56"
                    phone_editText_costumerActivity.requestFocus()
                    return false
                }
                8-> {
                    phone_editText_costumerActivity.error = "incluya el +569"
                    phone_editText_costumerActivity.requestFocus()
                    return false
                }
                else -> {
                    phone_editText_costumerActivity.error = "inválido"
                    phone_editText_costumerActivity.requestFocus()
                    return false
                }
            }
        }
        return true
    }

    private fun fetchConsumption(costumer: CostumerObject){
        val adapter = GroupAdapter<GroupieViewHolder>()
        consumption_recyclerView_costumerActivity.adapter = adapter /**Cargando el ReclyclerView de esta Actividad*/

        val ref   = FirebaseFirestore.getInstance()
                .collection("userApr")
                .document(A01SplashActivity.currentApr!!.uidApr)
                .collection("userCostumer")
                .document(costumer.medidorNumber.toString())
                .collection("costumerConsumptionPersonal")
                .orderBy("timestamp", Query.Direction.ASCENDING)

        ref
            .addSnapshotListener(MetadataChanges.INCLUDE) { result, e ->
                if (e != null) {
                    Log.d("Search", "Listen consumption failed.", e)
                    return@addSnapshotListener
                }

                for (document in result!!.documentChanges) {
                    when (document.type){
                        DocumentChange.Type.ADDED ->{
                            val consumption = document.document.toObject(ConsumptionObject::class.java)
                            adapter.add(ConsumptionItemAdapter(consumption,true))  /* IMPORTANTE : cargando datos a los items del adaptador personalizado */
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            Log.d("Consumption", "Indice del modificado : ${document.oldIndex}")
                            val consumption = document.document.toObject(ConsumptionObject::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,ConsumptionItemAdapter(consumption,true))
                            adapter.notifyItemChanged(document.oldIndex)/*actualizando datos a los items del adaptador personalizado*/
                        }
                        DocumentChange.Type.REMOVED     ->{
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.notifyItemRemoved(document.oldIndex)
                        }
                    }
                }

                /** Cargando estadisticas */
                val consumptionStatictics = calculateConsumptionStatistics(adapter)
                consumptionOnDebt_textView_costumerActivity.text = consumptionStatictics["consumptionOnDebt"].toString()

                /** actualizar deuda en caso de error de contabilidad */
                if(debtOnCloud != consumptionStatictics["totalDebt"]?.toDouble()){
                    //updateCostumerDebt(consumptionStatictics["totalDebt"],costumer)
                }

                /** cargar chart */
                buildConsumptionChart(adapter)
            }

        /** click en el item del recyclerView */
        adapter.setOnItemClickListener { item, _ ->
            val consumption = item as ConsumptionItemAdapter
            ConsumptionOperation(consumption.consumption).updateConsumptionDialog(this)
        }

    }/* fin del fetchConsumption() */

    private fun updateCostumerDebt(debt:Double?, costumer:CostumerObject){
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(costumer.uidApr)
            .collection("userCostumer")
            .document(costumer.medidorNumber.toString())
        val updateValues = hashMapOf("userCostumerDebt" to debt)
        ref.set(updateValues, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("updateCostumer", "actualizada la deuda a :$debt!")
                updateStatus_textView_costumerActivity.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                    e -> Log.w("updateCostumer", "no pude actualizar la deuda", e)
            }
    }

    /** calcular estadisticas de consumo del consumidor*/
    var consumptionAvg = 0.0
    private fun calculateConsumptionStatistics(adapter:GroupAdapter<GroupieViewHolder>):HashMap<String,Double>{
        var consumptionAverage = 0.0
        var consumptionOnDebt = 0.0
        var totalDebt = 0.0
        if(adapter.itemCount>0){
            for(index in 1 until adapter.itemCount){
                val consumption = adapter.getItem(index) as ConsumptionItemAdapter
                consumptionAverage += consumption.consumption.consumptionCurrent
                if(!consumption.consumption.paymentStatus){
                    consumptionOnDebt+= consumption.consumption.consumptionCurrent
                    totalDebt += consumption.consumption.consumptionBill
                }
            }
        }

        val consumptionAverageR = if(adapter.itemCount>0){(consumptionAverage/adapter.itemCount*100).roundToInt().toDouble() / 100}else{0.0}
        val consumptionOnDebtR = if(adapter.itemCount>0){(consumptionOnDebt*100).roundToInt().toDouble()/100}else{0.0}
        val totalDebtR = if(adapter.itemCount>0){(totalDebt*100).roundToInt().toDouble()/100}else{0.0}
        consumptionAvg = consumptionAverageR

        Log.d("Statistics", "promedio de consumo : $consumptionAverageR m3, consumo adeudado : $consumptionOnDebtR m3")
        return hashMapOf<String,Double>(
            "consumptionAverage" to consumptionAverageR,
            "consumptionOnDebt" to consumptionOnDebtR,
            "totalDebt" to totalDebtR
        )
    }

    /** construir gráfico de consumos */
    private fun buildConsumptionChart(adapter: GroupAdapter<GroupieViewHolder>){
        /* line charts: https://medium.com/@yilmazvolkan/kotlinlinecharts-c2a730226ff1 */
        /* dependency: https://github.com/PhilJay/MPAndroidChart */

        /* instando Arrays con objetos Entry/BarEntry */
        val chart = consumption_chart_costumerActivity /* Instando layout */

        /* Poblando arrayList principal(barra) y secundario (linea)*/
        val listMain = populateChartDataMain(adapter)
        val listSecond = populateChartDataSecondary(adapter)

        /* DataSet  con el los valores de los Arrays más los  formato y colores  de las barras y lineas */
        val dataSetMain         = BarDataSet(listMain,"consumo") /* BARRAS  */
        dataSetMain.color       = resources.getColor(R.color.purpleLight)
        dataSetMain.valueTextSize       = 10f

        val dataSetSecondary    = LineDataSet(listSecond,"promedio") /* LINEAS  */
        dataSetSecondary.color  = resources.getColor(R.color.purpleMedium)
        dataSetSecondary.lineWidth      = 2.5f
        dataSetSecondary.valueTextSize  = 8f
        dataSetSecondary.circleRadius   = 4f
        dataSetSecondary.fillColor      = resources.getColor(R.color.colorAccent)
        dataSetSecondary.setDrawValues(false)

        /** Encapsulado de la del dataSet en Data  */
        val dataBar     = BarData(dataSetMain)
        val dataLine    = LineData(dataSetSecondary)

        /* configurando estilo del recuadro gráfico  */
        chart.animateXY(1000,1500)
        chart.description.text           = "m3"
        chart.drawOrder                  = arrayOf(DrawOrder.BAR, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.LINE, DrawOrder.SCATTER)/* orden de las capas */
        chart.legend.isWordWrapEnabled   = true
        chart.legend.verticalAlignment   = Legend.LegendVerticalAlignment.BOTTOM
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        chart.legend.orientation         = Legend.LegendOrientation.HORIZONTAL
        chart.legend.setDrawInside(false)

        /* seteando eje Y izquierdo */
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.axisMinimum = 0f

        /* seteando eje Y derecho */
        chart.axisRight.setDrawGridLines(false)
        chart.axisRight.axisMinimum = 0f

        /* seteando eje X y etiquetas */
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.axisMaximum = dataBar.xMax+1f
        chart.xAxis.axisMinimum = dataBar.xMin-1f
        chart.xAxis.granularity = 1f
        chart.isDragEnabled = true
        chart.isScaleXEnabled = true
        chart.isScaleYEnabled = false

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            val formatDateLong = SimpleDateFormat("ddMMM") /* https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html */
/*            override fun getFormattedValue(value: Float,axis:AxisBase?): String {
                val timeMillis = TimeUnit.HOURS.toMillis(value.toLong())
                return formatDateLong.format(timeMillis)
            }*/

            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return xAxisLabelR.getOrNull(value.toInt()) ?: ""
            }
        }

        /* data combinada */
        val dataCombined = CombinedData()
        dataCombined.setData(dataBar)
        dataCombined.setData(dataLine)

        /* cargando datos al layout */
        chart.data = dataCombined

    }
    private var xAxisLabelR = arrayListOf<String>()
    private fun populateChartDataMain(adapter: GroupAdapter<GroupieViewHolder>):ArrayList<BarEntry>{
        /* consumos individuales cargados en el gráfico de barras */
        val list = arrayListOf<BarEntry>()
        val xAxisLabel= arrayListOf<String>()
        xAxisLabel.add("")
        val formatDateLong = SimpleDateFormat("ddMMM")

        if (adapter.itemCount>0){
            val dateLecture = Calendar.getInstance()

            for (index in 1 until adapter.itemCount){
                /* loading values x y bar chart */
                val consumption = adapter.getItem(index) as ConsumptionItemAdapter
                dateLecture.time = consumption.consumption.dateLectureNew /* objeto Date a Calendar */

                list.add(
                    BarEntry(
                        index.toFloat(),
                        consumption.consumption.consumptionCurrent.toFloat()
                    )
                )

                /* creating label of X axis */
                xAxisLabel.add(formatDateLong.format(consumption.consumption.dateLectureNew))
            }
            xAxisLabelR = xAxisLabel /* array con etiquetas del eje x */

        }else{
            list.add(BarEntry(1f,3f))
            list.add(BarEntry(2f,2f))
            list.add(BarEntry(3f,1f))
        }
        return list
    }
    private fun populateChartDataSecondary(adapter: GroupAdapter<GroupieViewHolder>):ArrayList<Entry>{
        /* consumo promedio calculado cargados en el gráfico de linea */
        val list = arrayListOf<Entry>()

        if (adapter.itemCount>0){

            for (index in 1 until adapter.itemCount){
                list.add( BarEntry(index.toFloat(),  consumptionAvg.toFloat()) )
            }
        }else{
            list.add(BarEntry(1f,3f))
            list.add(BarEntry(2f,2f))
            list.add(BarEntry(3f,1f))
        }
        return list
    }

    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }



}

