package cl.dvt.miaguaruralapr

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*





@Parcelize
data class SuscriptionPlan(
    val id: Int           = -1,    /** identificador numérico del plan */
    val limit: Int        = -1,   /** límite de usuarios que la APP permite suscribir */
    val days: Int         = -1,   /** límite de días que se permite subir Consumos*/
    val description:String  = "",   /** descripción del plan de la APP "*/
    val name:String         = "",   /** límite de usuarios que la APP permite suscribir"*/
    val price:Double        = 0.0,  /** precio de la mensualidad para la APP */
    val priceU:Double       = 0.0  /** precio unitario redondeado */
): Parcelable



@Parcelize
data class Tramo(
    val name:String             = "",/** nombre del plan de precios */
    val consumptionBase:Double  = 0.0,/** piso en metros cúbicos para los precios del consumo*/
    val priceBase:Int           = 0,/** precio del metro cúbico*/
    val description:String      = "",/** descripción del plan de precios del APR*/
    val edible:Boolean          = false,/** editable del valor piso m3, por defecto el piso es 0 y sólo se puede editar el piso*/
    val uidApr:String           = "",    /** string identificador del APR*/
    val uidTramo:String         = "", //uid del tramo
    var timestamp:Date          = Date()
): Parcelable
