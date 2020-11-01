package cl.dvt.miaguaruralapr.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import cl.dvt.miaguaruralapr.SplashActivity.Companion.user
import cl.dvt.miaguaruralapr.RegisterActivity.Companion.PERMISSION_ID
import cl.dvt.miaguaruralapr.CostumerActivity
import cl.dvt.miaguaruralapr.R
import cl.dvt.miaguaruralapr.models.Costumer
import cl.dvt.miaguaruralapr.adapters.CostumerItemAdapter
import cl.dvt.miaguaruralapr.models.SuscriptionPlan
import com.google.android.gms.location.*
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.dialog_create_costumer.view.*
import kotlinx.android.synthetic.main.fragment_costumer.*
import kotlinx.android.synthetic.main.dialog_create_costumer.*
import kotlinx.android.synthetic.main.dialog_create_costumer.view.location_button_costumer
import kotlinx.android.synthetic.main.dialog_create_costumer.view.name_editText_costumer
import java.util.*

class CostumerFragment : Fragment(){
    companion object{
        var costumerList = mutableListOf<Costumer>()
        var costumerKey: String = "COSTUMER_DETAIL"

        internal var costumersCount: Int = 0
        internal var currentCostumerLimit: Short = 0
    }

    //location services properties
    private lateinit var   fusedLocation: FusedLocationProviderClient
    private lateinit var    lastLocation:Location

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_costumer, container, false)
    }/** Fin onCreateView */

    override fun onViewCreated(view:View,savedInstanceState: Bundle?) {
        super.onViewCreated(view,savedInstanceState)

        /**Inicializado variables geolocación*/
        lastLocation =  Location("0")
        fusedLocation = LocationServices.getFusedLocationProviderClient(requireActivity())

        /**Obteniendo límite de usuarios costumer del Plan actual*/
        checkRemainingCostumers()

        /**fetch current user's costumers registered on batabase*/
        user!!.fetchCostumers(requireActivity(),costumers_recyclerView_costumer,numberOfCostumers_textView_costumer)

        /**Add Costumer to database*/
        addcostumer_floatingActionButton_costumer.setOnClickListener{
            initDialog(currentCostumerLimit.toInt())
            //TODO Costumer().initDialog(this.requireContext(), currentCostumerLimit.toInt())
        }
    }


    private fun initDialog(currentLimit:Int){
        //diálogo de inicialización con vacante/sin vacante

        /*Verify number of user*/
        Log.d("CurrentPlan", "Límite plan: $currentLimit, usados: $costumersCount")

        if(currentLimit> costumersCount){
            createDialog(requireActivity(),lastLocation)/*start add Costumer function*/
        }else{
            limitDialog() /* number or costumer sucription reach limit*/
        }
    }

    private fun createDialog(context: Context,lastLocation:Location){
        /** https://devofandroid.blogspot.com/2018/04/alertdialog-with-custom-layout-kotlin.html
         * agregar al context This "requiredContext()"
         * extraer datos de dialog: https://demonuts.com/android-custom-dialog-edittext/
         * https://code.luasoftware.com/tutorials/android/android-text-input-dialog-with-inflated-view-kotlin/ */

        //Inflate dialog
        val numberOfCostumersPlus1= costumersCount +1
        val mDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_costumer, null)
        val mBuilder = AlertDialog.Builder(context)
            .setView(mDialogView)
            .setTitle("Ingresar nuevo cliente $numberOfCostumersPlus1 de $currentCostumerLimit ")
        val  mAlertDialog = mBuilder.show()

        //Fetch current location
        var geoSwitchState    = false


        //Action button
        /**on location button*/
        mDialogView.location_button_costumer.setOnClickListener {
            mAlertDialog.location_button_costumer.text = getLocationManual()
            geoSwitchState = true
        }
        //CANCEL button
        mDialogView.cancel_button_costumer.setOnClickListener {
            mAlertDialog.dismiss()
        }
        //Save button
        mDialogView.save_button_costumer.setOnClickListener{
            val userCostumerName    = mDialogView.name_editText_costumer.text.toString()
            val medidorNumber       = mDialogView.medidorNumber_editText_costumer.text.toString()
            val medidorNumberR      = mDialogView.medidorNumberR_editText_costumer.text.toString()
            val medidorSerial       = mDialogView.serial_editText_costumer.text.toString()
            val userCostumerRut     = mDialogView.rut_editText_costumer.text.toString()
            val checkInputDataVal = checkInput(mDialogView,medidorNumber,medidorNumber,medidorNumberR,geoSwitchState)

            if (checkInputDataVal){
                create(userCostumerName, medidorNumber, medidorSerial, userCostumerRut, geoSwitchState,lastLocation)
                mAlertDialog.dismiss()
            }else{
                return@setOnClickListener
            }
        }
    }

    private fun limitDialog(){
        /** alert limit of costumers reached*/
        val mDialogViewA = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_create_costumer_denied, null)
        val mBuilderA = AlertDialog.Builder(requireActivity())
            .setView(mDialogViewA)
            .setTitle("Alerta de Límite")
            .setNegativeButton("OK", null)
            .setPositiveButton("AUMENTAR PLAN", null)
        mBuilderA.show()
    }


    //F02. Check de valores ingresados
    private fun checkInput(mDialogView:View, userCostumerName:String, medidorNumber:String, medidorNumberR:String, geoSwitchState:Boolean):Boolean{
        val x = this.requireContext()
        while (userCostumerName.isEmpty()){
            Toast.makeText(this.requireContext(),"ERROR: nombre vacio", Toast.LENGTH_SHORT).show()
            mDialogView.name_editText_costumer.error = "Vacio"
            return false}
        while (medidorNumber.isEmpty()){
            Toast.makeText(this.requireContext(),"ERROR: número vacio", Toast.LENGTH_SHORT).show()
            mDialogView.medidorNumber_editText_costumer.error = "vacio"
            return false}
        while (medidorNumberR.isEmpty()){
            Toast.makeText(this.requireContext(),"ERROR: verificación número vacio", Toast.LENGTH_SHORT).show()
            mDialogView.medidorNumberR_editText_costumer.error = "vacio"
            return false}
        while (medidorNumber != medidorNumberR ){
            Toast.makeText(this.requireContext(),"ERROR: número de medidor incorrecto", Toast.LENGTH_SHORT).show()
            mDialogView.medidorNumberR_editText_costumer.error = "no coinciden"
            return false}
        while (!geoSwitchState){
            Toast.makeText(this.requireContext(),"ADVERTENCIA: medidor sin localizar", Toast.LENGTH_SHORT).show()
            return true
        }
        for (element in costumerList){
            if (element.medidorNumber == medidorNumber.toInt()){
                Toast.makeText(this.requireContext(),"ERROR: Ya exíste este MEDIDOR", Toast.LENGTH_SHORT).show()
                mDialogView.medidorNumber_editText_costumer.error = "ya existe"
                return false
            }
        }
        return true
    }

    private fun create(userCostumerName:String, medidorNumber:String, medidorSerial:String, userCostumerRut:String, geoSwitchState:Boolean,lastLocation: Location) {
        //F03. Cargando datos a Firebase
        /*F03.01 Instando Cloud Firebase*/
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(user!!.uidApr)
            .collection("userCostumer")
            .document(medidorNumber)
        /*F03.02 Asignando valores a variables*/
        val uidCostumer: String         = medidorNumber
        val uidApr              = user!!.uidApr
        val userCostumerEmail           = ""
        val userCostumerPhone           = ""
        val userCostumerDir             = "calle, número , ${user!!.userAprLocalidad}"
        val medidorLocation= mapOf("Latitude" to lastLocation.latitude, "Longitude" to lastLocation.longitude)

        val dateMedidorRegister         = Calendar.getInstance().time
        val typeUser            = 3                /**tipo de usuario  administrador=1, apr=2, costumer=3*/
        val userStatus          = 1 /*1 activo 0 inactivo*/
        val userCostumerDebt    = 0.0

        /*F.03.03 Cargando object UserCostumer*/
        val costumer = Costumer(
            uidCostumer,
            uidApr,
            userCostumerName,
            userCostumerEmail,
            userCostumerPhone,
            userCostumerDir,
            userCostumerRut,
            medidorLocation,
            medidorNumber.toInt(),
            medidorSerial,
            dateMedidorRegister,
            typeUser,
            userStatus,
            userCostumerDebt,
            dateMedidorRegister
        )

        //F.03.04 Subiendo datos Consumidor a Firestore
        ref.set(costumer)
            .addOnSuccessListener {
                Log.d("Register Costumer", "Guardado en Firestore")
                //arrancar actividad de login
                Toast.makeText(
                    this.requireContext(),
                    "Cliente N°$medidorNumber agregado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Log.d("Register Costumer", "Fallo Guardado en Firestore")
                Toast.makeText(
                    this.requireContext(),
                    "Error en guardar cliente $medidorNumber",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun checkRemainingCostumers(){
        //F05. descargando límite de suscripción "planId"
        val ref = FirebaseFirestore.getInstance().collection("suscriptionPlan")
        ref.document(user!!.planId.toString()).get()
            .addOnSuccessListener {
                if(it!=null){
                    val plan = it.toObject(SuscriptionPlan::class.java)
                    plan?.let {plan ->
                        currentCostumerLimit = plan.limit.toShort()
                    }?:run{
                        currentCostumerLimit = 1
                    }
                    Log.d("Suscription", "límite de arranques permitidos:$currentCostumerLimit ")

                }

            }
            .addOnFailureListener { e ->
                Log.d("Suscription", "Error getting documents: ", e)
            }
    }


    //F04 : Obteniendo si locación está activada

    private fun getLocation(geoSwitchState:Boolean):Map<String,Double>{
        /* https://www.androdocs.com/kotlin/getting-current-location-latitude-longitude-in-android-using-kotlin.html */

        return if(geoSwitchState){
            getLastLocation()
            Log.d("Register", "GPS: Lat: ${lastLocation.latitude}, Long: ${lastLocation.longitude}")
            mapOf("Latitude" to lastLocation.latitude, "Longitude" to lastLocation.longitude)
        }else{
            mapOf("Latitude" to 0.00, "Longitude" to 0.00)
        }
    }

    private fun getLocationManual():String?{
        getLastLocation().also {
            return ("Lat ${lastLocation.latitude} Lon:${lastLocation.longitude}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(){

        if (gpsPermissionsChecking()) {
            if (gpsIsEnable()) {
                fusedLocation.lastLocation.addOnCompleteListener() { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()/* buscar datos de localización */
                    }
                    else{
                        Log.d("Register", "ubicación: $location")
                        lastLocation = location
                    }
                }
            }
            else {
                Toast.makeText(this.requireContext(), "Active el GPS", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else {
            gpsPermissionsRequest()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority           = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval           = 0
        mLocationRequest.fastestInterval    = 0
        mLocationRequest.numUpdates         = 1
        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this.requireContext())
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            lastLocation= locationResult.lastLocation
        }
    }

    private fun gpsIsEnable(): Boolean {
        val locationManager: LocationManager = this.requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun gpsPermissionsChecking(): Boolean {
        if (ActivityCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) { return true }
        return false
    }

    private fun gpsPermissionsRequest() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation(geoSwitchState = true)

                //Gps(requireActivity()).getLocation(true)
            }
        }
    }







} /**Fin class*/
