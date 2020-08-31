package cl.dvt.miaguaruralapr

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import cl.dvt.miaguaruralapr.MainActivity.Companion.block_key
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_a05_costumer.*
import java.io.FileNotFoundException
import java.io.IOException
import java.text.DecimalFormat


@Suppress("IMPLICIT_CAST_TO_ANY", "NAME_SHADOWING",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class A05Costumer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a05_costumer)

        // current costumer
        val costumer: Costumer? = intent.getParcelableExtra<Costumer>(F02CostumerFragment.COSTUMER_KEY)

        costumer?.let {
            // Fetch costumer any update

            fetchCostumer(costumer)
            // fetch all costumer's consumptions
            fetchConsumption(costumer,consumption_recyclerView_costumerActivity)

            //Update current costumer
            update_floatingButton_costumerActivity.setOnClickListener {
                updateCostumer(costumer)
            }


            //new Consumption
            addConsumption_button_costumerActivity.setOnClickListener {
                //Camera permision
                if (MainActivity.requestCameraResult || MainActivity.camPermissionBoolean){
                    //Capture image from camera
                    camera(it)
                }else{
                    Toast.makeText(it.context, "cámara denegada", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //Return to mainActivity
        back_button_costumerActivity.setOnClickListener {
            if (!block_key){finish()}
        }

    }

    private fun fetchCostumer(costumer: Costumer){

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
                    val costumer = document.toObject(Costumer::class.java)
                    Log.d("costumerActivity", "$source data: $costumer")
                    populateValues(costumer)

                } else {
                    Log.d("costumerActivity", "$source data: null")
                }
            }
    }

    private fun populateValues(costumer:Costumer?){
        // formatos
        val format      = DecimalFormat("$ #,###")
        // cargando datos inmutables */
        medidorNumber_textView_costumerActivity.text = costumer?.medidorNumber.toString()
        debt_textView_costumerActivity.text = format.format(costumer?.userCostumerDebt)

        // cargando datos  editables */
        this.name_editText_consumptionActivity.hint = costumer?.userCostumerName
        email_editText_costumerActivity.hint = costumer?.userCostumerEmail
        phone_editText_costumerActivity.hint = costumer?.userCostumerPhone
        dir_editText_costumerActivity.hint = costumer?.userCostumerDir

    }

    private fun updateCostumer(costumer: Costumer){
        //costumer reference
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(costumer.uidApr)
            .collection("userCostumer")
            .document(costumer.medidorNumber.toString())

        //making hashMap of inputs editText/
        val mapInputs = mapOf<String,EditText>(
            "userCostumerName" to name_editText_consumptionActivity,
            "userCostumerEmail" to email_editText_costumerActivity,
            "userCostumerPhone" to phone_editText_costumerActivity,
            "userCostumerDir" to dir_editText_costumerActivity
        )

        //Fetch result of input analysis
        val result = inputResult(mapInputs)

        /* checking input data */
        if(result.first){
            //update information on cloud*/
           ref.set(result.second, SetOptions.merge())
               .addOnSuccessListener {
                   Log.d("updateCostumer", "costumer: ${costumer.medidorNumber} update info: ${result.second}")
                   updateStatus_textView_costumerActivity.visibility = View.VISIBLE

                   //changing update button
                   update_floatingButton_costumerActivity
                       .setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_done_24))
                   update_floatingButton_costumerActivity
                       .setBackgroundColor(resources.getColor(R.color.greenSpring))

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

    private fun inputResult(map:Map<String,EditText>):Pair<Boolean,Map<String,String>>{

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

    private fun fetchConsumption(costumer: Costumer,recyclerView: RecyclerView){
        //Instando adapter
        val adapter = GroupAdapter<GroupieViewHolder>()
        recyclerView.adapter = adapter /**Cargando el ReclyclerView de esta Actividad*/

        //list of costumer's consumptions
        val listConsumption = arrayListOf<Consumption>()

        val ref   = FirebaseFirestore.getInstance()
                .collection("userApr")
                .document(currentApr!!.uidApr)
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
                            val consumption = document.document.toObject(Consumption::class.java)
                            adapter.add(document.newIndex,ConsumptionItemAdapter(consumption,true))  /* IMPORTANTE : cargando datos a los items del adaptador personalizado */

                            listConsumption.add(consumption)
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            Log.d("Consumption", "Indice del modificado : ${document.oldIndex}")
                            val consumption = document.document.toObject(Consumption::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,ConsumptionItemAdapter(consumption,true))
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
                val statistics = Chart(this, listConsumption).statistics

                consumptionTotal_textView_costumerActivity.text = statistics["avg"].toString()
                consumptionOnDebt_textView_costumerActivity.text = statistics["consumptionOnDebt"].toString()

                /** cargar chart */
                 Chart(this, listConsumption).buildBar(consumption_chart_costumerActivity)


            }

        /** click en el item del recyclerView */
        adapter.setOnItemClickListener { item, _ ->
            val consumption = item as ConsumptionItemAdapter
            consumption.consumption.editDialog(this)

        }

    }

    //Módulo de CÁMARA
    private var imageUri: Uri? = null
    private val captureCODE = 1001

    private fun camera(view: View){
        /*http://androidtrainningcenter.blogspot.com/2012/05/bitmap-operations-like-re-sizing.html*/

        val context = view.context

        imageUri  = context.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues())
        val cameraIntent    = Intent(MediaStore.ACTION_IMAGE_CAPTURE)  /* captura de foto */
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)    /* instando imagen */
        Log.d("Picture", "P01 imagen guardada como : ${MediaStore.EXTRA_OUTPUT}")

        startActivityForResult(cameraIntent, captureCODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // current costumer
        val costumer: Costumer? = intent.getParcelableExtra<Costumer>(F02CostumerFragment.COSTUMER_KEY)

        /* called when image was captured from camera intent */
        if (resultCode == Activity.RESULT_OK){
            try{
                Log.d("Picture", "P02 imageUri dirección : ${imageUri.toString()}")
            }catch( e1: FileNotFoundException){ e1.printStackTrace()}
            catch( e2: IOException){ e2.printStackTrace()}
            Consumption().initDialog(this,costumer,imageUri)
        }
    }


}

