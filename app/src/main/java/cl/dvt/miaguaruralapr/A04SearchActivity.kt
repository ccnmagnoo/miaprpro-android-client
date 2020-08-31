package cl.dvt.miaguaruralapr

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import cl.dvt.miaguaruralapr.F02CostumerFragment.Companion.costumerList
import cl.dvt.miaguaruralapr.MainActivity.Companion.block_key
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_a04_search.*
import kotlinx.android.synthetic.main.section_op_consumption.view.*
import kotlinx.android.synthetic.main.section_op_consumption_alert01yes.view.*
import java.lang.Exception
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class A04SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a04_search)

        val query= intent.getStringExtra(MainActivity.QUERY_KEY)
        search_searchView_search.isEnabled = true

        if(!query.isNullOrBlank()){
            fetchCostumer(query.toString())
            fetchConsumption(query.toString())
        }

        backMain_button_search.setOnClickListener {
        if (!block_key){finish()}
        }

        search_searchView_search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newQuery: String?): Boolean {
                val finder = costumerList.any { costumer -> costumer.medidorNumber.toString()==newQuery  }

                return when{
                    newQuery?.isBlank()!!->{
                        search_searchView_search.queryHint = "busca cliente medidor"
                        false
                    }
                    !finder ->{
                        search_searchView_search.queryHint = "no encontrado"
                        false
                    }
                    finder ->{
                        val costumer = costumerList.find { costumer -> costumer.medidorNumber.toString() == newQuery  }
                        fetchCostumer(newQuery.toString())
                        fetchConsumption(newQuery.toString())
                        false
                    }
                    else->{

                        false
                    }

                }
            }
        })
    }


    private fun fetchCostumer(query:String){
        val adapter  = GroupAdapter<GroupieViewHolder>()
        costumerSearchResult_recyclerView_search.adapter = adapter /* Cargando el ReclyclerView de esta Actividad */

        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(currentApr!!.uidApr)
            .collection("userCostumer")
            .whereEqualTo("uidCostumer",query)
            .limit(1)
        ref
            .addSnapshotListener(MetadataChanges.INCLUDE) { result, e ->
                if (e != null) {
                    Log.d("Search", "Listen failed on query: $query", e)
                    return@addSnapshotListener
                }
                for (document in result!!.documentChanges) {
                    when (document.type){
                        DocumentChange.Type.ADDED ->{
                            /* costumer agregado */
                            val costumer = document.document.toObject(Costumer::class.java)
                            Log.d("Search", "Descargando: $costumer")
                            adapter.add(CostumerItemAdapter(costumer))    /* cargando datos a los items del adaptador personalizado */
                        }
                        DocumentChange.Type.MODIFIED ->{
                            val costumer = document.document.toObject(Costumer::class.java)
                            Log.d("Search", "Update: $costumer on ${document.oldIndex}")
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,CostumerItemAdapter(costumer))
                            adapter.notifyDataSetChanged()
                        }
                        DocumentChange.Type.REMOVED ->{/***/}
                    }
                }
            }

        /* click en cliente Costumer */
        adapter.setOnItemClickListener {item, view ->
            val costumer = item as CostumerItemAdapter
            Log.d("Costumer", "abriendo detalle del costumer : ${costumer.costumer}")
            val intent = Intent(this, A05Costumer::class.java)
            intent.putExtra(F02CostumerFragment.COSTUMER_KEY, costumer.costumer)
            startActivity(intent)
        }

    }/* fin fetch costumers */

    private var lastConsumptionUUID = String()
    private fun fetchConsumption(query:String){
        val adapter = GroupAdapter<GroupieViewHolder>()
        consumptionSearchResult_recyclerView_search.adapter = adapter /**Cargando el ReclyclerView de esta Actividad*/
        val ref   = FirebaseFirestore.getInstance()
                .collection("userApr")
                .document(currentApr!!.uidApr)
                .collection("userCostumer")
                .document(query)
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
                            val consumptionObject = document.document.toObject(Consumption::class.java)
                            adapter.add(ConsumptionItemAdapter(consumptionObject,true))  /* IMPORTANTE : cargando datos a los items del adaptador personalizado */
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            Log.d("Consumption", "Indice del modificado : ${document.oldIndex}")
                            val consumptionObject = document.document.toObject(Consumption::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,ConsumptionItemAdapter(consumptionObject,true))
                            adapter.notifyItemChanged(document.oldIndex)/*actualizando datos a los items del adaptador personalizado*/

                        }
                        DocumentChange.Type.REMOVED     ->{
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.notifyItemRemoved(document.oldIndex)
                        }
                    }
                }
                /*obteniendo el UUID del  último consumo */
                val lastConsumptionItem = adapter.getItem(adapter.itemCount-1) as ConsumptionItemAdapter
                lastConsumptionUUID = lastConsumptionItem.consumption.uuidConsumption

            }
        adapter.setOnItemClickListener {item, view ->
            val consumption = item as ConsumptionItemAdapter
            openOpConsumptionDialog(consumption)
        }
    }/* fin del fetchConsumption() */

    @SuppressLint("SimpleDateFormat")
    private fun openOpConsumptionDialog(consumptionItem:ConsumptionItemAdapter){
        val consumption = consumptionItem.consumption

        /*Abriendo dialogo*/
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.section_op_consumption, null) /** Instando dialogView */
        val mBuilder = AlertDialog.Builder(this) /** Inflado del cuadro de dialogo */
                .setView(mDialogView)
        val  mAlertDialog = mBuilder.show()/** show dialog */

        /*Populando datos */
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
        mDialogView.payStatus_checkbox_opConsumption.isChecked =  consumption.paymentStatus
        mDialogView.payStatus_checkbox_opConsumption.text       = if (consumption.paymentStatus){"pagado"}else{"pagar"}

        /* Cargando imagen de captura  */
        Picasso.get().load(consumption.consumptionPicUrl).into(mDialogView.image_imageView_opConsumption, object: com.squareup.picasso.Callback{
            override fun onSuccess() {
                mDialogView.loading_progressBar_opConsumption.visibility = View.GONE
            }
            override fun onError(e: Exception?) {
                mDialogView.loading_progressBar_opConsumption.visibility = View.GONE
                mDialogView.noPic_imageView_opConsumption.visibility = View.VISIBLE
                mDialogView.image_imageView_opConsumption.background = getDrawable(R.drawable.app_draw_edittext_dark)
            }
        })

        /*Poblando recyclerView de detalle del cobro*/
        val adapter = GroupAdapter<GroupieViewHolder>()     /* adaptador para el recyclerView */
        mDialogView.consumptionDetail_recyclerView_opConsumption.adapter = adapter
        for ((index, billingData) in consumption.consumptionBillDetail.withIndex()){
            if (index > 0 ){
                adapter.add(ConsumptionDetailAdapter(billingData))
            }
        }

        /* bloqueando checkbox si billing es 0*/
        /** boton actualización de estado de pago */
        if (consumption.consumptionBill.toInt() == 0){
            mDialogView.payStatus_checkbox_opConsumption.isClickable = false /* checkbox inhabilitado si consumo es 0 m3 */
        }else{
            mDialogView.payStatus_checkbox_opConsumption.isClickable = true
            /** Boton cambio de estado de pago*/
            mDialogView.payStatus_checkbox_opConsumption.setOnClickListener {
                updateConsumption(consumption,mDialogView.payStatus_checkbox_opConsumption.isChecked, mDialogView )
            }
        }
        /** botón borrar (sólo si es el último registro de la serie) */
        mDialogView.del_imageButton_opConsumption.setOnClickListener {
            if (consumption.uuidConsumption == lastConsumptionUUID){
                /* en caso que sea el último registro y  se permite borrar*/
                mAlertDialog.dismiss()
                val mDialogAlertYesView         = LayoutInflater.from(this).inflate(R.layout.section_op_consumption_alert01yes, null)
                val mBuilderAlertYes  = AlertDialog.Builder(this)
                    .setView(mDialogAlertYesView)
                    .setTitle("advertencia")
                    .setNegativeButton("no |",null)
                    .setPositiveButton("quiero borrarlo") { dialog, id ->
                        deleteConsumption(consumption)
                    }
                mDialogAlertYesView.message_textView_opConsumptionDelete.text = "va a eliminar  consumo de ${consumption.consumptionCurrent} m³ del cliente N° ${consumption.medidorNumber}, tambien eliminará registro de pago asociado"
                val  mAlertDialogYes = mBuilderAlertYes.show()

            }else{
                /* en caso que NO sea el último registro*/
                mAlertDialog.dismiss()
                val mDialogAlertNoView         = LayoutInflater.from(this).inflate(R.layout.section_op_consumption_alert02no, null)
                val mBuilderAlertNo  = AlertDialog.Builder(this)
                    .setView(mDialogAlertNoView)
                    .setTitle("no se puede borrar")
                    .setPositiveButton("gracias, entiendo",null)
                val  mAlertDialogNo = mBuilderAlertNo.show()
            }
        }/* fin boton borrar */
    }
    //F12 Update consumo
    private fun updateConsumption(consumption: Consumption, payCheckBoxStatus:Boolean, mDialogView: View){
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

        /* botón actualización de estado de pago */
        if (payCheckBoxStatus){
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
                    mDialogView.payStatus_checkbox_opConsumption.text = "pagado"
                    /* reduciendo monto de deuda cliente */
                    refCostumer.update("userCostumerDebt", FieldValue.increment((-1)*consumption.consumptionBill.toDouble()))
                    refCostumer.set (hashMapOf("userCostumerLastPayDate" to Calendar.getInstance().time), SetOptions.merge())
                    MainActivity.block_key = false
                }.addOnFailureListener {
                    mDialogView.payStatus_checkbox_opConsumption.isChecked = false
                    mDialogView.payStatus_checkbox_opConsumption.text = "sin red"
                    MainActivity.block_key = true
                }
        } else{
            val newPaymentStatus = hashMapOf("paymentStatus" to false)
            /* actualizando status de pago FALSO */
            refSubCollection.set(newPaymentStatus, SetOptions.merge() )
                .addOnSuccessListener {
                    refMainCollection.set(newPaymentStatus, SetOptions.merge() )
                    mDialogView.payStatus_checkbox_opConsumption.text = "deuda"
                    /* reducir deuda de cliente costumer */
                    refCostumer.update("userCostumerDebt", FieldValue.increment(consumption.consumptionBill.toDouble()))
                }.addOnFailureListener {
                    mDialogView.payStatus_checkbox_opConsumption.isChecked = true
                    mDialogView.payStatus_checkbox_opConsumption.text = "sin red"
                }
        }
    }

    //F13 Borrar consumo
    private fun deleteConsumption(consumption: Consumption){

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
        refMainCollection.delete().addOnSuccessListener {
            /* borrar consumo en la colección secundaria*/
            refSubCollection.delete()
            /* reducir monto deuda de cliente */
            if (!consumption.paymentStatus){ /* descuenta sólo si el estado de pago es falso  = no pago*/
                refCostumer.update("userCostumerDebt", FieldValue.increment((-1)*consumption.consumptionBill))
            }
            /* borrar imagen del consumo */
            val refPic = FirebaseStorage.getInstance().reference.child(consumption.consumptionPicUrl)
            refPic.delete()
                .addOnSuccessListener {
                    Log.d("Delete", "borrada la foto")
                }
                .addOnFailureListener{
                    Log.d("Delete", "error al borrar")
                }

        }

    }

    private fun fetchLastConsumption(){

    }

}
