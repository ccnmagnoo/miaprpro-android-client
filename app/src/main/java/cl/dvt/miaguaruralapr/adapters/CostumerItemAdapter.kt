package cl.dvt.miaguaruralapr.adapters

import android.annotation.SuppressLint
import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.models.Costumer
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.adapter_costumer.view.*
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