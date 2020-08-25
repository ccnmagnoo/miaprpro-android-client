package cl.dvt.miaguaruralapr

import android.os.Parcelable
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class AprUser(
    val uidApr: String="",
    val userAprEmail:String     = "",
    val userAprName:String      = "",
    val userAprRol: String      = "",
    val userAprPhone:String     = "",
    val userAprLocalidad:String = "",
    val userAprDir:String       = "",
    val userAprComuna:List<String>          = listOf(),
    val userLocation: Map<String,Double>?   = mapOf("Latitude" to 0.0 ,"Longitude" to 0.0),
    val dateRegister: Date = Date(),/** fecha de registro de usuario APR */
    val dateLimitBuy: Date = Date(),/** fecha límite de compra activa SIN USAR*/
    val typeUser:Int            = 2,/**tipo de usuario  administrador=1, apr=2, costumer=3*/
    val planId:Int             = 30,/**tipo de plan suscrito 30:gratuito de prueba  o más; planes con precios*/
    val userStatus:Boolean       = false         /**1 activo 0 inactivo*/
): Parcelable{
    /** main functions */


    /** dialogs */

    /** auxiliar functions */

}