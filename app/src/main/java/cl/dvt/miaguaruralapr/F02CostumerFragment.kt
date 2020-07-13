package cl.dvt.miaguaruralapr

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import cl.dvt.miaguaruralapr.A03RegisterActivity.Companion.PERMISSION_ID
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Class
import com.google.android.gms.location.*
import com.google.firebase.firestore.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.section_add_costumer.view.*
import kotlinx.android.synthetic.main.fragment_f02_costumer.*
import kotlinx.android.synthetic.main.section_add_costumer.*
import kotlinx.android.synthetic.main.section_add_costumer.view.location_button_costumer
import kotlinx.android.synthetic.main.section_add_costumer.view.name_editText_costumer
import java.util.*

class F02CostumerFragment : Fragment(){
    companion object{
        var costumerList                = mutableListOf<CostumerObject>()
        private var currentCostumerLimit:Short    = 0
        var COSTUMER_KEY:String         = "COSTUMER_DETAIL"
        private var costumersCount:Int  = 0
    }
    lateinit var            costumerFusedLocation: FusedLocationProviderClient
    private lateinit var    costumerLastLocation:Location

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate fragment F02Costumer
        return inflater.inflate(R.layout.fragment_f02_costumer, container, false)
    }/** Fin onCreateView */

    override fun onViewCreated(view:View,savedInstanceState: Bundle?) {
        super.onViewCreated(view,savedInstanceState)

        Log.d("CurrentUser", "User Data en Fragment: $currentApr")

        costumerLastLocation =  Location("0")   /**Inicializado lateinit y geolocación*/
        checkRemainingCostumers()                       /**Obteniendo límite de usuarios costumer del Plan actual*/

        val x:Short? = checkRemainingCostumersUp()
        Log.d("test", "test: $x")


        fetchCostumers()                            /**Cargando usuarios suscritos actualmente y cantidad*/

        //Add Costumer to database
        addcostumer_floatingActionButton_costumer.setOnClickListener{
            addCostumerCheck()
        }/* add costumer*/

    }/* onViewCreated */

    //F01. Función abrir box dialogo
    private fun addCostumerCheck(){
        /*Verify number of user*/
        Log.d("CurrentPlan", "Límite plan: $currentCostumerLimit, usados: $costumersCount")

        val costumerLimit:Short? = null


        if(currentCostumerLimit>costumersCount){
            addCostumerDialog()/*start add Costumer function*/
        }else{
            addCostumerLimitDialog() /* number or costumer sucription reach limit*/
        }
    }

    private fun addCostumerLimitDialog(){
        /** alert limit of costumers reached*/
        val mDialogViewA = LayoutInflater.from(requireActivity()).inflate(R.layout.section_add_costumer_alert, null)
        val mBuilderA = AlertDialog.Builder(requireActivity())
            .setView(mDialogViewA)
            .setTitle("Alerta de Límite")
            .setNegativeButton("OK", null)
            .setPositiveButton("AUMENTAR PLAN", null)
        mBuilderA.show()
    }

    private fun addCostumerDialog(){
        /** https://devofandroid.blogspot.com/2018/04/alertdialog-with-custom-layout-kotlin.html
         * agregar al context This "requiredContext()"
         * extraer datos de dialog: https://demonuts.com/android-custom-dialog-edittext/
         * https://code.luasoftware.com/tutorials/android/android-text-input-dialog-with-inflated-view-kotlin/ */
        //F01.01 Inflate the dialog with custom view
        val numberOfCostumersPlus1=costumersCount+1
        val mDialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.section_add_costumer, null)
        /*F01.02 AlertDialogBuilder*/
        val mBuilder = AlertDialog.Builder(requireActivity())
            .setView(mDialogView)
            .setTitle("Ingresar nuevo cliente $numberOfCostumersPlus1 de $currentCostumerLimit ")
            /*.setPositiveButton("Guardar", null)*/
            /*.setNegativeButton("Cancel", null)*/
        val  mAlertDialog = mBuilder.show()

        /*F01.03 Llamando locationServices*/
        costumerFusedLocation = LocationServices.getFusedLocationProviderClient(requireActivity())
        var geoSwitchState    = false

        /*F01.04 show map*/
        val mMapView = mDialogView.map_mapView_costumer


        //F01.5 Acción de los botones de dialoxBox
            //GPS button
        mDialogView.location_button_costumer.setOnClickListener {
            mAlertDialog.location_button_costumer.text = getLocationValueManual()
            geoSwitchState = true
        }
            //CANCEL button
        mDialogView.cancel_button_costumer.setOnClickListener {
            mAlertDialog.dismiss()
        }
            //GUARDAR button
        mDialogView.save_button_costumer.setOnClickListener{
            val userCostumerName    = mDialogView.name_editText_costumer.text.toString()
            val medidorNumber       = mDialogView.medidorNumber_editText_costumer.text.toString()
            val medidorNumberR      = mDialogView.medidorNumberR_editText_costumer.text.toString()
            val medidorSerial       = mDialogView.serial_editText_costumer.text.toString()
            val userCostumerRut     = mDialogView.rut_editText_costumer.text.toString()
            val checkInputDataVal = checkInput(mDialogView,medidorNumber,medidorNumber,medidorNumberR,geoSwitchState)

            if (checkInputDataVal){
                uploadNewCostumer(userCostumerName, medidorNumber, medidorSerial, userCostumerRut, geoSwitchState)
                mAlertDialog.dismiss()
            }else{
                return@setOnClickListener
            }
        }
    }


    //F02. Check de valores ingresados
    private fun checkInput(mDialogView:View, userCostumerName:String, medidorNumber:String, medidorNumberR:String, geoSwitchState:Boolean):Boolean{
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
    //F03. Cargando datos a Firebase
    private fun uploadNewCostumer(userCostumerName:String, medidorNumber:String, medidorSerial:String, userCostumerRut:String, geoSwitchState:Boolean) {
        /*F03.01 Instando Cloud Firebase*/
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(currentApr!!.uidApr)
            .collection("userCostumer")
            .document(medidorNumber)
        /*F03.02 Asignando valores a variables*/
        val uidCostumer: String         = medidorNumber
        val uidApr              = currentApr!!.uidApr
        val userCostumerEmail           = ""
        val userCostumerPhone           = ""
        val userCostumerDir             = "calle, número , ${currentApr!!.userAprLocalidad}"
        val medidorLocation= getLocationValue(geoSwitchState)
        val dateMedidorRegister         = Calendar.getInstance().time
        val typeUser            = 3                /**tipo de usuario  administrador=1, apr=2, costumer=3*/
        val userStatus          = 1 /*1 activo 0 inactivo*/
        val userCostumerDebt    = 0.0 /** deuda activa */
        val userCostumerLastPayDate = dateMedidorRegister

        /*F.03.03 Cargando object UserCostumer*/
        val costumer = CostumerObject(
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
                userCostumerLastPayDate
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

    //F04 : Obteniendo si locación está activada
    /* https://www.androdocs.com/kotlin/getting-current-location-latitude-longitude-in-android-using-kotlin.html */
    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        if (checkLocationPermissions()) {
            if (isLocationEnabled()) {
                costumerFusedLocation.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()/* buscar datos de localización */
                    }
                    else{
                        Log.d("Register", "ubicación: $location")
                        costumerLastLocation = location
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
            requestLocationPermissions()
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
            costumerLastLocation= locationResult.lastLocation
        }
    }
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = this.requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
    private fun checkLocationPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) { return true }
        return false
    }
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    //F05. descargando límite de suscripción "planId"
    private fun checkRemainingCostumers(){
        val ref = FirebaseFirestore.getInstance().collection("suscriptionPlan")
        ref.document(currentApr!!.planId.toString()).get()
            .addOnSuccessListener { it ->
                if(it!=null){
                    val plan = it.toObject(SuscriptionObject::class.java)
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

    private fun checkRemainingCostumersUp():Short?{
        val ref = FirebaseFirestore.getInstance().collection("suscriptionPlan")
        val listOfPlan = arrayListOf<SuscriptionObject>()
        val listOfPlanIndex = arrayListOf<Short>()

        ref.get()
            .addOnSuccessListener { result ->
                for(document in result){
                    /*Descargar Datos de Plan actual*/
                    val planDocument = document.toObject(SuscriptionObject::class.java)
                    Log.d("test", "Plan stored on Database: $planDocument")
                    listOfPlan.add(planDocument)
                    listOfPlanIndex.add(planDocument.id.toShort())

                    val y = listOfPlan.find { element -> element.id == currentApr!!.planId }
                    val z = y?.limit?.toShort()
                    Log.d("test", "plan activo actual : $y ")

                }

            }
            .addOnFailureListener { e ->

                Log.d("test", "Error getting documents: ", e)
            }


return null
    }




    //F06. fetching suscribed Costumers
    private fun fetchCostumers(){
        val uidApr = currentApr!!.uidApr

        val adapter = GroupAdapter<GroupieViewHolder>()    /* declarando el groupie adapter*/
        costumers_recyclerView_costumer.adapter = adapter   /* cargando el ReclyclerView de esta Actividad*/

        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(uidApr)
            .collection("userCostumer")
        ref
            .orderBy("medidorNumber", Query.Direction.ASCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { documents, e ->
                if (e != null) {
                    Log.w("Costumers", "Listen failed.", e)
                    return@addSnapshotListener
                }
                for (document in documents!!.documentChanges) {
                    Log.d("Costumers", "Total suscritos : $costumersCount")
                    when (document.type){
                        //Para firestore COSTUMERS AGREGADOS
                        DocumentChange.Type.ADDED ->{
                            val costumer = document.document.toObject(CostumerObject::class.java)
                            Log.d("Costumers", "Descargando: $costumer")
                            adapter.add(CostumerItemAdapter(costumer))  /*cargando datos a los items del adaptador personalizado*/
                            costumerList.add(costumer)                  /*cargando listado de números de medidor*/
                        }
                        //Para firestore COSTUMERS MODIFICADOS
                        DocumentChange.Type.MODIFIED ->{
                            /* https://stackoverflow.com/questions/50754912/firebase-firestore-document-changes */
                            val costumerChangedUid = document.document.id /* numero del medidor en el firestore */
                            Log.d("Costumers", "Modificado el costumer: $costumerChangedUid")

                            val costumerObject = document.document.toObject(CostumerObject::class.java)
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.add(document.oldIndex,CostumerItemAdapter(costumerObject))
                            adapter.notifyDataSetChanged()                   /*actualizando datos a los items del adaptador personalizado*/
                            costumerList[document.oldIndex] = costumerObject /*actualizando listado de números de medidor*/

                        }
                        //Para firestore COSTUMERS BORRADOS
                        DocumentChange.Type.REMOVED ->{
                            adapter.removeGroupAtAdapterPosition(document.oldIndex)
                            adapter.notifyItemRemoved(document.oldIndex)
                            Log.d("Costumer", "id del costumer borrado: ${document.document.id}")
                        }
                    }
                }

                costumersCount= adapter.itemCount
                Log.d("Costumers", "Listado de consumidores: $costumerList")
                numberOfCostumers_textView_costumer.text = costumersCount.toString()
                numberOfCostumers_textView_costumer.bringToFront()



            }
        /* almacenamiento datos sin conexión */
        ref.firestore.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        /* click en cliente Costumer */
        adapter.setOnItemClickListener {item, view ->
            val costumer = item as CostumerItemAdapter
            Log.d("Costumer", "abriendo detalle del costumer : ${costumer.costumer}")
            val intent = Intent(requireActivity(), A05Costumer::class.java)
            intent.putExtra(COSTUMER_KEY, costumer.costumer)
            startActivity(intent)
        }

    }/* fin fetchCostumers F06*/

    //Aux A02 Retornando valor de geolocalización
    private fun getLocationValue(geoSwitchState:Boolean):Map<String,Double>{
        val it:Map<String,Double>?
        it = if(geoSwitchState){
            getLastLocation()
            Log.d("Register", "GPS: Lat: ${costumerLastLocation.latitude}, Long: ${costumerLastLocation.longitude}")
            mapOf("Latitude" to costumerLastLocation.latitude, "Longitude" to costumerLastLocation.longitude)
        }else{
            mapOf("Latitude" to 0.00, "Longitude" to 0.00)
        }
        return it
    }

    //Aux A03 Retornando valor de geolocalización
    private fun getLocationValueManual():String?{
        getLastLocation().also {
            return ("Lat ${costumerLastLocation.latitude} Long:${costumerLastLocation.longitude}")
        }
    }
    //Aux 04 Mostrar mapa
    private fun showMapView(){

    }

} /**Fin class*/
