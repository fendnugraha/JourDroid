package com.example.jourdroid.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    // Fungsi untuk mendapatkan tanggal hari ini di Jakarta dengan format YYYY-MM-DD
    @RequiresApi(Build.VERSION_CODES.O)
    fun getTodayJakartaFormat(): String {
        val jakartaZone = ZoneId.of("Asia/Jakarta")
        val todayInJakarta = ZonedDateTime.now(jakartaZone).toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return todayInJakarta.format(formatter)
    }
}