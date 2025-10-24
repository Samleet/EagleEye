package com.techsoft.eagle_eye

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.content.Intent
import android.content.IntentFilter
import android.app.ActivityManager

class MainActivity: FlutterActivity() {

    private val CHANNEL = "eagle_eye_listener"
    private lateinit var channel: MethodChannel
    private val intentName = "com.techsoft.eagle_eye.MEDIA_EVENT";


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        channel = MethodChannel(
            flutterEngine!!.dartExecutor.binaryMessenger, 
            CHANNEL
        )

        val filter = IntentFilter(intentName)
        registerReceiver(
            mediaReceiver, 
            filter, 
            RECEIVER_EXPORTED
        )

        // Log.d("EagleEye", "Starting Eagle Eye service...")
        requestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mediaReceiver)
    }

    private fun startEagleService() {
        if (isServiceRunning(EagleObserverService::class.java)) {
            return
        }

        val serviceIntent = Intent(
            this, EagleObserverService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) 
            as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }

        return false
    }

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val data = intent.getStringExtra("data")
            val json = JSONObject(data)

            val method = json.getString("method") //get method
            val value = json.getString("data") //get data

            try {
                channel.invokeMethod(method, value)
            } catch (e: Exception) {
                Log.e("method", "Channel not available (app closed)", e)
            }
        }
    }

    private fun requestPermissions() {
        val permissions = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            arrayOf(
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.ACCESS_MEDIA_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            arrayOf(
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_MEDIA_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }

        ActivityCompat.requestPermissions(this, permissions, 100)   
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, 
            grantResults)
        
        permissions.forEachIndexed { index, permission ->
            val result = if (
                grantResults[index] == PackageManager.PERMISSION_GRANTED
            )   "GRANTED" 
            else 
                "DENIED"
                
            Log.d("Permissions", "$permission -> $result")
        }

        if (requestCode == 100) {
            val granted = 
            grantResults.all {it == PackageManager.PERMISSION_GRANTED}

            if (granted) {

                startEagleService()

            } else {

                Log.e("EagleEye", "App service permissions is denied!")

            }
        }
    }

}
