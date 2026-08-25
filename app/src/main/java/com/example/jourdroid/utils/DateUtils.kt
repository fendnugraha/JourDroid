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

    fun calculateWorkDuration(hireDate: String?): String {
        if (hireDate.isNullOrEmpty()) return "-"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(hireDate.substring(0, 10)) ?: return "-"
            val now = Calendar.getInstance()
            val hire = Calendar.getInstance().apply { time = date }

            var years = now.get(Calendar.YEAR) - hire.get(Calendar.YEAR)
            var months = now.get(Calendar.MONTH) - hire.get(Calendar.MONTH)

            if (months < 0) {
                years--
                months += 12
            }

            when {
                years > 0 -> "$years Thn, $months Bln"
                months > 0 -> "$months Bln"
                else -> "Baru Bergabung"
            }
        } catch (e: Exception) {
            "-"
        }
    }
}
