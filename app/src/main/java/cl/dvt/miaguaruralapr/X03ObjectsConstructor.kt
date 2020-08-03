package cl.dvt.miaguaruralapr

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.ArrayList

@Parcelize
data class AprObject(
    val uidApr: String="",
    val userAprEmail:String     = "",
    val userAprName:String      = "",
    val userAprRol: String      = "",
    val userAprPhone:String     = "",
    val userAprLocalidad:String = "",
    val userAprDir:String       = "",
    val userAprComuna:List<String>          = listOf(),
    val userLocation: Map<String,Double>?   = mapOf("Latitude" to 0.0 ,"Longitude" to 0.0),
    val dateRegister: Date      = Date(),/** fecha de registro de usuario APR */
    val dateLimitBuy: Date      = Date(),/** fecha límite de compra activa SIN USAR*/
    val typeUser:Int            = 2,  /**tipo de usuario  administrador=1, apr=2, costumer=3*/
    val planId:Int             = 30,  /**tipo de plan suscrito 30:gratuito de prueba  o más; planes con precios*/
    val userStatus:Boolean       = false         /**1 activo 0 inactivo*/
): Parcelable

@Parcelize
data class CostumerObject(
    val uidCostumer: String="",
    val uidApr:String="",
    val userCostumerName:String     = "",
    val userCostumerEmail:String    = "",
    val userCostumerPhone:String    = "",
    val userCostumerDir:String      = "",
    val userCostumerRut:String?     = "",
    val medidorLocation: Map<String,Double>? = mapOf("" to 0.0 ,"" to 0.0),
    val medidorNumber:Int           =0,
    val medidorSerial:String?       = "",
    val dateMedidorRegister: Date   = Date(),
    val typeUser:Int                = -1,       /**tipo de usuario  administrador=1, apr=2, costumer=3*/
    val userStatus:Int              = -1,       /**1 activo 0 inactivo*/
    val userCostumerDebt:Double     = 0.0,        /** deuda actual del consumidor */
    val userCostumerLastPayDate:Date    = Date()/** fecha del último pago */
): Parcelable

@Parcelize
data class SuscriptionObject(
    val id: Int           = -1,    /** identificador numérico del plan */
    val limit: Int        = -1,   /** límite de usuarios que la APP permite suscribir */
    val days: Int         = -1,   /** límite de días que se permite subir Consumos*/
    val description:String  = "",   /** descripción del plan de la APP "*/
    val name:String         = "",   /** límite de usuarios que la APP permite suscribir"*/
    val price:Double        = 0.0,  /** precio de la mensualidad para la APP */
    val priceU:Double       = 0.0  /** precio unitario redondeado */
): Parcelable

@Parcelize
data class ConsumptionObject(
    val timestamp:Long              =0,
    val uidApr: String              ="",
    val uuidConsumption:String      ="",/** Identificador único de consumo de agua*/
    val medidorNumber: Int          =0,
    val dateLectureNew: Date        = Date(),
    val dateLectureOld: Date        = Date(),
    val logLectureNew:Double        = 0.0,/** lectura anterior */
    val logLectureOld:Double        = 0.0,/** lectura actual */
    val consumptionCurrent:Double   = 0.0,/** total consumo m3 */
    val consumptionBillDetail: List<Map<String,Double>> = listOf(mapOf("0" to 0.0)),/* detalle del cobro por tramo */
    val consumptionBill:Double         = 0.0,/** total a pagar */
    val consumptionPicUrl:String    = "",/** URL de la foto de respaldo medición en firebase */
    val paymentStatus:Boolean       = false,/** false sin pagar, true pagado */
    val paymentDate:Date           = Date() /** fecha de pago de cliente consumidor */
): Parcelable

@Parcelize
data class TramoObject(
    val name:String             = "",/** nombre del plan de precios */
    val consumptionBase:Double  = 0.0,/** piso en metros cúbicos para los precios del consumo*/
    val priceBase:Int           = 0,/** precio del metro cúbico*/
    val description:String      = "",/** descripción del plan de precios del APR*/
    val edible:Boolean          = false,/** editable del valor piso m3, por defecto el piso es 0 y sólo se puede editar el piso*/
    val uidApr:String           = "",    /** string identificador del APR*/
    val uidTramo:String         = "", //uid del tramo
    var timestamp:Date          = Date()
): Parcelable
