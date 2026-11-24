package com.garage.maxobdpro

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val btManager = KonnweiBluetoothManager()
    private lateinit var tvStatus: TextView
    private lateinit var tvVoltage: TextView
    private lateinit var tvAdapterAlert: TextView
    private lateinit var tvConsole: TextView
    private lateinit var spinnerBrand: Spinner

    private val carBrands = listOf(
        "Isuzu D-Max / MU-X (2004-2026)",
        "Toyota Vigo / Revo / Fortuner (2004-2026)",
        "Mitsubishi Triton / Pajero (2005-2023)",
        "Honda City / Civic / Jazz (2008-2026) [CAN 29-Bit]",
        "Ford Ranger / Mazda (MS-CAN) [Adapter]",
        "BMW / Benz / Europe (2001-2026)",
        "Truck 24V (Hino / Isuzu) [24V->12V]"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            tvStatus = findViewById(R.id.tvStatus)
            tvVoltage = findViewById(R.id.tvVoltage)
            tvAdapterAlert = findViewById(R.id.tvAdapterAlert)
            tvConsole = findViewById(R.id.tvConsole)
            spinnerBrand = findViewById(R.id.spinnerBrand)
            setupBrandSpinner()
            setupButtons()
            requestBluetoothPermissions()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            tvStatus.text = "Connecting KONNWEI..."
            lifecycleScope.launch {
                try {
                    val ok = btManager.connectToKonnwei("KONNWEI")
                    if (ok) {
                        tvStatus.text = "Connected: KONNWEI MAXOBD"
                        val volt = btManager.sendCommand("AT RV")
                        tvVoltage.text = volt
                        log("Connected! Voltage: $volt")
                    } else {
                        tvStatus.text = "Connection Failed (Pair Bluetooth First)"
                    }
                } catch (e: Exception) {
                    tvStatus.text = "Error: ${e.message}"
                }
            }
        }

        findViewById<Button>(R.id.btnReadDtc).setOnClickListener {
            lifecycleScope.launch {
                log("Reading DTC...")
                val res = btManager.sendCommand("03")
                log("Response: $res\n${parseDtcResponse(res)}")
            }
        }

        findViewById<Button>(R.id.btnClearDtc).setOnClickListener {
            lifecycleScope.launch {
                log("Clearing DTC...")
                val res = btManager.sendCommand("04")
                log("Clear Result: $res")
            }
        }

        findViewById<Button>(R.id.btnLiveData).setOnClickListener {
            lifecycleScope.launch {
                log("Reading Live Data...")
                btManager.sendCommand("AT SH 7E0")
                val rpmHex = btManager.sendCommand("010C")
                val railHex = btManager.sendCommand("221124")
                log("RPM Raw: $rpmHex\nRail Pressure Raw: $railHex")
            }
        }

        findViewById<Button>(R.id.btnActiveTest).setOnClickListener {
            lifecycleScope.launch {
                log("Injector #1 Cut Test (5 sec)...")
                btManager.sendCommand("AT SH 7E0")
                val resOn = btManager.sendCommand("2F 10 01 03 01")
                log("Cut ON: $resOn")
                delay(5000)
                val resOff = btManager.sendCommand("2F 10 01 00")
                log("Cut OFF (Normal): $resOff")
            }
        }
    }

    private fun setupBrandSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, carBrands)
        spinnerBrand.adapter = adapter
        spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = carBrands[pos]
                lifecycleScope.launch {
                    when {
                        selected.contains("CAN 29-Bit") -> {
                            btManager.sendCommand("AT SP 7")
                            setAlert(false, "Protocol: Honda CAN 29-Bit Ready")
                        }
                        selected.contains("MS-CAN") -> setAlert(true, "WARNING: Switch Adapter to MS-CAN (Pin 3/11)")
                        selected.contains("24V") -> setAlert(true, "DANGER: Use 24V->12V Step-Down Cable First!")
                        else -> {
                            btManager.sendCommand("AT SP 6")
                            setAlert(false, "Direct Plug: KONNWEI MAXOBD Ready")
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setAlert(isWarning: Boolean, msg: String) {
        tvAdapterAlert.text = msg
        tvAdapterAlert.setBackgroundColor(if (isWarning) 0xFFE65100.toInt() else 0xFF1B5E20.toInt())
    }

    private fun parseDtcResponse(raw: String): String {
        if (raw.contains("NO DATA") || raw.contains("43 00")) return "No Fault Codes (Normal)"
        if (raw.contains("06 53")) return "P0653: Sensor Reference Voltage B High (>5V)"
        if (raw.contains("00 87")) return "P0087: Fuel Rail Pressure Too Low"
        if (raw.contains("03 80")) return "P0380: Glow Plug / Heater Circuit"
        return "Check Hex Code Above"
    }

    private fun log(text: String) {
        tvConsole.text = "$text\n-----------------------------\n${tvConsole.text}"
    }

    private fun requestBluetoothPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 101)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        btManager.disconnect()
    }
}
