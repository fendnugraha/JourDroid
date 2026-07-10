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

    @SuppressLint("MissingPermission")
    suspend fun testConnection(device: BluetoothDevice): Result<Unit> {
        return try {
            val socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            socket.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
        writeLine("JOURDROID SHOP")
        writeCommand(textNormal)
        writeCommand(boldOn)
        writeLine("BUKTI MUTASI JURNAL")
        writeCommand(boldOff)
        writeLine("================================")
        writeCommand(lf)

        // 3. Body
        writeCommand(alignLeft)
        writeLine("No. Journal : ${journalData.id}")
        writeLine("Tanggal     : ${journalData.dateIssued}")
        writeLine("Invoice No. : ${journalData.invoice}")
        writeLine("Petugas     : ${agentName ?: "Staff"}")
        writeLine("Gudang      : ${warehouseName ?: "Utama"}")
        writeLine("--------------------------------")
        writeLine("JUMLAH TOTAL: ${formatRupiah(journalData.amount)}")
        writeLine("--------------------------------")

        writeLine("Note: ${journalData.description}")

        // 4. Status
        writeCommand(alignCenter)
        writeCommand(boldOn)
        writeLine("STATUS: BERHASIL")
        writeCommand(boldOff)
        writeCommand(lf)

        // 5. Footer
        writeCommand(alignCenter)
        writeLine("Simpan resi ini sebagai")
        writeLine("bukti transaksi yang sah.")
        writeCommand(boldOn)
        writeLine("TERIMA KASIH")
        writeCommand(boldOff)
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
        }
    }
    
    
}