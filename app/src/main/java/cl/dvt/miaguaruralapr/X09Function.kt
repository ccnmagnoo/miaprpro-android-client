package cl.dvt.miaguaruralapr

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import cl.dvt.miaguaruralapr.F02CostumerFragment.Companion.costumerList
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.section_add_consumption.view.*
import kotlinx.android.synthetic.main.section_op_consumption.view.*
import kotlinx.android.synthetic.main.section_op_consumption_alert01yes.view.*
import java.io.FileNotFoundException
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class ConsumptionOperation(
    private val consumption: ConsumptionObject
){
    /* agregar consumo */
    internal fun newConsumption(mAlertDialogLoading: AlertDialog, context:Context){
        /** instando base de datos firebase*/
        /** guardar objeto en 2 partes distintas, collecciòn particular del cliente
         * para la app cliente y un main collection para efectos de app administrador*/

        val refSubCollection   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("userCostumer")
            .document(consumption.medidorNumber.toString())
            .collection("costumerConsumptionPersonal")
            .document(consumption.uuidConsumption)

        /* --actualizado a CLOUD FUNCTION-- */
/*        val refMainCollection = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("costumerConsumptionMain")
            .document(consumption.uuidConsumption)*/

        /* colección del cliente consumidor */
        refSubCollection.set(consumption)
            .addOnSuccessListener {
                Log.d("Consumption", "guardado en sub-coleccion del cliente")
                //arrancar actividad de login
                Toast.makeText(context, "Registrado consumo cliente N°${consumption.medidorNumber} : ${consumption.consumptionCurrent} m3", Toast.LENGTH_SHORT ).show()
                mAlertDialogLoading.dismiss()
            }
            .addOnFailureListener {
                Log.d("Consumption", "fallo almacenamiento en Firestore")
                Toast.makeText(context, "Error en guardar cliente ${consumption.medidorNumber} ", Toast.LENGTH_SHORT).show()
            }

        /* copia consumo en el mainCollection --actualizado a CLOUD FUNCTION-- */
/*        refMainCollection.set(consumption)
            .addOnSuccessListener {
                Log.d("Consumption", "guardado en collección principal")
                //arrancar actividad de login
                Toast.makeText(context, "Registrado consumo cliente N°${consumption.medidorNumber} : ${consumption.consumptionCurrent} m3", Toast.LENGTH_SHORT ).show()
                mAlertDialogLoading.dismiss()
            }
            .addOnFailureListener {
                Log.d("Consumption", "Fallo Guardado en Firestore")
                Toast.makeText(context, "Error en guardar cliente ${consumption.medidorNumber} ", Toast.LENGTH_SHORT).show()
            }*/

        /*  adición de deuda del costumer --actualizado a CLOUD FUNCTION--*/
/*        val refCostumerDebt = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("userCostumer")
            .document(consumption.medidorNumber.toString())
        refCostumerDebt.update("userCostumerDebt", FieldValue.increment(consumption.consumptionBill))*/
    }

    /* borrar consumo */
    private fun deleteConsumption(){

        val refSubCollection   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("userCostumer")
            .document(consumption.medidorNumber.toString())
            .collection("costumerConsumptionPersonal")
            .document(consumption.uuidConsumption)

        /* --actualizado a CLOUD FUNCTION-- */
/*        val refMainCollection = FirebaseFirestore.getInstance().collection("userApr").document(consumption.uidApr).collection("costumerConsumptionMain").document(consumption.uuidConsumption)*/
        /* --actualizado a CLOUD FUNCTION-- */
/*        val refCostumer = FirebaseFirestore.getInstance().collection("userApr").document(consumption.uidApr).collection("userCostumer").document(consumption.medidorNumber.toString())*/

        /* borrar consumo en la colección principal */
        refSubCollection.delete().addOnSuccessListener {
            /* borrar consumo en la colección secundaria --actualizado a CLOUD FUNCTION-- */
            //refMainCollection.delete()
            /* reducir monto deuda de cliente: --actualizado a CLOUD FUNCTION-- */
/*             if (!consumption.paymentStatus){
               *//* descuenta sólo si el estado de pago es falso  = no pago *//*
                refCostumer.update("userCostumerDebt", FieldValue.increment((-1)*consumption.consumptionBill))
            }*/
            /* borrar imagen del consumo */
            val refPic = FirebaseStorage.getInstance().reference.child(consumption.consumptionPicUrl)
            refPic.delete()
                .addOnSuccessListener {
                    Log.d("Delete Pic", "borrada la foto")
                }
                .addOnFailureListener{
                    Log.d("Delete Pic", "error al borrar")
                }
        }

    }

    /* actualizar consumo en el recyclerView desde el cuadro de dialogo*/
    internal fun updateConsumptionPayment(payCheckBoxStatus:CheckBox){
        val refSubCollection   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("userCostumer")
            .document(consumption.medidorNumber.toString())
            .collection("costumerConsumptionPersonal")
            .document(consumption.uuidConsumption)

        /* actualizado a --cloud function-- */
        /*
        val refMainCollection = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("costumerConsumptionMain")
            .document(consumption.uuidConsumption)
            */

        /* botón actualización de estado de pago */
        if (payCheckBoxStatus.isChecked){
            MainActivity.block_key = true /* se bloquea el permitir cerrar la actividad*/
            val newPaymentStatus = hashMapOf(
                "paymentStatus" to true,
                "paymentDate" to Calendar.getInstance().time
            )

            /* actualizando status de pago TRUE*/
            refSubCollection.set(newPaymentStatus, SetOptions.merge() )
                .addOnSuccessListener {
                    /* --actualizado a CLOUD FUNCTION-- */
                    //refMainCollection.set(newPaymentStatus, SetOptions.merge() )
                    /* actualizando fecha de pago */
                    payCheckBoxStatus.text = "pagado"
                    /* reduciendo monto de deuda cliente --actualizado a CLOUD FUNCTION-- */
                    //refCostumer.update("userCostumerDebt", FieldValue.increment((-1)*consumption.consumptionBill.toDouble()))
                    //refCostumer.set (hashMapOf("userCostumerLastPayDate" to Calendar.getInstance().time), SetOptions.merge())
                    MainActivity.block_key = false
                }.addOnFailureListener {
                    payCheckBoxStatus.isChecked = false
                    payCheckBoxStatus.text = "sin red"
                    MainActivity.block_key = true
                }
        } else{
            val newPaymentStatus = hashMapOf(
                "paymentStatus" to false,
                "paymentDate" to Calendar.getInstance().time
            )
            /* actualizando status de pago FALSO */
            refSubCollection.set(newPaymentStatus, SetOptions.merge() )
                .addOnSuccessListener {
                    /* --actualizado a CLOUD FUNCTION-- */
                    //refMainCollection.set(newPaymentStatus, SetOptions.merge() )
                    payCheckBoxStatus.text = "deuda"
                    /* aumentando deuda de cliente costumer --actualizado a CLOUD FUNCTION-- */
                    //refCostumer.update("userCostumerDebt", FieldValue.increment(consumption.consumptionBill.toDouble()))
                }.addOnFailureListener {
                    payCheckBoxStatus.isChecked = true
                    payCheckBoxStatus.text = "sin red"
                }
        }
        Handler().postDelayed({
            payCheckBoxStatus.text = ""
        }, 2000)
    }

    /* cuadro de dialogo de consumo individual*/
    fun editConsumptionDialog(context:Context){

        /*Abiendo dialogo*/
        val mDialogView = LayoutInflater.from(context).inflate(R.layout.section_op_consumption, null) /** Instando dialogView */
        val mBuilder = AlertDialog.Builder(context) /** Inflado del cuadro de dialogo */
                .setView(mDialogView)
        val  mAlertDialog = mBuilder.show()/* show dialog */

        /*Populando datos del cuadro de dialogo */
        val formatCurrency      = DecimalFormat("$ #,###")
        val formatDateLong      = SimpleDateFormat("EEEE dd MMMM yyyy")
        val formatDateShort     = SimpleDateFormat("dd/MM/yyyy")
        mDialogView.medidorNumber_textView_opConsumption.text   = consumption.medidorNumber.toString()
        mDialogView.date_textView_opConsumption.text            = formatDateLong.format(consumption.paymentDate).toString()
        mDialogView.dateLectureNew_textView_opConsumption.text  = formatDateShort.format(consumption.dateLectureNew).toString()
        mDialogView.dateLectureOld_textView_opConsumption.text  = formatDateShort.format(consumption.dateLectureOld).toString()
        mDialogView.logNew_textView_opConsumption.text          = consumption.logLectureNew.toString()
        mDialogView.logOld_textView_opConsumption.text          = consumption.logLectureOld.toString()
        mDialogView.consumption_textView_opConsumption.text     = consumption.consumptionCurrent.toString()
        mDialogView.importe_textView_opConsumption.text         = formatCurrency.format(consumption.consumptionBill.toInt())
        mDialogView.payStatus_checkbox_opConsumption.isChecked  = consumption.paymentStatus
        mDialogView.payStatus_checkbox_opConsumption.text       = if (consumption.paymentStatus){"pagado"}else{"pagar"}

        /* Cargando imagen de captura  */
        Picasso.get().load(consumption.consumptionPicUrl).into(mDialogView.image_imageView_opConsumption,object: com.squareup.picasso.Callback{
            override fun onSuccess() {
                mDialogView.loading_progressBar_opConsumption.visibility = View.GONE
            }
            override fun onError(e: Exception?) {
                /*cuando la imagen no existe en firestores*/
                mDialogView.loading_progressBar_opConsumption.visibility = View.GONE
                mDialogView.noPic_imageView_opConsumption.visibility = View.VISIBLE
                mDialogView.image_imageView_opConsumption.background = ContextCompat.getDrawable(context, R.drawable.app_draw_background)
                //mDialogView.image_imageView_opConsumption.alpha = 0.5f
            }
        })

        /* Poblando mini recyclerView de detalle del cobro */
        val adapter = GroupAdapter<GroupieViewHolder>()     /* adaptador para el recyclerView */
        mDialogView.consumptionDetail_recyclerView_opConsumption.adapter = adapter
        for ((index, billingData) in consumption.consumptionBillDetail.withIndex()){
            if (index > 0 ){
                adapter.add(ConsumptionDetailAdapter(billingData))
            }
        }

        /* Bloqueando checkbox si billing es 0 */
        /** boton actualización de estado de pago */
        if (consumption.consumptionBill.toInt() == 0){
            mDialogView.payStatus_checkbox_opConsumption.isClickable = false /* checkbox inhabilitado si consumo es 0 m3 */
        }else{
            mDialogView.payStatus_checkbox_opConsumption.isClickable = true
            /** Boton cambio de estado de pago*/
            mDialogView.payStatus_checkbox_opConsumption.setOnClickListener {
                ConsumptionOperation(consumption).updateConsumptionPayment(mDialogView.payStatus_checkbox_opConsumption)
            }
        }

        /** botón borrar (sólo si es el último registro de la serie) */
        mDialogView.del_imageButton_opConsumption.setOnClickListener {

            /*último consumo del cliente */
            val refLastConsumption = FirebaseFirestore.getInstance()
                .collection("userApr")
                .document(consumption.uidApr)
                .collection("userCostumer")
                .document(consumption.medidorNumber.toString())
                .collection("costumerConsumptionPersonal")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)

            refLastConsumption.get()
                .addOnSuccessListener { result ->
                    /* verificando el último registro de consumo */
                    if (result.documents.size!=0){
                        val document = result.documents[0].toObject(ConsumptionObject::class.java)
                        val lastConsumptionUUID = document?.uuidConsumption
                        Log.d("lastConsumption", "documento obtenido: $document")
                        if (consumption.uuidConsumption == lastConsumptionUUID){
                            /* en caso que sea el último registro y  se permite borrar*/
                            mAlertDialog.dismiss()
                            val mDialogAlertYesView         = LayoutInflater.from(context).inflate(R.layout.section_op_consumption_alert01yes, null)
                            val mBuilderAlertYes  = AlertDialog.Builder(context)
                                .setView(mDialogAlertYesView)
                                .setTitle("advertencia")
                                .setNegativeButton("no      |    ",null)
                                .setPositiveButton("quiero borrarlo") { dialog, id ->
                                    ConsumptionOperation(consumption).deleteConsumption()
                                }
                            mDialogAlertYesView.message_textView_opConsumptionDelete.text = "va a eliminar  consumo de ${consumption.consumptionCurrent} m³ del cliente N° ${consumption.medidorNumber}, tambien eliminará registro de pago asociado"
                            val  mAlertDialogYes = mBuilderAlertYes.show()

                        }else{
                            /* en caso que NO sea el último registro mostrar dialogo de aviso*/
                            mAlertDialog.dismiss()
                            val mDialogAlertNoView         = LayoutInflater.from(context).inflate(R.layout.section_op_consumption_alert02no, null)
                            val mBuilderAlertNo  = AlertDialog.Builder(context)
                                .setView(mDialogAlertNoView)
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


    var bitmap:Bitmap?= null
    var imageUri:Uri?= null

    /*cuadro de diálogo para ingresar un nuevo consumo*/
    fun newConsumptionDialog(context:Context){

        /* Ingreso de nuevo consumo */

        //Cargando cuadro de diálogo de carga
        val mDialogView         = LayoutInflater.from(context).inflate(R.layout.section_add_consumption, null) /** Instando dialogView */
        val mBuilder = AlertDialog.Builder(context) /** Inflado del cuadro de dialogo */
                .setView(mDialogView)
                .setTitle("nueva lectura")
        val  mAlertDialog = mBuilder.show()/** show dialog */

        //Setting data de fecha y timestamp
        val currentDate  = Calendar.getInstance().time
        val formaterDate       = SimpleDateFormat("EEE dd 'de' MMM 'de' yyyy 'a las' h:mm aaa")
        val formatedDate= formaterDate.format(currentDate)
        mDialogView.currentDate_textView_consumption.text = formatedDate

        //Carga de AutoCompleteTextView*/
        autocompleteMedidorList(mDialogView,context)

        //Acciones del Photo button*/
        mDialogView.photo_button_consumption?.setOnClickListener{
            mAlertDialog.dismiss()

            if (MainActivity.requestCameraResult || MainActivity.camPermissionBoolean){
                imageUri = openCamera(mDialogView)

            }else{
                Toast.makeText(context, "cámara denegada", Toast.LENGTH_SHORT).show()
            }
        }
        if(bitmap != null){
            val matrix = Matrix()
            matrix.postRotate(270f)  /* Rotate the Bitmap thanks to a rotated matrix. This seems to work */
            val bitmapRotated = Bitmap.createBitmap(bitmap!!,0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
            //mDialogView.photo_button_consumption.setImageDrawable(resources(R.drawable.ic_ico_refresh))
            mDialogView.photo_button_consumption.alpha = 0.5f
            mDialogView.photo_imageView_consumption.setImageBitmap(bitmapRotated)
        }

        //Acciones del SAVE button*/
        mDialogView.save_button_consumption.setOnClickListener {
            val medidorNumber   = mDialogView.number_autoTextView_consumption.text.toString()
            val timeStamp        = System.currentTimeMillis()/1000
            val lecturaEntero   = mDialogView.logEntero_editText_consumption.text.toString()
            val lecturaDecimal  = if (mDialogView.logDecimal_editText_consumption.text.isNotEmpty()){
                mDialogView.logDecimal_editText_consumption.text.toString()
            }else{"00"} /* si la parte decimal no se llena, queda se setea en 0  */


            /*Chequear datos ingresados*/
            val checkInputDataVal = checkInput(context,mDialogView,lecturaEntero,lecturaDecimal,medidorNumber)
            if (checkInputDataVal){
                /*Comenzar proceso de carga a firebase*/
                assemblyConsumptionObject(context, lecturaEntero,lecturaDecimal,medidorNumber,currentDate,timeStamp)
                mAlertDialog.dismiss()
            }else{
                return@setOnClickListener
            }
            bitmap = null /** borrar el bitmap */
        }
        /**Acciones del CANCEL button*/
        mDialogView.cancel_button_consumption.setOnClickListener {
            mAlertDialog.dismiss()
            bitmap = null
        }
    }
    private var costumerNumberList:ArrayList<Short> = arrayListOf()
    private fun autocompleteMedidorList(mDialogView:View,context:Context){
        //val costumerNumberList:ArrayList<Short> = arrayListOf()
        for (element in costumerList){ costumerNumberList.add(element.medidorNumber.toShort()) } /** Cargando Array List */
        Log.d("Consumption", "Array Medidores: $costumerNumberList")
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line,costumerNumberList)
        mDialogView.number_autoTextView_consumption.setAdapter(adapter)
        mDialogView.number_autoTextView_consumption.threshold=1 /* minimo de caracteres para comenzar con el autocomplete */
        //DialogView.number_multiAutoCompleteTextView_consumption.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer()) //
        /* https://tutorialwing.com/android-multiautocompletetextview-using-kotlin-with-example/ */

        //mDialogView.number_multiAutoCompleteTextView_consumption.showDropDown()
        mDialogView.number_autoTextView_consumption.requestFocus()
        /* https://stackoverflow.com/questions/46003242/multiautocompletetextview-not-showing-dropdown-in-alertdialog */
    }
    //chequeo de los valores de entrada diálogo
    private fun checkInput(context:Context,mDialogView: View, lecturaEntero:String, lecturaDecimal:String, medidorNumber:String):Boolean{
        while (lecturaEntero.isEmpty()){
            Toast.makeText(context,"ERROR: lectura vacía", Toast.LENGTH_SHORT).show()
            mDialogView.logEntero_editText_consumption.error = "vacio"
            return false
        }
        while (lecturaEntero.toInt()>99999){
            Toast.makeText(context,"Revise lectura", Toast.LENGTH_SHORT).show()
            mDialogView.logEntero_editText_consumption.error = "número grande"
            return false
        }
        while (lecturaDecimal.isEmpty()){
            Toast.makeText(context,"ERROR: decimal vacio", Toast.LENGTH_SHORT).show()
            mDialogView.logDecimal_editText_consumption.error = "vacio"
            return false
        }
        while (medidorNumber.isEmpty()){
            Toast.makeText(context,"ERROR: medidor vacío", Toast.LENGTH_SHORT).show()
            mDialogView.number_autoTextView_consumption.error = "vacio"
            return false}
        while (bitmap == null){
            Toast.makeText(context,"sin imagen respaldo", Toast.LENGTH_SHORT).show()
            mDialogView.photo_imageView_consumption.setColorFilter(Color.RED)
            return false}
        while (!costumerNumberList.contains(medidorNumber.toShort())){
            Toast.makeText(context,"ERROR: no existe medidor", Toast.LENGTH_SHORT).show()
            mDialogView.number_autoTextView_consumption.error = "vacio"
            return false
        }

        return true
    }
    //
    private fun assemblyConsumptionObject(context:Context,lecturaEntero:String, lecturaDecimal:String, medidorNumber:String, currentDate:Date, timeStamp:Long){
        //F01.02. Cargando cuadro de diálogo de carga*/
        val mDialogLoadingView = LayoutInflater.from(context).inflate(R.layout.section_add_consumption_loading, null) /* Instando dialogo "cargando" */
        val mBuilderLoading = AlertDialog.Builder(context).setView(mDialogLoadingView)   /* Inflado del cuadro de dialogo "cargando" */
        val mAlertDialogLoading = mBuilderLoading.show() /* show dialog "cargando" */

        //F02.B.0 Instando firebase*/
        val uidApr          = A01SplashActivity.currentApr!!.uidApr
        val uuidConsumption = UUID.randomUUID().toString()/** identificador único de consumo*/
        /*F02.B.1 Ensamblando variables actuales y fetch de última lectura*/
        val logLectureNew = ("$lecturaEntero.$lecturaDecimal").toDouble()

        // fetching última lectura pasada del cliente */
        var logLectureOld = logLectureNew
        var dateLectureOld = currentDate

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
                    val document = result.documents[0].toObject(ConsumptionObject::class.java)
                    Log.d("lastConsumption", "documento obtenido: $document")
                    logLectureOld = document!!.logLectureNew
                    dateLectureOld = document.dateLectureNew

                }else{
                    Log.d("lastConsumption", "documento obtenido: vacio/nulo ")
                }
                /* cualcula el consumo en caso de medidor retorna a 0 */
                val consumptionCurrent= calculateConsumptionCurrent(logLectureNew,logLectureOld)

                /* Seteando el precio del m3 de agua desde list */
                //val wasterPricePlanListFix = tramoPriceList.sortedBy{ tramo -> tramo.consumptionBase}.reversed()
                /* traspasados a --cloud function-- */
                //val currentBill = calculateCurrentBill(wasterPricePlanListFix,consumptionCurrent) /** total a pagar */
                //val currentBillDetail = calculateCurrentBillDetail(wasterPricePlanListFix,consumptionCurrent).toList() /** detalle del cobro por tramo  */
                //val currentBilltotal = currentBillDetail[currentBillDetail.lastIndex]["total"]

                /** status de pago TRUE si consumo es 0.0 */
                val paymentStatus = consumptionCurrent == 0.0

                /**F02.B.1 Cargando IMAGEN CONSUMO a base de datos*/
                /* nombrar archivo de imagen*/
                val formaterDateYM          = SimpleDateFormat("yyyy.MM")
                val formatedDateYM   = formaterDateYM.format(currentDate)
                val formaterDateYMD         = SimpleDateFormat("yyyyMMdd")
                val formatedDateYMD  = formaterDateYMD.format(currentDate)
                val uidAprLt = uidApr.substring(startIndex = 0, endIndex = 5) /* id corto */
                val filename = "$uidAprLt.$formatedDateYMD.$medidorNumber " /* genera un nombre largo*/
                val refPic = FirebaseStorage.getInstance().reference.child("/lectureBackupPic/$formatedDateYM/$uidAprLt/$filename")
                /* subiendo imagen y obteniendo url */

                refPic.putFile(imageUri!!)
                    .addOnSuccessListener{ it ->
                        Log.d("Consumption","Se subió la foto: ${it.metadata?.path}")
                        refPic.downloadUrl.addOnSuccessListener {
                            val consumptionPicUrl= it.toString()
                            Log.d("Consumption","Ubicación foto: $it")
                            val consumption = ConsumptionObject(
                                timeStamp,
                                uidApr,
                                uuidConsumption,
                                medidorNumber.toInt(),
                                currentDate,
                                dateLectureOld,
                                logLectureNew,
                                logLectureOld,
                                consumptionCurrent,
                                listOf(mapOf("0" to 0.0)),  /* cálculo en cloud function */
                                0.0,        /* cálculo en cloud function*/
                                consumptionPicUrl,
                                paymentStatus
                            )

                            ConsumptionOperation(consumption).newConsumption(mAlertDialogLoading,context)

                        }
                    }

            }
            .addOnFailureListener{e ->
                Log.d("lastConsumption", "Error getting documents: ", e)
            }

        /** fetching desde consumos desde mutableListOf */
        /* RES: https://cursokotlin.com/capitulo-10-listas-en-kotlin/ */
        /* RES : https://stackoverflow.com/questions/46868903/sort-data-from-a-mutablelist-in-kotlin */

        /*
        val consumptionLastData = consumptionList
            .filter { it.medidorNumber == medidorNumber.toInt()}
            .sortedBy { consumption -> consumption.timestamp }
            .last()

        val logLectureOld = if (consumptionLastData.uuidConsumption.isEmpty()){
            logLectureNew
        } else{
            consumptionLastData.logLectureNew
            }
        val dateLectureOld = if(consumptionLastData.uuidConsumption.isEmpty()){
            currentDate
        } else{
            consumptionLastData.dateLectureNew
            }

        loop@ for (document in wasterPricePlanListFix){
            if(consumptionCurrent >= document.consumptionBase){
                currentPriceBase = document.priceBase
                Log.d("Consumption", "Precio unitario del m3 de agua: $currentPriceBase")
                break@loop
            }
        }
        */


    }// fin assembly

    private fun calculateConsumptionCurrent(logLectureNew:Double,logLectureOld:Double):Double{
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

    private fun openCamera(mDialogView:View): Uri? {
        /*http://androidtrainningcenter.blogspot.com/2012/05/bitmap-operations-like-re-sizing.html*/

        val imageUri = mDialogView.context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,ContentValues())
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)  /* captura de foto */
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)    /* instando imagen */
        Log.d("Picture", "P01 imagen guardada como : ${MediaStore.EXTRA_OUTPUT}")

        MainActivity().startActivityForResult(cameraIntent, 1001)
        return imageUri
    }


}