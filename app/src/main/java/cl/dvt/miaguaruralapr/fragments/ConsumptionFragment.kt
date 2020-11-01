package cl.dvt.miaguaruralapr.fragments
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
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
import cl.dvt.miaguaruralapr.SplashActivity.Companion.user
import cl.dvt.miaguaruralapr.MainActivity.Companion.camPermissionBoolean
import cl.dvt.miaguaruralapr.MainActivity.Companion.requestCameraResult
import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.models.Consumption
import cl.dvt.miaguaruralapr.adapters.ConsumptionItemAdapter
import cl.dvt.miaguaruralapr.models.User
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_consumption.*
import java.io.FileNotFoundException
import java.io.IOException

class ConsumptionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_consumption, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // fech consuptiom in Main Comsumption Collection
        user!!.fechtConsumptions(requireActivity(),consumption_recyclerView_consumption,50,debtFilter_switch_consumption)

        // mostrar en pantalla días remanentes
        remainingDays_textView_consumption.text = User().getRemainingDays(user).toString()/* set textView de dias remanentes */
        remainingDays_textView_consumption.bringToFront()

        //agregar consumo
        addConsumption_floatingActionButton_consumption.setOnClickListener{
            //diálogo ingreso de consumo
            if (requestCameraResult || camPermissionBoolean){
                //Capurar imagen camera y arrancar
                openCamera()
            }else{
                Toast.makeText(requireContext(), "cámara denegada", Toast.LENGTH_SHORT).show()
            }
        }

        //switch filtro mostrar all/sin_pagos
        debtFilter_switch_consumption.setOnClickListener {

            user!!.fechtConsumptions(requireActivity(),consumption_recyclerView_consumption,50,debtFilter_switch_consumption)
            Log.d("Filter", "current switch: ${debtFilter_switch_consumption.isChecked}")

            /*change Switch text  */
            if (debtFilter_switch_consumption.isChecked){
                debtFilter_switch_consumption.text = "mostrar todo"
            }else{
                debtFilter_switch_consumption.text = "sólo deudas"
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
        val uidApr          = user!!.uidApr
        val fetchLimit:Long = 50 /* límite de documentos a cargar en recyclerView*/
        /*Groupie RecyclerView : https://github.com/lisawray/groupie */
        val adapter = GroupAdapter<GroupieViewHolder>()    /* adaptador para el recyclerView */
        consumption_recyclerView_consumption.adapter = adapter /* Cargando el ReclyclerView de esta Actividad*/

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
                            adapter.add(document.newIndex,
                                ConsumptionItemAdapter(consumption,debtFilter_switch_consumption.isChecked)
                            ) /* IMPORTANTE : cargando datos a los items del adaptador personalizado */
                            adapter.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */
                            Log.d("Consumption", "Indice del modificado : ${document.oldIndex}")
                            val consumption = document.document.toObject(Consumption::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,
                                ConsumptionItemAdapter(consumption,debtFilter_switch_consumption.isChecked)
                            )
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
            }

        /** click en el item del recyclerView */
        adapter.setOnItemClickListener {item, view ->
            val consumptionItemAdapter = item as ConsumptionItemAdapter
            //ConsumptionOperation(consumptionItem.consumption).updateConsumptionDialog(requireActivity())
            consumptionItemAdapter.consumption.updateDialog(requireActivity())
        }


    }

}


