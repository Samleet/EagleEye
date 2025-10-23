package com.techsoft.eagle_eye

import android.Manifest
import android.os.Build
import android.content.Intent
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class RestartWorker(
    context: Context, 
    params: WorkerParameters) : Worker(context, params) {    
    
    override fun doWork(): Result {
        Log.d("EagleEye", "Restaring service...")

        val serviceIntent = Intent(applicationContext, EagleObserverService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(serviceIntent)
        } else {
            applicationContext.startService(serviceIntent)
        }

        return Result.success()
    }
}
