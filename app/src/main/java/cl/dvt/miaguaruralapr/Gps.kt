package cl.dvt.miaguaruralapr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.location.*

class Gps(
    val context: Context
) {
    lateinit var lastLocation:Location
    lateinit var fusedLocation: FusedLocationProviderClient

    fun getLocation(geoSwitchState:Boolean):Map<String,Double>{
        return if(true){
            getLastLocation()
            Log.d("Gps", "location detected")
            mapOf("Latitude" to (lastLocation.latitude ?:0.0), "Longitude" to (lastLocation.longitude
                ?:0.0))
        }else{
            mapOf("Latitude" to 0.0, "Longitude" to 0.0)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        lastLocation = Location("0")
        fusedLocation = LocationServices.getFusedLocationProviderClient(context)

        if (checkLocationPermissions()) {

            if (isLocationEnabled()) {
                fusedLocation.lastLocation.addOnCompleteListener { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()/* buscar datos de localización */
                    }
                    else{
                        lastLocation = location
                        Log.d("Gps", "ubicación: $location")
                    }
                }
            }
            else {
                Toast.makeText(context, "Active el GPS", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(context,intent,null)
            }
        }
        else {
            gpsPermissionsRequest()
        }

    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest()
        with(locationRequest){
            priority           = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval           = 0
            fastestInterval    = 0
            numUpdates         = 1
        }
        val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
        fusedLocation!!.requestLocationUpdates(
            locationRequest, locationCallback, Looper.myLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            lastLocation= locationResult.lastLocation
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkLocationPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) { return true }
        return false
    }

    private fun gpsPermissionsRequest() {
        /* https://youtu.be/rdfjT0bQBgs permisos en kotlin*/
        ActivityCompat.requestPermissions(MainActivity(), arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            RegisterActivity.PERMISSION_ID
        )
    }


}