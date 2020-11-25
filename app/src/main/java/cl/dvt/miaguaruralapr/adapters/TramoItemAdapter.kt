package cl.dvt.miaguaruralapr.adapters

import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.models.Tramo
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.adapter_tramo.view.*
import java.text.SimpleDateFormat

class TramoItemAdapter(val tramo: Tramo): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        /* populating basic data */
        viewHolder.itemView.consumptionBase_textView_tramoAdapter.text = tramo.consumptionBase.toString()
        viewHolder.itemView.price_textView_tramoAdapter.text = tramo.priceBase.toString()
        viewHolder.itemView.name_textView_tramoAdapter.text = tramo.name

        /*date's updating */
        val formatDateLong      = SimpleDateFormat("EEEE dd MMMM yyyy")
        val dateUpdating = formatDateLong.format(tramo.timestamp)
        viewHolder.itemView.dateUpdating_textView_tramoAdapter.text = dateUpdating
    }
    override fun getLayout(): Int {
        return R.layout.adapter_tramo
    }
}