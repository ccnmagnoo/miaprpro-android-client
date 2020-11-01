package cl.dvt.miaguaruralapr.adapters

import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.models.Tramo
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.adapter_tramo.view.*

class TramoItemAdapter(val tramo: Tramo): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.consumptionBase_textView_tramoAdapter.text = tramo.consumptionBase.toString()
        viewHolder.itemView.price_textView_tramoAdapter.text = tramo.priceBase.toString()
        viewHolder.itemView.name_textView_tramoAdapter.text = tramo.name
    }
    override fun getLayout(): Int {
        return R.layout.adapter_tramo
    }
}