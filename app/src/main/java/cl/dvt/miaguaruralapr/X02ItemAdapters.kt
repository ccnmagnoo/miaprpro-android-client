package cl.dvt.miaguaruralapr

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.adapter_consumo.view.*
import kotlinx.android.synthetic.main.adapter_costumer.view.*
import kotlinx.android.synthetic.main.adapter_op_consumption.view.*
import kotlinx.android.synthetic.main.adapter_tramo.view.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat

class CostumerItemAdapter(val costumer: Costumer): Item<GroupieViewHolder>(){
    @SuppressLint("SimpleDateFormat")

    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        //cargado textView del adaptador

        //val Latitude:Double =  costumer.medidorLocation!!.getValue(key = "Latitude")
        //val Longitude:Double =  costumer.medidorLocation!!.getValue(key = "Longitude")
        val formatDateLong = SimpleDateFormat("EEEE dd MMMM yyyy")
        val formatedDate = formatDateLong.format(costumer.userCostumerLastPayDate)

        viewHolder.itemView.dateLastPay_textView_costumerAdapter.text       = formatedDate
        viewHolder.itemView.medidorNumber_textView_costumerAdapter.text     = costumer.medidorNumber.toString()
        viewHolder.itemView.costumerName_textView_costumerAdapter.text      = costumer.userCostumerName
        viewHolder.itemView.costumerPhone_textView_costumerAdapter.text     = costumer.userCostumerPhone
        viewHolder.itemView.costumerEmail_textView_costumerAdapter.text     = costumer.userCostumerEmail

        val formatCurrency = DecimalFormat("$ #,###")

        if (costumer.userCostumerDebt.toInt() == 0){
            viewHolder.itemView.currentDebt_textView_costumerAparter.alpha = 0.3f
            viewHolder.itemView.currentDebt_textView_costumerAparter.text = "pagado"
        }else{
            viewHolder.itemView.currentDebt_textView_costumerAparter.alpha = 1f
            viewHolder.itemView.currentDebt_textView_costumerAparter.text = formatCurrency.format(costumer.userCostumerDebt.toInt())
        }

        //cargando galerìa a imageView en el adaptador
        //Picasso.get().load(userToChat.profileImageUrl).into(viewHolder.itemView.usernamephoto_imageView_newmessage)
    }
    override fun getLayout(): Int {
        return R.layout.adapter_costumer /*cargando el formato desde el adaptador*/
    }
}

class ConsumptionItemAdapter(val consumption: Consumption, private val consumptionFilter: Boolean): Item<GroupieViewHolder>(){
    @SuppressLint("SimpleDateFormat")
    override fun bind(viewHolder: GroupieViewHolder, position: Int){
        //cargado textView del adaptador
        /* carga de valores de lectura */
        val view = viewHolder.itemView
        view.medidorNumber_textView_consumptionAdapter.text  = consumption.medidorNumber.toString()
        view.logNew_textView_consumptionAdapter.text         = consumption.logLectureNew.toString()
        view.logOld_textView_consumptionAdapter.text         = consumption.logLectureOld.toString()
        /* carga de valores de consumo */
        val formatDateLong      = SimpleDateFormat("EEEE dd MMMM yyyy")
        val formatedDate = formatDateLong.format(consumption.dateLectureNew)
        view.date_textView_consumptionAdapter.text           = formatedDate.toString()
        view.consumption_textView_consumptionAdapter.text    = consumption.consumptionCurrent.toString()
        /* carga de valores deuda */
        val formatCurrency      = DecimalFormat("$ #,###")
        view.importe_textView_consumptionAdapter.text        = formatCurrency.format(consumption.consumptionBill.toInt())
        view.payStatus_checkbox_ConsumptionAdapter.isChecked =  consumption.paymentStatus

        if (consumption.consumptionBill.toInt() == 0){
            view.payStatus_checkbox_ConsumptionAdapter.isClickable = false /* checkbox inhabilitado si consumo es 0 m3 */
        }else{
            view.payStatus_checkbox_ConsumptionAdapter.isClickable = true /* checkbox habilitado */
            /* Modificación del estado de pago de consumo */

            view.payStatus_checkbox_ConsumptionAdapter.setOnClickListener {
                consumption.paymentUpdate(it.payStatus_checkbox_ConsumptionAdapter)
            }//fin del onClick
        }

        val formatDateShort = SimpleDateFormat("dd/MM/yyyy")
        val dateLectureNew = formatDateShort.format(consumption.dateLectureNew)
        val dateLectureOld = formatDateShort.format(consumption.dateLectureOld)
        viewHolder.itemView.dateLectureNew_textView_consumptionAdapter.text  = dateLectureNew.toString()
        viewHolder.itemView.dateLectureOld_textView_consumptionAdapter.text  = dateLectureOld.toString()

//        Cambio de la visibilitad del item en el RecyclerView
        /*https://stackoverflow.com/questions/41223413/how-to-hide-an-item-from-recycler-view-on-a-particular-condition*/
        if (!consumptionFilter){
            if (consumption.paymentStatus){
                viewHolder.itemView.visibility = View.GONE
                viewHolder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            }else{
                viewHolder.itemView.visibility = View.VISIBLE
                viewHolder.itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
        //cargando galerìa a imageView en el adaptador
        /*Picasso.get().load(userToChat.profileImageUrl).into(viewHolder.itemView.usernamephoto_imageView_newmessage)*/
    }
    override fun getLayout(): Int {
        return R.layout.adapter_consumo /*cargando el formato desde el adaptador*/
    }

}

class ConsumptionDetailAdapter(private val consumptionDetail:Map<String,Double>): Item<GroupieViewHolder>(){
    /* item */
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.tramoId_textView_opConsumptionAdapter.text = consumptionDetail["tramo"]?.toInt().toString()
        viewHolder.itemView.consumption_textView_opConsumptionAdapter.text = consumptionDetail["consumo"].toString()
        val formatCurrency      = DecimalFormat("$ #,###")
        viewHolder.itemView.tramoBill_textView_opConsumptionAdapter.text = formatCurrency.format(consumptionDetail["subtotal"])
    }
    override fun getLayout(): Int {
        return R.layout.adapter_op_consumption
    }
}

class TramoItemAdapter(val tramo:Tramo): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.consumptionBase_textView_tramoAdapter.text = tramo.consumptionBase.toString()
        viewHolder.itemView.price_textView_tramoAdapter.text = tramo.priceBase.toString()
        viewHolder.itemView.name_textView_tramoAdapter.text = tramo.name
    }
    override fun getLayout(): Int {
        return R.layout.adapter_tramo
    }
}