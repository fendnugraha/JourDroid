package com.example.jourdroid.utils

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    // Fungsi untuk mendapatkan tanggal hari ini di Jakarta dengan format YYYY-MM-DD
    fun getTodayJakartaFormat(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jakartaZone = java.time.ZoneId.of("Asia/Jakarta")
            val todayInJakarta = java.time.ZonedDateTime.now(jakartaZone).toLocalDate()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            todayInJakarta.format(formatter)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            sdf.format(Calendar.getInstance().time)
        }
    }

    fun getNowJakartaFormat(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jakartaZone = java.time.ZoneId.of("Asia/Jakarta")
            val nowInJakarta = java.time.ZonedDateTime.now(jakartaZone)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            nowInJakarta.format(formatter)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            sdf.format(Calendar.getInstance().time)
        }
    }
}
