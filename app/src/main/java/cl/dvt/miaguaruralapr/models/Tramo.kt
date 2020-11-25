package cl.dvt.miaguaruralapr.models

import android.app.AlertDialog
import android.content.Context
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.SplashActivity.Companion.user
import cl.dvt.miaguaruralapr.Tools
import cl.dvt.miaguaruralapr.adapters.TramoItemAdapter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.dialog_update_tramo.view.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

/** Tramo is the section's cost of Water   */

@Parcelize
data class Tramo(
    val name:String             = "",/* nombre del plan de precios */
    val consumptionBase:Double  = 0.0,/* piso en metros cúbicos para los precios del consumo*/
    val priceBase:Int           = 0,/* precio del metro cúbico*/
    val description:String      = "",/* descripción del plan de precios del APR*/
    val edible:Boolean          = false,/* editable del valor piso m3, por defecto el piso es 0 y sólo se puede editar el piso*/
    val uidApr:String           = "",    /* string identificador del APR*/
    val uidTramo:String         = "", /*uid del tramo*/
    var timestamp:Date          = Date()
): Parcelable{
    /** properties */


    /** public functions */

    fun fetchTramo(context: Context, recyclerView: RecyclerView,chart: LineChart){
        val adapter = GroupAdapter<GroupieViewHolder>()
        recyclerView.adapter = adapter /* Cargando el ReclyclerView de esta Actividad*/

        val ref   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(user!!.uidApr)
            .collection("userTramo")
            .orderBy("consumptionBase", Query.Direction.ASCENDING)
        ref
            .addSnapshotListener(MetadataChanges.INCLUDE){ result, e ->
                if (e != null) {
                    Log.d("Consumption", "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (document in result?.documentChanges!!) {
                    when (document.type){
                        /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */

                        DocumentChange.Type.ADDED ->{
                            /* loading adapter */
                            val tramo = document.document.toObject(Tramo::class.java)
                            adapter.add(document.newIndex, TramoItemAdapter(tramo)) /* IMPORTANTE : cargando datos a los items del adaptador personalizado */
                            adapter.notifyDataSetChanged()

                            /* add object mutable list of user */
                            user?.tramoList?.add(document.newIndex,tramo)

                            Log.d("Tramo", "Indice del tramo : ${document.newIndex}")
                        }
                        DocumentChange.Type.MODIFIED    ->{

                            val tramo = document.document.toObject(Tramo::class.java)
                            /* change adapter */
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex, TramoItemAdapter(tramo))
                            adapter.notifyItemChanged(document.oldIndex)     /*actualizando datos a los items del adaptador personalizado*/
                            /* change mutable list of user */
                            user?.tramoList?.set(document.oldIndex,tramo)

                            Log.d("Tramo", "id del documento modificado: ${document.document.id}")
                        }
                        DocumentChange.Type.REMOVED     ->{
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.notifyItemRemoved(document.oldIndex)
                            user?.tramoList?.removeAt(document.oldIndex)

                            Log.d("Tramo", "id del consumo borrado: ${document.document.id}")
                        }
                    }
                }

                /** cargar chart */
                builtTramoChart(context,adapter,chart)

                //abrir dialogo de edición
                adapter.setOnItemClickListener{item, _ ->
                    item as TramoItemAdapter
                    updateDialog(item.tramo,context,user!!.tramoList)
                }

                //
            }/* end listener */

    }

    /** CRUD functions*/


    private fun create(context: Context,newTramo: Tramo){
        val loadingDialog = Tools().dialogUploading(context)

        val refTramo   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(user!!.uidApr)
            .collection("userTramo")
            .document(newTramo.uidTramo)

        refTramo.set(newTramo)
            .addOnSuccessListener{
                loadingDialog.dismiss()
                Toast.makeText(context,"tramo actualizado",Toast.LENGTH_LONG).show()
                Log.d("tramo create", "success creating Tramo id: ${newTramo.uidTramo}")
            }
            .addOnFailureListener {
                loadingDialog.dismiss()
                Toast.makeText(context,"error conexión",Toast.LENGTH_LONG).show()
                Log.d("tramo create", "fail creating Tramo id: ${newTramo.uidTramo}")
            }
    }

    private fun instanceCreation(context: Context, dialogView: View, dialogBuilder:AlertDialog, tramoList: ArrayList<Tramo>):Boolean{
        /** this function returns false() if theres errors or the tramo Object created on dialog Create
         * if nothing happens in this subject, continues to update this object in firebase database
         * dialogView: editor tramo dialog
         * dialog builder: for close dialog
         * */

        /*fetching input values*/
        val name = if(dialogView.name_editText_tramoOp.text.toString().isBlank()){
            "Tramo ${user?.tramoList?.size?.plus(1) ?:1}"
        }else{
            dialogView.name_editText_tramoOp.text.toString()
        }

        val base = if(dialogView.base_editText_tramoOp.text.toString().isBlank()){
            dialogView.base_editText_tramoOp.error = "vacio"
            dialogView.base_editText_tramoOp.requestFocus()
            return false
        }else{
            dialogView.base_editText_tramoOp.text.toString().toDouble()
        }

        val price = if(dialogView.price_editText_tramoOp.text.toString().isBlank()){
            dialogView.price_editText_tramoOp.error = "vacio"
            dialogView.price_editText_tramoOp.requestFocus()
            return false
        }else{
            dialogView.price_editText_tramoOp.text.toString().toInt()
        }

        val description = if(dialogView.description_editText_tramoOp.text.toString().isBlank()){
            "Tramo para consumos por sobre $base m3"
        }else{
            dialogView.description_editText_tramoOp.text.toString()
        }

        /*re-building new tramo with autocomplete */
        val newTramo = Tramo(
            name, /*automatic if's blank*/
            base, /*restricted*/
            price, /*restricted*/
            description, /*automatic if's blank*/
            true,
            user!!.uidApr,
            UUID.randomUUID().toString(),
            Calendar.getInstance().time
        )

        /* sorted by "base" Tramo list*/
        val tramoListSorted = tramoList.sortedBy { item->item.consumptionBase }

        /* fetch the last Tramo */
        val lastTramo:Tramo? = tramoListSorted.last()

        //* check razonable limits of new Tramo */
        val baseRange = Pair(lastTramo?.consumptionBase?:0.0,(lastTramo?.consumptionBase?:0.0)+100.0)
        if(newTramo.consumptionBase in baseRange.first..baseRange.second){
            /** if new tramo base is between */
        }else{
            dialogView.base_editText_tramoOp.error = "sólo rango de ${baseRange.first} a ${baseRange.second} m³"
            dialogView.base_editText_tramoOp.requestFocus()
            Toast.makeText(context,"fuera de rango", Toast.LENGTH_LONG).show()
            return false
        }

        //* check reazonable prices of new Tramo */
        val priceRange = Pair(lastTramo?.priceBase?:100,(lastTramo?.priceBase?:100)*50)
        if(newTramo.priceBase in priceRange.first..priceRange.second){
            //
        }else{
            dialogView.price_editText_tramoOp.error = "sólo rango de $${priceRange.first} a $${priceRange.second} clp"
            Toast.makeText(context,"fuera de rango", Toast.LENGTH_LONG).show()
            return false
        }

        //checking main user pass
        val pass = dialogView.password_editText_tramoOp
        val email = user?.userAprEmail

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
                        dialogBuilder.dismiss()
                        create(context,newTramo)
                    }
                    .addOnFailureListener {
                        pass.error = "incorrecto"
                        pass.requestFocus()
                        return@addOnFailureListener
                    }
                return false
            }
        }
    }

    private fun update(context:Context, newTramo: Tramo) {
        val loadingDialog = Tools().dialogUploading(context)

        newTramo.timestamp = Calendar.getInstance().time /*update timestamp */

        val refTramo   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(user!!.uidApr)
            .collection("userTramo")
            .document(newTramo.uidTramo)

        refTramo.set(newTramo)
            .addOnSuccessListener{
                Toast.makeText(context,"tramo actualizado",Toast.LENGTH_LONG).show()
                Log.d("tramo update", "success updating Tramo id: ${newTramo.uidTramo}")

                loadingDialog.dismiss() /*close loading screen */
            }
            .addOnFailureListener {
                Log.d("tramo update", "fail updating Tramo id: ${newTramo.uidTramo}")

                loadingDialog.dismiss() /*close loading screen */
            }


    }

    private fun instanceUpdate(context: Context, dialogView: View, dialogBuilder:AlertDialog, oldTramo: Tramo, tramoList: ArrayList<Tramo>):Boolean {
        /** this function returns false() if theres errors or the tramo Object's unchanged
         * if nothing happens in this subject, continues to update this object in firebase database
         * dialogView: editor tramo dialog
         * dialog builder: for close dialog
         * */


        /*fechting editText values*/
        val name = if (dialogView.name_editText_tramoOp.text.toString().isBlank()) {
            oldTramo.name
        } else {
            dialogView.name_editText_tramoOp.text.toString()
        }
        val base = if (dialogView.base_editText_tramoOp.text.toString().isBlank()) {
            oldTramo.consumptionBase
        } else {
            dialogView.base_editText_tramoOp.text.toString().toDouble()
        }
        val price = if (dialogView.price_editText_tramoOp.text.toString().isBlank()) {
            oldTramo.priceBase
        } else {
            dialogView.price_editText_tramoOp.text.toString().toInt()
        }
        val description = if (dialogView.description_editText_tramoOp.text.toString().isBlank()) {
            oldTramo.description
        } else {
            dialogView.description_editText_tramoOp.text.toString()
        }

        /*build newTramo*/
        /** building a new object joining edible values with steady values (old tramo properties) */
        val newTramo: Tramo = Tramo(
            /*variable parts*/
            name,
            base,
            price,
            description,
            /*steady parts*/
            oldTramo.edible,
            oldTramo.uidApr,
            oldTramo.uidTramo,
            oldTramo.timestamp
        )

        /*Get sorted tramo list consumption base 0 to max base floor*/
        val tramoListSorted = tramoList.sortedBy { item -> item.consumptionBase }

        //sorround tramos on current tramo on edition
        val index = tramoListSorted.indexOf(oldTramo)
        val tramoDown: Tramo? = tramoListSorted.elementAtOrNull(index-1)
        val tramoUp: Tramo? = tramoListSorted.elementAtOrNull(index+1)

        //checking limits Range values

        //base limits range
        val baseRange = Pair(tramoDown?.consumptionBase?:0.0, (tramoUp?.consumptionBase?:0.0)+100.0)
        if(newTramo.consumptionBase in baseRange.first..baseRange.second){
            /** if new tramo base is between */
        }else{
            dialogView.base_editText_tramoOp.error = "sólo rango de ${baseRange.first} a ${baseRange.second} m³"
            Toast.makeText(context,"error", Toast.LENGTH_LONG).show()
            return false
        }

        //pricing limits Range values
        val priceRange = Pair(tramoDown?.priceBase?:0, tramoUp?.priceBase?:50000)
        if(newTramo.priceBase in priceRange.first..priceRange.second){
            //
        }else{
            dialogView.price_editText_tramoOp.error = "sólo rango de $${priceRange.first} a $${priceRange.second} clp"
            Toast.makeText(context,"error", Toast.LENGTH_LONG).show()
            return false
        }

        //checking user account
        val pass = dialogView.password_editText_tramoOp
        val email = user?.userAprEmail

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
                        dialogBuilder.dismiss()
                        if (newTramo!=oldTramo){
                            Toast.makeText(context,"tramo actualizado", Toast.LENGTH_LONG).show()
                            update(context,newTramo)
                        }else{
                            Toast.makeText(context,"sin cambios", Toast.LENGTH_LONG).show()
                        }

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

    /** Tramo dialogs */

    public fun createDialog(context: Context, tramoList: ArrayList<Tramo>){
        //creating dialog, same as update xml
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_tramo,null)
        val mBuilder = AlertDialog.Builder(context).setView(dialogView)
        val mDialogBuilder = mBuilder.show()

        //write guide hints
        dialogView.name_editText_tramoOp.hint          = "nuevo Tramo ${user!!.tramoList.size+1}"
        dialogView.base_editText_tramoOp.hint          = "base"
        dialogView.price_editText_tramoOp.hint         = "precio"
        dialogView.description_editText_tramoOp.hint   = "(Descripción opcional) la base y el precio del nuevo tramo deben ser superior al Tramo ${user!!.tramoList.size}"

        dialogView.ok_button_tramoOp.setOnClickListener {

            /*verify changes on tramo object, check user account and build new object tramo */
            if (!instanceCreation(context,dialogView,mDialogBuilder,tramoList)) {
                return@setOnClickListener
            }else{
                mDialogBuilder.dismiss()
            }
        }

        dialogView.cancel_button_tramoOp.setOnClickListener {
            mDialogBuilder.dismiss()
        }



    }

    private fun updateDialog(tramo: Tramo, context: Context, tramoList: ArrayList<Tramo>) {
        //abriendo diálogo
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_tramo,null)
        val mBuilder = AlertDialog.Builder(context).setView(dialogView)
        val mDialogBuilder = mBuilder.show()

        //poblando datos del editText
        dialogView.name_editText_tramoOp.hint          = tramo.name
        dialogView.base_editText_tramoOp.hint          = (tramo.consumptionBase.toString())
        dialogView.price_editText_tramoOp.hint         = (tramo.priceBase.toString())
        dialogView.description_editText_tramoOp.hint   = (tramo.description)

        //configurando edible base
        if (!tramo.edible){
            dialogView.base_editText_tramoOp.isEnabled = false
        }

        dialogView.ok_button_tramoOp.setOnClickListener {


            /*verify changes on tramo object, check user account and build new object tramo */
            if (!instanceUpdate(context,dialogView,mDialogBuilder,tramo,tramoList)) {
                return@setOnClickListener
            }else{
                mDialogBuilder.dismiss()
            }
        }

        dialogView.cancel_button_tramoOp.setOnClickListener {
            mDialogBuilder.dismiss()
        }
    }

    /** Auxiliar functions */

    private fun builtTramoChart(context: Context, adapter: GroupAdapter<GroupieViewHolder>,chart:LineChart){
        /* dependency: https://github.com/PhilJay/MPAndroidChart */

        /** carga List con Entry(x,y) -> se crea el LineDataSet(List) -> se setea DataLine(dataSet) -> chart.data = DataLine */

        /* instando Arrays con objetos Entry/BarEntry */

        /* Poblando arrayList principal(lineas)*/
        val (listOfEntries,listOfLabels) = populateChartData(adapter)

        /* Data Set list */
        val dataSet = LineDataSet(listOfEntries,"precio $")
        dataSet.color       = getColor(context,R.color.purpleLight)
        dataSet.lineWidth      = 5f
        dataSet.valueTextSize  = 12f
        dataSet.circleRadius   = 10f
        dataSet.fillColor      = getColor(context,R.color.colorAccent)

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
