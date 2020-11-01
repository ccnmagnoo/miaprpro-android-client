package cl.dvt.miaguaruralapr.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*
/** Tramo is the unitary section's cost of Water   */

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
