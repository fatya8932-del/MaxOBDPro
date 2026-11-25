package com.garage.maxobdpro

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
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
    private lateinit var btnConnect: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button
    private lateinit var btnLiveData: Button
    private lateinit var btnActiveTest: Button

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
        buildUiDirectly()
        setupBrandSpinner()
        setupButtons()
        requestBluetoothPermissions()
    }

    private fun buildUiDirectly() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(24, 24, 24, 24)
        }

        // แถบสถานะและวัดโวลต์
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 12)
        }
        tvStatus = TextView(this).apply {
            text = "สถานะ: รอเชื่อมต่อ KONNWEI"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvVoltage = TextView(this).apply {
            text = "0.0 V"
            setTextColor(Color.parseColor("#29B6F6"))
            textSize = 20f
            gravity = Gravity.END
        }
        topRow.addView(tvStatus)
        topRow.addView(tvVoltage)
        root.addView(topRow)

        // แถบแจ้งเตือนหัวสแกน
        tvAdapterAlert = TextView(this).apply {
            text = "Direct Plug: KONNWEI MAXOBD Ready"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(16, 12, 16, 12)
            textSize = 14f
        }
        root.addView(tvAdapterAlert)

        // ตัวเลือกยี่ห้อรถ
        spinnerBrand = Spinner(this).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                110
            )
            params.setMargins(0, 16, 0, 16)
            layoutParams = params
        }
        root.addView(spinnerBrand)

        // แถวปุ่มที่ 1
        val btnRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 12)
        }
        btnConnect = createBtn("เชื่อมต่อ", "#1976D2")
        btnReadDtc = createBtn("อ่านโค้ด", "#388E3C")
        btnClearDtc = createBtn("ลบโค้ด", "#D32F2F")
        btnRow1.addView(btnConnect)
        btnRow1.addView(btnReadDtc)
        btnRow1.addView(btnClearDtc)
        root.addView(btnRow1)

        // แถวปุ่มที่ 2
        val btnRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }
        btnLiveData = createBtn("ค่าสด (Live Data)", "#00838F")
        btnActiveTest = createBtn("เทสหัวฉีด (Active Test)", "#EF6C00")
        btnRow2.addView(btnLiveData)
        btnRow2.addView(btnActiveTest)
        root.addView(btnRow2)

        // หน้าต่าง Console แสดงผล
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        tvConsole = TextView(this).apply {
            text = "MaxOBDPro พร้อมทำงาน... กรุณาเลือกยี่ห้อรถและกดเชื่อมต่อ"
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 15f
        }
        scrollView.addView(tvConsole)
        root.addView(scrollView)

        setContentView(root)
    }

    private fun createBtn(label: String, colorHex: String): Button {
        return Button(this).apply {
            text = label
            setBackgroundColor(Color.parseColor(colorHex))
            setTextColor(Color.WHITE)
            textSize = 14f
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(6, 0, 6, 0)
            layoutParams = params
        }
    }

    private fun setupButtons() {
        btnConnect.setOnClickListener {
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

        btnReadDtc.setOnClickListener {
            lifecycleScope.launch {
                log("Reading DTC...")
                val res = btManager.sendCommand("03")
                log("Response: $res\n${parseDtcResponse(res)}")
            }
        }

        btnClearDtc.setOnClickListener {
            lifecycleScope.launch {
                log("Clearing DTC...")
                val res = btManager.sendCommand("04")
                log("Clear Result: $res")
            }
        }

        btnLiveData.setOnClickListener {
            lifecycleScope.launch {
                log("Reading Live Data...")
                btManager.sendCommand("AT SH 7E0")
                val rpmHex = btManager.sendCommand("010C")
                val railHex = btManager.sendCommand("221124")
                log("RPM Raw: $rpmHex\nRail Pressure Raw: $railHex")
            }
        }

        btnActiveTest.setOnClickListener {
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
                    try {
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
                    } catch (_: Exception) {}
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
        try {
            btManager.disconnect()
        } catch (_: Exception) {}
    }
}

