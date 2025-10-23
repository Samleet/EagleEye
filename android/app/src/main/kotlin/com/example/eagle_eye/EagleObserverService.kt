package com.techsoft.eagle_eye

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
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

class EagleObserverService : Service() {

    private var lastUri: String? = null

    private lateinit var imageObserver: ContentObserver
    private lateinit var videoObserver: ContentObserver
    

    override fun onCreate() {
        super.onCreate()
        
        startForegroundService()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "eagle_observer"
        val channelName = "Eagle Observer Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Eagle Eye Running...")
            .setContentText("Monitoring new photos and videos")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1, notification)
        registerMediaObservers()
        scheduleServiceMonitor()
    }

    private fun registerMediaObservers() {
        Log.d("EagleEye", "Registering Observers...")
        val handler = Handler(Looper.getMainLooper())
        val intent = Intent("com.techsoft.eagle_eye.MEDIA_EVENT")

        // Observe new images
        if (!this::imageObserver.isInitialized) {
            imageObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    Log.d("EagleEye", "imageObserver fired!")
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
        }

        // Observe new videos
        if (!this::videoObserver.isInitialized) {
            videoObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    Log.d("EagleEye", "videoObserver fired!")

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
        }
    }

    private fun scheduleServiceMonitor() {
        val work = PeriodicWorkRequestBuilder<RestartWorker>(
            15, TimeUnit.MINUTES // Run every 15 minutes
        ).build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "EagleRestartWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
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

        // ✅ Safely unregister observers if initialized
        if (this::imageObserver.isInitialized)
            contentResolver.unregisterContentObserver(imageObserver)

        if (this::videoObserver.isInitialized)
            contentResolver.unregisterContentObserver(videoObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

}
