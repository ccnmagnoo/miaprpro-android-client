package cl.dvt.miaguaruralapr.models

import android.app.AlertDialog
import android.content.Context
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import cl.dvt.miaguaruralapr.Chart
import cl.dvt.miaguaruralapr.SplashActivity.Companion.user
import cl.dvt.miaguaruralapr.fragments.CostumerFragment
import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.Tools
import cl.dvt.miaguaruralapr.adapters.ConsumptionItemAdapter
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.dialog_create_costumer.view.*
import java.util.*

@Parcelize
data class Costumer(
    val uidCostumer: String="",
    val uidApr:String="",
    val userCostumerName:String     = "",
    val userCostumerEmail:String    = "",
    val userCostumerPhone:String    = "",
    val userCostumerDir:String      = "",
    val userCostumerRut:String?     = "",
    val medidorLocation: Map<String,Double>? = mapOf(),
    val medidorNumber:Int           = 0,
    val medidorSerial:String?       = "",
    val dateMedidorRegister: Date = Date(),
    val typeUser:Int                = -1,   /**tipo de usuario  administrador=1, apr=2, costumer=3*/
    val userStatus:Int              = -1,   /**1 activo 0 inactivo*/
    val userCostumerDebt:Double     = 0.0,  /** deuda actual del consumidor */
    val userCostumerLastPayDate: Date = Date() /** fecha del último pago */
): Parcelable{
    /** public functions request */
    fun fetchConsumption(context: Context, recyclerView: RecyclerView,consumptionTotal:TextView?,consumptionDebt:TextView?,barChart:BarChart){
        /** fetch ALL costumer's consumptions from secondary firebase's Database */
        //Instando adapter
        val adapter = GroupAdapter<GroupieViewHolder>()
        recyclerView.adapter = adapter /**Cargando el ReclyclerView de esta Actividad*/

        //list of costumer's consumptions
        val listConsumption = arrayListOf<Consumption>()

        val ref   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(this.uidApr)
            .collection("userCostumer")
            .document(this.medidorNumber.toString())
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
                            val consumption = document.document.toObject(Consumption::class.java)
                            adapter.add(document.newIndex, ConsumptionItemAdapter(consumption,true))  /* IMPORTANTE : cargando datos a los items del adaptador personalizado */

                            listConsumption.add(consumption)
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            Log.d("Consumption", "Indice del modificado : ${document.oldIndex}")
                            val consumption = document.document.toObject(Consumption::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex, ConsumptionItemAdapter(consumption,true))
                            adapter.notifyItemChanged(document.oldIndex)/*actualizando datos a los items del adaptador personalizado*/

                            listConsumption[document.oldIndex] = consumption
                        }
                        DocumentChange.Type.REMOVED     ->{
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.notifyItemRemoved(document.oldIndex)

                            listConsumption.removeAt(document.oldIndex)
                        }
                    }
                }
                Log.d("Consumptions",listConsumption.toString())

                /** Cargando estadisticas */
                val statistics = Chart(context, listConsumption).statistics

                consumptionTotal?.text = statistics["avg"].toString()
                consumptionDebt?.text = statistics["consumptionOnDebt"].toString()

                /** cargar chart */
                Chart(context, listConsumption).buildBar(barChart)


            }

        /** click en el item del recyclerView */
        adapter.setOnItemClickListener { item, _ ->
            val consumption = item as ConsumptionItemAdapter
            consumption.consumption.updateDialog(context)

        }

    }

    /** CRUD functions */
    private fun instance(context: Context,medidorNumber:String, medidorSerial:String, userCostumerRut:String, geoSwitchState:Boolean){
        /** function to build Costumer Object */
        val uploadingDialog = Tools().dialogUploading(context)

        /*F03.02 Asignando valores a variables*/
        val uidCostumer: String         = medidorNumber
        val uidApr              = user!!.uidApr
        val userCostumerEmail           = ""
        val userCostumerPhone           = ""
        val userCostumerDir             = "calle, número , ${user!!.userAprLocalidad}"
        //TODO: val medidorLocation             = getLocationValue(geoSwitchState)
        val dateMedidorRegister         = Calendar.getInstance().time
        val typeUser            = 3                /**tipo de usuario  administrador=1, apr=2, costumer=3*/
        val userStatus          = 1 /*1 activo 0 inactivo*/
        val userCostumerDebt    = 0.0 /** deuda activa */
        val userCostumerLastPayDate = dateMedidorRegister

        /*F.03.03 building object costumer*/
        val costumer = Costumer(
            uidCostumer,
            uidApr,
            userCostumerName,
            userCostumerEmail,
            userCostumerPhone,
            userCostumerDir,
            userCostumerRut,
            medidorLocation,
            medidorNumber.toInt(),
            medidorSerial,
            dateMedidorRegister,
            typeUser,
            userStatus,
            userCostumerDebt,
            userCostumerLastPayDate
        )

        costumer.create(context,uploadingDialog)
    }

    private fun create(context: Context, uploadingDialog: AlertDialog) {
        /** create a new costumer in Firebase Database */
        /*F03.01 Instando Cloud Firebase*/
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(user!!.uidApr)
            .collection("userCostumer")
            .document(medidorNumber.toString())

        //F.03.04 Subiendo datos Consumidor a Firestore
        ref.set(this)
            .addOnSuccessListener {
                Log.d("Register Costumer", "Guardado en Firestore")
                //arrancar actividad de login
                Toast.makeText(
                    context,
                    "Cliente N°$medidorNumber agregado",
                    Toast.LENGTH_SHORT
                ).show()
                uploadingDialog.dismiss()
            }
            .addOnFailureListener {
                Log.d("Register Costumer", "Fallo Guardado en Firestore")
                Toast.makeText(
                    context,
                    "Error en guardar cliente $medidorNumber",
                    Toast.LENGTH_SHORT
                ).show()
                uploadingDialog.dismiss()
            }
    }

    fun update(context: Context,name:EditText,email:EditText,phone:EditText,dir:EditText,updateStatus:TextView,floatingButton:FloatingActionButton){
        //costumer reference
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(this.uidApr)
            .collection("userCostumer")
            .document(this.medidorNumber.toString())

        //making hashMap of inputs editText/
        val mapInputs = mapOf<String, EditText>(
            "userCostumerName" to name,
            "userCostumerEmail" to email,
            "userCostumerPhone" to phone,
            "userCostumerDir" to dir
        )

        // get pair with there is changes?, and if its, whitch ones
        val result = getInputsWithChanges(mapInputs)

        /* checking input data */
        if(result.first){
            //update information on cloud*/
            ref.set(result.second, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("updateCostumer", "costumer: ${this.medidorNumber} update info: ${result.second}")
                    updateStatus.visibility = View.VISIBLE

                    //changing update button
                    floatingButton
                        .setImageDrawable(getDrawable(context,R.drawable.ic_baseline_done_24))
                    floatingButton
                        .setBackgroundColor(getColor(context,R.color.greenSpring))

                    //changing focus
                    for(item in mapInputs){
                        item.value.clearFocus()
                    }

                }
                .addOnFailureListener {
                        e -> Log.w("updateCostumer", "Error writing document", e)
                }
        }else{
            return
        }
    }

    private fun delete(){
        /** deletes costumer from APR */

    }

    /** Dialogs */

    //Init dialog to create costumer
    fun initDialog(context: Context, currentLimit:Int){
        /*Verify number of user*/
        Log.d("CurrentPlan", "Límite plan: $currentLimit, usados: ${CostumerFragment.costumersCount}")

        if(currentLimit> CostumerFragment.costumersCount){
            createDialog(context)/*start add Costumer function*/
        }else{
            limitDialog(context) /* number or costumer sucription reach limit*/
        }
    }

    //Dialog in case of allow create costumer
    private fun createDialog(context: Context){
        /** https://devofandroid.blogspot.com/2018/04/alertdialog-with-custom-layout-kotlin.html
         * agregar al context This "requiredContext()"
         * extraer datos de dialog: https://demonuts.com/android-custom-dialog-edittext/
         * https://code.luasoftware.com/tutorials/android/android-text-input-dialog-with-inflated-view-kotlin/ */

        //Inflate dialog
        val numberOfCostumersPlus1= CostumerFragment.costumersCount +1
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_create_costumer, null)
        val mBuilder = AlertDialog.Builder(context)
            .setView(view)
            .setTitle("Ingresar nuevo cliente $numberOfCostumersPlus1 de ${CostumerFragment.currentCostumerLimit} ")
        val  formDialog = mBuilder.show()

        //Fetch current location
        var geoSwitchState    = false

        //F01.04 show map*/


        //Action button
        /*on location button*/
        view.location_button_costumer.setOnClickListener {
            //TODO:it.location_button_costumer.text = getLocationValueManual()
            geoSwitchState = true
        }
        //CANCEL button
        view.cancel_button_costumer.setOnClickListener {
            formDialog.dismiss()
        }
        //Save button
        view.save_button_costumer.setOnClickListener{
            val userCostumerName    = view.name_editText_costumer.text.toString()
            val medidorNumber       = view.medidorNumber_editText_costumer.text.toString()
            val medidorNumberR      = view.medidorNumberR_editText_costumer.text.toString()
            val medidorSerial       = view.serial_editText_costumer.text.toString()
            val userCostumerRut     = view.rut_editText_costumer.text.toString()
            val checkInputDataVal = checkInput(view,context,medidorNumber,medidorNumber,medidorNumberR,geoSwitchState)

            if (checkInputDataVal){
                instance(context, medidorNumber, medidorSerial, userCostumerRut, geoSwitchState)
                formDialog.dismiss()

            }else{
                return@setOnClickListener
            }
        }
    }

    //Dialog in case of overpass costumer limits suscription
    private fun limitDialog(context: Context){
        /** alert limit of costumers reached*/
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_create_costumer_denied, null)
        val mBuilderA = AlertDialog.Builder(context)
            .setView(view)
            .setTitle("Alerta de Límite")
            .setNegativeButton("OK", null)
            .setPositiveButton("AUMENTAR PLAN", null)
        mBuilderA.show()
    }

    /** Auxiliar functions */

    //Check value's inputs values on create Dialog form
    private fun checkInput(view: View,context: Context, userCostumerName:String, medidorNumber:String, medidorNumberR:String, geoSwitchState:Boolean):Boolean{
        while (userCostumerName.isEmpty()){
            Toast.makeText(context,"ERROR: nombre vacio", Toast.LENGTH_SHORT).show()
            view.name_editText_costumer.error = "Vacio"
            return false}
        while (medidorNumber.isEmpty()){
            Toast.makeText(context,"ERROR: número vacio", Toast.LENGTH_SHORT).show()
            view.medidorNumber_editText_costumer.error = "vacio"
            return false}
        while (medidorNumberR.isEmpty()){
            Toast.makeText(context,"ERROR: verificación número vacio", Toast.LENGTH_SHORT).show()
            view.medidorNumberR_editText_costumer.error = "vacio"
            return false}
        while (medidorNumber != medidorNumberR ){
            Toast.makeText(context,"ERROR: número de medidor incorrecto", Toast.LENGTH_SHORT).show()
            view.medidorNumberR_editText_costumer.error = "no coinciden"
            return false}
        while (!geoSwitchState){
            Toast.makeText(context,"ADVERTENCIA: medidor sin localizar", Toast.LENGTH_SHORT).show()
            return true
        }
        for (element in CostumerFragment.costumerList){
            if (element.medidorNumber == medidorNumber.toInt()){
                Toast.makeText(context,"ERROR: Ya exíste este MEDIDOR", Toast.LENGTH_SHORT).show()
                view.medidorNumber_editText_costumer.error = "ya existe"
                return false
            }
        }
        return true
    }

    private fun getInputsWithChanges(map:Map<String,EditText>):Pair<Boolean,Map<String,String>>{
        /** return a pair with the responding the answer of
         * pair.first = there's changes on values between: Boolean
         * pair.second = a map with values changed
         * */

        //Pair in case of SUCCESS
        /*map without empty editTexts*/
        val mapEditText = mutableMapOf<String,EditText>()
        /*map with just string*/
        val mapString = mutableMapOf<String,String>()

        for(item in map){
            if(item.value.text.toString().isNotBlank()){
                mapEditText[item.key] = item.value
                mapString[item.key] = item.value.text.toString()
            }
        }
        val success = Pair(true, mapString.toMap())

        //Pair in case of FAILURE
        val failure = Pair(false, mapOf("" to ""))

        if(mapEditText.isEmpty()){
            return failure
        }

        if(mapEditText.containsKey("userCostumerName")){
            val it = mapEditText["userCostumerName"]
            if (it?.text.toString().length < 5){
                it?.error = "nombre muy corto"
                it?.requestFocus()
                return failure
            }
        }
        if(mapEditText.containsKey("userCostumerEmail")){
            val it = mapEditText["userCostumerEmail"]
            if(!Tools().isEmailValid(it?.text.toString())){
                it?.error = "inválido"
                it?.requestFocus()
                return failure
            }
        }
        //checking size of phone number
        if (mapEditText.containsKey("userCostumerPhone")){
            val it = mapEditText["userCostumerPhone"]
            when (it?.text.toString().length){
                16-> {
                    //format +56_9_123_45_486
                    return success
                }
                9 -> {
                    it?.error = "incluya el +56"
                    it?.requestFocus()
                    return failure
                }
                8-> {
                    it?.error = "incluya el +569"
                    it?.requestFocus()
                    return failure
                }
                else -> {
                    it?.error = "inválido"
                    it?.requestFocus()
                    return failure
                }
            }
        }
        return success
    }

    //GPS & locations

    /** this only works inside Activity/Fragment Class*/
}