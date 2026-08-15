package com.example.jourdroid.utils

import java.text.NumberFormat
import java.util.Locale

object FormatterUtils {

    /**
     * Mengubah Double/Int menjadi format Rupiah modern
     * Contoh: 15000000.0 -> "Rp 15.000.000"
     */
    fun formatRupiah(amount: Long?): String {
        if (amount == null) return "Rp 0"
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            format.format(amount).replace(",00", "").replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp 0"
        }
    }

    fun formatRupiah(amount: Int?): String {
        return formatRupiah(amount?.toLong())
    }

    /**
     * Mengubah Angka biasa menjadi ber-titik ribuan (Tanpa embel-embel Rp)
     * Contoh: 15000 -> "15.000"
     */
    fun formatNumberWithDots(number: Int?): String {
        return formatNumberWithDots(number?.toLong())
    }

    fun formatNumberWithDots(number: Long?): String {
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
            if (timeStr.contains("T")) {
                timeStr.substring(11, 19)
            } else if (timeStr.length >= 19) {
                timeStr.substring(11, 19)
            } else {
                timeStr
            }
        } catch (e: Exception) {
            timeStr
        }
    }

    /**
     * Mengubah format ISO 8601 atau YYYY-MM-DD menjadi format yang lebih rapi dengan Nama Hari
     * Contoh: "2026-08-15T09:59:09.778372Z" -> "Sabtu, 15 Agustus 2026"
     */
    fun formatLongDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "-"
        return try {
            val date = if (dateStr.contains("T")) {
                // ISO format: "2026-08-15T09:59:09.778372Z"
                val cleanedStr = dateStr.substring(0, 19).replace("T", " ")
                val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                isoFormat.parse(cleanedStr)
            } else {
                // YYYY-MM-DD format
                val shortFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                shortFormat.parse(dateStr)
            }
            
            val outputFormat = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            outputFormat.format(date!!)
        } catch (e: Exception) {
            formatShortDate(dateStr)
        }
    }
}