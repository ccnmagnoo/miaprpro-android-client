package cl.dvt.miaguaruralapr.adapters

import cl.dvt.miaguaruralapr.R
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.adapter_consumption_detail.view.*
import java.text.DecimalFormat




class ConsumptionItemDetailAdapter(private val consumptionDetail:Map<String,Double>): Item<GroupieViewHolder>(){
    /* item */
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.tramoId_textView_opConsumptionAdapter.text = consumptionDetail["tramo"]?.toInt().toString()
        viewHolder.itemView.consumption_textView_opConsumptionAdapter.text = consumptionDetail["consumo"].toString()
        val formatCurrency      = DecimalFormat("$ #,###")
        viewHolder.itemView.tramoBill_textView_opConsumptionAdapter.text = formatCurrency.format(consumptionDetail["subtotal"])
    }
    override fun getLayout(): Int {
        return R.layout.adapter_consumption_detail
    }
}

