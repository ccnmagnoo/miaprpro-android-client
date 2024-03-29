package cl.dvt.miaguaruralapr.models

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.content.ContextCompat
import cl.dvt.miaguaruralapr.*
import cl.dvt.miaguaruralapr.SplashActivity.Companion.user
import cl.dvt.miaguaruralapr.adapters.ConsumptionItemDetailAdapter
import cl.dvt.miaguaruralapr.fragments.CostumerFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.dialog_create_consumption.view.*
import kotlinx.android.synthetic.main.dialog_update_consumption.view.*
import kotlinx.android.synthetic.main.dialog_update_consumption_allow.view.*
import kotlinx.android.synthetic.main.dialog_create_consumption_denied.view.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Parcelize
data class Consumption(
    val timestamp:Long              =0,
    val uidApr: String              ="",
    val uuidConsumption:String      ="",/** Identificador único de consumo de agua*/
    val medidorNumber: Int          =0,
    val dateLectureNew: Date        = Date(),
    val dateLectureOld: Date        = Date(),
    val logLectureNew:Double        = 0.0,/** lectura anterior */
    val logLectureOld:Double        = 0.0,/** lectura actual */
    val consumptionCurrent:Double   = 0.0,/** total consumo m3 */
    val consumptionBillDetail: List<Map<String,Double>> = listOf(mapOf("0" to 0.0)),/* detalle del cobro por tramo */
    val consumptionBill:Double      = 0.0,/** total a pagar */
    val consumptionPicUrl:String    = "",/** URL de la foto de respaldo medición en firebase */
    val paymentStatus:Boolean       = false,/** false sin pagar, true pagado */
    val paymentDate: Date = Date() /** fecha de pago de cliente consumidor */
): Parcelable{

    /** Firebase CRUD operations */
    private fun instance(context:Context, lecturaEntero:String, lecturaDecimal:String, medidorNumber:String, imageUri: Uri){
        /**build Consumption object*/

        //build uploading dialog
        val uploadingDialog = Tools().dialogUploading(context)

        //Fetching and generating identificators
        val time    = Calendar.getInstance()
        val uidApr     = user!!.uidApr

        //Joint lectures components
        val logLectureNew = ("$lecturaEntero.$lecturaDecimal").toDouble()

        // Init previous consumption */
        var logLectureOld:Double
        var dateLectureOld: Date

        val refLastConsumption = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(uidApr)
            .collection("userCostumer")
            .document(medidorNumber)
            .collection("costumerConsumptionPersonal")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)

        refLastConsumption.get()
            .addOnSuccessListener { result ->
                if (result.documents.size!=0){
                    val previousConsumption = result.documents.firstOrNull()?.toObject(Consumption::class.java)
                    Log.d("lastConsumption", "documento obtenido: $previousConsumption")
                    logLectureOld = previousConsumption?.logLectureNew?:logLectureNew
                    dateLectureOld = previousConsumption?.dateLectureNew?:time.time

                }else{
                    Log.d("lastConsumption", "documento obtenido: vacio/nulo ")
                    return@addOnSuccessListener
                }

                /* Calculo el consumo en caso de medidor retorna a 0 */
                val consumptionCurrent= calculateConsumption(logLectureNew,logLectureOld)

                /** status de pago TRUE si consumo es 0.0 */
                val paymentStatus = consumptionCurrent == 0.0

                /**F02.B.1 Cargando IMAGEN CONSUMO a base de datos*/
                /* nombrar archivo de imagen*/
                val formatShort          = SimpleDateFormat("yyyy.MM")
                val formatedDateYM   = formatShort.format(time.time)
                val formatLong         = SimpleDateFormat("yyyyMMdd")
                val formatedDateYMD  = formatLong.format(time.time)
                val uidAprLt = uidApr.substring(startIndex = 0, endIndex = 5) /* id corto */
                val filename = "$uidAprLt.$formatedDateYMD.$medidorNumber " /* genera un nombre largo*/
                val refPic = FirebaseStorage.getInstance().reference.child("/lectureBackupPic/$formatedDateYM/$uidAprLt/$filename")

                /* subiendo imagen y obteniendo url */
                refPic.putFile(imageUri)
                    .addOnSuccessListener{ it ->
                        Log.d("Consumption","Se subió la foto: ${it.metadata?.path}")
                        refPic.downloadUrl.addOnSuccessListener {
                            val url= it.toString()
                            Log.d("Consumption","Ubicación foto: $it")

                            val consumption = Consumption(
                                time.timeInMillis/1000,
                                /** time in seconds */ /** time in seconds */
                                uidApr,
                                UUID.randomUUID().toString(),
                                medidorNumber.toInt(),
                                time.time,
                                dateLectureOld,
                                logLectureNew,
                                logLectureOld,
                                consumptionCurrent,
                                listOf(mapOf("0" to 0.0)),  /* cálculo en cloud function */
                                0.0,        /* cálculo en cloud function*/
                                url,
                                paymentStatus
                            )
                            Log.d("builded","consumption:$consumption")

                            consumption.create(context,uploadingDialog)

                        }
                    }

            }
            .addOnFailureListener{e ->
                Log.d("lastConsumption", "Error getting documents: ", e)
            }

        /** fetching desde consumos desde mutableListOf */
        /* RES: https://cursokotlin.com/capitulo-10-listas-en-kotlin/ */
        /* RES : https://stackoverflow.com/questions/46868903/sort-data-from-a-mutablelist-in-kotlin */

    }

    private fun create(context: Context, uploadingDialog:AlertDialog){
        /** instando base de datos firebase*/
        /** guardar objeto en 2 partes distintas, collecciòn particular del cliente
         * para la app cliente y un main collection para efectos de app administrador*/

        /* --CLOUD FUNCTION: copia consumption a Main Collection-- */

        // colección de consumos del Costumer */
        val refSubCollection   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(this.uidApr)
            .collection("userCostumer")
            .document(this.medidorNumber.toString())
            .collection("costumerConsumptionPersonal")
            .document(this.uuidConsumption)

        refSubCollection.set(this)
            .addOnSuccessListener {
                Log.d("Consumption", "uploaded:$this")
                //arrancar actividad de login
                Toast.makeText(context, "new consumption Costumer: ${this.medidorNumber} volumen: ${this.consumptionCurrent} m3", Toast.LENGTH_SHORT ).show()
                uploadingDialog.dismiss()
            }
            .addOnFailureListener {
                Log.d("Consumption", "fallo almacenamiento en Firestore")
                Toast.makeText(context, "Error en guardar cliente ${this.medidorNumber} ", Toast.LENGTH_SHORT).show()
                uploadingDialog.dismiss()
            }


        /*--CLOUD FUNCTION: incrementa deuda del consumidor*/
        /*--CLOUD FUNCTION: incrementa estadística global de UserAPR*/
    }

    //borrar consumo en Firebase
    private fun delete(){
        val refSubCollection   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(this.uidApr)
            .collection("userCostumer")
            .document(this.medidorNumber.toString())
            .collection("costumerConsumptionPersonal")
            .document(this.uuidConsumption)


        // borrar consumo en la colección principal
        refSubCollection.delete().addOnSuccessListener {
            Log.d("Consumption","consumption deleted: ${this.uuidConsumption}")
            /*--CLOUD FUNCTION: Borrar copia consumption en Main Collection*/
            /*--CLOUD FUNCTION: User statistics Updated*/
            /*--CLOUD FUNCTION: Costumer debt Updated*/

            //Borrar imagen del Firestore
            val refPic = FirebaseStorage.getInstance()
                .reference.child(this.consumptionPicUrl)
            refPic.delete()
                .addOnSuccessListener {
                    Log.d("Delete Pic", "borrada la foto")
                }
                .addOnFailureListener{
                    Log.d("Delete Pic", "error al borrar")
                }
        }
    }

    //actualizar estado de pago
    internal fun update(payCheckBox: CheckBox){

        /* botón actualización de estado de pago */
        if (payCheckBox.isChecked){
            MainActivity.block_key = true /* se bloquea el permitir cerrar la actividad*/
            val newPaymentStatus = hashMapOf(
                "paymentStatus" to true,
                "paymentDate" to Calendar.getInstance().time
            )

            // actualizando status de pago en TRUE*/
            val refSubCollection   = FirebaseFirestore.getInstance()
                .collection("userApr")
                .document(this.uidApr)
                .collection("userCostumer")
                .document(this.medidorNumber.toString())
                .collection("costumerConsumptionPersonal")
                .document(this.uuidConsumption)

            refSubCollection.set(newPaymentStatus, SetOptions.merge() )
                .addOnSuccessListener {
                    // actualizando fecha de pago */
                    payCheckBox.text = "pagado"
                    /*--CLOUD FUNCTION: actualizando el consumption en el MainCollection */
                    /*--CLOUD FUNCTION: reducción deuda de costumer */
                    MainActivity.block_key = false
                }.addOnFailureListener {
                    payCheckBox.isChecked = false
                    payCheckBox.text = "sin red"
                    MainActivity.block_key = true
                }
        } else{
            val newPaymentStatus = hashMapOf(
                "paymentStatus" to false,
                "paymentDate" to Calendar.getInstance().time
            )

            // actualizando status de pago en FALSE
            val refSubCollection   = FirebaseFirestore.getInstance()
                .collection("userApr")
                .document(this.uidApr)
                .collection("userCostumer")
                .document(this.medidorNumber.toString())
                .collection("costumerConsumptionPersonal")
                .document(this.uuidConsumption)

            refSubCollection.set(newPaymentStatus, SetOptions.merge() )
                .addOnSuccessListener {
                    payCheckBox.text = "deuda"
                    /*--CLOUD FUNCTION: actualizando el consumption en el MainCollection */
                    /*--CLOUD FUNCTION: aumentando deuda de costumer */                }.addOnFailureListener {
                    payCheckBox.isChecked = true
                    payCheckBox.text = "sin red"
                }
        }
        Handler().postDelayed({
            payCheckBox.text = ""
        }, 2000)
    }

    /**Dialogs screens*/

    //DIALOG edit consumption
    fun updateDialog(context:Context){

        //Building dialog
        val dialog = LayoutInflater.from(context).inflate(R.layout.dialog_update_consumption, null) /** Instando dialogView */
        val mBuilder = AlertDialog.Builder(context).setView(dialog)
        val mAlertDialog = mBuilder.show()/* show dialog */

        //Populating Data on Dialog
        val formatCurrency      = DecimalFormat("$ #,###")
        val formatDateLong      = SimpleDateFormat("EEEE dd MMMM yyyy")
        val formatDateShort     = SimpleDateFormat("dd/MM/yyyy")
        dialog.medidorNumber_textView_opConsumption.text   = this.medidorNumber.toString()
        dialog.date_textView_opConsumption.text            = formatDateLong.format(this.paymentDate).toString()
        dialog.dateLectureNew_textView_opConsumption.text  = formatDateShort.format(this.dateLectureNew).toString()
        dialog.dateLectureOld_textView_opConsumption.text  = formatDateShort.format(this.dateLectureOld).toString()
        dialog.logNew_textView_opConsumption.text          = this.logLectureNew.toString()
        dialog.logOld_textView_opConsumption.text          = this.logLectureOld.toString()
        dialog.consumption_textView_opConsumption.text     = this.consumptionCurrent.toString()
        dialog.importe_textView_opConsumption.text         = formatCurrency.format(this.consumptionBill.toInt())
        dialog.payStatus_checkbox_opConsumption.isChecked  = this.paymentStatus
        dialog.payStatus_checkbox_opConsumption.text       = if (this.paymentStatus){"pagado"}else{"pagar"}

        //Loaging PIC storaged in firebase
        Picasso.get().load(this.consumptionPicUrl).into(dialog.image_imageView_opConsumption,object: Callback{
            override fun onSuccess() {
                dialog.loading_progressBar_opConsumption.visibility = View.GONE
            }
            override fun onError(e: Exception?) {
                /*cuando la imagen no existe en firestores*/
                dialog.loading_progressBar_opConsumption.visibility = View.GONE
                dialog.noPic_imageView_opConsumption.visibility = View.VISIBLE
                dialog.image_imageView_opConsumption.background = ContextCompat.getDrawable(context,
                    R.drawable.app_draw_background
                )
            }
        })

        //Populating data on mini-recyclerView with billing detail
        val adapter = GroupAdapter<GroupieViewHolder>()     /* adaptador para el recyclerView */
        dialog.consumptionDetail_recyclerView_opConsumption.adapter = adapter
        for ((index, billingData) in this.consumptionBillDetail.withIndex()){
            if (index > 0 ){
                adapter.add(ConsumptionItemDetailAdapter(billingData))
            }
        }

        // Bloqueando checkbox si billing es 0 */
        /** boton actualización de estado de pago */
        val checkBox = dialog.payStatus_checkbox_opConsumption
        if (this.consumptionBill.toInt() == 0){
            checkBox.isClickable = false /* checkbox inhabilitado si consumo es 0 m3 */
        }else{
            checkBox.isClickable = true
            /** Boton cambio de estado de pago*/
            dialog.payStatus_checkbox_opConsumption.setOnClickListener {
                this.update(checkBox)
            }
        }
        /** botón borrar (sólo si es el último registro de la serie) */
        dialog.del_imageButton_opConsumption.setOnClickListener {

            //fetching last consumption */
            val refLastConsumption = FirebaseFirestore.getInstance()
                .collection("userApr")
                .document(this.uidApr)
                .collection("userCostumer")
                .document(this.medidorNumber.toString())
                .collection("costumerConsumptionPersonal")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)

            refLastConsumption.get()
                .addOnSuccessListener { result ->
                    // verificando el último registro de consumo */
                    if (result.documents.size!=0){
                        val document = result.documents[0].toObject(Consumption::class.java)
                        val lastConsumptionUUID = document?.uuidConsumption
                        Log.d("lastConsumption", "documento obtenido: $document")
                        if (this.uuidConsumption == lastConsumptionUUID){
                            /* en caso que sea el último registro y  se permite borrar*/
                            mAlertDialog.dismiss()
                            val dialogAlertYes         = LayoutInflater.from(context).inflate(R.layout.dialog_update_consumption_allow, null)
                            val mBuilderAlertYes  = AlertDialog.Builder(context)
                                .setView(dialogAlertYes)
                                .setTitle("advertencia")
                                .setNegativeButton("cancel\t|\t",null)
                                .setPositiveButton("quiero borrarlo") { _, _ ->
                                    delete()
                                }
                            dialogAlertYes.message_textView_opConsumptionDelete.text = "va a eliminar  consumo de ${this.consumptionCurrent} m³ del cliente N° ${this.medidorNumber}, tambien eliminará registro de pago asociado"
                            val  mAlertDialogYes = mBuilderAlertYes.show()

                        }else{
                            /* en caso que NO sea el último registro mostrar dialogo de aviso*/
                            mAlertDialog.dismiss()
                            val dialogAlertNo         = LayoutInflater.from(context).inflate(R.layout.dialog_update_consumption_denied, null)
                            val mBuilderAlertNo  = AlertDialog.Builder(context)
                                .setView(dialogAlertNo)
                                .setTitle("no se puede borrar")
                                .setPositiveButton("gracias, entiendo",null)
                            val  mAlertDialogNo = mBuilderAlertNo.show()
                        }
                    }else{
                        Log.d("lastConsumption", "documento obtenido: vacio/nulo ")
                    }
                }
                .addOnFailureListener{e ->
                    Log.d("lastConsumption", "Error getting documents: ", e)
                }
        }/* fin boton borrar */
    }

    //DIALOG create consumption
    fun initDialog(context: Context, costumer: Costumer?, imageUri: Uri?){

        val remainingDays = User().getRemainingDays(user)

            if(remainingDays >= 0){
                //onSuccess start creating consumption
                createDialog(context,costumer,imageUri)

            }else{
                //onFailure: no remaining days
                overlimitDialog(context, remainingDays)
            }

    }

    private fun createDialog(context: Context, costumer: Costumer?, imageUri: Uri?){

        /** para fragments context= requireActivity()
         * para activities context= this */

        //Inflating create Dialog
        val view= LayoutInflater.from(context).inflate(R.layout.dialog_create_consumption, null)
        val builder = AlertDialog.Builder(context)
                .setView(view)
                .setTitle("nueva lectura")
        val  formDialog = builder.show()

        //Shows current date
        val today  = Calendar.getInstance().time
        val format   = SimpleDateFormat("EEE dd 'de' MMM 'de' yyyy 'a las' h:mm aaa")
        val todayString= format.format(today)
        view.currentDate_textView_consumption.text = todayString

        //Load costumer data if it's not NULL
        costumer?.let {
            view.number_autoTextView_consumption.setText(it.medidorNumber.toString())
            view.number_autoTextView_consumption.isFocusable = false
        }

        //Erase image button
        view.photo_button_consumption?.setOnClickListener{
            formDialog.dismiss()
        }

        //Create bitmap and set main imageView
        imageUri?.let{
            /*http://androidtrainningcenter.blogspot.com/2012/05/bitmap-operations-like-re-sizing.html*/
            //Create bitmap from Uri rotate
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver,imageUri)
            val matrix = Matrix()
            matrix.postRotate(90f)  /* Rotate the Bitmap thanks to a rotated matrix. This seems to work */
            val bitmapR        = Bitmap.createBitmap(bitmap,0, 0, bitmap.width, bitmap.height, matrix, true)

            //Compress bitmap and compress Image Uri on memory
            val outputStream = context.contentResolver.openOutputStream(imageUri)
            bitmapR.compress(Bitmap.CompressFormat.JPEG, 10, outputStream)
            Log.d("Picture", "P02 imageUri dirección : $imageUri")
            outputStream?.flush()
            outputStream?.close()

            //set imageView
            view.photo_button_consumption.setImageDrawable(ContextCompat.getDrawable(context,
                R.drawable.ic_delete_black_24dp
            ))
            view.photo_button_consumption.alpha = 0.5f
            view.photo_imageView_consumption.setImageBitmap(bitmapR)
        }

        //Carga de AutoCompleteTextView y generando listado de números de clientes
        val costumerNumberList= costumerList(view,context)

        //SAVE button
        view.save_button_consumption.setOnClickListener {

            //Getting editText values
            val medidorNumber   = view.number_autoTextView_consumption.text.toString()
            val lecturaEntero   = view.logEntero_editText_consumption.text.toString()
            val lecturaDecimal  = if (view.logDecimal_editText_consumption.text.isNotEmpty()){
                view.logDecimal_editText_consumption.text.toString()
            }else{"00"}


            //Chequear datos ingresados
            val result = checkInput(view,context,lecturaEntero,medidorNumber,costumerNumberList,imageUri)

            if (result){
                /*Comenzar proceso de carga a firebase*/
                instance(context, lecturaEntero,lecturaDecimal,medidorNumber,imageUri!!)
                formDialog.dismiss()
            }else{
                return@setOnClickListener //retorna al listener sin cerrar el diálogo
            }
        }

        //CANCEL button
        view.cancel_button_consumption.setOnClickListener {
            formDialog.dismiss()
        }
    }

    private fun overlimitDialog(context: Context, days:Short){
        //Inflate dialog
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_create_consumption_denied, null) /** Instando dialogView */
        val builder = AlertDialog.Builder(context)
                .setView(view)
                .setTitle("fecha límite")
                .setNegativeButton("no gracias\t|", null)
                .setPositiveButton("comprar", null)
        builder.show()/* show dialog */

        //Setting text
        val formatDate        = SimpleDateFormat("EEEE dd 'de' MMMM 'de' yyyy")
        val formatedDate    = "fecha límite: ${formatDate.format(user!!.dateLimitBuy)}"
        view.dateLimit_textView_consumptionAlert.text = formatedDate
        view.daysDeuda_textView_consumptionAlert.text = "${days*-1} días"
    }

    /**Funciones axiliares */

    //generador de listado de medidores y autoCompleteView
    private fun costumerList(view:View, context: Context):List<Short>{
        /* https://tutorialwing.com/android-multiautocompletetextview-using-kotlin-with-example/ */
        /* https://stackoverflow.com/questions/46003242/multiautocompletetextview-not-showing-dropdown-in-alertdialog */

        //List of costumers numbers
        val list:ArrayList<Short> = arrayListOf()
        for (costumer in CostumerFragment.costumerList) {
            list.add(costumer.medidorNumber.toShort())
        }

        //Deploy list inflate
        Log.d("Consumption", "Array Medidores: $list")
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line,list)

        //Autocomplete textView
        val input = view.number_autoTextView_consumption
        input.setAdapter(adapter)
        input.threshold=1 /* minimo de caracteres para comenzar con el autocomplete */
        input.requestFocus()
        return list.toList()
    }

    //chequeo de los valores de entrada diálogo
    private fun checkInput(view: View, context:Context, lecturaEntero:String, medidorNumber:String, costumerNumberList:List<Short>,imageUri: Uri?):Boolean{
        while (lecturaEntero.isBlank()){
            Toast.makeText(context,"ERROR: lectura vacía", Toast.LENGTH_SHORT).show()
            view.logEntero_editText_consumption.error = "vacio"
            return false
        }
        while (medidorNumber.isBlank()){
            Toast.makeText(context,"ERROR: medidor vacío", Toast.LENGTH_SHORT).show()
            view.number_autoTextView_consumption.error = "vacio"
            return false}
        while (!costumerNumberList.contains(medidorNumber.toShort())){
            Toast.makeText(context,"ERROR: no existe medidor", Toast.LENGTH_SHORT).show()
            view.number_autoTextView_consumption.error = "vacio"
            return false
        }
        imageUri?:run{
            return false
        }
        while (lecturaEntero.toInt()>9999){
            Toast.makeText(context,"Revise lectura", Toast.LENGTH_SHORT).show()
            view.logEntero_editText_consumption.error = "número grande"
            return false
        }
        return true
    }

    //Ensamblado de objeto consumo

    //Calc current volumen consumption
    private fun calculateConsumption(logLectureNew:Double, logLectureOld:Double):Double{
        return if (logLectureNew >=logLectureOld){
            val it = ((logLectureNew - logLectureOld) * 100.0).roundToInt() /100.0 /** consumo actual */
            it
        }else{
            val lectureNumberLength = (logLectureOld.roundToLong()).toString().length
            val aux                 = 10.0
            val lectureAuxA = ((aux.pow(lectureNumberLength) - logLectureOld) * 100.0).roundToLong() /100.0
            val it          = ((logLectureNew +lectureAuxA)*100.0).roundToInt() /100.0
            it
        }
    }

}
