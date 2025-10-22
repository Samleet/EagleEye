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
        registerMediaObservers()
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
            .setContentText("Monitoring new photos and videos...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1, notification)
    }

    private fun registerMediaObservers() {
        val handler = Handler(Looper.getMainLooper())
        val intent = Intent("com.techsoft.eagle_eye.MEDIA_EVENT")

        // Observe new images
        imageObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let {
                    val now = System.currentTimeMillis()
                    val path = getRealPathFromUri(it)
                    val sameAsLast = (it.toString() == lastUri)
                    val json = JSONObject(
                        mapOf(
                        "method" to "eagle.media", 
                        "data" to path
                    )).toString()

                    lastUri = it.toString()

                    if (sameAsLast == true) {
                        return
                    }

                    Log.d("EagleObserver", "📸 New image detected: $uri")

                    intent.putExtra("data", json)
                    sendBroadcast(intent)

                }
            }
        }

        // Observe new videos
        videoObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let {
                    val now = System.currentTimeMillis()
                    val path = getRealPathFromUri(it)
                    val sameAsLast = (it.toString() == lastUri)
                    val json = JSONObject(
                        mapOf(
                        "method" to "eagle.media", 
                        "data" to path
                    )).toString()

                    lastUri = it.toString()

                    if (sameAsLast == true) {
                        return
                    }

                    Log.d("EagleObserver", "🎥 New video detected: $uri")

                    intent.putExtra("data", json)
                    sendBroadcast(intent)
                }
            }
        }

        // Register observers
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imageObserver
        )

        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoObserver
        )
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

            //fallback original
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
