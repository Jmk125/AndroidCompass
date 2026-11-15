package com.example.compass

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build

object Permissions {
    fun hasFine(context: Context) =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasBackground(context: Context) = Build.VERSION.SDK_INT < 29 ||
        context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Foreground-service type bits; the location bit is only added when it's allowed to be used. */
    fun fgsType(withLocation: Boolean): Int {
        var type = 0
        if (Build.VERSION.SDK_INT >= 34) type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        if (withLocation && Build.VERSION.SDK_INT >= 29) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        return type
    }
}
