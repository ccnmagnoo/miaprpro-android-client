package cl.dvt.miaguaruralapr

import android.app.AlertDialog
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt



class Tools() {

    fun isEmailValid(email:String): Boolean {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isSocialNumberValid(rol:String):Pair<Boolean,String?>{
        /** return pair value
         * pair.first: boolean response if it's valid
         * pair.second: string with the cause of invalidation
         * */

        fun verification(rol:String):Pair<Boolean,String?>{
            //Rol basics components body-cv
            val serial = listOf<Int>(2,3,4,5,6,7)
            val rolCv = rol.split("-")[1]
            val rolBody = rol
                .split("-")[0] /*extract number sequence part*/
                .replace(".","") /*removes points*/
                .chunked(1)
            if (rolBody.contains("k").or(rolBody.contains("K"))){
                return Pair(false,"rol mal escrito")
            }

            //Body sumatory
            val sucum = rolBody.sumBy { s: String -> s.toInt()}

            //Matricial multiply inverse rolBody[]X serial[]
            var matricialMultiply = 0
            var indexAux = 0
            for ((index, value) in rolBody.reversed().withIndex()){
                matricialMultiply += value.toInt()*serial[indexAux]
                if (index != serial.size-1) {indexAux+=1} else {indexAux = 0}
            }

            //calc CV
            val key = 11
            val code = key- matricialMultiply % key
            val calcCv = if(code!=10){code.toString()}else{"K"}

            //comparison
            return if(calcCv==rolCv.toUpperCase(Locale.ROOT)) {
                Pair(true,null)
            }else{
                Pair(false,"rol inválido")
            }



        }

        return when{
            !rol.contains("-",ignoreCase = true)->{
                //if dont have VC
                Pair(false,"sin verificador")
            }
            rol.contains(Regex("[a-zA-Z&&[^kK]]"))->{
                //if string contains alphabethic chars
                Pair(false,"carácter inválido")
            }
            rol.contains(" ",false)->{
                Pair(false,"espacios inválidos")
            }
            rol.isBlank()->{
                //if containst
                Pair(false,"rol vacio")
            }
            else->{
                return verification(rol)
            }
        }
    }

    fun dialogUploading(context:Context):AlertDialog{
        //Show dialog "uploading"
        val mDialogLoadingView = LayoutInflater.from(context).inflate(R.layout.section_add_consumption_loading, null) /* Instando dialogo "cargando" */
        val mBuilderLoading = AlertDialog.Builder(context).setView(mDialogLoadingView)   /* Inflado del cuadro de dialogo "cargando" */
        return mBuilderLoading.show()
    }
}


class Chart(val context: Context, private val list: ArrayList<Consumption>){
    /** Properties */

    val statistics = calculateStatistics(list)
    /** statistics return hashMap of doubles with this structure;
     * max : maximum value of the list
     * min : minimum value
     * avg : average value with out the firts ZERO consumption (initial value onCreate)
     * consumptionOnDebt: total costumer's consumption without payment
     * totalDebt: total debt on current costumer
     *  */

    /** type of charts */

    fun buildCombined(chart: CombinedChart){
        /** chart declared in layout as combinedChart */
        /* line charts: https://medium.com/@yilmazvolkan/kotlinlinecharts-c2a730226ff1 */
        /* dependency: https://github.com/PhilJay/MPAndroidChart */


        /* Poblando arrayList principal(barra) y secundario (linea)*/
        val listMain = getBarEntry(list)
        val listSecond = getLineEntry(list)

        /* DataSet  con el los valores de los Arrays más los  formato y colores  de las barras y lineas */
        val dataSetMain         = BarDataSet(listMain.first,"consumo") /* BARRAS  */
        with(dataSetMain){
            color           = context.resources.getColor(R.color.purpleLight)
            valueTextSize   = 10f
        }

        val dataSetSecondary    = LineDataSet(listSecond.first,"promedio") /* LINEAS  */
        with(dataSetSecondary){
            color           = context.resources.getColor(R.color.purpleMedium)
            lineWidth      = 2.5f
            valueTextSize  = 8f
            circleRadius   = 4f
            fillColor      = context.resources.getColor(R.color.colorAccent)
            setDrawValues(false)
        }

        // Encapsulado de la del dataSet en Data  */
        val dataBar     = BarData(dataSetMain)
        val dataLine    = LineData(dataSetSecondary)

        // data combinada */
        val dataCombined = CombinedData()
        dataCombined.setData(dataBar)
        dataCombined.setData(dataLine)

        /* Chart style  */
        with(chart){
            animateXY(1000,1500)
            description.text           = "m3"
            drawOrder                  = arrayOf(CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.BUBBLE, CombinedChart.DrawOrder.CANDLE, CombinedChart.DrawOrder.LINE, CombinedChart.DrawOrder.SCATTER)/* orden de las capas */
            legend.isWordWrapEnabled   = true
            legend.verticalAlignment   = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation         = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)

            /* seteando eje Y izquierdo */
            axisLeft.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            //statistics["consumptionOnDebt"]!!.toFloat()
            axisLeft.addLimitLine(LimitLine(40f,"promedio"))

            /* seteando eje Y derecho */
            axisRight.isEnabled = false
            axisRight.setDrawGridLines(false)
            axisRight.axisMinimum = 0f

            /* seteando eje X y etiquetas */
            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.axisMaximum = dataBar.xMax+1f
            xAxis.axisMinimum = dataBar.xMin-1f
            xAxis.granularity = 1f
            isDragEnabled = true
            isScaleXEnabled = true
            isScaleYEnabled = false

            xAxis.valueFormatter = object : ValueFormatter() {
                val formatDateLong = SimpleDateFormat("ddMMM") /* https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html */
/*            override fun getFormattedValue(value: Float,axis:AxisBase?): String {
                val timeMillis = TimeUnit.HOURS.toMillis(value.toLong())
                return formatDateLong.format(timeMillis)
            }*/

                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return listMain.second?.getOrNull(value.toInt()) ?: ""
                }
            }
        }

        /* cargando datos al layout */
        chart.data = dataCombined

    }

    fun buildBar(chart: BarChart){
        /** chart declared in layout as combinedChart */
        /* line charts: https://medium.com/@yilmazvolkan/kotlinlinecharts-c2a730226ff1 */
        /* dependency: https://github.com/PhilJay/MPAndroidChart */


        /* Poblando arrayList principal(barra) y secundario (linea)*/
        val listMain = getBarEntry(list)


        /* DataSet  con el los valores de los Arrays más los  formato y colores  de las barras y lineas */
        val dataSetMain         = BarDataSet(listMain.first,"consumo") /* BARRAS  */
        with(dataSetMain){
            color           = context.resources.getColor(R.color.purpleLight)
            valueTextSize   = 10f
        }

        // Encapsulado de la del dataSet en Data
        val dataBar     = BarData(dataSetMain)

        /* Chart style  */
        with(chart){
            animateXY(1000,1500)
            description.text           = "m3"

            with(legend){
                isWordWrapEnabled   = true
                verticalAlignment   = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation         = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }


            // seteando eje Y izquierdo
            axisLeft.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f

            //statistics["consumptionOnDebt"]!!.toFloat()
            val llMax = LimitLine(statistics["max"]!!.toFloat(),"max")
            val llMed = LimitLine(statistics["avg"]!!.toFloat(),"med")
            val llMin = LimitLine(statistics["min"]!!.toFloat(),"min")

            llMax.lineColor = context.resources.getColor(R.color.purpleMedium)
            llMed.lineColor = context.resources.getColor(R.color.blueMedium)
            llMin.lineColor = context.resources.getColor(R.color.purpleMedium)

            axisLeft.addLimitLine(llMax)
            axisLeft.addLimitLine(llMed)
            axisLeft.addLimitLine(llMin)

            // seteando eje Y derecho: falso
            axisRight.isEnabled = false
            axisRight.setDrawGridLines(false)
            axisRight.axisMinimum = 0f

            /* seteando eje X y etiquetas */
            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.axisMaximum = dataBar.xMax+1f
            xAxis.axisMinimum = dataBar.xMin-1f
            xAxis.granularity = 1f

            //Etiquetas
            isDragEnabled = true
            isScaleXEnabled = true
            isScaleYEnabled = false

            xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return listMain.second?.getOrNull(value.toInt()) ?: ""
                }
            }
        }

        /* cargando datos al layout */
        chart.data = dataBar

    }

    /**Entry builders*/
    private fun getBarEntry(consumptions:ArrayList<Consumption>):Pair<ArrayList<BarEntry>,ArrayList<String>?>{
        /* consumos individuales cargados en el gráfico de barras */
        val entries = arrayListOf<BarEntry>()
        val labels= arrayListOf<String>()

        val format = SimpleDateFormat("ddMMM")

        if (consumptions.isNotEmpty()){
            val dateLecture = Calendar.getInstance()

            for ((index,consumption) in consumptions.withIndex()){
                /* loading values x y bar chart */
                dateLecture.time = consumption.dateLectureNew /* objeto Date a Calendar */

                entries.add(
                    BarEntry(
                        (index+1).toFloat(), /*desfase 1 a la derecha */
                        consumption.consumptionCurrent.toFloat()
                    )
                )

                /* creating label of X axis */
                labels.add(format.format(consumption.dateLectureNew))
            }
        }else{
            //si no hay datos
            labels.add("") /*desfase 1 a la derecha */
            entries.add(BarEntry(1f,3f))
            entries.add(BarEntry(2f,2f))
            entries.add(BarEntry(3f,1f))
        }

        return Pair(entries,labels)
    }


    private fun getLineEntry(consumptions:ArrayList<Consumption>):Pair<ArrayList<Entry>,ArrayList<String>?>{
        /* consumo promedio calculado cargados en el gráfico de linea */
        val entries = arrayListOf<Entry>()



        if (consumptions.isNotEmpty()){
            for ((index,consumption) in consumptions.withIndex()){
                entries.add(
                    BarEntry(
                        (index+1).toFloat(),
                        statistics["avg"]!!.toFloat()
                    )
                )
            }
        }else{
            entries.add(BarEntry(1f,3f))
            entries.add(BarEntry(2f,2f))
            entries.add(BarEntry(3f,1f))
        }
        return Pair(entries,null)
    }

    /**Get avegare consumption, total umpayed one, and all the time consumption */
    private fun calculateStatistics(list: ArrayList<Consumption>):HashMap<String,Double>{

        //Extracting first consumption 0.0
        val subList = if(list.isNotEmpty()){
            //list without index 0 value
            list.subList(1, list.size)
        }else{
            //getting empty list
            arrayListOf(Consumption())
        }

        if (subList[0].javaClass == Consumption::class.java){"hola"}else{"chao"}

        var max = 0.0
        var min = 0.0
        var avg = 0.0
        var sumConsumption = 0.0
        var sumConsumptionUnpayed = 0.0
        var debt = 0.0

        subList.isNotEmpty().let{
            //Max consumption
            max = subList.maxBy { it -> it.consumptionCurrent  }?.consumptionCurrent?:0.0
            //Min consumption
            min = subList.minBy { it -> it.consumptionCurrent   }?.consumptionCurrent?:0.0

            //Total consumption and total  unpayed consumption
            sumConsumption = subList.sumByDouble { it->it.consumptionCurrent }
            sumConsumptionUnpayed = subList
                .filter {!it.paymentStatus }
                .sumByDouble { it.consumptionCurrent }
            debt = subList
                .filter {!it.paymentStatus }
                .sumByDouble { it.consumptionBill }

            //Average consumption ROUNDED
            avg = if(sumConsumption>0){
                (sumConsumption/subList.size*100).roundToInt().toDouble() / 100
            }else{0.0}


            //Rounded consumption ROUNDED
            sumConsumptionUnpayed = (sumConsumptionUnpayed*100).roundToInt().toDouble()/100

            //Rounded debt
            debt = (debt*100).roundToInt().toDouble()/100

            Log.d("Statistics", "promedio de consumo : $avg m3, consumo adeudado : $sumConsumptionUnpayed m3")

        }

        return hashMapOf<String,Double>(
            "max" to max,
            "min" to min,
            "avg" to avg,
            "consumptionOnDebt" to sumConsumptionUnpayed,
            "totalDebt" to debt
        )
    }

}