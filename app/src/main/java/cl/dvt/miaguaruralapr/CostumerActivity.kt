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
import cl.dvt.miaguaruralapr.MainActivity.Companion.block_key
import cl.dvt.miaguaruralapr.models.Consumption
import cl.dvt.miaguaruralapr.fragments.CostumerFragment
import cl.dvt.miaguaruralapr.models.Costumer
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_costumer.*
import java.io.FileNotFoundException
import java.io.IOException
import java.text.DecimalFormat


@Suppress("IMPLICIT_CAST_TO_ANY", "NAME_SHADOWING",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class CostumerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_costumer)

        // current costumer
        val costumer: Costumer? = intent.getParcelableExtra<Costumer>(CostumerFragment.costumerKey)

        costumer?.let {
            /* Fetch costumer on  any update */
            refreshCostumer(costumer)

            /* fetch all costumer's consumptions on anyupdate */
            costumer.fetchConsumption(
                this,
                consumption_recyclerView_costumerActivity,
                consumptionTotal_textView_costumerActivity,
                consumptionOnDebt_textView_costumerActivity,
                consumption_chart_costumerActivity
            )

            /* Update current costumer */
            update_floatingButton_costumerActivity.setOnClickListener {
                costumer.update(
                    this,
                    name_editText_costumerActivity,
                    email_editText_costumerActivity,
                    phone_editText_costumerActivity,
                    dir_editText_costumerActivity,
                    updateStatus_textView_costumerActivity,
                    update_floatingButton_costumerActivity
                )
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

    private fun refreshCostumer(costumer: Costumer){
        /** refreshing costumer data on any changes in firebase Database*/

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

    private fun populateValues(costumer: Costumer?){
        // formatos
        val format      = DecimalFormat("$ #,###")
        // cargando datos inmutables */
        medidorNumber_textView_costumerActivity.text = costumer?.medidorNumber.toString()
        debt_textView_costumerActivity.text = format.format(costumer?.userCostumerDebt)

        // cargando datos  editables */
        this.name_editText_costumerActivity.hint = costumer?.userCostumerName
        email_editText_costumerActivity.hint = costumer?.userCostumerEmail
        phone_editText_costumerActivity.hint = costumer?.userCostumerPhone
        dir_editText_costumerActivity.hint = costumer?.userCostumerDir

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
        val costumer: Costumer? = intent.getParcelableExtra<Costumer>(CostumerFragment.costumerKey)

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

