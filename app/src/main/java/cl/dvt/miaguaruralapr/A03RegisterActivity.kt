package cl.dvt.miaguaruralapr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_a03_register.*
import java.util.*


class A03RegisterActivity : AppCompatActivity() {
    companion object{
        //Valor de localización
        val PERMISSION_ID = 42
        lateinit var mFusedLocationClient: FusedLocationProviderClient
        lateinit var mLastLocation:Location
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a03_register)
        //Permisos de Geolocalización
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()

        autoCompleteDataCities() /*Asistencia autocompletado de Registros de ciudades*/
        toLogin_textView_register.setOnClickListener {returnLoginActivity()} /*ir a loggin */
        register_button_register.setOnClickListener { performRegister()}    /* realizar registro */
    }/**Fin onCreate*/

    //F.01 Función Cargar Array de Comuna desde Resources
    private fun autoCompleteDataCities(){
    val cities = resources.getStringArray(R.array.comuna_nombre)
    val adapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line,cities)
    comuna_autoCompleteTextView_register.setAdapter(adapter)
    comuna_autoCompleteTextView_register.threshold=1 /**minimo de caracteres*/
    }

    //F.02 Función retornar a pantalla Login
    private fun returnLoginActivity(){
        val intent = Intent(this, A02LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    //F.03 Función Registro
    private fun performRegister(){

        /*Extrayendo variables de formulario*/
        val email       = email_editText_register.text.toString()
        val emailR      = emailR_editText_register.text.toString()
        val password    = password_editText_register.text.toString()
        val passwordR   = passwordR_editText_register.text.toString()

        //Filtrando errores de digitado en registro
        val listOfInputs = arrayOf(
            email_editText_register,
            emailR_editText_register,
            password_editText_register,
            passwordR_editText_register,
            razonSocial_editText_register,
            rol_editText_register,
            phone_editText_register,
            localidad_editText_register,
            comuna_autoCompleteTextView_register,
            dir_editText_register
        )

        for (input in listOfInputs){
            while (input.text.toString().isEmpty()){
                input.error = "vacio"
                input.requestFocus()
                return
            }
        }

        if (!email.isEmailValid()){
            email_editText_register?.error = "inválido"
            email_editText_register.requestFocus()
            return }
        while(email != emailR){
            emailR_editText_register?.error = "distinto!"
            emailR_editText_register.requestFocus()
            return }

        while(password.length < 6 ){
            password_editText_register?.error = "al menos 6 letras"
            password_editText_register.requestFocus()
            return}
        while(password != passwordR){
            passwordR_editText_register?.error = "repetir pass correctamente"
            passwordR_editText_register.requestFocus()
            return}

        register_button_register.text = "registrando..."
        //Llamando función F.04
        createNewUser(email,password)
    }

    //F.04 Registrado USER Authentication en Firebase
    private fun createNewUser(email:String, password:String){
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener
                Log.d("Register","Usuario registrado correctamente: ${it.result?.user?.uid}")
                emailVerification()
                saveUserDataOnDatabase()/*Llamando función guardar datos en base de datos*/
            }
            .addOnFailureListener{
                Log.d("Register", "Fallo la creación usuario: ${it.message}")
            }
    }

    //F.05 : Guardando datos de usuario en base de datos Firestore
    private fun saveUserDataOnDatabase(){
        //F.05.01 : declarando Firestore
        val uid = FirebaseAuth.getInstance().uid?:""
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document("$uid")
        //F.05.02 : declarando variables
        val email       = email_editText_register.text.toString()

        val razonSocial = razonSocial_editText_register.text.toString()
        val rolSociedad = rol_editText_register.text.toString() /** nombre APR */
        val phoneContact= phone_editText_register.text.toString()

        val localidad   = localidad_editText_register.text.toString()
        val comuna      = comuna_autoCompleteTextView_register.text.toString()
        val direccion   = dir_editText_register.text.toString()

        val cityIndex          = resources.getStringArray(R.array.comuna_nombre).indexOf(comuna)                               /* https://stackoverflow.com/questions/7256514/search-value-for-key-in-string-array-android*/
        val comunaArray = listOf(
            resources.getStringArray(R.array.comuna_id)[cityIndex],         //comuna id
            comuna_autoCompleteTextView_register.text.toString(),           //comuna
            resources.getStringArray(R.array.provincia_id)[cityIndex],      //provincia id
            resources.getStringArray(R.array.provincia_nombre)[cityIndex],  //provincia
            resources.getStringArray(R.array.region_id)[cityIndex],         //región id
            resources.getStringArray(R.array.region_nombre)[cityIndex])     //región
        val userLocation =  mapOf("Latitude" to mLastLocation.latitude, "Longitude" to mLastLocation.longitude) /**getLocation en F.06*/
        val dateStampRegister = Calendar.getInstance().time //formato DATE

        /** obteniendo  días de regalo*/ /* RES: https://www.mkyong.com/java/java-how-to-add-days-to-current-date/ */
        val calendarLastPurchase = Calendar.getInstance()
        calendarLastPurchase.time = dateStampRegister
        calendarLastPurchase.add(Calendar.MONTH, 3)
        val dateLimitBuy = calendarLastPurchase.time /* Calendar() to Date()*/

        val typeUser:Int    = 2    /*tipo de usuario APR administrador=1, apr=2, costumer=3*/
        val planId          = 30   /*tipo de plan suscrito 30:gratuito inicial*/
        val userStatus      = true     /* true: activo */

        //F.05.03 Cargando object UserApr
        val userAprData = AprObject(
                uid,
                email,
                razonSocial,
                rolSociedad,
                phoneContact,
                localidad,
                direccion,
                comunaArray,
                userLocation,
                dateStampRegister,
                dateLimitBuy,
                typeUser,
                planId,
                userStatus
            )

        //F.05.04 Conectando a Base de datos
        ref.set(userAprData)
            .addOnSuccessListener {
                Log.d("RegisterActivity","Guardado en Firestore $uid")
                currentApr = userAprData /* creando parceleable de usuario actual*/
                createTramo()      /* crear plan de precios del APR automático*/

                //TODO: enviar a login si no ha verificado EMAIL

                //arrancar actividad de login
                val intent = Intent(this, A02LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addOnFailureListener{
                Log.d("RegisterActivity","Fallo Guardado en Firestore")
            }

    }
    //F.05 : Creando plan de precios base del usuarios
    /* http://www.doh.gov.cl/APR/documentos/EscuelaDirigentesAPR/Tarifas.pdf */
    private fun createTramo(){
        val timestamp = Calendar.getInstance().time
        val uid = FirebaseAuth.getInstance().uid?:""
        val tramo1 =  TramoObject(
            "Tramo 1",
            0.0,
            500,
            "Precio consumo doméstrico tramo 1 hasta los 15m3",
            false,
            uid,
            UUID.randomUUID().toString(),
            timestamp
        )
        val tramo2 =  TramoObject(
            "Tramo 2",
            16.0,
            800,
            "Precio consumo doméstrico tramo 2 sobre los 16m3",
            true,
            uid,
            UUID.randomUUID().toString(),
            timestamp
        )
        val tramo3 =  TramoObject(
            "Tramo 3",
            31.0,
            1000,
            "Precio sobre consumo por sobre los 31 m3",
            true,
            uid,
            UUID.randomUUID().toString(),
            timestamp
        )

        val tramoList = listOf<TramoObject>(tramo1,tramo2,tramo3)

        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(uid)
            .collection("userPrices")


        for (item in tramoList){
            ref.document(item.uidTramo).set(item)
                .addOnSuccessListener {
                    Log.d("RegisterActivity","cargado plan ${item.name} para consumos sobre ${item.consumptionBase} m3")
                }
                .addOnFailureListener{
                    Log.d("RegisterActivity","Fallo creando plan en firestore")
                }
        }
    }

    //F.06 : Obteniendo si locación està activada
    /** https://www.androdocs.com/kotlin/getting-current-location-latitude-longitude-in-android-using-kotlin.html */
    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        if (checkLocationPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    }
                    else{
                        Log.d("Register", "ubicación: $location")
                        mLastLocation = location
                    }
                }
            }
            else {
                Toast.makeText(this, "Active GPS", Toast.LENGTH_LONG).show()
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
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            mLastLocation= locationResult.lastLocation
        }
    }
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
    private fun checkLocationPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }


    //Función AUX verificar email
    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }

    private fun emailVerification(){
        val auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("es")
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Register", "Email sent.")
                }
            }
    }


}
