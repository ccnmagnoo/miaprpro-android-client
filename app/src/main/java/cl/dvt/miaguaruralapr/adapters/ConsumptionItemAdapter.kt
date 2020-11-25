package cl.dvt.miaguaruralapr.adapters

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.models.Consumption
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.adapter_consumption.view.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat

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
        view.date_textView_consumptionAdapter.text           = formatedDate
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
                consumption.update(it.payStatus_checkbox_ConsumptionAdapter)
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
        return R.layout.adapter_consumption /*cargando el formato desde el adaptador*/
    }

}
