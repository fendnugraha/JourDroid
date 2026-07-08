package com.example.jourdroid.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // UUID standar untuk SPP (Serial Port Profile) pada printer thermal
    private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    // 1. Fungsi mengambil daftar printer yang sudah PAIRED (terhubung) di HP
    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        val pairedDevices = bluetoothAdapter?.bondedDevices
        return pairedDevices?.filter { device ->
            // Filter perangkat yang sekiranya berupa printer atau audio/peripheral thermal
            device.bluetoothClass?.majorDeviceClass == 1536 || device.name.lowercase().contains("printer")
        } ?: emptyList()
    }

    // 2. Fungsi menghubungkan HP ke Printer
    @SuppressLint("MissingPermission")
    fun connectToPrinter(device: BluetoothDevice): Boolean {
        return try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 3. Fungsi cetak teks struk (Format ESC/POS dasar)
    fun printText(text: String) {
        try {
            outputStream?.let { stream ->
                // Mengubah text string menjadi byte array biasa
                stream.write(text.toByteArray(charset("GBK")))
                // Beri jarak kertas di akhir cetakan (Feed lines)
                stream.write("\n\n\n".toByteArray())
                stream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 4. Fungsi putus koneksi printer
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}