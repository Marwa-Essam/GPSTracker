package com.example.gpstracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import com.example.gpstracker.base.BaseAcivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task

class MainActivity : BaseAcivity(), OnMapReadyCallback {
    private  lateinit var fusedLocationClient: FusedLocationProviderClient
     var googleMap:GoogleMap?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment=supportFragmentManager.findFragmentById(R.id.map)as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        if(isGPSPermissionAllowed()){
            getUserLocation()
        }else{
            requestPermission()

        }
    }

    val requestGPSPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                getUserLocation()
            } else {
                showDialog("we can't get the nearest driver to you ,"+
                        "to use this feature allow location permission")
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
        //show explanation to the user
        //show dialog
            showDialog(message = "please enable location permission", posActionName = "Allow",
                posAction = { dialogInterface, i ->
                    dialogInterface.dismiss()
                    requestGPSPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
            negActionName = "No", negAction = { dialogInterface, i ->
                    dialogInterface.dismiss()

                }, )

     }else{
         requestGPSPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

       }
    }


    val locationCallBack:LocationCallback=object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result?:return
            for(location in result.locations){
                Log.e("location update",""+location.latitude + ""+location.longitude)
                userLocation=location
                drawUserMarkerOnMap()

            }
        }

    }
    val loctionRequest =LocationRequest.create().apply {
        interval=10000
        fastestInterval=5000
        priority=LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    val REQUEST_LOCATION_CODE=120

    @SuppressLint("MissingPermission")
    fun getUserLocation(){
        val builder=LocationSettingsRequest.Builder().addLocationRequest(loctionRequest)
        val client:SettingsClient=LocationServices.getSettingsClient(this)
        val task:Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
        fusedLocationClient.requestLocationUpdates(loctionRequest,locationCallBack,Looper.getMainLooper())
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                try {
                    exception.startResolutionForResult(this@MainActivity,REQUEST_LOCATION_CODE)
                } catch (sendEX: IntentSender.SendIntentException){

                }
            }
        }
      @Suppress("DEPRECATION")
      fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode ==REQUEST_LOCATION_CODE){
                if (resultCode== RESULT_OK){
                    getUserLocation()
                }

            }
        }
//        fusedLocationClient.lastLocation.addOnSuccessListener {location: Location?->
//            if (location==null){
//                Log.e("location","null")
//                return@addOnSuccessListener
//
//            }
//            Log.e("lat",""+location.latitude)
//            Log.e("long",""+location.longitude)
//            Log.e("speed",""+location.speed)
//        }
//        Toast.makeText(this,"we can access user location",Toast.LENGTH_LONG)
//            .show()

    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallBack)
    }

    fun isGPSPermissionAllowed():Boolean{
        return  ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED
    }

    override fun onMapReady(googleMap: GoogleMap) {

        this.googleMap=googleMap
        drawUserMarkerOnMap()
    }
    var userLocation: Location?=null

    private fun drawUserMarkerOnMap() {
        if (userLocation==null)return
        if (googleMap==null) return
        val ltlng=LatLng(userLocation?.latitude?:0.0,userLocation?.longitude?:0.0)
        val markerOption =MarkerOptions()
        markerOption.position(ltlng) //get this from user location
        markerOption.title("current location")
        googleMap?.addMarker(markerOption)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(ltlng,12.0f))
    }
}