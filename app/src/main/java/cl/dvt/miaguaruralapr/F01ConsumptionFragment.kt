package cl.dvt.miaguaruralapr
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import cl.dvt.miaguaruralapr.F02CostumerFragment.Companion.costumerList
import cl.dvt.miaguaruralapr.MainActivity.Companion.camPermissionBoolean
import cl.dvt.miaguaruralapr.MainActivity.Companion.remainingDays
import cl.dvt.miaguaruralapr.MainActivity.Companion.requestCameraResult
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_f01_consumo.*
import kotlinx.android.synthetic.main.section_add_consumption.view.*
import kotlinx.android.synthetic.main.section_add_consumption_alert.view.*
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class F01ConsumptionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_f01_consumo, container, false)
    }


    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // buscar los consumos
        fetchConsumption()

        // mostrar en pantalla días remanentes
        remainingDays_textView_consumption.text = remainingDays.toString() /* set textView de dias remanentes */
        remainingDays_textView_consumption.bringToFront()

        //agregar consumo
        addConsumption_floatingActionButton_consumo.setOnClickListener{
            conditionalDialog()
        }

        //switch filtro mostrar all/sin_pagos
        debtFilter_switch_costumer.setOnClickListener {
            fetchConsumption()
            Log.d("Filter", "current switch: ${debtFilter_switch_costumer.isChecked}")
            if (debtFilter_switch_costumer.isChecked){
                debtFilter_switch_costumer.text = "mostrar todo"
            }else{
                debtFilter_switch_costumer.text = "sólo deudas"
            }
        }

    }/* onViewCreated */

    private fun conditionalDialog(){
        if(remainingDays>=0){
            //diálogo ingreso de consumo
            newConsumptionDialog()
        }else{
            //diálogo de tiempo expirado: compra más tokens
            expiredTimeDialog()
        }
    }

    /* ingreso nuevo consumo */
    @SuppressLint("SimpleDateFormat")
    fun newConsumptionDialog(){
        /* Ingreso de nuevo consumo */
        //Cargando cuadro de diálogo de carga
        val mDialogView         = LayoutInflater.from(requireActivity()).inflate(R.layout.section_add_consumption, null) /** Instando dialogView */
        val mBuilder = AlertDialog.Builder(requireActivity()) /** Inflado del cuadro de dialogo */
            .setView(mDialogView)
            .setTitle("nueva lectura")
        val  mAlertDialog = mBuilder.show()/** show dialog */

        //Setting data de fecha y timestamp
        val currentDate  = Calendar.getInstance().time
        val formaterDate       = SimpleDateFormat("EEE dd 'de' MMM 'de' yyyy 'a las' h:mm aaa")
        val formatedDate= formaterDate.format(currentDate)
        mDialogView.currentDate_textView_consumption.text = formatedDate

        //CAMERA button
        mDialogView.photo_button_consumption?.setOnClickListener{
            mAlertDialog.dismiss()

            if (requestCameraResult || camPermissionBoolean){
                openCamera(mDialogView) //TODO: averiguar como lanzar cámara desde classe externa sin OVERRIDE
            }else{
                Toast.makeText(requireContext(), "cámara denegada", Toast.LENGTH_SHORT).show()
            }
        }

        //si el bitmap es no es nulo: set imageView
        if(bitmap != null){
            val matrix = Matrix()
            matrix.postRotate(270f)  /* Rotate the Bitmap thanks to a rotated matrix. This seems to work */
            val bitmapRotated = Bitmap.createBitmap(bitmap!!,0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
            mDialogView.photo_button_consumption.setImageDrawable(resources.getDrawable(R.drawable.ic_ico_refresh))
            mDialogView.photo_button_consumption.alpha = 0.5f
            mDialogView.photo_imageView_consumption.setImageBitmap(bitmapRotated)
        }

        //Carga de AutoCompleteTextView y generando listado de números de clientes
        val costumerNumberList= autocompleteMedidorList(mDialogView)

        //SAVE button
        mDialogView.save_button_consumption.setOnClickListener {
            val medidorNumber   = mDialogView.number_autoTextView_consumption.text.toString()
            val timeStamp        = System.currentTimeMillis()/1000
            val lecturaEntero   = mDialogView.logEntero_editText_consumption.text.toString()
            val lecturaDecimal  = if (mDialogView.logDecimal_editText_consumption.text.isNotEmpty()){
                mDialogView.logDecimal_editText_consumption.text.toString()
            }else{"00"} /* si la parte decimal no se llena, queda se setea en 0  */


            /*Chequear datos ingresados*/
            val checkInputDataVal = checkInput(mDialogView,lecturaEntero,lecturaDecimal,medidorNumber,costumerNumberList)
            if (checkInputDataVal){
                /*Comenzar proceso de carga a firebase*/
                assemblyConsumptionObject(lecturaEntero,lecturaDecimal,medidorNumber,currentDate,timeStamp)
                mAlertDialog.dismiss()
            }else{
                return@setOnClickListener
            }
            bitmap = null /** borrar el bitmap */
        }
        //CANCEL button
        mDialogView.cancel_button_consumption.setOnClickListener {
            mAlertDialog.dismiss()
            bitmap = null
        }
    }

    // tiempo expirado
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun expiredTimeDialog(){
        /* dialogo por tiempo expirado */
        val mDialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.section_add_consumption_alert, null) /** Instando dialogView */
        val mBuilder = AlertDialog.Builder(requireActivity()) /** Inflado del cuadro de dialogo */
                .setView(mDialogView)
                .setTitle("fecha límite")
                .setNegativeButton("no gracias\t|", null)
                .setPositiveButton("comprar", null)
        mBuilder.show()/* show dialog */

        //F01.04 Setting date limit y current debt
        val formatDate        = SimpleDateFormat("EEEE dd 'de' MMMM 'de' yyyy")
        val formatedDate    = "fecha límite: ${formatDate.format(currentApr!!.dateLimitBuy)}"
        mDialogView.dateLimit_textView_consumptionAlert.text = formatedDate
        mDialogView.daysDeuda_textView_consumptionAlert.text = "${remainingDays*-1} días"
    }

    //revisión de campos ingresados
    private fun checkInput(mDialogView: View, lecturaEntero:String, lecturaDecimal:String, medidorNumber:String,costumerNumberList:List<Short> ):Boolean{
        while (lecturaEntero.isEmpty()){
            Toast.makeText(this.requireContext(),"ERROR: lectura vacía", Toast.LENGTH_SHORT).show()
            mDialogView.logEntero_editText_consumption.error = "vacio"
            return false
        }
        while (lecturaEntero.toInt()>99999){
            Toast.makeText(this.requireContext(),"Revise lectura", Toast.LENGTH_SHORT).show()
            mDialogView.logEntero_editText_consumption.error = "número grande"
            return false
        }
        while (lecturaDecimal.isEmpty()){
            Toast.makeText(this.requireContext(),"ERROR: decimal vacio", Toast.LENGTH_SHORT).show()
            mDialogView.logDecimal_editText_consumption.error = "vacio"
            return false
        }
        while (medidorNumber.isEmpty()){
            Toast.makeText(this.requireContext(),"ERROR: medidor vacío", Toast.LENGTH_SHORT).show()
            mDialogView.number_autoTextView_consumption.error = "vacio"
            return false}
        while (bitmap == null){
            Toast.makeText(this.requireContext(),"sin imagen respaldo", Toast.LENGTH_SHORT).show()
            mDialogView.photo_imageView_consumption.setColorFilter(Color.RED)
            return false}
      while (!costumerNumberList.contains(medidorNumber.toShort())){
          Toast.makeText(this.requireContext(),"ERROR: no existe medidor", Toast.LENGTH_SHORT).show()
          mDialogView.number_autoTextView_consumption.error = "vacio"
          return false
      }
        return true
    }

    // ensamblando getters para crear un objeto consumption
    @SuppressLint("SimpleDateFormat", "InflateParams")
    private fun assemblyConsumptionObject(lecturaEntero:String, lecturaDecimal:String, medidorNumber:String, currentDate:Date, timeStamp:Long){
        /*F01.02. Cargando cuadro de diálogo de carga*/
        val mDialogLoadingView = LayoutInflater.from(requireActivity()).inflate(R.layout.section_add_consumption_loading, null) /* Instando dialogo "cargando" */
        val mBuilderLoading = AlertDialog.Builder(requireActivity()).setView(mDialogLoadingView)   /* Inflado del cuadro de dialogo "cargando" */
        val mAlertDialogLoading = mBuilderLoading.show() /* show dialog "cargando" */

        /*F02.B.0 Instando firebase*/
        val uidApr          = currentApr!!.uidApr
        val uuidConsumption = UUID.randomUUID().toString()/** identificador único de consumo*/
        /*F02.B.1 Ensamblando variables actuales y fetch de última lectura*/
        val logLectureNew = ("$lecturaEntero.$lecturaDecimal").toDouble()

        /* fetching última lectura pasada del cliente */
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

                // status de pago TRUE si consumo es 0.0
                val paymentStatus = consumptionCurrent == 0.0

                //F02.B.1 Cargando IMAGEN CONSUMO a base de datos
                /* nombrar archivo de imagen*/
                val formaterDateYM          = SimpleDateFormat("yyyy.MM")
                val formatedDateYM   = formaterDateYM.format(currentDate)
                val formaterDateYMD         = SimpleDateFormat("yyyyMMdd")
                val formatedDateYMD  = formaterDateYMD.format(currentDate)
                val uidAprLt = uidApr.substring(startIndex = 0, endIndex = 5) /* id corto */
                val filename = "$uidAprLt.$formatedDateYMD.$medidorNumber " /* genera un nombre largo*/
                val refPic = FirebaseStorage.getInstance().reference.child("/lectureBackupPic/$formatedDateYM/$uidAprLt/$filename")
                //subiendo imagen y obteniendo url
                refPic.putFile(imageUri!!)
                    .addOnSuccessListener{ it ->
                        Log.d("Consumption","Se subió la foto: ${it.metadata?.path}")
                        refPic.downloadUrl.addOnSuccessListener {
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
                                listOf(mapOf("0" to 0.0)),  /* cálculo en --cloud function-- */
                                0.0,         /* cálculo en --cloud function--*/
                                it.toString(),              /*pic Url firestore*/
                                paymentStatus
                            )

                            ConsumptionOperation(consumption).newConsumption(mAlertDialogLoading,this.requireContext())

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

    //F03 Cargar items al AutocompleteTextView
    private fun autocompleteMedidorList(mDialogView:View):List<Short>{
        val costumerNumberList:ArrayList<Short> = arrayListOf()
        for (element in costumerList){ costumerNumberList.add(element.medidorNumber.toShort()) } /** Cargando Array List */
        Log.d("Consumption", "Array Medidores: $costumerNumberList")
        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_dropdown_item_1line,costumerNumberList)
        mDialogView.number_autoTextView_consumption.setAdapter(adapter)
        mDialogView.number_autoTextView_consumption.threshold=1 /* minimo de caracteres para comenzar con el autocomplete */
        //DialogView.number_multiAutoCompleteTextView_consumption.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer()) //
        /* https://tutorialwing.com/android-multiautocompletetextview-using-kotlin-with-example/ */

        //mDialogView.number_multiAutoCompleteTextView_consumption.showDropDown()
        mDialogView.number_autoTextView_consumption.requestFocus()
        /* https://stackoverflow.com/questions/46003242/multiautocompletetextview-not-showing-dropdown-in-alertdialog */
        return costumerNumberList.toList()
    }

    //F04 Módulo de CÁMARA
    private var imageUri: Uri? = null /* el para subir a firestore requiere un URI */
    private var bitmap:Bitmap? = null /* los imageView requiere bitmap */
    private val captureCODE = 1001
    private fun openCamera(mDialogView:View) {
        /*http://androidtrainningcenter.blogspot.com/2012/05/bitmap-operations-like-re-sizing.html*/

        imageUri = mDialogView.context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,ContentValues())
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)  /* captura de foto */
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)    /* instando imagen */
            Log.d("Picture", "P01 imagen guardada como : ${MediaStore.EXTRA_OUTPUT}")

        startActivityForResult(cameraIntent, captureCODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* called when image was captured from camera intent */
        if (resultCode == Activity.RESULT_OK){
            try{
                bitmap = MediaStore.Images.Media.getBitmap(this.requireContext().contentResolver,imageUri)
                val matrix = Matrix()
                matrix.postRotate(90f)  /* Rotate the Bitmap thanks to a rotated matrix. This seems to work */
                val bitmapOnFix        = Bitmap.createBitmap(bitmap!!,0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
                val outputStream = this.requireContext().contentResolver.openOutputStream(imageUri!!)
                bitmapOnFix.compress(Bitmap.CompressFormat.JPEG, 5, outputStream)
                Log.d("Picture", "P02 imageUri dirección : ${imageUri.toString()}")
                outputStream?.flush()
                outputStream?.close()
            }catch( e1: FileNotFoundException){ e1.printStackTrace()}
            catch( e2: IOException){ e2.printStackTrace()}
            newConsumptionDialog()
        }
    }


    //F05 descargar consumos de todos los clientes últimos 2 meses : IMPORTANTE
    var numberOfLectures:Int = 0
    var fetchLimit:Long = 50 /* límite de documentos a cargar en recyclerView*/
    private fun fetchConsumption(){
        val uidApr          = currentApr!!.uidApr

        /*Groupie RecyclerView : https://github.com/lisawray/groupie */
        val adapter = GroupAdapter<GroupieViewHolder>()    /* adaptador para el recyclerView */
        consumption_recyclerView_consumo.adapter = adapter /* Cargando el ReclyclerView de esta Actividad*/

        val ref   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(uidApr)
            .collection("costumerConsumptionMain")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(fetchLimit)

        ref
            .addSnapshotListener(MetadataChanges.INCLUDE) { result, e ->
                if (e != null) {
                    Log.d("Consumption", "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (document in result?.documentChanges!!) {
                    when (document.type){
                        DocumentChange.Type.ADDED ->{
                            Log.d("Consumption", "Indice del consumo : ${document.newIndex}")
                            val consumption = document.document.toObject(ConsumptionObject::class.java)
                            adapter.add(document.newIndex,ConsumptionItemAdapter(consumption,debtFilter_switch_costumer.isChecked)) /* IMPORTANTE : cargando datos a los items del adaptador personalizado */
                            adapter.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */
                            Log.d("Consumption", "Indice del modificado : ${document.oldIndex}")
                            val consumption = document.document.toObject(ConsumptionObject::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,ConsumptionItemAdapter(consumption,debtFilter_switch_costumer.isChecked))
                            adapter.notifyItemChanged(document.oldIndex)     /*actualizando datos a los items del adaptador personalizado*/
                            Log.d("Consumption", "id del documento modificado: ${document.document.id}")
                        }
                        DocumentChange.Type.REMOVED     ->{
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.notifyItemRemoved(document.oldIndex)
                            Log.d("Consumption", "id del consumo borrado: ${document.document.id}")
                        }
                    }
                }


                numberOfLectures    = adapter.itemCount
                numberOfConsumption_textView_consumo.text = "últimas $numberOfLectures lecturas"
            }



        ref.firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()/* almacenamiento de datos sin conexión*/

        /** click en el item del recyclerView */
        adapter.setOnItemClickListener {item, view ->
            val consumption = item as ConsumptionItemAdapter
            ConsumptionOperation(consumption.consumption).editConsumptionDialog(requireActivity())
        }


    }/* fin del fetchConsumption() */

    //F07: Descargado listado de precios del agua
    //private var tramoPriceList = mutableListOf<TramoObject>()
    /*
    private fun fetchTramoList(){
        /* fetching precios del metro cúbico de agua */
        val refPricesWater = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(currentApr!!.uidApr)
            .collection("userPrices")
            .orderBy("consumptionBase", Query.Direction.DESCENDING)

        refPricesWater
            .get()
            .addOnSuccessListener{result ->
                tramoPriceList.clear()
                for(document in result?.documentChanges!!){
                    when (document.type){
                        DocumentChange.Type.ADDED ->{
                            val tramo = document.document.toObject(TramoObject::class.java)
                            tramoPriceList.add(document.newIndex,tramo)
                            Log.d("Tramo", "added INDEX:${document.newIndex} ID: ${document.document.id}")
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */
                            val tramo = document.document.toObject(TramoObject::class.java)
                            tramoPriceList[document.oldIndex] = tramo
                            Log.d("Tramo", "edited INDEX:${document.oldIndex} ID: ${document.document.id}")
                        }
                        DocumentChange.Type.REMOVED     ->{
                            tramoPriceList.removeAt(document.oldIndex)
                            Log.d("Tramo", "deleted INDEX:${document.oldIndex} ID: ${document.document.id}")
                        }
                    }

                }
                Log.d("Tramo", "RESULT  List SIZE :${tramoPriceList.size}")

            }
            .addOnFailureListener { exception -> Log.d("Consumption", "error obteniendo documentos: ", exception)}
    }

     */

    /* Calcular cobros por tramo --actualizado a CLOUD FUNCTION -- */
    /*
    private fun calculateCurrentBill(tramoList:List<TramoObject>, consumption:Double):Double{
        var currentBill = 0.0
        // https://kotlinlang.org/docs/reference/control-flow.html
        if (consumption >0.0){
            for ((index,tramo) in tramoList.withIndex()) {
                if (consumption >= tramo.consumptionBase){
                    si consumo actual es mayor que el presente tramo en loop sumar
                    currentBill += when (index){
                        0 ->{
                            //si el consumo actual es superior al PISO del tramo MAXIMO
                            ((consumption - tramo.consumptionBase)*tramo.priceBase).roundToInt().toDouble()
                        }
                        else ->{
                            if (consumption >= tramoList[index-1].consumptionBase){
                                // si el consumo actual es superior al techo del presente tramo, sumar T0D0 el tramo
                                ((tramoList[index-1].consumptionBase-tramo.consumptionBase)*tramo.priceBase).roundToInt().toDouble()
                            }else{
                                //si el consumo actual es inferior al techo del presente tramo, solo sumar la porción del tramo
                                ((consumption - tramo.consumptionBase) * tramo.priceBase).roundToInt().toDouble()
                            }
                        }
                    }
                }
                Log.d("Billing", "importe $index de ${tramoList.lastIndex}: $currentBill")
            }
        }
        Log.d("Billing", "importe total : $currentBill")
        return currentBill
    } */
    /*
    private fun calculateCurrentBillDetail(tramoList:List<TramoObject>, consumption:Double):List<Map<String,Double>>{
        val currentBillDetail = mutableListOf<Map<String,Double>>()
        //tramo ordenado de MAYOR a menor
        var currentBillTotal = 0.0

        //https://kotlinlang.org/docs/reference/control-flow.html
        //https://stackoverflow.com/questions/47566187/is-it-possible-to-get-all-documents-in-a-firestore-cloud-function
        if (consumption >0.0){
            for ((index,tramo) in tramoList.withIndex()) {
                if (consumption >= tramo.consumptionBase){
                    // si consumo actual es mayor que el presente tramo en loop sumar
                    when (index){
                        0 ->{
                            //consumo supera en el Tramo superior
                            val tramoMap = mapOf(
                                "tramo"     to (tramoList.size-index).toDouble(),
                                "consumo"   to ((consumption - tramo.consumptionBase)*100).roundToInt().toDouble()/100,
                                "precio"     to tramo.priceBase.toDouble(),
                                "subtotal"  to ((consumption - tramo.consumptionBase)*tramo.priceBase).roundToInt().toDouble()
                            )
                            currentBillDetail.add(0,tramoMap)
                            currentBillTotal += (consumption - tramo.consumptionBase)*tramo.priceBase
                        }
                        else ->{
                            if (consumption >= tramoList[index-1].consumptionBase){
                                //consumo es superior Tramo actual -> se suma tod0 el valor del tramo
                                val tramoMap = mapOf(
                                    "tramo"     to (tramoList.size-index).toDouble(),
                                    "consumo"   to ((tramoList[index-1].consumptionBase-tramo.consumptionBase)*100).roundToInt().toDouble()/100,
                                    "precio"    to tramo.priceBase.toDouble(),
                                    "subtotal"  to ((tramoList[index-1].consumptionBase-tramo.consumptionBase)*tramo.priceBase).roundToInt().toDouble()
                                )
                                currentBillDetail.add(0,tramoMap)
                                currentBillTotal +=(tramoList[index-1].consumptionBase-tramo.consumptionBase)*tramo.priceBase
                            }else{
                                //consumo es inferior al Tramo actual -> se sumar el propocional del tramo
                                val tramoMap = mapOf(
                                    "tramo"     to (tramoList.size-index).toDouble(),
                                    "consumo"   to ((consumption - tramo.consumptionBase)*100).roundToInt().toDouble()/100,
                                    "precio"    to tramo.priceBase.toDouble(),
                                    "subtotal"  to ((consumption - tramo.consumptionBase) * tramo.priceBase).roundToInt().toDouble()
                                )
                                currentBillDetail.add(0,tramoMap)
                                currentBillTotal +=(consumption - tramo.consumptionBase) * tramo.priceBase
                            }
                        }
                    }
                }
            }
        }
        //cargando en Index:0 el total del cobro
        currentBillTotal = (currentBillTotal*100).roundToInt().toDouble() / 100
        currentBillDetail.add(0,mapOf("total" to currentBillTotal))
        Log.d("Billing", "Detalle importe total : $currentBillDetail")


        return currentBillDetail.toList()
    }*/


    /* Calcular consumo, incluyendo ciclo retorno de medido*/
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






}


