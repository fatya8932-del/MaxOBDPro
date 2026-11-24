package com.garage.maxobdpro

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class KonnweiBluetoothManager {
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    suspend fun connectToKonnwei(deviceNameKeyword: String = "KONNWEI"): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext false
            val device = adapter.bondedDevices.firstOrNull {
                it.name.contains(deviceNameKeyword, ignoreCase = true) || it.name.contains("OBD", ignoreCase = true)
            } ?: return@withContext false
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket?.connect()
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            sendCommand("AT Z")
            delay(800)
            sendCommand("AT E0")
            sendCommand("AT L0")
            sendCommand("AT H1")
            sendCommand("AT SP 0")
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun sendCommand(cmd: String): String = withContext(Dispatchers.IO) {
        try {
            val out = outputStream ?: return@withContext "ERR_NO_CONN"
            val inp = inputStream ?: return@withContext "ERR_NO_CONN"
            while (inp.available() > 0) inp.read()
            out.write((cmd.trim() + "\r").toByteArray(Charsets.US_ASCII))
            out.flush()
            val sb = StringBuilder()
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 2500) {
                if (inp.available() > 0) {
                    val c = inp.read().toChar()
                    if (c == '>') break
                    sb.append(c)
                } else {
                    delay(20)
                }
            }
            return@withContext sb.toString().replace("\r", " ").replace("\n", " ").trim()
        } catch (e: Exception) {
            return@withContext "ERR: ${e.message}"
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {}
    }
}
