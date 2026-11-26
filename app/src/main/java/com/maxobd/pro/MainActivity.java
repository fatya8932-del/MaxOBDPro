package com.maxobd.pro;
import android.Manifest;
import android.bluetooth.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
    private Spinner spDevices, spVehicles;
    private TextView tvStatus, tvLiveData, tvAlert, tvLog;
    private List<BluetoothDevice> pairedList = new ArrayList<>();
    private List<VehicleDatabase.CarProfile> vehicleList;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isScanningLive = false;
    private String lastReportSummary = "ยังไม่มีข้อมลการสแกน";

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
        root.setPadding(28, 28, 28, 28);

        TextView title = new TextView(this);
        title.setText("MaxOBDPro v5.2 - ศนยวิเคราะหและสแกนกล่อง ECU");
        title.setTextColor(Color.parseColor("#00E676"));
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        root.addView(createLabel("1. เลือกหัวอ่าน OBD2 (KONNWEI / ELM327):"));
        spDevices = new Spinner(this);
        spDevices.setBackgroundColor(Color.parseColor("#2C2C2C"));
        root.addView(spDevices);

        Button btnConnect = createButton("เชื่อมต่อหัวอ่าน OBD2", "#1976D2");
        btnConnect.setOnClickListener(v -> connectSelectedDevice());
        root.addView(btnConnect);

        root.addView(createLabel("2. เลือกรุ่นรถจากานข้อมลกลาง (ออฟไลน 100%):"));
        spVehicles = new Spinner(this);
        spVehicles.setBackgroundColor(Color.parseColor("#2C2C2C"));
        vehicleList = VehicleDatabase.getAllVehicles();
        spVehicles.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vehicleList));
        root.addView(spVehicles);

        LinearLayout row1 = createRow();
        Button btnAutoScan = createRowButton("สแกน & จับค่ปรตคอลอัตนมัติ", "#388E3C");
        btnAutoScan.setOnClickListener(v -> runAutoProtocolScan());
        Button btnReadDtc = createRowButton("อ่านค้ดเสีย (DTC)", "#F57C00");
        btnReadDtc.setOnClickListener(v -> readFaultCodes());
        row1.addView(btnAutoScan); row1.addView(btnReadDtc);
        root.addView(row1);

        LinearLayout row2 = createRow();
        Button btnLive = createRowButton("ค่าสดเรียลไทม (Live Data)", "#0097A7");
        btnLive.setOnClickListener(v -> toggleLiveData());
        Button btnClearDtc = createRowButton("ลบค้ดเสีย (Clear)", "#D32F2F");
        btnClearDtc.setOnClickListener(v -> clearFaultCodes());
        row2.addView(btnLive); row2.addView(btnClearDtc);
        root.addView(row2);

        root.addView(createLabel("3. หมดทดสอบการทำงานอุปกร (Active Test):"));
        LinearLayout row3 = createRow();
        Button btnTestInj = createRowButton("เทสตัดหัวีด", "#5E35B1");
        btnTestInj.setOnClickListener(v -> runActiveTest("ตัดหัวีดสบ 1", 1));
        Button btnTestFan = createRowButton("เทสพัดลมหม้อน้ำ", "#5E35B1");
        btnTestFan.setOnClickListener(v -> runActiveTest("เปิดพัดลมหม้อน้ำ", 2));
        Button btnTestEgr = createRowButton("เทสวาลว EGR/SCV", "#5E35B1");
        btnTestEgr.setOnClickListener(v -> runActiveTest("สั่งเปิดวาลว EGR/SCV", 3));
        row3.addView(btnTestInj); row3.addView(btnTestFan); row3.addView(btnTestEgr);
        root.addView(row3);

        tvLiveData = new TextView(this);
        tvLiveData.setText("ค่าสด: รอบ: -- RPM | น้ำ: -- C | แรงดันราง: -- MPa | แบต: -- V");
        tvLiveData.setTextColor(Color.parseColor("#FFEB3B"));
        tvLiveData.setTextSize(16);
        tvLiveData.setPadding(20, 20, 20, 12);
        tvLiveData.setBackgroundColor(Color.parseColor("#1E1E1E"));
        root.addView(tvLiveData);

        tvAlert = new TextView(this);
        tvAlert.setText("สถานะค่าเครื่องยนต: ปกติ (เขียว)");
        tvAlert.setTextColor(Color.WHITE);
        tvAlert.setBackgroundColor(Color.parseColor("#2E7D32"));
        tvAlert.setPadding(20, 10, 20, 10);
        root.addView(tvAlert);

        tvStatus = new TextView(this);
        tvStatus.setText("ผลการวิเคราะห: พร้อมทำงาน (รองรับตรวจสอบเก CAN-H 3.2V / CAN-L 1.8V)");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(15);
        tvStatus.setPadding(20, 20, 20, 20);
        tvStatus.setBackgroundColor(Color.parseColor("#263238"));
        root.addView(tvStatus);

        Button btnShareReport = createButton(" บันทึกและส่งรายงาน่อมให้ลกค้า (LINE / แชร)", "#00897B");
        btnShareReport.setOnClickListener(v -> shareRepairReport());
        root.addView(btnShareReport);

        tvLog = new TextView(this);
        tvLog.setText("Log การสื่อสาร:\n");
        tvLog.setTextColor(Color.parseColor("#B0BEC5"));
        tvLog.setTextSize(12);
        tvLog.setPadding(12, 12, 12, 12);
        root.addView(tvLog);

        scrollView.addView(root);
        setContentView(scrollView);
    }
    private LinearLayout createRow() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    private Button createRowButton(String t, String c) {
        Button b = createButton(t, c);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(5, 8, 5, 8); b.setLayoutParams(p); return b;
    }
    private TextView createLabel(String t) {
        TextView tv = new TextView(this); tv.setText(t); tv.setTextColor(Color.parseColor("#CFD8DC")); tv.setTextSize(14); tv.setPadding(0, 14, 0, 6); return tv;
    }
    private Button createButton(String t, String c) {
        Button b = new Button(this); b.setText(t); b.setBackgroundColor(Color.parseColor(c)); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(4, 8, 4, 8); b.setLayoutParams(p); return b;
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
        if (pos < 0 || pos >= pairedList.size()) { Toast.makeText(this, "กรุาจับค่หัวอ่านในตั้งค่า Bluetooth ก่อนครับ", Toast.LENGTH_SHORT).show(); return; }
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
                mainHandler.post(() -> { tvStatus.setText(" เชื่อมต่อสำเรจ! แรงดันไฟ OBD (ขา 16): " + volt); appendLog("Connected -> Batt: " + volt); });
            } catch (Exception e) { mainHandler.post(() -> tvStatus.setText(" เชื่อมต่อไม่สำเรจ: " + e.getMessage())); }
        }).start();
    }
    private void runAutoProtocolScan() {
        if (!isConnected()) return;
        isScanningLive = false;
        VehicleDatabase.CarProfile car = vehicleList.get(spVehicles.getSelectedItemPosition());
        tvStatus.setText("กำลังสแกนและจับค่ปรตคอลอัตนมัติสำหรับ " + car.modelName + "...");
        new Thread(() -> {
            sendAtCommand(car.protocolCmd); sendAtCommand(car.headerCmd);
            String resp = sendAtCommand(car.initCmd);
            if (resp.contains("NO DATA") || resp.contains("ERROR") || resp.isEmpty()) {
                mainHandler.post(() -> appendLog("ปรตคอลหลักไม่ตอบ -> กำลังสลับปรตคอลอัตนมัติ..."));
                String[] fallbackProtocols = {"ATSP6", "ATSP7", "ATSP5", "ATSP4", "ATSP0"};
                for (String proto : fallbackProtocols) {
                    sendAtCommand(proto);
                    String testResp = sendAtCommand("0100");
                    if (!testResp.contains("NO DATA") && !testResp.contains("ERROR") && !testResp.isEmpty()) {
                        resp = "จับค่สำเรจด้วยปรตคอล " + proto + " -> " + testResp;
                        break;
                    }
                }
            }
            final String finalAnalysis = VehicleDatabase.analyzeObdResponse(resp);
            final String raw = resp;
            lastReportSummary = "รุ่นรถ: " + car.toString() + "\nผลการตรวจเชก:\n" + finalAnalysis;
            mainHandler.post(() -> { tvStatus.setText(finalAnalysis); appendLog("Scan Result: " + raw); });
        }).start();
    }
    private void readFaultCodes() {
        if (!isConnected()) return;
        isScanningLive = false;
        VehicleDatabase.CarProfile car = vehicleList.get(spVehicles.getSelectedItemPosition());
        new Thread(() -> {
            String resp = sendAtCommand("03");
            Map<String, String> dict = VehicleDatabase.getDtcDictionary();
            StringBuilder sb = new StringBuilder("รายการค้ดเสีย (DTC) - " + car.modelName + ":\n");
            boolean found = false;
            for (Map.Entry<String, String> entry : dict.entrySet()) {
                if (resp.toUpperCase().contains(entry.getKey().substring(1))) {
                    sb.append(" ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    found = true;
                }
            }
            if (!found) sb.append(VehicleDatabase.analyzeObdResponse(resp));
            lastReportSummary = sb.toString();
            mainHandler.post(() -> { tvStatus.setText(sb.toString()); appendLog("DTC (03) -> " + resp); });
        }).start();
    }
    private void clearFaultCodes() {
        if (!isConnected()) return;
        isScanningLive = false;
        new Thread(() -> {
            String resp = sendAtCommand("04");
            mainHandler.post(() -> { tvStatus.setText("สั่งลบค้ดเสียเรียบร้อย (คำตอบ: " + resp + ")"); appendLog("Clear DTC -> " + resp); });
        }).start();
    }
    private void runActiveTest(String testName, int type) {
        if (!isConnected()) return;
        isScanningLive = false;
        VehicleDatabase.CarProfile car = vehicleList.get(spVehicles.getSelectedItemPosition());
        String cmd = (type == 1) ? car.actInjector : (type == 2) ? car.actFan : car.actEgr;
        tvStatus.setText("กำลังส่งคำสั่ง Active Test: " + testName + " (" + cmd + ")...");
        new Thread(() -> {
            sendAtCommand(car.headerCmd); sendAtCommand("1003");
            String resp = sendAtCommand(cmd);
            mainHandler.post(() -> {
                tvStatus.setText("ผลการทดสอบ [" + testName + "]:\n" + VehicleDatabase.analyzeObdResponse(resp));
                appendLog("ActiveTest [" + cmd + "] -> " + resp);
            });
        }).start();
    }
    private void toggleLiveData() {
        if (!isConnected()) return;
        isScanningLive = !isScanningLive;
        if (!isScanningLive) { tvStatus.setText("หยุดการอ่านค่าสดแล้ว"); return; }
        VehicleDatabase.CarProfile car = vehicleList.get(spVehicles.getSelectedItemPosition());
        tvStatus.setText("กำลังอ่านค่าสดแบบเรียลไทม (" + car.engineCode + ")...");
        new Thread(() -> {
            while (isScanningLive && isConnected()) {
                int rpm = parseRpm(sendAtCommand(car.rpmPid));
                int temp = parseTemp(sendAtCommand(car.coolantPid));
                int rail = parseRail(sendAtCommand(car.railPid));
                String volt = sendAtCommand("ATRV");
                mainHandler.post(() -> {
                    tvLiveData.setText("ค่าสด: รอบ: " + rpm + " RPM | น้ำ: " + temp + " C | ราง: " + rail + " MPa | แบต: " + volt);
                    if (temp >= 98) {
                        tvAlert.setText(" แจ้งเตือนวิกต: อุหภมิน้ำสงเกิน 98C (" + temp + "C)!");
                        tvAlert.setBackgroundColor(Color.parseColor("#C62828"));
                    } else {
                        tvAlert.setText("สถานะค่าเครื่องยนต: ปกติ (เขียว)");
                        tvAlert.setBackgroundColor(Color.parseColor("#2E7D32"));
                    }
                });
                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            }
        }).start();
    }
    private void shareRepairReport() {
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String fullReport = "=== ใบรายงานตรวจเชกรถยนต MaxOBDPro ===\nวันที่/เวลา: " + dateStr + "\n\n" + lastReportSummary + "\n\n----------------------------------------\nตรวจสอบดยระบบ MaxOBDPro v5.2";
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, fullReport);
        startActivity(Intent.createChooser(sendIntent, "ส่งรายงาน่อมให้ลกค้าผ่าน..."));
    }
    private int parseRpm(String hex) {
        try { String c = hex.replaceAll("[^0-9A-Fa-f]", ""); if (c.length() >= 8) return ((Integer.parseInt(c.substring(4, 6), 16) * 256) + Integer.parseInt(c.substring(6, 8), 16)) / 4; } catch (Exception ignored) {}
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
            while (System.currentTimeMillis() - start < 2200) {
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
        if (btSocket == null || !btSocket.isConnected()) { Toast.makeText(this, "กรุาเชื่อมต่อหัวอ่าน OBD2 ก่อนครับ", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }
    private void appendLog(String msg) { mainHandler.post(() -> tvLog.append(" " + msg + "\n")); }
}
