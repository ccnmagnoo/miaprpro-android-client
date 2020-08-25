package cl.dvt.miaguaruralapr
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import cl.dvt.miaguaruralapr.MainActivity.Companion.camPermissionBoolean
import cl.dvt.miaguaruralapr.MainActivity.Companion.remainingDays
import cl.dvt.miaguaruralapr.MainActivity.Companion.requestCameraResult
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_f01_consumo.*
import kotlinx.android.synthetic.main.section_add_consumption_alert.view.*
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat

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
            //diálogo ingreso de consumo
            if (requestCameraResult || camPermissionBoolean){
                //Capurar imagen camera y arrancar
                openCamera()
            }else{
                Toast.makeText(requireContext(), "cámara denegada", Toast.LENGTH_SHORT).show()
            }
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



    //Módulo de CÁMARA
    private var imageUri: Uri? = null
    private val captureCODE = 1001

    private fun openCamera(){
        /*http://androidtrainningcenter.blogspot.com/2012/05/bitmap-operations-like-re-sizing.html*/
        val context = this.view?.context

        imageUri            = context?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,ContentValues())
        val cameraIntent    = Intent(MediaStore.ACTION_IMAGE_CAPTURE)  /* captura de foto */
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)    /* instando imagen */
        Log.d("Picture", "P01 imagen guardada como : ${MediaStore.EXTRA_OUTPUT}")

        startActivityForResult(cameraIntent, captureCODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* called when image was captured from camera intent */
        if (resultCode == Activity.RESULT_OK){
            try{
                Log.d("Picture", "P02 imageUri dirección : ${imageUri.toString()}")
            }catch( e1: FileNotFoundException){ e1.printStackTrace()}
            catch( e2: IOException){ e2.printStackTrace()}
            Consumption().initDialog(requireActivity(),null,imageUri)
        }
    }


    //F05 descargar consumos de todos los clientes últimos 2 meses : IMPORTANTE
    private fun fetchConsumption(){
        val uidApr          = currentApr!!.uidApr
        val fetchLimit:Long = 50 /* límite de documentos a cargar en recyclerView*/
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
                            val consumption = document.document.toObject(Consumption::class.java)
                            adapter.add(document.newIndex,ConsumptionItemAdapter(consumption,debtFilter_switch_costumer.isChecked)) /* IMPORTANTE : cargando datos a los items del adaptador personalizado */
                            adapter.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */
                            Log.d("Consumption", "Indice del modificado : ${document.oldIndex}")
                            val consumption = document.document.toObject(Consumption::class.java)
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

                //numberOfConsumption_textView_consumo.text = "últimas $fetchLimit lecturas"
            }



        ref.firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()/* almacenamiento de datos sin conexión*/

        /** click en el item del recyclerView */
        adapter.setOnItemClickListener {item, view ->
            val consumption = item as ConsumptionItemAdapter
            //ConsumptionOperation(consumptionItem.consumption).updateConsumptionDialog(requireActivity())
            consumption.consumption.editDialog(requireActivity())
        }


    }/* fin del fetchConsumption() */


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



}


