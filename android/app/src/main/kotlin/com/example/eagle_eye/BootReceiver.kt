package com.techsoft.eagle_eye

import android.os.Build
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import android.util.Log
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("EagleEye", "Booting service...")

            val serviceIntent = Intent(context, EagleObserverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
        }
    }
    
}
