package com.example.jourdroid.utils

import java.text.NumberFormat
import java.util.Locale

object FormatterUtils {

    /**
     * Mengubah Double/Int menjadi format Rupiah modern
     * Contoh: 15000000.0 -> "Rp 15.000.000"
     */
    fun formatRupiah(amount: Int): String {
        if (amount == null) return "Rp 0"
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            format.format(amount).replace(",00", "").replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp 0"
        }
    }

    /**
     * Mengubah Angka biasa menjadi ber-titik ribuan (Tanpa embel-embel Rp)
     * Contoh: 15000 -> "15.000"
     */
    fun formatNumberWithDots(number: Int?): String {
        if (number == null) return "0"
        return try {
            NumberFormat.getNumberInstance(Locale("in", "ID")).format(number)
        } catch (e: Exception) {
            "0"
        }
    }

    /**
     * Memotong Tanggal dari API Laravel agar menyisakan YYYY-MM-DD saja
     * Contoh: "2026-07-09 00:42:00" -> "2026-07-09"
     */
    fun formatShortDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "-"
        return try {
            dateStr.substring(0, 10)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatTimeOnly(timeStr: String?): String {
        if (timeStr.isNullOrEmpty()) return "-"
        return try {
            timeStr.substring(11, 19)
        } catch (e: Exception) {
            timeStr
        }
    }
}