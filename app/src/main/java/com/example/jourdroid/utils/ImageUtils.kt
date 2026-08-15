package com.example.jourdroid.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    
    /**
     * Compresses and scales image to a target size (in KB)
     */
    fun compressImage(context: Context, uri: Uri, targetSizeKb: Int = 500): File {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        context.contentResolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it, null, options)
        }
        
        // Target resolution around 1080p max to save memory
        options.inSampleSize = calculateInSampleSize(options, 1080, 1080)
        options.inJustDecodeBounds = false
        
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw Exception("Failed to decode bitmap")
        
        val file = File(context.cacheDir, "compressed_attendance_${System.currentTimeMillis()}.jpg")
        
        var quality = 90
        val byteArrayOutputStream = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        
        // Iteratively reduce quality until size is below target or quality is too low
        while (byteArrayOutputStream.toByteArray().size / 1024 > targetSizeKb && quality > 15) {
            byteArrayOutputStream.reset()
            quality -= 10
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        }
        
        FileOutputStream(file).use { outputStream ->
            outputStream.write(byteArrayOutputStream.toByteArray())
            outputStream.flush()
        }
        
        Log.d("ImageUtils", "Compressed image size: ${file.length() / 1024} KB")
        
        return file
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
