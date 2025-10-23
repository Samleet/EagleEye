package com.techsoft.eagle_eye

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class EagleWorker(
    context: Context, 
    params: WorkerParameters) : Worker(context, params) {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    
    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val method = inputData.getString("method") ?: "unknown"
        val data = inputData.getString("data") ?: "none"

        // Check location permission
        val hasPermission = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w("EagleWorker", "❌ Location permission not granted")
            return Result.failure()
        }

        //google-play-service
        /*
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(
            applicationContext
        )

        try {
            // Request fresh, high-accuracy location
            // var lasLoc = fusedLocationClient.lastLocation.result;
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude

                Log.d(
                "EagleEye", "📍 $method | $data | Lat=${lat}, Lng=${lng}"
                )

                // TODO: embed in metadata

                return Result.success()
            }

            return Result.failure()

        } catch (e: Exception) {

            Log.e("EagleEye", "❌ Error getting location fingerprint", e)

            return Result.failure()

        }
        */


        //device based location
        try {
            val locationManager = applicationContext.getSystemService(
                Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)

            var location: Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) 
                    ?: continue
                
                val elapsedTime = SystemClock
                    .elapsedRealtimeNanos();
                val lasLocTime = loc
                    .elapsedRealtimeNanos;
                
                // Prefer the most recent and most accurate location
                if (location == null ||
                    loc.accuracy < location.accuracy ||
                    elapsedTime - lasLocTime < 60_000_000_000L //within 60s
                ) {
                    location = loc
                }
            }

            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                Log.d(
                "EagleEye", "📍 $method | $data | Lat=${lat}, Lng=${lng}"
                )
    
                // TODO: embed in metadata

                return Result.success()
            }

            return Result.failure()

        } catch (e: Exception) {

            Log.e("EagleEye", "❌ Error getting location fingerprint", e)

            return Result.failure()

        }

    }
}
