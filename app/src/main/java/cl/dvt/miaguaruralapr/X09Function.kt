package cl.dvt.miaguaruralapr

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.section_op_consumption.view.*
import kotlinx.android.synthetic.main.section_op_consumption_alert01yes.view.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

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

        val refMainCollection = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("costumerConsumptionMain")
            .document(consumption.uuidConsumption)

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

        /* colección del administrador APR */
        refMainCollection.set(consumption)
            .addOnSuccessListener {
                Log.d("Consumption", "guardado en collección principal")
                //arrancar actividad de login
                Toast.makeText(context, "Registrado consumo cliente N°${consumption.medidorNumber} : ${consumption.consumptionCurrent} m3", Toast.LENGTH_SHORT ).show()
                mAlertDialogLoading.dismiss()
            }
            .addOnFailureListener {
                Log.d("Consumption", "Fallo Guardado en Firestore")
                Toast.makeText(context, "Error en guardar cliente ${consumption.medidorNumber} ", Toast.LENGTH_SHORT).show()
            }

        /* coleción para carga y adición de deuda --actualizado a CLOUD FUNCTION--*/
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

        val refMainCollection = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("costumerConsumptionMain")
            .document(consumption.uuidConsumption)

        val refCostumer = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("userCostumer")
            .document(consumption.medidorNumber.toString())

        /* borrar consumo en la colección principal */
        refSubCollection.delete().addOnSuccessListener {
            /* borrar consumo en la colección secundaria*/
            refMainCollection.delete()
            /* reducir monto deuda de cliente: --actualizado a CLOUD FUNCTION-- */
//            if (!consumption.paymentStatus){
//                /* descuenta sólo si el estado de pago es falso  = no pago */
//                refCostumer.update("userCostumerDebt", FieldValue.increment((-1)*consumption.consumptionBill))
//            }
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
        val refMainCollection = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(consumption.uidApr)
            .collection("costumerConsumptionMain")
            .document(consumption.uuidConsumption)

        /* botón actualización de estado de pago */
        if (payCheckBoxStatus.isChecked){
            MainActivity.block_key = true /* se bloquea el permitir cerrar la actividad*/
            val newPaymentStatus = hashMapOf(
                "paymentStatus" to true,
                "paymentDate" to Calendar.getInstance().time
            )

            /* actualizando status de pago VERDADERO*/
            refSubCollection.set(newPaymentStatus, SetOptions.merge() )
                .addOnSuccessListener {
                    refMainCollection.set(newPaymentStatus, SetOptions.merge() )
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
            val newPaymentStatus = hashMapOf("paymentStatus" to false)
            /* actualizando status de pago FALSO */
            refSubCollection.set(newPaymentStatus, SetOptions.merge() )
                .addOnSuccessListener {
                    refMainCollection.set(newPaymentStatus, SetOptions.merge() )
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
    internal fun editConsumptionDialog(context:Context){

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
                mDialogView.image_imageView_opConsumption.background =
                    ContextCompat.getDrawable(context, R.drawable.app_draw_background)
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

    /*cuadro de dialogo para ingresar un nuevo consumo*/
    fun newConsumptionDialog(context:Context){

    }

}