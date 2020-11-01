package cl.dvt.miaguaruralapr.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
/**sucripction model for payments*/
@Parcelize
data class SuscriptionPlan(

    val id: Int           = -1,    /** identificador numérico del plan */
    val limit: Int        = -1,   /** límite de usuarios que la APP permite suscribir */
    val days: Int         = -1,   /** límite de días que se permite subir Consumos*/
    val description:String  = "",   /** descripción del plan de la APP "*/
    val name:String         = "",   /** límite de usuarios que la APP permite suscribir"*/
    val price:Double        = 0.0,  /** precio de la mensualidad para la APP */
    val priceU:Double       = 0.0  /** precio unitario redondeado */
): Parcelable{

}