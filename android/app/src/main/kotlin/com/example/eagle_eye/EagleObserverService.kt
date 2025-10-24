package com.techsoft.eagle_eye

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import java.io.File
import android.database.Cursor
import java.io.InputStream
import java.io.OutputStream
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock

class EagleObserverService : Service() {

    private var lastUri: String? = null
    private var observerRegistered = false
    private lateinit var imageObserver: ContentObserver
    private lateinit var videoObserver: ContentObserver

    override fun onCreate() {
        super.onCreate()
        Log.d("EagleEye", "onCreate called — registering service")

        startForegroundService()
        registerMediaObservers()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("EagleEye", "onStart called — registering sticky")
        registerMediaObservers()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val channelId = "eagle_observer"
        val channelName = "Eagle Observer Service"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )

            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Eagle Eye Running...")
            .setContentText("Monitoring new photos and videos")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1, notification)
    }

    private fun registerMediaObservers() {
        val handler = Handler(Looper.getMainLooper())
        val intent = Intent("com.techsoft.eagle_eye.MEDIA_EVENT")

        if (observerRegistered == false) {
            // Observe new images
            imageObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)

                    uri?.let {
                        val now = System.currentTimeMillis()
                        val path = getRealPathFromUri(it)
                        val sameAsLast = (it.toString() == lastUri)
                        val method = "eagle.media";
                        
                        val json = JSONObject(
                            mapOf(
                            "method" to method, 
                            "data" to path
                        )).toString()
                        Log.e("data", json)

                        if (sameAsLast == true) {
                            return
                        }

                        lastUri = it.toString()

                        Log.d("EagleObserver", "📸 New image detected: $uri")

                        dispatchWorkManager(
                            method, 
                            path
                        )
                        
                        intent.putExtra("data", json)
                        sendBroadcast(intent)
                    }
                }
            }

            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imageObserver
            )

            // Observe new videos
            videoObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)

                    uri?.let {
                        val now = System.currentTimeMillis()
                        val path = getRealPathFromUri(it)
                        val sameAsLast = (it.toString() == lastUri)
                        val method = "eagle.media";

                        val json = JSONObject(
                            mapOf(
                            "method" to method, 
                            "data" to path
                        )).toString()
                        Log.e("data", json)

                        if (sameAsLast == true) {
                            return
                        }

                        lastUri = it.toString()

                        Log.d("EagleObserver", "🎥 New video detected: $uri")

                        dispatchWorkManager(
                            method, 
                            path
                        )

                        intent.putExtra("data", json)
                        sendBroadcast(intent)
                    }
                }
            }

            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoObserver
            )

            observerRegistered = true
        }
    }

    private fun dispatchWorkManager(method: String, value: Any?) {
        // 🔹 Call WorkManager
        val work = OneTimeWorkRequestBuilder<EagleWorker>()
            .setInputData(
                workDataOf(
                    "method" to method,
                    "data" to value
                )
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueue(work)
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        var filePath: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
    
        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val columnIndex = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DATA
                )
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            filePath = uri.toString();
        }
    
        return filePath
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("EagleEye", "onDestroy called — unregistering observer")

        try {
            
            if (this::imageObserver.isInitialized)
            contentResolver.unregisterContentObserver(imageObserver)

            if (this::imageObserver.isInitialized)
            contentResolver.unregisterContentObserver(videoObserver)

        } catch(e: Exception) {

            val er: String? = e.message;
            Log.d("EagleEye", "Fail to unregistering observer: $er")

        }
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.d("EagleEye", "onTaskRemoved — restarting service")

        observerRegistered = false;

        if (this::imageObserver.isInitialized)
        contentResolver.unregisterContentObserver(imageObserver)

        if (this::videoObserver.isInitialized)
        contentResolver.unregisterContentObserver(videoObserver)

        val serviceIntent = Intent(
            applicationContext, EagleObserverService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(serviceIntent)
        } else {
            applicationContext.startService(serviceIntent)
        }

    }  

}
