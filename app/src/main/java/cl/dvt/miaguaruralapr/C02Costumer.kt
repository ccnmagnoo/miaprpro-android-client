package cl.dvt.miaguaruralapr

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Costumer(
    val uidCostumer: String="",
    val uidApr:String="",
    val userCostumerName:String     = "",
    val userCostumerEmail:String    = "",
    val userCostumerPhone:String    = "",
    val userCostumerDir:String      = "",
    val userCostumerRut:String?     = "",
    val medidorLocation: Map<String,Double>? = mapOf(),
    val medidorNumber:Int           = 0,
    val medidorSerial:String?       = "",
    val dateMedidorRegister: Date = Date(),
    val typeUser:Int                = -1,   /**tipo de usuario  administrador=1, apr=2, costumer=3*/
    val userStatus:Int              = -1,   /**1 activo 0 inactivo*/
    val userCostumerDebt:Double     = 0.0,  /** deuda actual del consumidor */
    val userCostumerLastPayDate: Date = Date() /** fecha del último pago */
): Parcelable{
    /** Main functions */


    /** Dialogs */

    /** Secondary functions */


}