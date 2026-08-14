package com.example.jourdroid.utils

import kotlin.math.*

object LocationUtils {

    /**
     * Calculates distance between two points in km using Haversine formula
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Estimates ETA in minutes based on distance and average speed (km/h)
     * Incorporates a Road Multiplier to account for real-world road layout.
     */
    fun estimateETA(distanceKm: Double, avgSpeedKmH: Double = 20.0): Int {
        if (distanceKm <= 0) return 0
        
        // 🟢 Road Multiplier: Straight line distance is usually 1.4x longer on actual roads
        val realRoadDistance = distanceKm * 1.4
        
        val hours = realRoadDistance / avgSpeedKmH
        
        // 🟢 Add 2 minutes for base overhead (parking/pickup)
        return (hours * 60).roundToInt() + 2
    }

    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            "${(distanceKm * 1000).toInt()} m"
        } else {
            String.format("%.1f km", distanceKm)
        }
    }
}
