package com.example.jourdroid.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object LocationHelper {
    private const val TAG = "LocationHelper"
    
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return try {
            // Priority.PRIORITY_HIGH_ACCURACY is better for delivery tracking
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            if (location == null) {
                Log.w(TAG, "getCurrentLocation: location is null")
            }
            location
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentLocation: error occurred", e)
            null
        }
    }
}
