package com.steppy.app.platform.sensor.permission
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object ActivityRecognitionPermission {

    const val PERMISSION : String = Manifest.permission.ACTIVITY_RECOGNITION


    //**
    //
    // Return True if the app is allowed to read StepCounter
    //  Before Android 10, the permission did not exist so return true by default
    // */

    fun isGrated (context: Context) : Boolean {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            return true
        }
        val result: Int = ContextCompat.checkSelfPermission(context, PERMISSION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun isPermanentlyDenied (activity: Activity, alreadyAsked: Boolean) : Boolean{
        if(isGrated(activity) || !alreadyAsked){
            return false
        }
        val canExplainAndAskAgain: Boolean =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION)

        return  !canExplainAndAskAgain
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

}