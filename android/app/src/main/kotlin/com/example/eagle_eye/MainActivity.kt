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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class MainActivity: FlutterActivity() {

    private val CHANNEL = "eagle_eye_listener"
    private lateinit var channel: MethodChannel


    private fun startEagleService() {
        val serviceIntent = Intent(this, EagleObserverService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val data = intent.getStringExtra("data")
            val json = JSONObject(data)

            val method = json.getString("method")
            val value = json.getString("data")


            // 🔹 Call WorkManager
            val work = OneTimeWorkRequestBuilder<EagleWorker>()
            .setInputData(
                workDataOf(
                    "method" to method,
                    "data" to value
                )
            )
            .build()

            WorkManager.getInstance(context).enqueue(work)


            // Optional: Flutter UI

            try {

                channel.invokeMethod(method, value)

            } catch (e: Exception) {
                
                Log.e("method", "Channel not available (app closed)", e)

            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channel = MethodChannel(
            flutterEngine!!.dartExecutor.binaryMessenger, 
            CHANNEL
        )

        val filter = IntentFilter("com.techsoft.eagle_eye.MEDIA_EVENT")
        registerReceiver(mediaReceiver, filter, RECEIVER_EXPORTED)
    }

    override fun onStart() {
        super.onStart()
        // This runs every time the activity becomes visible
        // (including after onCreate)
        
        // Request permissions here as app is visible when asking
        requestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mediaReceiver)
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

    private fun requestForegroundServiceLocation() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION),
            101
        )
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
                ) "GRANTED" else "DENIED"
                Log.d("Permissions", "$permission -> $result")
            }


            when(requestCode) {
                100 -> {
                    val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                    if (allGranted) {
                        // requestForegroundServiceLocation()
                        startEagleService()
                    }
                }
        
                // 101 -> {
                //     if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                //         Log.d("Permissions", "FOREGROUND_SERVICE_LOCATION granted, starting service")
                //         startEagleService()
                //     } else {
                //         Log.e("Permissions", "FOREGROUND_SERVICE_LOCATION denied! Cannot start FGS")
                //     }
                // }
            }

    }
}
