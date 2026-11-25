package com.garage.maxobdpro

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
    private lateinit var btnDemo: Button
    private lateinit var btnReadDtc: Button
    private lateinit var btnClearDtc: Button
    private lateinit var btnLiveData: Button
    private lateinit var btnActiveTest: Button

    private var isDemoMode = false

    private val carBrands = listOf(
        "Isuzu D-Max / MU-X (2004-2026)",
        "Toyota Vigo / Revo / Fortuner (2004-2026)",
        "Mitsubishi Triton / Pajero (2005-2023)",
        "Honda City / Civic / Jazz (2008-2026) [CAN 29-Bit]",
        "Ford Ranger / Mazda (MS-CAN) [Adapter]",
        "BMW / Benz / Europe (2001-2026)",
        "Truck 24V (Hino / Isuzu) [24V->12V]"
    )

    // ฐานข้อมูลรหัสโค้ดเสียภาษาไทย (รวม Isuzu, Toyota, Mitsubishi และมาตรฐาน OBD2)
    private val dtcDatabase = mapOf(
        "P0087" to "แรงดันรางคอมมอนเรลต่ำเกินไป (เช็กวาล์ว SCV / กรองโซล่า / ปั๊มติ๊ก / ท้ายราง)",
        "P0088" to "แรงดันรางคอมมอนเรลสูงเกินไป (เช็กวาล์ว SCV ค้าง / เซ็นเซอร์ท้ายราง)",
        "P0089" to "การทำงานของวาล์วควบคุมแรงดันน้ำมันผิดปกติ (SCV Valve - Mitsubishi/Isuzu)",
        "P0093" to "ตรวจพบการรั่วไหลของน้ำมันเชื้อเพลิงแรงดันสูงในระบบราง (หัวฉีดรั่วไหลกลับเยอะ)",
        "P0102" to "สัญญาณเซ็นเซอร์แอร์โฟลว์ (MAF) ต่ำเกินไป / ปลั๊กหลุด / สายขาด",
        "P0103" to "สัญญาณเซ็นเซอร์แอร์โฟลว์ (MAF) สูงเกินไป",
        "P0113" to "เซ็นเซอร์อุณหภูมิไอดี (IAT) ขาดวงจร / แรงดันสูงเกิน",
        "P0118" to "เซ็นเซอร์อุณหภูมิน้ำหล่อเย็น (ECT) ขาดวงจร / แรงดันสูงเกิน",
        "P0192" to "สัญญาณเซ็นเซอร์แรงดันราง (FRP) ต่ำเกินไป",
        "P0193" to "สัญญาณเซ็นเซอร์แรงดันราง (FRP) สูงเกินไป / สายช็อต",
        "P0234" to "บูสต์เทอร์โบสูงเกินกำหนด (Overboost - เช็กครีบเทอร์โบแปรผัน / โซลินอยด์คุมบูสต์)",
        "P0299" to "บูสต์เทอร์โบต่ำกว่ากำหนด (Underboost - ท่ออินเตอร์รั่ว / แวคคั่มรั่ว)",
        "P0335" to "เซ็นเซอร์ตำแหน่งเพลาข้อเหวี่ยง (CKP) ผิดปกติ",
        "P0340" to "เซ็นเซอร์ตำแหน่งเพลาลูกเบี้ยว (CMP) ผิดปกติ",
        "P0380" to "วงจรหัวเผา / รีเลย์หัวเผาขัดข้อง (Isuzu / Toyota / Mitsubishi)",
        "P0401" to "ปริมาณการไหลวนไอเสีย (EGR) น้อยเกินไป (ท่อ EGR ตัน)",
        "P0403" to "วงจรโซลินอยด์ / มอเตอร์วาล์ว EGR ขัดข้อง",
        "P0404" to "ตำแหน่งวาล์ว EGR ค้างหรือทำงานผิดปกติ",
        "P0500" to "เซ็นเซอร์ความเร็วรถ (VSS) ไม่มีสัญญาณ",
        "P0653" to "แรงดันไฟเลี้ยงเซ็นเซอร์ชุด B สูงเกิน 5V (มีสายไฟช็อตลงบวก)",
        "P1229" to "ปั๊มเชื้อเพลิงแรงดันสูงจ่ายน้ำมันเกิน (Toyota Vigo/Fortuner - เช็ก SCV)",
        "P1604" to "เครื่องยนต์สตาร์ทติดยากผิดปกติ (Toyota)",
        "P2121" to "เซ็นเซอร์ตำแหน่งขาคันเร่งไฟฟ้าชุดที่ 1 ผิดปกติ"
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

        // ตัวเลือกยี่ห้อรถ (ปรับสีเข้มอ่านง่าย)
        spinnerBrand = Spinner(this).apply {
            setBackgroundColor(Color.parseColor("#2C2C2C"))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                115
            )
            params.setMargins(0, 16, 0, 16)
            layoutParams = params
        }
        root.addView(spinnerBrand)

        // แถวปุ่มที่ 1: เชื่อมต่อ / โหมดจำลอง / อ่านโค้ด / ลบโค้ด
        val btnRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 12)
        }
        btnConnect = createBtn("เชื่อมต่อจริง", "#1976D2")
        btnDemo = createBtn("โหมดจำลอง (Demo)", "#6A1B9A")
        btnReadDtc = createBtn("อ่านโค้ด", "#388E3C")
        btnClearDtc = createBtn("ลบโค้ด", "#D32F2F")
        btnRow1.addView(btnConnect)
        btnRow1.addView(btnDemo)
        btnRow1.addView(btnReadDtc)
        btnRow1.addView(btnClearDtc)
        root.addView(btnRow1)

        // แถวปุ่มที่ 2: ค่าสด / เทสตัดหัวฉีด
        val btnRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }
        btnLiveData = createBtn("ค่าสด (Live Data)", "#00838F")
        btnActiveTest = createBtn("เทสตัดหัวฉีด #1", "#EF6C00")
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
            text = "MaxOBDPro v2.0 พร้อมทำงาน\n• หากยังไม่ต่อรถ กดปุ่ม 'โหมดจำลอง (Demo)' เพื่อทดสอบระบบได้ทันที"
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
            val params = LinearLayout.LayoutParams(0, 110, 1f)
            params.setMargins(6, 0, 6, 0)
            layoutParams = params
        }
    }

    private fun setupButtons() {
        btnConnect.setOnClickListener {
            isDemoMode = false
            tvStatus.text = "กำลังเชื่อมต่อ KONNWEI..."
            lifecycleScope.launch {
                try {
                    val ok = btManager.connectToKonnwei("KONNWEI")
                    if (ok) {
                        tvStatus.text = "เชื่อมต่อสำเร็จ: KONNWEI"
                        val volt = btManager.sendCommand("AT RV")
                        tvVoltage.text = volt
                        log("เชื่อมต่อหัวสแกนสำเร็จ! แรงดันแบตเตอรี่: $volt")
                    } else {
                        tvStatus.text = "เชื่อมต่อไม่สำเร็จ (เช็ก Bluetooth)"
                        log("ไม่พบหัวสแกน KONNWEI กรุณาจับคู่ Bluetooth ก่อน")
                    }
                } catch (e: Exception) {
                    tvStatus.text = "Error: ${e.message}"
                }
            }
        }

        btnDemo.setOnClickListener {
            isDemoMode = true
            tvStatus.text = "สถานะ: โหมดจำลอง (DEMO)"
            tvVoltage.text = "13.8 V"
            log("เปิดใช้งานโหมดจำลอง (Demo Mode) สำหรับ ${spinnerBrand.selectedItem}\nสามารถกดปุ่ม อ่านโค้ด / ลบโค้ด / ค่าสด / เทสหัวฉีด ได้ทันที")
        }

        btnReadDtc.setOnClickListener {
            lifecycleScope.launch {
                log("กำลังสแกนหารหัสโค้ดปัญหา (DTC)...")
                if (isDemoMode) {
                    delay(500)
                    val demoHex = "43 02 00 87 03 80"
                    log("Raw Hex: $demoHex\n${parseDtcResponse(demoHex)}")
                } else {
                    val res = btManager.sendCommand("03")
                    log("Raw Hex: $res\n${parseDtcResponse(res)}")
                }
            }
        }

        btnClearDtc.setOnClickListener {
            lifecycleScope.launch {
                log("กำลังส่งคำสั่งลบโค้ด (Mode 04)...")
                if (isDemoMode) {
                    delay(400)
                    log("ผลลัพธ์: 44 (ลบโค้ดและดับไฟโชว์สำเร็จ)")
                } else {
                    val res = btManager.sendCommand("04")
                    log("ผลลัพธ์การลบโค้ด: $res")
                }
            }
        }

               btnLiveData.setOnClickListener {
            lifecycleScope.launch {
                log("Reading Live Data from ECU...")
                if (isDemoMode) {
                    delay(400)
                    val rpm = calculateRpm("41 0C 0D 48")
                    val coolant = calculateCoolant("41 05 7D")
                    val rail = calculateRailPressure("41 23 0C 80")
                    log("[Live Data]\n- RPM: $rpm rpm\n- Coolant Temp: $coolant C\n- Fuel Rail: $rail MPa\n- Battery: 13.8 V")
                } else {
                    btManager.sendCommand("AT SH 7E0")
                    val rpmHex = btManager.sendCommand("010C")
                    val tempHex = btManager.sendCommand("0105")
                    val railHex = btManager.sendCommand("0123")
                    val volt = btManager.sendCommand("AT RV")
                    tvVoltage.text = volt
                    log("[Live Data]\n- RPM: ${calculateRpm(rpmHex)} ($rpmHex)\n- Coolant: ${calculateCoolant(tempHex)} C ($tempHex)\n- Rail: ${calculateRailPressure(railHex)} MPa ($railHex)\n- Battery: $volt")
                }
            }
        }

        btnActiveTest.setOnClickListener {
            lifecycleScope.launch {
                log("Injector #1 Cut Test (5 sec)...")
                if (isDemoMode) {
                    log("Injector #1 Cut: ON (6F 10 01 03 01)")
                    delay(5000)
                    log("Injector #1 Cut: OFF Normal (6F 10 01 00)")
                } else {
                    btManager.sendCommand("AT SH 7E0")
                    val resOn = btManager.sendCommand("2F 10 01 03 01")
                    log("Cut ON: $resOn")
                    delay(5000)
                    val resOff = btManager.sendCommand("2F 10 01 00")
                    log("Cut OFF: $resOff")
                }
            }
        }
    }

    private fun calculateRpm(hex: String): String {
        return try {
            val clean = hex.replace(" ", "")
            val idx = clean.indexOf("410C")
            if (idx != -1 && clean.length >= idx + 8) {
                val a = clean.substring(idx + 4, idx + 6).toInt(16)
                val b = clean.substring(idx + 6, idx + 8).toInt(16)
                (((a * 256) + b) / 4).toString()
            } else "N/A"
        } catch (_: Exception) { "N/A" }
    }

    private fun calculateCoolant(hex: String): String {
        return try {
            val clean = hex.replace(" ", "")
            val idx = clean.indexOf("4105")
            if (idx != -1 && clean.length >= idx + 6) {
                val a = clean.substring(idx + 4, idx + 6).toInt(16)
                (a - 40).toString()
            } else "N/A"
        } catch (_: Exception) { "N/A" }
    }

    private fun calculateRailPressure(hex: String): String {
        return try {
            val clean = hex.replace(" ", "")
            val idx = clean.indexOf("4123")
            if (idx != -1 && clean.length >= idx + 8) {
                val a = clean.substring(idx + 4, idx + 6).toInt(16)
                val b = clean.substring(idx + 6, idx + 8).toInt(16)
                val kpa = ((a * 256) + b) * 10
                String.format("%.1f", kpa / 1000.0)
            } else "N/A"
        } catch (_: Exception) { "N/A" }
    }

    private fun setupBrandSpinner() {
               val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, carBrands) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return TextView(context).apply {
                    text = carBrands[position]
                    setTextColor(Color.WHITE)
                    textSize = 18f
                    setPadding(24, 24, 24, 24)
                }
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return TextView(context).apply {
                    text = carBrands[position]
                    setBackgroundColor(Color.parseColor("#2C2C2C"))
                    setTextColor(Color.WHITE)
                    textSize = 18f
                    setPadding(32, 32, 32, 32)
                }
            }
        }

        spinnerBrand.adapter = adapter
        spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = carBrands[pos]
                lifecycleScope.launch {
                    try {
                        when {
                            selected.contains("CAN 29-Bit") -> {
                                if (!isDemoMode) btManager.sendCommand("AT SP 7")
                                setAlert(false, "Protocol: Honda CAN 29-Bit Ready")
                            }
                            selected.contains("MS-CAN") -> setAlert(true, "WARNING: Switch Adapter to MS-CAN (Pin 3/11)")
                            selected.contains("24V") -> setAlert(true, "DANGER: Use 24V->12V Step-Down Cable First!")
                            else -> {
                                if (!isDemoMode) btManager.sendCommand("AT SP 6")
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
        if (raw.contains("NO DATA") || raw.contains("43 00")) return "OK: No Fault Codes (Normal)"
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("43")
        if (idx == -1 || clean.length < idx + 6) return "Cannot decode DTC, check Raw Hex above"

        val result = StringBuilder("Found Fault Codes:\n")
        var pos = idx + 4
        if (clean.length == idx + 6) pos = idx + 2

        while (pos + 4 <= clean.length) {
            val codeHex = clean.substring(pos, pos + 4)
            if (codeHex != "0000") {
                val dtcCode = decodeDtcHex(codeHex)
                val desc = dtcDatabase[dtcCode] ?: "Manufacturer Specific Code"
                result.append("- [$dtcCode] : $desc\n")
            }
            pos += 4
        }
        return result.toString().trim()
    }

    private fun decodeDtcHex(hex4: String): String {
        val firstChar = when (hex4[0]) {
            '0' -> "P0"; '1' -> "P1"; '2' -> "P2"; '3' -> "P3"
            '4' -> "C0"; '5' -> "C1"; '6' -> "C2"; '7' -> "C3"
            '8' -> "B0"; '9' -> "B1"; 'A', 'a' -> "B2"; 'B', 'b' -> "B3"
            'C', 'c' -> "U0"; 'D', 'd' -> "U1"; 'E', 'e' -> "U2"; 'F', 'f' -> "U3"
            else -> "P0"
        }
        return firstChar + hex4.substring(1).uppercase()
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
