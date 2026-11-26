package com.maxobd.pro;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outStream;
    private InputStream inStream;
    private Spinner spDevices, spBrands, spModels, spModules;
    private TextView tvEcuIdentity, tvStatus, tvLiveData, tvInjectorData, tvAlert, tvLog;
    private TextView dashRpm, dashSpeed, dashTemp, dashRail, dashVolt, dashInj;
    private List<BluetoothDevice> pairedList = new ArrayList<>();
    private List<String> brandList;
    private List<VehicleDatabase.CarProfile> currentModelList = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isScanningLive = false;
    private volatile boolean isAutoScanningAll = false;
    private String lastReportSummary = "ยังไม่มีข้อมูลการสแกน";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildGarageUI();
        checkBluetoothPermissions();
    }

    private void buildGarageUI() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#121212"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 22, 22, 22);

        TextView title = new TextView(this);
        title.setText("MaxOBDPro v6.3 - Dashboard & Deep Scan");
        title.setTextColor(Color.parseColor("#00E676"));
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 10);
        root.addView(title);

        // กล่องป้ายโชว์ข้อมูลประจำตัวกล่อง ECU
        tvEcuIdentity = new TextView(this);
        tvEcuIdentity.setText("📟 ข้อมูลกล่องที่เชื่อมต่อ: ยังไม่ได้เชื่อมต่อ\n• ตระกูลรถ / รุ่นปี: --\n• หมายเลขกล่อง (Part / Cal ID): --");
        tvEcuIdentity.setTextColor(Color.parseColor("#E1F5FE"));
        tvEcuIdentity.setTextSize(13);
        tvEcuIdentity.setPadding(16, 14, 16, 14);
        tvEcuIdentity.setBackgroundColor(Color.parseColor("#0D47A1"));
        root.addView(tvEcuIdentity);

        // ปุ่มเมนูหลักด้านบน (เปิด Dashboard & คู่มือขาปลั๊ก)
        LinearLayout topMenuRow = createRow();
        Button btnOpenDash = createRowButton("📊 เปิดหน้าปัด (Dashboard)", "#00838F");
        btnOpenDash.setOnClickListener(v -> showDashboardDialog());
        Button btnPinoutManual = createRowButton("📘 คู่มือขาปลั๊ก OBD2 / ECU", "#0277BD");
        btnPinoutManual.setOnClickListener(v -> showPinoutManualDialog());
        topMenuRow.addView(btnOpenDash);
        topMenuRow.addView(btnPinoutManual);
        root.addView(topMenuRow);

        root.addView(createLabel("1. เลือกหัวอ่าน OBD2 (KONNWEI / ELM327):"));
        spDevices = new Spinner(this);
        spDevices.setBackgroundColor(Color.parseColor("#2C2C2C"));
        root.addView(spDevices);

        Button btnConnect = createButton("🔌 เชื่อมต่อหัวอ่าน OBD2", "#1976D2");
        btnConnect.setOnClickListener(v -> connectSelectedDevice());
        root.addView(btnConnect);

        root.addView(createLabel("2. เลือกยี่ห้อและรุ่นย่อย / ปีผลิต:"));
        spBrands = new Spinner(this);
        spBrands.setBackgroundColor(Color.parseColor("#2C2C2C"));
        brandList = VehicleDatabase.getBrands();
        spBrands.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, brandList));
        root.addView(spBrands);

        spModels = new Spinner(this);
        spModels.setBackgroundColor(Color.parseColor("#2C2C2C"));
        root.addView(spModels);

        spBrands.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedBrand = brandList.get(pos);
                currentModelList = VehicleDatabase.getModelsByBrand(selectedBrand);
                spModels.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, currentModelList));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        root.addView(createLabel("3. เลือกกล่องระบบที่จะสแกน (ECU / TCU / ABS / SRS / BCM / EPS):"));
        spModules = new Spinner(this);
        spModules.setBackgroundColor(Color.parseColor("#1B5E20"));
        String[] modules = new String[]{
            "🔧 1. กล่องเครื่องยนต์ (Engine ECU - 7E0)",
            "⚙️ 2. กล่องเกียร์อัตโนมัติ (TCU - 7E1)",
            "🛑 3. กล่องระบบเบรก (ABS / VSC - 7B0 / 760)",
            "🎈 4. กล่องถุงลมนิรภัย (SRS Airbag - 780 / 720)",
            "💡 5. กล่องควบคุมตัวถัง (BCM - 750 / 726)",
            "🛞 6. กล่องพวงมาลัยไฟฟ้า (EPS - 7A0 / 730)",
            "❄️ 7. กล่องระบบปรับอากาศ (A/C - 7C4)"
        };
        spModules.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modules));
        root.addView(spModules);

        // แถวปุ่มสแกนหลัก: สแกนทั้งหมดทั้งคัน vs สแกนเชิงลึกกล่องที่เลือก
        LinearLayout scanRow = createRow();
        Button btnScanAll = createRowButton("🔍 สแกนทั้งหมดทั้งคัน (7 ระบบ)", "#2E7D32");
        btnScanAll.setOnClickListener(v -> runAllSystemsAutoScan());
        Button btnDeepScan = createRowButton("🧬 สแกนเชิงลึกกล่องที่เลือก", "#388E3C");
        btnDeepScan.setOnClickListener(v -> runDeepSingleModuleScan());
        scanRow.addView(btnScanAll);
        scanRow.addView(btnDeepScan);
        root.addView(scanRow);

        // แถวปุ่มอ่านโค้ด / ลบโค้ด / ค่าสด
        LinearLayout dtcRow = createRow();
        Button btnReadDtc = createRowButton("📋 อ่านโค้ดเสีย (DTC)", "#F57C00");
        btnReadDtc.setOnClickListener(v -> readFaultCodes());
        Button btnClearDtc = createRowButton("🗑️ ลบโค้ดเสีย (Clear)", "#D32F2F");
        btnClearDtc.setOnClickListener(v -> clearFaultCodes());
        Button btnLive = createRowButton("⚡ เริ่ม/หยุด ค่าสด", "#0097A7");
        btnLive.setOnClickListener(v -> toggleLiveData());
        dtcRow.addView(btnReadDtc);
        dtcRow.addView(btnClearDtc);
        dtcRow.addView(btnLive);
        root.addView(dtcRow);

        // โหมดตัดหัวฉีดรายสูบ 1-4
        root.addView(createLabel("4. โหมดตัดหัวฉีดรายสูบ (Injector Cut Test 1-4):"));
        LinearLayout rowInj = createRow();
        Button btnInj1 = createRowButton("ตัดสูบ 1", "#4527A0");
        btnInj1.setOnClickListener(v -> runActiveTest("ตัดหัวฉีดสูบ 1", 1));
        Button btnInj2 = createRowButton("ตัดสูบ 2", "#4527A0");
        btnInj2.setOnClickListener(v -> runActiveTest("ตัดหัวฉีดสูบ 2", 2));
        Button btnInj3 = createRowButton("ตัดสูบ 3", "#4527A0");
        btnInj3.setOnClickListener(v -> runActiveTest("ตัดหัวฉีดสูบ 3", 3));
        Button btnInj4 = createRowButton("ตัดสูบ 4", "#4527A0");
        btnInj4.setOnClickListener(v -> runActiveTest("ตัดหัวฉีดสูบ 4", 4));
        rowInj.addView(btnInj1); rowInj.addView(btnInj2); rowInj.addView(btnInj3); rowInj.addView(btnInj4);
        root.addView(rowInj);

        // โหมดทดสอบอุปกรณ์อื่น
        root.addView(createLabel("5. โหมดทดสอบอุปกรณ์อื่น (Active Test):"));
        LinearLayout rowOther = createRow();
        Button btnTestCoil = createRowButton("เทสคอยล์ / หัวเทียน", "#6A1B9A");
        btnTestCoil.setOnClickListener(v -> runActiveTest("สั่งจุดระเบิดคอยล์ / หัวเทียนสูบ 1", 5));
        Button btnTestFan = createRowButton("เทสพัดลมหม้อน้ำ", "#5E35B1");
        btnTestFan.setOnClickListener(v -> runActiveTest("เปิดพัดลมหม้อน้ำ", 6));
        Button btnTestEgr = createRowButton("เทสวาล์ว EGR / SCV", "#5E35B1");
        btnTestEgr.setOnClickListener(v -> runActiveTest("สั่งเปิดวาล์ว EGR / SCV", 7));
        rowOther.addView(btnTestCoil); rowOther.addView(btnTestFan); rowOther.addView(btnTestEgr);
        root.addView(rowOther);

        // แถบค่าสดสรุปบนหน้าหลัก
        tvLiveData = new TextView(this);
        tvLiveData.setText("ค่าสด: รอบ: -- RPM | น้ำ: -- °C | ราง: -- MPa | แบต: -- V");
        tvLiveData.setTextColor(Color.parseColor("#FFEB3B"));
        tvLiveData.setTextSize(14);
        tvLiveData.setPadding(16, 14, 16, 8);
        tvLiveData.setBackgroundColor(Color.parseColor("#1E1E1E"));
        root.addView(tvLiveData);

        tvInjectorData = new TextView(this);
        tvInjectorData.setText("💉 ชดเชยหัวฉีด (mm³/st): สูบ1: -- | สูบ2: -- | สูบ3: -- | สูบ4: --");
        tvInjectorData.setTextColor(Color.parseColor("#80DEEA"));
        tvInjectorData.setTextSize(13);
        tvInjectorData.setPadding(16, 8, 16, 14);
        tvInjectorData.setBackgroundColor(Color.parseColor("#1A237E"));
        root.addView(tvInjectorData);

        tvAlert = new TextView(this);
        tvAlert.setText("สถานะค่าเครื่องยนต์: ปกติ (สีเขียว) | เกณฑ์ชดเชยหัวฉีด ±3.0 mm³/st");
        tvAlert.setTextColor(Color.WHITE);
        tvAlert.setBackgroundColor(Color.parseColor("#2E7D32"));
        tvAlert.setPadding(16, 10, 16, 10);
        root.addView(tvAlert);

        tvStatus = new TextView(this);
        tvStatus.setText("ผลการวิเคราะห์เชิงลึก: พร้อมทำงาน");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(14);
        tvStatus.setPadding(16, 16, 16, 16);
        tvStatus.setBackgroundColor(Color.parseColor("#263238"));
        root.addView(tvStatus);

        Button btnShareReport = createButton("📄 บันทึกและส่งรายงานซ่อมให้ลูกค้า (LINE / แชร์)", "#00897B");
        btnShareReport.setOnClickListener(v -> shareRepairReport());
        root.addView(btnShareReport);

        tvLog = new TextView(this);
        tvLog.setText("บันทึกการสื่อสาร (Log):\n");
        tvLog.setTextColor(Color.parseColor("#B0BEC5"));
        tvLog.setTextSize(11);
        tvLog.setPadding(10, 10, 10, 10);
        root.addView(tvLog);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void showDashboardDialog() {
        LinearLayout dashRoot = new LinearLayout(this);
        dashRoot.setOrientation(LinearLayout.VERTICAL);
        dashRoot.setBackgroundColor(Color.parseColor("#0A0F14"));
        dashRoot.setPadding(24, 24, 24, 24);

        LinearLayout grid1 = createRow();
        dashRpm = createGaugeBox("รอบเครื่องยนต์ (RPM)", "0", "#00E676");
        dashSpeed = createGaugeBox("ความเร็ว (KM/H)", "0", "#29B6F6");
        grid1.addView(dashRpm);
        grid1.addView(dashSpeed);
        dashRoot.addView(grid1);

        LinearLayout grid2 = createRow();
        dashTemp = createGaugeBox("อุณหภูมิน้ำ (°C)", "--", "#FF7043");
        dashRail = createGaugeBox("แรงดันราง (MPa)", "--", "#FFCA28");
        dashVolt = createGaugeBox("แบตเตอรี่ (V)", "--", "#AB47BC");
        grid2.addView(dashTemp);
        grid2.addView(dashRail);
        grid2.addView(dashVolt);
        dashRoot.addView(grid2);

        dashInj = new TextView(this);
        dashInj.setText("💉 ค่าชดเชยหัวฉีด (mm³/st):\nสูบ 1: 0.00  |  สูบ 2: 0.00\nสูบ 3: 0.00  |  สูบ 4: 0.00");
        dashInj.setTextColor(Color.parseColor("#80DEEA"));
        dashInj.setTextSize(16);
        dashInj.setGravity(Gravity.CENTER);
        dashInj.setPadding(16, 20, 16, 20);
        dashInj.setBackgroundColor(Color.parseColor("#16222E"));
        dashRoot.addView(dashInj);

        if (isConnected() && !isScanningLive) {
            toggleLiveData();
        }

        new AlertDialog.Builder(this)
                .setTitle("📊 หน้าปัดมาตรวัดเรียลไทม์ (Live Dashboard)")
                .setView(dashRoot)
                .setPositiveButton("ปิดหน้าปัด", (d, w) -> {
                    dashRpm = null; dashSpeed = null; dashTemp = null; dashRail = null; dashVolt = null; dashInj = null;
                })
                .setOnDismissListener(d -> {
                    dashRpm = null; dashSpeed = null; dashTemp = null; dashRail = null; dashVolt = null; dashInj = null;
                })
                .show();
    }

    private TextView createGaugeBox(String label, String val, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(label + "\n\n" + val);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTextSize(18);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(12, 24, 12, 24);
        tv.setBackgroundColor(Color.parseColor("#16222E"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(6, 6, 6, 6);
        tv.setLayoutParams(p);
        return tv;
    }

    private void showPinoutManualDialog() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#1E1E1E"));
        TextView tv = new TextView(this);
        tv.setText(VehicleDatabase.getObdAndEcuPinoutManual());
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(14);
        tv.setPadding(24, 24, 24, 24);
        sv.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle("📘 คู่มือขาปลั๊ก OBD2 & ผังไฟกล่อง ECU")
                .setView(sv)
                .setPositiveButton("ปิดหน้าต่าง", null)
                .show();
    }

    private VehicleDatabase.CarProfile getSelectedCar() {
        int pos = spModels.getSelectedItemPosition();
        if (pos >= 0 && pos < currentModelList.size()) return currentModelList.get(pos);
        return VehicleDatabase.getAllVehicles().get(0);
    }

    private String[] getModuleHeadersByIndex(int modIndex, VehicleDatabase.CarProfile car) {
        switch (modIndex) {
            case 0: return new String[]{car.headerCmd, "ATSH7E0"};
            case 1: return new String[]{"ATSH7E1", "ATSH7B8", "ATSH701"};
            case 2: return new String[]{"ATSH7B0", "ATSH760", "ATSH784", "ATSH7E2"};
            case 3: return new String[]{"ATSH780", "ATSH720", "ATSH770", "ATSH7E3"};
            case 4: return new String[]{"ATSH750", "ATSH726", "ATSH740", "ATSH7DF"};
            case 5: return new String[]{"ATSH7A0", "ATSH730", "ATSH746"};
            case 6: return new String[]{"ATSH7C4", "ATSH733", "ATSH783"};
            default: return new String[]{car.headerCmd};
        }
    }

    private void runAllSystemsAutoScan() {
        if (!isConnected() || isAutoScanningAll) return;
        isScanningLive = false;
        isAutoScanningAll = true;

        VehicleDatabase.CarProfile car = getSelectedCar();
        String[] sysNames = {
            "1. เครื่องยนต์ (Engine ECU)",
            "2. เกียร์อัตโนมัติ (TCU)",
            "3. ระบบเบรก (ABS / VSC)",
            "4. ถุงลมนิรภัย (SRS Airbag)",
            "5. ระบบตัวถัง (BCM)",
            "6. พวงมาลัยไฟฟ้า (EPS)",
            "7. ระบบปรับอากาศ (A/C)"
        };

        tvStatus.setText("🔍 กำลังสแกนทั้งหมดทั้งคัน (7 ระบบหลัก) สำหรับ " + car.brand + " " + car.modelYear + "...");

        new Thread(() -> {
            sendAtCommand(car.protocolCmd);
            String ecuInfo = readAndDisplayEcuIdentity(car);
            StringBuilder report = new StringBuilder("=== ผลการสแกนทั้งคัน (All-System Scan) ===\n");
            int activeCount = 0;
            int faultCount = 0;

            for (int i = 0; i < sysNames.length; i++) {
                final int idx = i;
                mainHandler.post(() -> tvStatus.setText("🔍 กำลังตรวจสอบ: " + sysNames[idx] + " (" + (idx + 1) + "/7)..."));

                String[] headers = getModuleHeadersByIndex(i, car);
                boolean responded = false;
                String matchedHeader = "";
                String dtcResp = "";

                for (String h : headers) {
                    sendAtCommand(h);
                    String ping = sendAtCommand("1003");
                    if (ping.contains("NO DATA") || ping.contains("ERROR") || ping.isEmpty()) {
                        ping = sendAtCommand("1001");
                    }
                    if (!ping.contains("NO DATA") && !ping.contains("ERROR") && !ping.isEmpty()) {
                        responded = true;
                        matchedHeader = h.replace("ATSH", "");
                        dtcResp = sendAtCommand(i == 0 ? "03" : "1902AF");
                        if (dtcResp.contains("NO DATA") || dtcResp.contains("7F")) {
                            dtcResp = sendAtCommand("03");
                        }
                        break;
                    }
                }

                if (responded) {
                    activeCount++;
                    String translatedDtc = translateRawDtc(dtcResp);
                    if (!translatedDtc.isEmpty()) {
                        faultCount++;
                        report.append("⚠️ ").append(sysNames[i]).append(" [ID:").append(matchedHeader).append("] -> พบโค้ดเสีย:\n").append(translatedDtc);
                    } else if (dtcResp.contains("43 00") || dtcResp.contains("4300") || dtcResp.contains("59 02") || dtcResp.contains("5902")) {
                        report.append("✅ ").append(sysNames[i]).append(" [ID:").append(matchedHeader).append("] -> ปกติ (ไม่มีโค้ดเสีย)\n");
                    } else {
                        report.append("✅ ").append(sysNames[i]).append(" [ID:").append(matchedHeader).append("] -> เชื่อมต่อได้\n");
                    }
                } else {
                    report.append("⚪ ").append(sysNames[i]).append(" -> ไม่ตอบสนอง / ไม่มีในรุ่นนี้\n");
                }
            }

            report.append("\nสรุปภาพรวม: พบกล่องทำงาน ").append(activeCount).append(" ระบบ | พบความผิดปกติ ").append(faultCount).append(" ระบบ");
            final String finalReport = report.toString();
            lastReportSummary = ecuInfo + "\n\n" + finalReport;
            isAutoScanningAll = false;

            mainHandler.post(() -> {
                tvStatus.setText(finalReport);
                appendLog("All-System Scan: Active=" + activeCount + ", Faults=" + faultCount);
            });
        }).start();
    }

    private void runDeepSingleModuleScan() {
        if (!isConnected()) return;
        isScanningLive = false;
        VehicleDatabase.CarProfile car = getSelectedCar();
        int modIdx = spModules.getSelectedItemPosition();
        String moduleName = spModules.getSelectedItem().toString();

        tvStatus.setText("🧬 กำลังสแกนเชิงลึก [" + moduleName + "]...");
        new Thread(() -> {
            sendAtCommand(car.protocolCmd);
            String[] headers = getModuleHeadersByIndex(modIdx, car);
            String resp = "";
            String matchedHeader = "";

            for (String h : headers) {
                sendAtCommand(h);
                String test = sendAtCommand("1003");
                if (test.contains("NO DATA") || test.contains("ERROR") || test.isEmpty()) {
                    test = sendAtCommand("1001");
                }
                if (!test.contains("NO DATA") && !test.contains("ERROR") && !test.isEmpty()) {
                    matchedHeader = h.replace("ATSH", "");
                    String swVer = hexToAscii(sendAtCommand("22F189"));
                    String hwVer = hexToAscii(sendAtCommand("22F191"));
                    String dtcConfirmed = sendAtCommand(modIdx == 0 ? "03" : "1902AF");
                    String dtcPending = (modIdx == 0) ? sendAtCommand("07") : "";

                    StringBuilder deepSb = new StringBuilder("✅ สแกนเชิงลึกสำเร็จ: ").append(moduleName).append(" [CAN ID: ").append(matchedHeader).append("]\n");
                    if (!hwVer.isEmpty()) deepSb.append("• รหัสฮาร์ดแวร์กล่อง (HW): ").append(hwVer).append("\n");
                    if (!swVer.isEmpty()) deepSb.append("• รหัสซอฟต์แวร์ (SW): ").append(swVer).append("\n");

                    String dtcText = translateRawDtc(dtcConfirmed);
                    String pendText = translateRawDtc(dtcPending);
                    if (!dtcText.isEmpty()) deepSb.append("• โค้ดเสียปัจจุบัน (Confirmed):\n").append(dtcText);
                    else deepSb.append("• โค้ดเสียปัจจุบัน: ปกติ (ไม่มีโค้ดค้าง)\n");

                    if (!pendText.isEmpty()) deepSb.append("• โค้ดรอเกิดซ้ำ (Pending):\n").append(pendText);

                    resp = deepSb.toString();
                    break;
                }
            }

            if (resp.isEmpty()) {
                resp = VehicleDatabase.analyzeObdResponse("NO DATA");
            }

            final String finalOutput = resp;
            String ecuInfo = readAndDisplayEcuIdentity(car);
            lastReportSummary = ecuInfo + "\n\n" + finalOutput;
            mainHandler.post(() -> {
                tvStatus.setText(finalOutput);
                appendLog("Deep Scan [" + moduleName + "] -> ID:" + matchedHeader);
            });
        }).start();
    }

    private String translateRawDtc(String rawResp) {
        if (rawResp == null || rawResp.isEmpty() || rawResp.contains("NO DATA") || rawResp.contains("ERROR")) return "";
        Map<String, String> dict = VehicleDatabase.getDtcDictionary();
        StringBuilder sb = new StringBuilder();
        String upper = rawResp.toUpperCase().replace(" ", "");
        for (Map.Entry<String, String> entry : dict.entrySet()) {
            String codeDigits = entry.getKey().substring(1);
            if (upper.contains(codeDigits)) {
                sb.append("   -> [").append(entry.getKey()).append("] ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private LinearLayout createRow() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    private Button createRowButton(String t, String c) {
        Button b = createButton(t, c);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(4, 5, 4, 5); b.setLayoutParams(p); return b;
    }
    private TextView createLabel(String t) {
        TextView tv = new TextView(this); tv.setText(t); tv.setTextColor(Color.parseColor("#CFD8DC")); tv.setTextSize(13); tv.setPadding(0, 10, 0, 4); return tv;
    }
    private Button createButton(String t, String c) {
        Button b = new Button(this); b.setText(t); b.setBackgroundColor(Color.parseColor(c)); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(4, 5, 4, 5); b.setLayoutParams(p); return b;
    }

    private void checkBluetoothPermissions() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 101);
            return;
        }
        loadPairedDevices();
    }

    private void loadPairedDevices() {
        try {
            Set<BluetoothDevice> bonded = btAdapter.getBondedDevices();
            List<String> names = new ArrayList<>(); pairedList.clear();
            for (BluetoothDevice dev : bonded) { pairedList.add(dev); names.add(dev.getName() + " [" + dev.getAddress() + "]"); }
            spDevices.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        } catch (SecurityException ignored) {}
    }

    private void connectSelectedDevice() {
        int pos = spDevices.getSelectedItemPosition();
        if (pos < 0 || pos >= pairedList.size()) { Toast.makeText(this, "กรุณาจับคู่หัวอ่านในตั้งค่า Bluetooth ก่อนครับ", Toast.LENGTH_SHORT).show(); return; }
        BluetoothDevice device = pairedList.get(pos);
        tvStatus.setText("กำลังเชื่อมต่อ " + device.getName() + "...");
        new Thread(() -> {
            try {
                if (btSocket != null && btSocket.isConnected()) btSocket.close();
                btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                btSocket.connect();
                outStream = btSocket.getOutputStream(); inStream = btSocket.getInputStream();
                sendAtCommand("ATZ"); sendAtCommand("ATE0"); sendAtCommand("ATL0");
                String volt = sendAtCommand("ATRV");
                mainHandler.post(() -> {
                    tvStatus.setText("✅ เชื่อมต่อหัวอ่านสำเร็จ! แรงดันไฟ OBD (ขา 16): " + volt);
                    appendLog("Connected -> Batt: " + volt);
                });
                readAndDisplayEcuIdentity(getSelectedCar());
            } catch (Exception e) { mainHandler.post(() -> tvStatus.setText("❌ เชื่อมต่อไม่สำเร็จ: " + e.getMessage())); }
        }).start();
    }

    private String readAndDisplayEcuIdentity(VehicleDatabase.CarProfile car) {
        sendAtCommand(car.headerCmd);
        String vinHex = sendAtCommand("0902");
        String calHex = sendAtCommand("0904");
        if (calHex.contains("NO DATA") || calHex.contains("ERROR") || calHex.isEmpty()) {
            calHex = sendAtCommand("22F187");
        }
        String vinText = hexToAscii(vinHex);
        String partNo = hexToAscii(calHex);
        String detectedYear = decodeVinYear(vinText);

        String displayPart = partNo.isEmpty() ? car.engineCode : partNo + " (" + car.engineCode + ")";
        String displayYear = detectedYear.isEmpty() ? car.modelYear : car.modelYear + " [ปีจริงจาก VIN: " + detectedYear + "]";

        String summary = "📟 ข้อมูลกล่องที่เชื่อมต่อ:\n" +
                         "• ตระกูลรถ / รุ่นปี: " + car.brand + " - " + displayYear + "\n" +
                         "• หมายเลขกล่อง (Part / Cal ID): " + displayPart +
                         (vinText.length() >= 10 ? "\n• เลขตัวถัง (VIN): " + vinText : "");

        mainHandler.post(() -> tvEcuIdentity.setText(summary));
        return summary;
    }

    private String hexToAscii(String hexResp) {
        if (hexResp == null || hexResp.contains("NO DATA") || hexResp.contains("ERROR") || hexResp.contains("7F")) return "";
        String clean = hexResp.replaceAll("[^0-9A-Fa-f]", "");
        clean = clean.replaceFirst("^(490201|490401|62F187|62F189|62F191|62F190)", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < clean.length(); i += 2) {
            try {
                int val = Integer.parseInt(clean.substring(i, i + 2), 16);
                if (val >= 32 && val <= 126) sb.append((char) val);
            } catch (Exception ignored) {}
        }
        return sb.toString().trim();
    }

    private String decodeVinYear(String vin) {
        if (vin == null || vin.length() < 10) return "";
        char code = Character.toUpperCase(vin.charAt(9));
        Map<Character, String> yearMap = new HashMap<>();
        yearMap.put('5', "2005"); yearMap.put('6', "2006"); yearMap.put('7', "2007");
        yearMap.put('8', "2008"); yearMap.put('9', "2009"); yearMap.put('A', "2010");
        yearMap.put('B', "2011"); yearMap.put('C', "2012"); yearMap.put('D', "2013");
        yearMap.put('E', "2014"); yearMap.put('F', "2015"); yearMap.put('G', "2016");
        yearMap.put('H', "2017"); yearMap.put('J', "2018"); yearMap.put('K', "2019");
        yearMap.put('L', "2020"); yearMap.put('M', "2021"); yearMap.put('N', "2022");
        yearMap.put('P', "2023"); yearMap.put('R', "2024"); yearMap.put('S', "2025");
        yearMap.put('T', "2026");
        return yearMap.containsKey(code) ? yearMap.get(code) : "";
    }

    private void readFaultCodes() {
        if (!isConnected()) return;
        isScanningLive = false;
        VehicleDatabase.CarProfile car = getSelectedCar();
        int mod = spModules.getSelectedItemPosition();
        String moduleName = spModules.getSelectedItem().toString();

        new Thread(() -> {
            String[] headers = getModuleHeadersByIndex(mod, car);
            String resp = "";
            String pendResp = "";
            for (String h : headers) {
                sendAtCommand(h);
                String cmd = (mod == 0) ? "03" : "1902AF";
                resp = sendAtCommand(cmd);
                if (resp.contains("NO DATA") || resp.contains("7F")) {
                    resp = sendAtCommand("03");
                }
                if (mod == 0) pendResp = sendAtCommand("07");
                if (!resp.contains("NO DATA") && !resp.contains("ERROR") && !resp.isEmpty()) break;
            }

            String translated = translateRawDtc(resp);
            String translatedPend = translateRawDtc(pendResp);
            StringBuilder sb = new StringBuilder("รายการโค้ดเสียเชิงลึก [" + moduleName + "]:\n");
            if (!translated.isEmpty()) sb.append("• โค้ดปัจจุบัน (Confirmed):\n").append(translated);
            if (!translatedPend.isEmpty()) sb.append("• โค้ดรอเกิดซ้ำ (Pending):\n").append(translatedPend);
            if (translated.isEmpty() && translatedPend.isEmpty()) {
                sb.append(VehicleDatabase.analyzeObdResponse(resp));
            }

            lastReportSummary = tvEcuIdentity.getText().toString() + "\n\n" + sb.toString();
            final String logResp = resp;
            mainHandler.post(() -> { tvStatus.setText(sb.toString()); appendLog("Deep DTC [" + moduleName + "] -> " + logResp); });
        }).start();
    }

    private void clearFaultCodes() {
        if (!isConnected()) return;
        isScanningLive = false;
        VehicleDatabase.CarProfile car = getSelectedCar();
        int mod = spModules.getSelectedItemPosition();
        String moduleName = spModules.getSelectedItem().toString();

        new Thread(() -> {
            String[] headers = getModuleHeadersByIndex(mod, car);
            String resp = "";
            for (String h : headers) {
                sendAtCommand(h);
                resp = sendAtCommand(mod == 0 ? "04" : "14FFFFFF");
                if (resp.contains("7F") || resp.contains("NO DATA")) {
                    resp = sendAtCommand("04");
                }
                if (resp.contains("54") || resp.contains("44") || resp.contains("OK")) break;
            }
            final String finalResp = resp;
            mainHandler.post(() -> {
                tvStatus.setText("สั่งลบโค้ดเสีย [" + moduleName + "] เรียบร้อย (คำตอบ: " + finalResp + ")");
                appendLog("Clear DTC [" + moduleName + "] -> " + finalResp);
            });
        }).start();
    }

    private void runActiveTest(String testName, int type) {
        if (!isConnected()) return;
        isScanningLive = false;
        VehicleDatabase.CarProfile car = getSelectedCar();
        String cmd = "";
        switch (type) {
            case 1: cmd = car.actInj1; break;
            case 2: cmd = car.actInj2; break;
            case 3: cmd = car.actInj3; break;
            case 4: cmd = car.actInj4; break;
            case 5: cmd = car.actCoil; break;
            case 6: cmd = car.actFan; break;
            case 7: cmd = car.actEgr; break;
        }
        final String finalCmd = cmd;
        tvStatus.setText("กำลังส่งคำสั่ง Active Test: " + testName + " (" + finalCmd + ")...");
        new Thread(() -> {
            sendAtCommand(car.headerCmd); sendAtCommand("1003");
            String resp = sendAtCommand(finalCmd);
            mainHandler.post(() -> {
                tvStatus.setText("ผลการทดสอบ [" + testName + "]:\n" + VehicleDatabase.analyzeObdResponse(resp));
                appendLog("ActiveTest [" + finalCmd + "] -> " + resp);
            });
        }).start();
    }

    private void toggleLiveData() {
        if (!isConnected()) return;
        isScanningLive = !isScanningLive;
        if (!isScanningLive) { tvStatus.setText("หยุดการอ่านค่าสดแล้ว"); return; }
        VehicleDatabase.CarProfile car = getSelectedCar();
        tvStatus.setText("กำลังอ่านค่าสดแบบเรียลไทม์ (" + car.engineCode + ")...");
        new Thread(() -> {
            sendAtCommand(car.headerCmd);
            while (isScanningLive && isConnected()) {
                int rpm = parseRpm(sendAtCommand(car.rpmPid));
                int speed = parseSpeed(sendAtCommand("010D"));
                int temp = parseTemp(sendAtCommand(car.coolantPid));
                int rail = parseRail(sendAtCommand(car.railPid));
                String volt = sendAtCommand("ATRV");
                double[] inj = parseInjectorFeedback(sendAtCommand(car.injFeedbackPid));

                mainHandler.post(() -> {
                    tvLiveData.setText("ค่าสด: รอบ: " + rpm + " RPM | เร็ว: " + speed + " km/h | น้ำ: " + temp + " °C | ราง: " + rail + " MPa | แบต: " + volt);
                    String injText = String.format(Locale.US, "💉 ชดเชยหัวฉีด (mm³/st): สูบ1: %+.2f | สูบ2: %+.2f | สูบ3: %+.2f | สูบ4: %+.2f",
                            inj, inj, inj, inj);
                    tvInjectorData.setText(injText);

                    if (dashRpm != null) dashRpm.setText("รอบเครื่องยนต์ (RPM)\n\n" + rpm);
                    if (dashSpeed != null) dashSpeed.setText("ความเร็ว (KM/H)\n\n" + speed);
                    if (dashTemp != null) dashTemp.setText("อุณหภูมิน้ำ (°C)\n\n" + temp);
                    if (dashRail != null) dashRail.setText("แรงดันราง (MPa)\n\n" + rail);
                    if (dashVolt != null) dashVolt.setText("แบตเตอรี่ (V)\n\n" + volt);
                    if (dashInj != null) {
                        dashInj.setText(String.format(Locale.US, "💉 ค่าชดเชยหัวฉีด (mm³/st):\nสูบ 1: %+.2f  |  สูบ 2: %+.2f\nสูบ 3: %+.2f  |  สูบ 4: %+.2f",
                                inj, inj, inj, inj));
                    }

                    boolean injAbnormal = (Math.abs(inj) > 3.0 || Math.abs(inj) > 3.0 || Math.abs(inj) > 3.0 || Math.abs(inj) > 3.0);
                    if (temp >= 98) {
                        tvAlert.setText("⚠️ แจ้งเตือนวิกฤต: อุณหภูมิน้ำสูงเกิน 98°C (" + temp + "°C)!");
                        tvAlert.setBackgroundColor(Color.parseColor("#C62828"));
                    } else if (injAbnormal) {
                        tvAlert.setText("⚠️ พบหัวฉีดชดเชยผิดปกติเกิน ±3.0 mm³/st (เช็กหัวฉีด/กำลังอัดสูบนั้น)");
                        tvAlert.setBackgroundColor(Color.parseColor("#EF6C00"));
                    } else {
                        tvAlert.setText("สถานะค่าเครื่องยนต์: ปกติ (สีเขียว) | เกณฑ์ชดเชยหัวฉีด ±3.0 mm³/st");
                        tvAlert.setBackgroundColor(Color.parseColor("#2E7D32"));
                    }
                    lastReportSummary = tvEcuIdentity.getText().toString() + "\n\n" + tvLiveData.getText().toString() + "\n" + injText;
                });
                try { Thread.sleep(450); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private double[] parseInjectorFeedback(String hex) {
        double[] vals = new double[]{0.0, 0.0, 0.0, 0.0};
        try {
            String c = hex.replaceAll("[^0-9A-Fa-f]", "");
            c = c.replaceFirst("^(6145|6130|6140|621410|621040|4106)", "");
            if (c.length() >= 8) {
                for (int i = 0; i < 4; i++) {
                    int raw = Integer.parseInt(c.substring(i * 2, (i * 2) + 2), 16);
                    vals[i] = (raw - 128) * 0.05;
                }
            }
        } catch (Exception ignored) {}
        return vals;
    }

    private void shareRepairReport() {
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String fullReport = "=== ใบรายงานตรวจเช็กรถยนต์ MaxOBDPro ===\nวันที่/เวลา: " + dateStr + "\n\n" + lastReportSummary + "\n\n----------------------------------------\nตรวจสอบโดยระบบ MaxOBDPro v6.3";
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, fullReport);
        startActivity(Intent.createChooser(sendIntent, "ส่งรายงานซ่อมให้ลูกค้าผ่าน..."));
    }

    private int parseRpm(String hex) {
        try { String c = hex.replaceAll("[^0-9A-Fa-f]", ""); if (c.length() >= 8) return ((Integer.parseInt(c.substring(4, 6), 16) * 256) + Integer.parseInt(c.substring(6, 8), 16)) / 4; } catch (Exception ignored) {}
        return 0;
    }
    private int parseSpeed(String hex) {
        try { String c = hex.replaceAll("[^0-9A-Fa-f]", ""); if (c.length() >= 6) return Integer.parseInt(c.substring(4, 6), 16); } catch (Exception ignored) {}
        return 0;
    }
    private int parseTemp(String hex) {
        try { String c = hex.replaceAll("[^0-9A-Fa-f]", ""); if (c.length() >= 6) return Integer.parseInt(c.substring(4, 6), 16) - 40; } catch (Exception ignored) {}
        return 0;
    }
    private int parseRail(String hex) {
        try { String c = hex.replaceAll("[^0-9A-Fa-f]", ""); if (c.length() >= 8) return ((Integer.parseInt(c.substring(4, 6), 16) * 256) + Integer.parseInt(c.substring(6, 8), 16)) / 100; } catch (Exception ignored) {}
        return 0;
    }

    private synchronized String sendAtCommand(String cmd) {
        try {
            while (inStream.available() > 0) inStream.read();
            outStream.write((cmd + "\r").getBytes()); outStream.flush();
            StringBuilder sb = new StringBuilder();
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 1800) {
                if (inStream.available() > 0) {
                    char c = (char) inStream.read();
                    if (c == '>') break;
                    sb.append(c);
                } else { Thread.sleep(20); }
            }
            return sb.toString().replace("\r", " ").replace("\n", " ").trim();
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    private boolean isConnected() {
        if (btSocket == null || !btSocket.isConnected()) { Toast.makeText(this, "กรุณาเชื่อมต่อหัวอ่าน OBD2 ก่อนครับ", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }
    private void appendLog(String msg) { mainHandler.post(() -> tvLog.append("• " + msg + "\n")); }
}
