package cl.dvt.miaguaruralapr.models

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import cl.dvt.miaguaruralapr.CostumerActivity
import cl.dvt.miaguaruralapr.SplashActivity
import cl.dvt.miaguaruralapr.adapters.ConsumptionItemAdapter
import cl.dvt.miaguaruralapr.adapters.CostumerItemAdapter
import cl.dvt.miaguaruralapr.fragments.CostumerFragment
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_consumption.*
import java.util.*
import java.util.concurrent.TimeUnit

@Parcelize
/** this is de APR user ,and administrator who does payments to the app*/

data class User(
    val uidApr: String="",
    val userAprEmail:String     = "",
    val userAprName:String      = "",
    val userAprRol: String      = "",
    val userAprPhone:String     = "",
    val userAprLocalidad:String = "",
    val userAprDir:String       = "",
    val userAprComuna:List<String>          = listOf(),
    val userLocation: Map<String,Double>?   = mapOf("Latitude" to 0.0 ,"Longitude" to 0.0),
    val dateRegister: Date = Date(), /* fecha de registro de usuario APR */
    val dateLimitBuy: Date = Date(), /* fecha límite de compra activa SIN USAR*/
    val typeUser:Int            = 2, /* tipo de usuario  administrador=1, apr=2, costumer=3*/
    val planId:Int             = 30, /* tipo de plan suscrito 30:gratuito de prueba  o más; planes con precios*/
    val userStatus:Boolean       = false         /* false activo 0 inactivo*/
): Parcelable{
    /** properties */
    var suscriptionPlan:SuscriptionPlan = SuscriptionPlan()
    var costumerList= mutableListOf<Costumer>()
    var tramoList = arrayListOf<Tramo>()

    /** public functions */
    fun fetchCostumers(context: Context,recyclerView:RecyclerView,costumerCounterTextView:TextView){
        //fetching suscribed Costumers

        val adapter = GroupAdapter<GroupieViewHolder>() /* declarando el groupie adapter*/
        recyclerView.adapter = adapter /* cargando el ReclyclerView de esta Actividad*/

        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(this.uidApr)
            .collection("userCostumer")

        ref
            .orderBy("medidorNumber", Query.Direction.ASCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { documents, e ->
                if (e != null) {
                    Log.w("Costumers", "Listen failed.", e)
                    return@addSnapshotListener
                }
                for (document in documents!!.documentChanges) {
                    Log.d("Costumers", "Total suscritos : ${CostumerFragment.costumersCount}")
                    when (document.type){
                        //Para firestore COSTUMERS AGREGADOS
                        DocumentChange.Type.ADDED ->{
                            val costumer = document.document.toObject(Costumer::class.java)
                            Log.d("Costumers", "Descargando: $costumer")
                            adapter.add(document.newIndex,CostumerItemAdapter(costumer))  /*cargando datos a los items del adaptador personalizado*/
                            costumerList.add(costumer)                  /*cargando listado de números de medidor*/
                        }
                        //Para firestore COSTUMERS MODIFICADOS
                        DocumentChange.Type.MODIFIED ->{
                            /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */
                            val costumerChangedUid = document.document.id /* numero del medidor en el firestore */
                            Log.d("Costumers", "Modificado el costumer: $costumerChangedUid")

                            val costumerObject = document.document.toObject(Costumer::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex, CostumerItemAdapter(costumerObject))
                            adapter.notifyDataSetChanged()                   /*actualizando datos a los items del adaptador personalizado*/
                            costumerList[document.oldIndex] = costumerObject /*actualizando listado de números de medidor*/

                        }
                        //Para firestore COSTUMERS BORRADOS
                        DocumentChange.Type.REMOVED ->{
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.notifyItemRemoved(document.oldIndex)
                            costumerList.removeAt(document.oldIndex)
                            Log.d("Costumer", "id del costumer borrado: ${document.document.id}")
                        }
                    }
                }

                //loading companion object Costumer Fragment
                CostumerFragment.costumerList = costumerList
                CostumerFragment.costumersCount = adapter.itemCount
                Log.d("Costumers", "Listado de consumidores: ${costumerList}")

                //loading costumer's counter textView
                costumerCounterTextView.text = adapter.itemCount.toString()
                costumerCounterTextView.bringToFront()

            }

        /* almacenamiento datos sin conexión */
        ref.firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()

        /* trigger costumer Activity on item click  */
        adapter.setOnItemClickListener {item, _ ->
            item as CostumerItemAdapter
            Log.d("Costumer", "abriendo detalle del costumer : ${item.costumer}")
            val intent = Intent(context, CostumerActivity::class.java)
            intent.putExtra(CostumerFragment.costumerKey, item.costumer)
            startActivity(context,intent,null)
        }

    }

    fun fechtConsumptions(context: Context, recyclerView:RecyclerView, limiter:Long, filterIsOn:Switch){
        /** fecth consumptiong from Main collection on firebase, with a limit of 50 */
        val fetchLimit:Long = limiter /* límite de documentos a cargar en recyclerView*/
        /*Groupie RecyclerView : https://github.com/lisawray/groupie */
        val adapter = GroupAdapter<GroupieViewHolder>()    /* adaptador para el recyclerView */
        recyclerView.adapter = adapter /* Cargando el ReclyclerView de esta Actividad*/

        val ref   = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(this.uidApr)
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
                                ConsumptionItemAdapter(consumption,filterIsOn.isChecked)
                            ) /* IMPORTANTE : cargando datos a los items del adaptador personalizado */
                            adapter.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED    ->{
                            /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */
                            Log.d("Consumption", "Indice del modificado : ${document.oldIndex}")
                            val consumption = document.document.toObject(Consumption::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,
                                ConsumptionItemAdapter(consumption,filterIsOn.isChecked)
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
        adapter.setOnItemClickListener {item, _ ->
            item as ConsumptionItemAdapter
            //ConsumptionOperation(consumptionItem.consumption).updateConsumptionDialog(requireActivity())
            item.consumption.updateDialog(context)
        }


    }

    /** CRUD functions */


    /** dialogs */

    /** auxiliar functions */

    fun getRemainingDays(apr: User?):Short{
        /** input apr user class and return remaining days */
        return when (apr?.planId){
            30      ->  {
                30 /* id 30: plan gratuito inicial */
            }
            null    ->  {
                0 /* id null: error no hay plan */
            }
            else    ->  {
                //get current time
                val currentTime     = Calendar.getInstance()
                //fetch user limit date
                val dateLastPayment = Calendar.getInstance()
                dateLastPayment.time          = apr.dateLimitBuy /* fecha límite de operación */
                //calculate remaining days
                val days = TimeUnit.MILLISECONDS.toDays((dateLastPayment.timeInMillis - currentTime.timeInMillis)).toShort()
                Log.d("Consumption", "Tiempo remanente para carga de consumos $days dias")
                days
            }
        }
    }

}