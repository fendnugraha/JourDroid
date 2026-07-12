package com.example.jourdroid.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
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
        return try {
            val pairedDevices = bluetoothAdapter?.bondedDevices
            pairedDevices?.filter { device ->
                val deviceName = device.name?.lowercase() ?: ""
                // Filter perangkat yang sekiranya berupa printer atau audio/peripheral thermal
                device.bluetoothClass?.majorDeviceClass == 1536 || deviceName.contains("printer")
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 2. Fungsi menghubungkan HP ke Printer
    @SuppressLint("MissingPermission")
    fun connectToPrinter(device: BluetoothDevice): Boolean {
        // Coba hingga 2 kali jika gagal (Seringkali percobaan kedua berhasil setelah stack reset)
        for (attempt in 1..2) {
            try {
                // 1. Matikan discovery
                bluetoothAdapter?.cancelDiscovery()
                
                // 2. Beri jeda agar hardware siap
                Thread.sleep(1000)

                // 3. Bersihkan sisa koneksi
                disconnect()

                var success = false
                
                // Strategi 1: Insecure RFCOMM (Paling kompatibel)
                try {
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(PRINTER_UUID)
                    bluetoothSocket?.connect()
                    success = true
                } catch (e: Exception) {
                    disconnect()
                    // Strategi 2: Reflection Port 1 (Fix umum untuk 'read failed')
                    try {
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        bluetoothSocket = m.invoke(device, 1) as BluetoothSocket
                        bluetoothSocket?.connect()
                        success = true
                    } catch (e2: Exception) {
                        disconnect()
                        // Strategi 3: Secure RFCOMM
                        try {
                            bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
                            bluetoothSocket?.connect()
                            success = true
                        } catch (e3: Exception) {
                            success = false
                        }
                    }
                }

                if (success) {
                    outputStream = bluetoothSocket?.outputStream
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                disconnect()
            }
            // Jika percobaan pertama gagal, tunggu sebentar sebelum coba lagi
            Thread.sleep(1000)
        }
        return false
    }

    @SuppressLint("MissingPermission")
    suspend fun testConnection(device: BluetoothDevice): Result<Unit> {
        // Mirip dengan connectToPrinter, coba hingga 2 kali
        for (attempt in 1..2) {
            try {
                bluetoothAdapter?.cancelDiscovery()
                Thread.sleep(1000)
                
                var socket: BluetoothSocket? = null
                var success = false

                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(PRINTER_UUID)
                    socket.connect()
                    success = true
                } catch (e: Exception) {
                    socket?.close()
                    try {
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, 1) as BluetoothSocket
                        socket.connect()
                        success = true
                    } catch (e2: Exception) {
                        socket?.close()
                        try {
                            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
                            socket.connect()
                            success = true
                        } catch (e3: Exception) {
                            success = false
                        }
                    }
                }

                if (success && socket != null) {
                    socket.close()
                    Thread.sleep(1000) // Jeda lebih lama setelah test agar port benar-benar bebas
                    return Result.success(Unit)
                }
            } catch (e: Exception) {
                if (attempt == 2) return Result.failure(e)
            }
            Thread.sleep(1000)
        }
        return Result.failure(Exception("Koneksi gagal setelah beberapa kali percobaan. Pastikan printer tidak sedang terhubung ke perangkat lain."))
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

    fun printBytes(bytes: ByteArray) {
        try {
            outputStream?.let { stream ->
                stream.write(bytes)
                stream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateReceiptBytes(journalData: JournalData, agentName: String?, warehouseName: String?): ByteArray {
        val bytes = ArrayList<Byte>()

        // ESC/POS Command Constants
        val initPrinter = byteArrayOf(0x1B, 0x40)
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01)
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00)
        val alignRight = byteArrayOf(0x1B, 0x61, 0x02)
        val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
        val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
        val doubleHeightOn = byteArrayOf(0x1B, 0x21, 0x10)
        val textNormal = byteArrayOf(0x1B, 0x21, 0x00)
        val lf = byteArrayOf(0x0A)

        fun writeText(text: String) {
            bytes.addAll(text.toByteArray(charset("GBK")).toList())
        }

        fun writeLine(text: String) {
            writeText(text)
            bytes.addAll(lf.toList())
        }

        fun writeCommand(cmd: ByteArray) {
            bytes.addAll(cmd.toList())
        }

        // 1. Initialize
        writeCommand(initPrinter)

        // 2. Header
        writeCommand(alignCenter)
        writeCommand(doubleHeightOn)
        writeCommand(boldOn)
        writeLine("BRILINK THREEKOMUNIKA")
        writeCommand(boldOff)
        writeCommand(textNormal)
        writeLine("Penambahan Kas")
        writeLine(journalData.dateIssued)
        writeLine("================================")
        writeCommand(lf)

        // 3. Body
        writeCommand(alignLeft)
        writeLine("No. Journal : ${journalData.id}")
        writeLine("Pengirim    : ${agentName ?: "Staff"}")
        writeLine("Kurir       : Nurjaelani")
        writeLine("Tujuan      :")
        writeCommand(alignRight)
        writeLine(journalData.debt?.warehouse?.name ?: "Cabang")
        writeCommand(alignLeft)

        writeLine("--------------------------------")
        writeCommand(alignRight)
        writeCommand(doubleHeightOn)
        writeCommand(boldOn)
        writeLine(formatRupiah(journalData.amount))
        writeCommand(boldOff)
        writeCommand(textNormal)
        writeCommand(alignLeft)
        writeLine("--------------------------------")

        writeLine("Note: ${journalData.description}")
        writeCommand(lf)

        // 4. Status
        writeCommand(alignCenter)
        writeLine("Ttd Penerima")
        writeCommand(lf)
        writeCommand(lf)
        writeCommand(lf)
        writeCommand(lf)

        // 5. Footer
        writeCommand(alignCenter)
        writeLine("Hitung sebelum diterima")
        writeLine("TERIMA KASIH")
        writeCommand(lf)
        writeCommand(lf)
        writeCommand(lf) // Feeds for cutting

        return bytes.toByteArray()
    }

    // 4. Fungsi putus koneksi printer
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream = null
            bluetoothSocket = null
        }
    }
    
    
}