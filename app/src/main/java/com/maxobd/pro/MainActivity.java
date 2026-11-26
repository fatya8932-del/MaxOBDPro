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
    private Spinner spDevices, spBrands, spModels;
    private TextView tvEcuIdentity, tvStatus, tvLiveData, tvAlert, tvLog;
    private List<BluetoothDevice> pairedList = new ArrayList<>();
    private List<String> brandList;
    private List<VehicleDatabase.CarProfile> currentModelList = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isScanningLive = false;
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
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("MaxOBDPro v5.5 - ศูนย์วิเคราะห์และสแกนกล่อง ECU");
        title.setTextColor(Color.parseColor("#00E676"));
        title.setTextSize(19);
        title.setPadding(0, 0, 0, 12);
        root.addView(title);

        // กล่องป้ายโชว์ข้อมูลประจำตัวกล่อง ECU
        tvEcuIdentity = new TextView(this);
        tvEcuIdentity.setText("📟 ข้อมูลกล่อง ECU: ยังไม่ได้เชื่อมต่อ\n• ตระกูลรถ / รุ่นปี: --\n• หมายเลขกล่อง (Part / Cal ID): --");
        tvEcuIdentity.setTextColor(Color.parseColor("#E1F5FE"));
        tvEcuIdentity.setTextSize(14);
        tvEcuIdentity.setPadding(18, 16, 18, 16);
        tvEcuIdentity.setBackgroundColor(Color.parseColor("#0D47A1"));
        root.addView(tvEcuIdentity);

        root.addView(createLabel("1. เลือกหัวอ่าน OBD2 (KONNWEI / ELM327):"));
        spDevices = new Spinner(this);
        spDevices.setBackgroundColor(Color.parseColor("#2C2C2C"));
        root.addView(spDevices);

        Button btnConnect = createButton("เชื่อมต่อหัวอ่าน OBD2", "#1976D2");
        btnConnect.setOnClickListener(v -> connectSelectedDevice());
        root.addView(btnConnect);

        root.addView(createLabel("2. เลือกยี่ห้อรถยนต์:"));
        spBrands = new Spinner(this);
        spBrands.setBackgroundColor(Color.parseColor("#2C2C2C"));
        brandList = VehicleDatabase.getBrands();
        spBrands.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, brandList));
        root.addView(spBrands);

        root.addView(createLabel("3. เลือกรุ่นย่อย / ปีผลิต / รหัสเครื่องยนต์:"));
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

        LinearLayout row1 = createRow();
        Button btnAutoScan = createRowButton("สแกน & อ่านหมายเลขกล่อง", "#388E3C");
        btnAutoScan.setOnClickListener(v -> runAutoProtocolScan());
        Button btnReadDtc = createRowButton("อ่านโค้ดเสีย (DTC)", "#F57C00");
        btnReadDtc.setOnClickListener(v -> readFaultCodes());
        row1.addView(btnAutoScan); row1.addView(btnReadDtc);
        root.addView(row1);

        LinearLayout row2 = createRow();
        Button btnLive = createRowButton("ค่าสดเรียลไทม์ (Live Data)", "#0097A7");
        btnLive.setOnClickListener(v -> toggleLiveData());
        Button btnClearDtc = createRowButton("ลบโค้ดเสีย (Clear DTC)", "#D32F2F");
        btnClearDtc.setOnClickListener(v -> clearFaultCodes());
        row2.addView(btnLive); row2.addView(btnClearDtc);
        root.addView(row2);

        // โหมดทดสอบการทำงานอุปกรณ์ (Active Test) จัดเป็น 2 แถว แถวละ 2 ปุ่ม เต็มจออ่านง่าย
        root.addView(createLabel("4. โหมดทดสอบการทำงานอุปกรณ์ (Active Test):"));
        LinearLayout row3a = createRow();
        Button btnTestInj = createRowButton("เทสตัดหัวฉีด", "#5E35B1");
        btnTestInj.setOnClickListener(v -> runActiveTest("ตัดหัวฉีดสูบ 1", 1));
        Button btnTestCoil = createRowButton("เทสคอยล์ / หัวเทียน", "#6A1B9A");
        btnTestCoil.setOnClickListener(v -> runActiveTest("สั่งจุดระเบิดคอยล์ / หัวเทียนสูบ 1", 2));
        row3a.addView(btnTestInj); row3a.addView(btnTestCoil);
        root.addView(row3a);

        LinearLayout row3b = createRow();
        Button btnTestFan = createRowButton("เทสพัดลมหม้อน้ำ", "#5E35B1");
        btnTestFan.setOnClickListener(v -> runActiveTest("เปิดพัดลมหม้อน้ำ", 3));
        Button btnTestEgr = createRowButton("เทสวาล์ว EGR / SCV", "#5E35B1");
        btnTestEgr.setOnClickListener(v -> runActiveTest("สั่งเปิดวาล์ว EGR / SCV", 4));
        row3b.addView(btnTestFan); row3b.addView(btnTestEgr);
        root.addView(row3b);

        tvLiveData = new TextView(this);
        tvLiveData.setText("ค่าสด: รอบ: -- RPM | น้ำ: -- °C | แรงดันราง: -- MPa | แบต: -- V");
        tvLiveData.setTextColor(Color.parseColor("#FFEB3B"));
        tvLiveData.setTextSize(15);
        tvLiveData.setPadding(18, 18, 18, 12);
        tvLiveData.setBackgroundColor(Color.parseColor("#1E1E1E"));
        root.addView(tvLiveData);

        tvAlert = new TextView(this);
        tvAlert.setText("สถานะค่าเครื่องยนต์: ปกติ (สีเขียว)");
        tvAlert.setTextColor(Color.WHITE);
        tvAlert.setBackgroundColor(Color.parseColor("#2E7D32"));
        tvAlert.setPadding(18, 10, 18, 10);
        root.addView(tvAlert);

        tvStatus = new TextView(this);
        tvStatus.setText("ผลการวิเคราะห์: พร้อมทำงาน (รองรับตรวจสอบเกณฑ์ CAN-H 3.2V / CAN-L 1.8V)");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(14);
        tvStatus.setPadding(18, 18, 18, 18);
        tvStatus.setBackgroundColor(Color.parseColor("#263238"));
        root.addView(tvStatus);

        Button btnShareReport = createButton("📄 บันทึกและส่งรายงานซ่อมให้ลูกค้า (LINE / แชร์)", "#00897B");
        btnShareReport.setOnClickListener(v -> shareRepairReport());
        root.addView(btnShareReport);

        tvLog = new TextView(this);
        tvLog.setText("บันทึกการสื่อสาร (Log):\n");
        tvLog.setTextColor(Color.parseColor("#B0BEC5"));
        tvLog.setTextSize(12);
        tvLog.setPadding(12, 12, 12, 12);
        root.addView(tvLog);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private VehicleDatabase.CarProfile getSelectedCar() {
        int pos = spModels.getSelectedItemPosition();
        if (pos >= 0 && pos < currentModelList.size()) return currentModelList.get(pos);
        return VehicleDatabase.getAllVehicles().get(0);
    }

    private LinearLayout createRow() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    private Button createRowButton(String t, String c) {
        Button b = createButton(t, c);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(5, 6, 5, 6); b.setLayoutParams(p); return b;
    }
    private TextView createLabel(String t) {
        TextView tv = new TextView(this); tv.setText(t); tv.setTextColor(Color.parseColor("#CFD8DC")); tv.setTextSize(14); tv.setPadding(0, 12, 0, 6); return tv;
    }
    private Button createButton(String t, String c) {
        Button b = new Button(this); b.setText(t); b.setBackgroundColor(Color.parseColor(c)); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(4, 6, 4, 6); b.setLayoutParams(p); return b;
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
