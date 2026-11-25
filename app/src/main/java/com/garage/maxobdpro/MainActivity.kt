package com.garage.maxobdpro

import android.Manifest
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private val btManager = KonnweiBluetoothManager()
    private lateinit var viewFlipper: ViewFlipper

    // หน้าหลัก (Page 0)
    private lateinit var tvStatus: TextView
    private lateinit var tvVoltage: TextView
    private lateinit var tvAdapterAlert: TextView
    private lateinit var spinnerBrand: Spinner

    // หน้าสแกน (Page 1)
    private lateinit var tvSubTitle: TextView
    private lateinit var tvConsole: TextView
    private lateinit var btnSubRescan: Button
    private lateinit var btnSubClear: Button

    // หน้าดิสเพลย์เกจวัด (Page 2)
    private lateinit var gaugeContainer: LinearLayout
    private lateinit var btnToggleStyle: Button
    private var isAnalogMode = false
    private var liveGaugeJob: Job? = null

    // รายการค่าที่เลือกโชว์บนดิสเพลย์
    private val gaugeEnabled = mutableMapOf(
        "VOLT" to true,
        "TEMP" to true,
        "RPM" to true,
        "RAIL" to true,
        "SPEED" to false,
        "BOOST" to false
    )
    private val gaugeValues = mutableMapOf(
        "VOLT" to 0.0f,
        "TEMP" to 0.0f,
        "RPM" to 0.0f,
        "RAIL" to 0.0f,
        "SPEED" to 0.0f,
        "BOOST" to 0.0f
    )

    private var isDemoMode = false
    private var currentModule = "OBD2"

    private val carBrands = listOf(
        "Isuzu D-Max / MU-X (2004-2026)",
        "Toyota Vigo / Revo / Fortuner (2004-2026)",
        "Mitsubishi Triton / Pajero (2005-2023)",
        "Honda City / Civic / Jazz (2008-2026) [CAN 29-Bit]",
        "Ford Ranger / Mazda (MS-CAN) [Adapter]",
        "BMW / Benz / Europe (2001-2026)",
        "Truck 24V (Hino / Isuzu) [24V->12V]"
    )

    private val dtcDatabase = mapOf(
        "P0087" to "[เครื่องยนต์] แรงดันรางคอมมอนเรลต่ำเกินไป (เช็ก SCV / กรองโซล่า / ท้ายราง)",
        "P0088" to "[เครื่องยนต์] แรงดันรางคอมมอนเรลสูงเกินไป (เช็กวาล์ว SCV ค้าง)",
        "P0089" to "[เครื่องยนต์] วาล์วควบคุมแรงดันน้ำมันผิดปกติ (SCV Valve)",
        "P0093" to "[เครื่องยนต์] น้ำมันเชื้อเพลิงแรงดันสูงรั่วในระบบราง (หัวฉีดรั่วไหลกลับ)",
        "P0102" to "[เครื่องยนต์] สัญญาณแอร์โฟลว์ (MAF) ต่ำเกินไป / ปลั๊กหลุด",
        "P0234" to "[เครื่องยนต์] บูสต์เทอร์โบสูงเกินกำหนด (Overboost)",
        "P0299" to "[เครื่องยนต์] บูสต์เทอร์โบต่ำกว่ากำหนด (Underboost / ท่ออินเตอร์รั่ว)",
        "P0335" to "[เครื่องยนต์] เซ็นเซอร์เพลาข้อเหวี่ยง (CKP) ผิดปกติ",
        "P0340" to "[เครื่องยนต์] เซ็นเซอร์เพลาลูกเบี้ยว (CMP) ผิดปกติ",
        "P0380" to "[เครื่องยนต์] วงจรหัวเผา / รีเลย์หัวเผาขัดข้อง",
        "P0401" to "[เครื่องยนต์] ปริมาณไอเสียวนกลับ (EGR) น้อยเกินไป / ท่อตัน",
        "P0404" to "[เครื่องยนต์] มอเตอร์วาล์ว EGR ค้างหรือทำงานผิดปกติ",
        "P0653" to "[เครื่องยนต์] ไฟเลี้ยงเซ็นเซอร์ชุด B สูงเกิน 5V (สายไฟช็อต)",
        "P1229" to "[เครื่องยนต์] ปั๊มแรงดันสูงจ่ายน้ำมันเกิน (Toyota Vigo/Fortuner)",
        "C0031" to "[ABS] เซ็นเซอร์ความเร็วล้อหน้าซ้าย (FL) ขาดวงจร / สกปรก",
        "C0034" to "[ABS] เซ็นเซอร์ความเร็วล้อหน้าขวา (FR) ขาดวงจร / สกปรก",
        "C0037" to "[ABS] เซ็นเซอร์ความเร็วล้อหลังซ้าย (RL) ขาดวงจร",
        "C0040" to "[ABS] เซ็นเซอร์ความเร็วล้อหลังขวา (RR) ขาดวงจร",
        "C0110" to "[ABS] มอเตอร์ปั๊ม ABS ทำงานผิดปกติ / ฟิวส์ขาด",
        "B0001" to "[SRS] วงจรถุงลมนิรภัยฝั่งคนขับความต้านทานสูง (เช็กสายแพรแตร)",
        "B0010" to "[SRS] วงจรถุงลมนิรภัยฝั่งผู้โดยสารผิดปกติ",
        "B1000" to "[SRS] กล่องควบคุมถุงลมนิรภัย (ECU SRS) ขัดข้องภายใน"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewFlipper = ViewFlipper(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
        }

        viewFlipper.addView(buildMainMenuPage())   // Page 0: หน้าหลัก
        viewFlipper.addView(buildScanDetailPage()) // Page 1: หน้าสแกน / Database
        viewFlipper.addView(buildDisplayPage())    // Page 2: หน้าดิสเพลย์เกจวัด

        setContentView(viewFlipper)
        setupBrandSpinner()
        requestBluetoothPermissions()
    }

    // ==================== PAGE 0: หน้าเมนูหลัก ====================
    private fun buildMainMenuPage(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
        }

        // แถบสถานะ + โวลต์
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 8)
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
        topBar.addView(tvStatus)
        topBar.addView(tvVoltage)
        root.addView(topBar)

        // แถบเตือนโปรโตคอล + ปุ่มเชื่อมต่อ/Demo
        val connBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 10)
        }
        tvAdapterAlert = TextView(this).apply {
            text = "KONNWEI MAXOBD Ready"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B5E20"))
            setPadding(16, 14, 16, 14)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnConn = Button(this).apply {
            text = "เชื่อมต่อจริง"
            setBackgroundColor(Color.parseColor("#1565C0"))
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            setOnClickListener { connectReal() }
        }
        val btnDemo = Button(this).apply {
            text = "โหมด Demo"
            setBackgroundColor(Color.parseColor("#6A1B9A"))
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            setOnClickListener { enableDemo() }
        }
        connBar.addView(tvAdapterAlert)
        connBar.addView(spaceBox(10))
        connBar.addView(btnConn)
        connBar.addView(spaceBox(10))
        connBar.addView(btnDemo)
        root.addView(connBar)

        // ตัวเลือกยี่ห้อรถ
        spinnerBrand = Spinner(this).apply {
            setBackgroundColor(Color.parseColor("#2C2C2C"))
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 90)
            params.setMargins(0, 0, 0, 14)
            layoutParams = params
        }
        root.addView(spinnerBrand)

        // ตารางปุ่มเมนูใหญ่ 2 แถว x 3 ปุ่ม
        val gridRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding(0, 0, 0, 10)
        }
        gridRow1.addView(createMenuCard("สแกนโค้ดทั่วไป\n(OBD2 Standard)", "#2E7D32") { openModule("OBD2") })
        gridRow1.addView(spaceBox(12))
        gridRow1.addView(createMenuCard("สแกนเชิงลึก\n(ECM / Engine)", "#00695C") { openModule("ECM") })
        gridRow1.addView(spaceBox(12))
        gridRow1.addView(createMenuCard("ระบบเบรก\n(ABS Module)", "#D84315") { openModule("ABS") })
        root.addView(gridRow1)

        val gridRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        gridRow2.addView(createMenuCard("ถุงลมนิรภัย\n(SRS / Airbag)", "#AD1457") { openModule("SRS") })
        gridRow2.addView(spaceBox(12))
        gridRow2.addView(createMenuCard("ดิสเพลย์เกจวัด\n(เข็ม / ดิจิตอล)", "#0277BD") { openDisplayDashboard() })
        gridRow2.addView(spaceBox(12))
        gridRow2.addView(createMenuCard("ฐานข้อมูลโค้ด\n(Database ภาษาไทย)", "#4527A0") { openModule("DB") })
        root.addView(gridRow2)

        return root
    }

    private fun createMenuCard(title: String, colorHex: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = title
            setBackgroundColor(Color.parseColor(colorHex))
            setTextColor(Color.WHITE)
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { onClick() }
        }
    }

    // ==================== PAGE 1: หน้าสแกนแต่ละระบบ / Database ====================
    private fun buildScanDetailPage(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
        }

        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 12)
        }
        val btnBack = Button(this).apply {
            text = "< กลับหน้าหลัก"
            setBackgroundColor(Color.parseColor("#424242"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(24, 14, 24, 14)
            setOnClickListener { viewFlipper.displayedChild = 0 }
        }
              tvSubTitle = TextView(this).apply {
            text = "ระบบสแกน"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 20f
            setPadding(20, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        btnSubRescan = Button(this).apply {
            text = "สแกนซ้ำ"
            setBackgroundColor(Color.parseColor("#1976D2"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(24, 14, 24, 14)
            setOnClickListener { executeModuleScan(currentModule) }
        }
        btnSubClear = Button(this).apply {
            text = "ลบโค้ด"
            setBackgroundColor(Color.parseColor("#C62828"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(24, 14, 24, 14)
            setOnClickListener { clearCurrentDtc() }
        }

        headerBar.addView(btnBack)
        headerBar.addView(tvSubTitle)
        headerBar.addView(btnSubRescan)
        headerBar.addView(spaceBox(10))
        headerBar.addView(btnSubClear)
        root.addView(headerBar)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        tvConsole = TextView(this).apply {
            text = "กำลังเตรียมข้อมูล..."
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 19f
        }
        scroll.addView(tvConsole)
        root.addView(scroll)

        return root
    }

    // ==================== PAGE 2: หน้าดิสเพลย์เกจวัด (เข็ม / ดิจิตอล) ====================
    private fun buildDisplayPage(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 10)
        }
        val btnBack = Button(this).apply {
            text = "< กลับหน้าหลัก"
            setBackgroundColor(Color.parseColor("#424242"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(24, 12, 24, 12)
            setOnClickListener {
                liveGaugeJob?.cancel()
                viewFlipper.displayedChild = 0
            }
        }
        val title = TextView(this).apply {
            text = "ดิสเพลย์เกจวัดค่าสดในรถ"
            setTextColor(Color.parseColor("#29B6F6"))
            textSize = 20f
            setPadding(20, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        btnToggleStyle = Button(this).apply {
            text = "รูปแบบ: ดิจิตอล (กดเปลี่ยนเป็นเข็ม)"
            setBackgroundColor(Color.parseColor("#EF6C00"))
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(20, 12, 20, 12)
            setOnClickListener {
                isAnalogMode = !isAnalogMode
                text = if (isAnalogMode) "รูปแบบ: แบบเข็ม (กดเปลี่ยนเป็นดิจิตอล)" else "รูปแบบ: ดิจิตอล (กดเปลี่ยนเป็นเข็ม)"
                renderGauges()
            }
        }
        val btnSelectItems = Button(this).apply {
            text = "เลือกค่าที่จะโชว์"
            setBackgroundColor(Color.parseColor("#00838F"))
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(20, 12, 20, 12)
            setOnClickListener { showGaugeSelectorDialog() }
        }

        topBar.addView(btnBack)
        topBar.addView(title)
        topBar.addView(btnToggleStyle)
        topBar.addView(spaceBox(10))
        topBar.addView(btnSelectItems)
        root.addView(topBar)

        gaugeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#181818"))
            setPadding(12, 12, 12, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(gaugeContainer)

        return root
    }

    private fun openDisplayDashboard() {
        viewFlipper.displayedChild = 2
        renderGauges()
        startLiveGaugeLoop()
    }

    private fun showGaugeSelectorDialog() {
        val keys = arrayOf("VOLT", "TEMP", "RPM", "RAIL", "SPEED", "BOOST")
        val names = arrayOf(
            "ไดชาร์จ / แรงดันแบตเตอรี่ (V)",
            "อุณหภูมิน้ำหล่อเย็น (C)",
            "รอบเครื่องยนต์ (RPM)",
            "แรงดันรางคอมมอนเรล (MPa)",
            "ความเร็วรถ (km/h)",
            "แรงดันบูสต์เทอร์โบ (kPa)"
        )
        val checked = BooleanArray(keys.size) { gaugeEnabled[keys[it]] == true }

        android.app.AlertDialog.Builder(this)
            .setTitle("เลือกค่าที่ต้องการให้แสดงบนหน้าจอ")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                gaugeEnabled[keys[which]] = isChecked
            }
            .setPositiveButton("ตกลง") { _, _ -> renderGauges() }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun renderGauges() {
        gaugeContainer.removeAllViews()
        val activeKeys = gaugeEnabled.filter { it.value }.keys.toList()
        if (activeKeys.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "กรุณากดปุ่ม 'เลือกค่าที่จะโชว์' ด้านบนเพื่อเพิ่มเกจวัด"
                setTextColor(Color.WHITE)
                textSize = 18f
            }
            gaugeContainer.addView(emptyTv)
            return
        }

        for (key in activeKeys) {
            val (label, unit, minV, maxV) = getGaugeMeta(key)
            val currentVal = gaugeValues[key] ?: 0f

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#242424"))
                setPadding(12, 12, 12, 12)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                p.setMargins(8, 8, 8, 8)
                layoutParams = p
            }

            val tvTitle = TextView(this).apply {
                text = label
                setTextColor(Color.parseColor("#B0BEC5"))
                textSize = 16f
                gravity = Gravity.CENTER
            }
            card.addView(tvTitle)

            if (isAnalogMode) {
                val dial = AnalogGaugeView(this, currentVal, minV, maxV, unit).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                }
                card.addView(dial)
            } else {
                val tvVal = TextView(this).apply {
                    text = if (key == "VOLT" || key == "RAIL") String.format("%.1f", currentVal) else currentVal.toInt().toString()
                    setTextColor(Color.parseColor("#00E676"))
                    textSize = 44f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                }
                val tvUnit = TextView(this).apply {
                    text = unit
                    setTextColor(Color.parseColor("#29B6F6"))
                    textSize = 18f
                    gravity = Gravity.CENTER
                }
                card.addView(tvVal)
                card.addView(tvUnit)
            }
            gaugeContainer.addView(card)
        }
    }

    private fun startLiveGaugeLoop() {
        liveGaugeJob?.cancel()
        liveGaugeJob = lifecycleScope.launch {
            var simTick = 0
            while (isActive) {
                if (isDemoMode) {
                    simTick++
                    val wave = (simTick % 10)
                    gaugeValues["VOLT"] = 13.8f + (wave * 0.05f)
                    gaugeValues["TEMP"] = 84f + (wave * 0.5f)
                    gaugeValues["RPM"] = 820f + (wave * 150f)
                    gaugeValues["RAIL"] = 31.5f + (wave * 2.2f)
                    gaugeValues["SPEED"] = (wave * 8f)
                    gaugeValues["BOOST"] = 102f + (wave * 6f)
                    tvVoltage.text = String.format("%.1f V", gaugeValues["VOLT"])
                    renderGauges()
                    delay(800)
                } else {
                    try {
                        val voltStr = btManager.sendCommand("AT RV").replace("V", "").trim()
                        voltStr.toFloatOrNull()?.let {
                            gaugeValues["VOLT"] = it
                            tvVoltage.text = String.format("%.1f V", it)
                        }
                        if (gaugeEnabled["RPM"] == true) {
                            calculateRpm(btManager.sendCommand("010C")).toFloatOrNull()?.let { gaugeValues["RPM"] = it }
                        }
                        if (gaugeEnabled["TEMP"] == true) {
                            calculateCoolant(btManager.sendCommand("0105")).toFloatOrNull()?.let { gaugeValues["TEMP"] = it }
                        }
                        if (gaugeEnabled["RAIL"] == true) {
                            calculateRailPressure(btManager.sendCommand("0123")).toFloatOrNull()?.let { gaugeValues["RAIL"] = it }
                        }
                        if (gaugeEnabled["SPEED"] == true) {
                            calculateSpeed(btManager.sendCommand("010D")).toFloatOrNull()?.let { gaugeValues["SPEED"] = it }
                        }
                        if (gaugeEnabled["BOOST"] == true) {
                            calculateBoost(btManager.sendCommand("010B")).toFloatOrNull()?.let { gaugeValues["BOOST"] = it }
                        }
                        renderGauges()
                    } catch (_: Exception) {}
                    delay(500)
                }
            }
        }
    }

    private data class GaugeMeta(val label: String, val unit: String, val min: Float, val max: Float)
    private fun getGaugeMeta(key: String): GaugeMeta {
        return when (key) {
            "VOLT" -> GaugeMeta("ไดชาร์จ / แบต", "Volt (V)", 8f, 16f)
            "TEMP" -> GaugeMeta("อุณหภูมิน้ำ", "C", 0f, 130f)
            "RPM" -> GaugeMeta("รอบเครื่องยนต์", "RPM", 0f, 5000f)
            "RAIL" -> GaugeMeta("แรงดันราง", "MPa", 0f, 200f)
            "SPEED" -> GaugeMeta("ความเร็วรถ", "km/h", 0f, 200f)
            "BOOST" -> GaugeMeta("แรงดันไอดี/บูสต์", "kPa", 0f, 250f)
            else -> GaugeMeta(key, "", 0f, 100f)
        }
    }

    private class AnalogGaugeView(
        context: Context,
        private val value: Float,
        private val minVal: Float,
        private val maxVal: Float,
        private val unit: String
    ) : View(context) {
        private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            color = Color.parseColor("#37474F")
        }
        private val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            color = Color.parseColor("#00E676")
        }
        private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 8f
            color = Color.parseColor("#FF3D00")
            strokeCap = Paint.Cap.ROUND
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height * 0.65f
            val radius = (minOf(width, height) * 0.42f).coerceAtLeast(40f)
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

            canvas.drawArc(rect, 150f, 240f, false, arcPaint)
            val ratio = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
            canvas.drawArc(rect, 150f, 240f * ratio, false, activeArcPaint)

            val angleRad = Math.toRadians((150f + 240f * ratio).toDouble())
            val nx = cx + (radius * 0.8f * cos(angleRad)).toFloat()
            val ny = cy + (radius * 0.8f * sin(angleRad)).toFloat()
            canvas.drawLine(cx, cy, nx, ny, needlePaint)
            canvas.drawCircle(cx, cy, 12f, needlePaint)

            val valText = if (unit.contains("V") || unit.contains("MPa")) String.format("%.1f %s", value, unit) else "${value.toInt()} $unit"
            canvas.drawText(valText, cx, cy + radius * 0.45f, textPaint)
        }
    }

    private fun openModule(module: String) {
        currentModule = module
        viewFlipper.displayedChild = 1
        btnSubRescan.visibility = if (module == "DB") View.GONE else View.VISIBLE
        btnSubClear.visibility = if (module == "DB") View.GONE else View.VISIBLE

        tvSubTitle.text = when (module) {
            "OBD2" -> "สแกนโค้ดทั่วไป (OBD2 Standard)"
            "ECM" -> "สแกนเชิงลึกเครื่องยนต์ (ECM 7E0)"
            "ABS" -> "ระบบเบรกป้องกันล้อล็อก (ABS 7B0)"
            "SRS" -> "ระบบถุงลมนิรภัย (SRS 780)"
            "DB" -> "ฐานข้อมูลรหัสโค้ดเสียภาษาไทย"
            else -> module
        }
        executeModuleScan(module)
    }

    private fun executeModuleScan(module: String) {
        if (module == "DB") {
            val sb = StringBuilder("=== ตารางแปลรหัสโค้ดเสียในระบบ (${dtcDatabase.size} รายการ) ===\n\n")
            for ((code, desc) in dtcDatabase) {
                sb.append("- $code : $desc\n\n")
            }
            tvConsole.text = sb.toString().trim()
            return
        }

        tvConsole.text = "กำลังสื่อสารกับกล่อง $module กรุณารอสักครู่..."
        lifecycleScope.launch {
            if (isDemoMode) {
                delay(400)
                val demoHex = when (module) {
                    "OBD2" -> "43 02 03 80 01 02"
                    "ECM" -> "43 02 00 87 04 04"
                    "ABS" -> "43 01 40 31"
                    "SRS" -> "43 01 80 01"
                    else -> "43 00"
                }
                tvConsole.text = "[โหมดจำลอง - กล่อง $module]\nRaw Hex: $demoHex\n\n${parseDtcResponse(demoHex)}"
            } else {
                try {
                    val header = when (module) {
                        "OBD2" -> "7DF"
                        "ECM" -> "7E0"
                        "ABS" -> "7B0"
                        "SRS" -> "780"
                        else -> "7DF"
                    }
                    btManager.sendCommand("AT SH $header")
                    val cmd = if (module == "OBD2") "03" else "19 02 0F"
                    val res = btManager.sendCommand(cmd)
                    tvConsole.text = "[กล่อง $module | Header: $header]\nRaw Hex: $res\n\n${parseDtcResponse(res)}"
                } catch (e: Exception) {
                    tvConsole.text = "เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message}"
                }
            }
        }
    }

    private fun clearCurrentDtc() {
        lifecycleScope.launch {
            tvConsole.text = "กำลังส่งคำสั่งลบโค้ดกล่อง $currentModule..."
            if (isDemoMode) {
                delay(400)
                tvConsole.text = "ลบโค้ดระบบ $currentModule สำเร็จ! (ไฟโชว์หน้าปัดดับเรียบร้อย)"
            } else {
                val cmd = if (currentModule == "OBD2") "04" else "14 FF FF FF"
                val res = btManager.sendCommand(cmd)
                tvConsole.text = "ผลลัพธ์การลบโค้ด ($currentModule): $res"
            }
        }
    }

    private fun connectReal() {
        isDemoMode = false
        tvStatus.text = "กำลังเชื่อมต่อ..."
        lifecycleScope.launch {
            try {
                val ok = btManager.connectToKonnwei("KONNWEI")
                if (ok) {
                    tvStatus.text = "เชื่อมต่อ: KONNWEI"
                    val volt = btManager.sendCommand("AT RV")
                    tvVoltage.text = volt
                } else {
                    tvStatus.text = "เชื่อมต่อไม่สำเร็จ"
                    Toast.makeText(this@MainActivity, "ไม่พบหัวสแกน KONNWEI กรุณาจับคู่ Bluetooth ก่อน", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                tvStatus.text = "Error: ${e.message}"
            }
        }
    }

    private fun enableDemo() {
        isDemoMode = true
        tvStatus.text = "โหมดจำลอง (DEMO)"
        tvVoltage.text = "13.8 V"
        Toast.makeText(this, "เปิดโหมดจำลอง (DEMO) สำเร็จ กดเข้าเมนูสแกนหรือดิสเพลย์ได้เลย", Toast.LENGTH_SHORT).show()
    }

    private fun calculateRpm(hex: String): String = parseTwoByte(hex, "410C") { a, b -> (((a * 256) + b) / 4).toString() }
    private fun calculateCoolant(hex: String): String = parseOneByte(hex, "4105") { a -> (a - 40).toString() }
    private fun calculateSpeed(hex: String): String = parseOneByte(hex, "410D") { a -> a.toString() }
    private fun calculateBoost(hex: String): String = parseOneByte(hex, "410B") { a -> a.toString() }
    private fun calculateRailPressure(hex: String): String = parseTwoByte(hex, "4123") { a, b -> String.format("%.1f", (((a * 256) + b) * 10) / 1000.0) }

    private fun parseOneByte(hex: String, pid: String, formula: (Int) -> String): String {
        return try {
            val clean = hex.replace(" ", "")
            val idx = clean.indexOf(pid)
            if (idx != -1 && clean.length >= idx + 6) formula(clean.substring(idx + 4, idx + 6).toInt(16)) else "N/A"
        } catch (_: Exception) { "N/A" }
    }

    private fun parseTwoByte(hex: String, pid: String, formula: (Int, Int) -> String): String {
        return try {
            val clean = hex.replace(" ", "")
            val idx = clean.indexOf(pid)
            if (idx != -1 && clean.length >= idx + 8) {
                formula(clean.substring(idx + 4, idx + 6).toInt(16), clean.substring(idx + 6, idx + 8).toInt(16))
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
                    setPadding(24, 20, 24, 20)
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
        if (raw.contains("NO DATA") || raw.contains("43 00")) return "OK: ไม่พบรหัสความผิดปกติ (ระบบปกติ)"
        val clean = raw.replace(" ", "")
        val idx = clean.indexOf("43")
        if (idx == -1 || clean.length < idx + 6) return "ไม่สามารถแปลรหัสได้ กรุณาตรวจสอบข้อมูลดิบด้านบน"

        val result = StringBuilder("พบรายการรหัสข้อผิดพลาด:\n")
        var pos = idx + 4
        if (clean.length == idx + 6) pos = idx + 2

        while (pos + 4 <= clean.length) {
            val codeHex = clean.substring(pos, pos + 4)
            if (codeHex != "0000") {
                val dtcCode = decodeDtcHex(codeHex)
                val desc = dtcDatabase[dtcCode] ?: "รหัสเฉพาะค่ายรถ (ตรวจสอบใน Database)"
                result.append("- [$dtcCode] : $desc\n\n")
            }
            pos += 4
        }
        return result.toString().trim()
    }

    private fun decodeDtcHex(hex4: String): String {
        val firstChar = when (hex4) {
            '0' -> "P0"; '1' -> "P1"; '2' -> "P2"; '3' -> "P3"
            '4' -> "C0"; '5' -> "C1"; '6' -> "C2"; '7' -> "C3"
            '8' -> "B0"; '9' -> "B1"; 'A', 'a' -> "B2"; 'B', 'b' -> "B3"
            'C', 'c' -> "U0"; 'D', 'd' -> "U1"; 'E', 'e' -> "U2"; 'F', 'f' -> "U3"
            else -> "P0"
        }
        return firstChar + hex4.substring(1).uppercase()
    }

    private fun spaceBox(dp: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp, dp)
        }
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
        liveGaugeJob?.cancel()
        try {
            btManager.disconnect()
        } catch (_: Exception) {}
    }
}
