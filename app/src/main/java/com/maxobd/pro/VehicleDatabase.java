package com.maxobd.pro;
import java.util.*;

public class VehicleDatabase {
    public static class CarProfile {
        public String brand, modelYear, engineCode, protocolCmd, headerCmd, initCmd;
        public String rpmPid, coolantPid, railPid, injFeedbackPid;
        public String actInj1, actInj2, actInj3, actInj4, actCoil, actFan, actEgr;

        public CarProfile(String b, String m, String e, String p, String h, String i,
                          String r, String c, String rp, String ifp,
                          String ai1, String ai2, String ai3, String ai4,
                          String ac, String af, String ae) {
            brand=b; modelYear=m; engineCode=e; protocolCmd=p; headerCmd=h; initCmd=i;
            rpmPid=r; coolantPid=c; railPid=rp; injFeedbackPid=ifp;
            actInj1=ai1; actInj2=ai2; actInj3=ai3; actInj4=ai4;
            actCoil=ac; actFan=af; actEgr=ae;
        }
        @Override public String toString() { return modelYear + " [" + engineCode + "]"; }
    }

    public static List<String> getBrands() {
        List<String> brands = new ArrayList<>();
        for (CarProfile c : getAllVehicles()) {
            if (!brands.contains(c.brand)) brands.add(c.brand);
        }
        return brands;
    }

    public static List<CarProfile> getModelsByBrand(String brand) {
        List<CarProfile> filtered = new ArrayList<>();
        for (CarProfile c : getAllVehicles()) {
            if (c.brand.equals(brand)) filtered.add(c);
        }
        return filtered;
    }

    public static List<CarProfile> getAllVehicles() {
        List<CarProfile> list = new ArrayList<>();

        // 1. TOYOTA
        list.add(new CarProfile("Toyota", "Hilux Vigo (ปี 2004-2008 รุ่นแรก K-Line)", "1KD / 2KD-FTV", "ATSP5", "ATSH8110F1", "81",
                "2101", "2105", "2110", "2145", "300101", "300102", "300103", "300104", "300401", "300201", "300301"));
        list.add(new CarProfile("Toyota", "Hilux Vigo Champ / Fortuner (ปี 2009-2015)", "1KD / 2KD VN-Turbo", "ATSP6", "ATSH7E0", "1003",
                "2101", "2105", "2110", "2145", "300101", "300102", "300103", "300104", "300401", "300201", "300301"));
        list.add(new CarProfile("Toyota", "Hilux Revo / Fortuner (ปี 2015-2026)", "1GD / 2GD-FTV", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "0123", "2145", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Toyota", "Vios / Yaris / Altis / Camry / Cross (เบนซิน/Hybrid)", "1NZ / 3NR / 1ZR / 2ZR", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "010E", "0106", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        // 2. ISUZU
        list.add(new CarProfile("Isuzu", "D-Max Commonrail รุ่นแรก / Platinum (ปี 2005-2011)", "4JK1 / 4JJ1 (K-Line)", "ATSP5", "ATSH8111F1", "81",
                "2101", "2105", "2110", "2130", "300501", "300502", "300503", "300504", "300801", "300601", "300701"));
        list.add(new CarProfile("Isuzu", "All New D-Max / MU-X (ปี 2012-2015)", "4JK1-TC / 4JJ1-TCX", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "0123", "221410", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Isuzu", "D-Max / MU-X 1.9 & 3.0 Blue Power (ปี 2016-2026)", "RZ4E-TC / 4JJ3-TCX", "ATSP6", "ATSH7E0", "1003",
                "22F40C", "22F405", "22F423", "221410", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        // 3. MITSUBISHI
        list.add(new CarProfile("Mitsubishi", "Strada / Triton รุ่นแรก (ปี 2005-2007 K-Line)", "4D56 KWP2000", "ATSP5", "ATSH8110F1", "81",
                "2109", "2105", "2123", "2140", "300101", "300102", "300103", "300104", "300401", "300201", "300301"));
        list.add(new CarProfile("Mitsubishi", "Triton / Pajero Sport (ปี 2008-2014 CAN)", "4D56 / 4M41 (1860A523)", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "0123", "2140", "2F030101", "2F030102", "2F030104", "2F030108", "2F070103", "2F050103", "2F060103"));
        list.add(new CarProfile("Mitsubishi", "All New Triton / Pajero Sport (ปี 2015-2026)", "4N15 MIVEC / 4N16", "ATSP6", "ATSH7E0", "1003",
                "221001", "221005", "221023", "221040", "2F040101", "2F040102", "2F040104", "2F040108", "2F070103", "2F050103", "2F060103"));
        list.add(new CarProfile("Mitsubishi", "Attrage / Mirage / Xpander (ปี 2012-2026)", "3A92 / 4A91 (เบนซิน)", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "010E", "0106", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        // 4. HONDA
        list.add(new CarProfile("Honda", "Civic ES / Jazz GD / City ZX (ปี 2001-2007)", "D17 / L15A (เบนซิน)", "ATSP5", "ATSH8110F1", "81",
                "010C", "0105", "010E", "0106", "300101", "300102", "300103", "300104", "300401", "300201", "300301"));
        list.add(new CarProfile("Honda", "Civic FD-FE / City Turbo / Jazz / HR-V (ปี 2006-2026)", "R18 / L15 / e:HEV", "ATSP7", "ATSH18DA10F1", "1003",
                "010C", "0105", "010E", "0106", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        // 5. NISSAN
        list.add(new CarProfile("Nissan", "Navara D40 / NP300 / Terra (ปี 2007-2026)", "YD25DDTi / YS23", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "0123", "2140", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Nissan", "March / Almera / Note / Kicks (ปี 2010-2026)", "HR12 / HR10 Turbo", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "010E", "0106", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        // 6. FORD & MAZDA
        list.add(new CarProfile("Ford / Mazda", "Ranger T6-NextGen / Everest / BT-50 Pro (ปี 2012-2026)", "2.0 / 2.2 / 3.2 ดีเซล", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "0123", "221410", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Ford / Mazda", "Mazda 2 / Mazda 3 / CX-5 / CX-30 (ปี 2014-2026)", "Skyactiv-G / Skyactiv-D", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "0123", "221410", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        // 7. SUZUKI
        list.add(new CarProfile("Suzuki", "Swift / Carry / Ertiga / XL7 (ปี 2012-2026)", "K12B / K12M / K15B", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "010E", "0106", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        // 8. รถจีน & รถไฟฟ้า EV
        list.add(new CarProfile("BYD / Denza", "Atto 3 / Dolphin / Seal / M6 (ปี 2022-2026)", "e-Platform 3.0 EV", "ATSP6", "ATSH7E0", "1003",
                "22F40C", "22F405", "22F423", "22F40B", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("MG (SAIC)", "MG4 / ZS / EP / Extender / MG5 (ปี 2017-2026)", "ICE / EV SAIC UDS", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "0123", "0106", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Chery / Aion / Changan / GWM", "Omoda / Jaecoo / Aion Y / Deepal / Ora / Haval", "EV / HEV UDS CAN", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "010E", "0106", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Geely / Zeekr / Volvo / Neta", "Zeekr X / Volvo EX30 / Neta V-X", "EV UDS CAN", "ATSP6", "ATSH7E0", "1003",
                "22F40C", "22F405", "22F423", "22F40B", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        // 9. รถเกาหลี & รถยุโรป
        list.add(new CarProfile("Hyundai / Kia / BMW / Benz", "H-1 / Staria / Carnival / Series 3-5 / C-Class", "CRDi / B47 / OM651", "ATSP6", "ATSH7E0", "1003",
                "010C", "0105", "0123", "2140", "2F010101", "2F010102", "2F010104", "2F010108", "2F040103", "2F020103", "2F030103"));

        return list;
    }

    public static String getObdAndEcuPinoutManual() {
        return "=== 1. ผังขาปลั๊ก OBD2 มาตรฐาน 16 ขา (มองหน้าตรงปลั๊กตัวเมียที่รถ) ===\n" +
               "• ขา 4: กราวด์ตัวถัง (Chassis GND)\n" +
               "• ขา 5: กราวด์สัญญาณ (Signal GND)\n" +
               "• ขา 6: CAN-High (สัญญาณปกติ ~2.8V - 3.2V)\n" +
               "• ขา 7: K-Line (รถรุ่นเก่า 2002-2010 ไฟปกติ ~9V - 12V)\n" +
               "• ขา 14: CAN-Low (สัญญาณปกติ ~1.8V - 2.2V)\n" +
               "• ขา 15: L-Line (ปลุกระบบ KWP2000 รุ่นเก่า)\n" +
               "• ขา 16: ไฟแบตเตอรี่ +12V ตลอดเวลา (B+)\n\n" +
               "=== 2. เกณฑ์วัดไฟ CAN Bus บนโต๊ะ (Bench Test) ===\n" +
               "• ใส่ตัวต้านทาน 120 โอห์ม คร่อมระหว่างขา CAN-H และ CAN-L เสมอ\n" +
               "• ค่าปกติ: CAN-H (~3.2V) + CAN-L (~1.8V) = รวมกันได้ ~5.0V\n" +
               "• ถ้าวัดได้ 2.5V เท่ากันทั้งคู่และไม่สวิงตอนกดสแกน = กล่องเงียบ (เช็กไฟ IG-ON, ระบบ Immo, ภาคจ่ายไฟ 5V ในกล่อง หรือชิป MCU เสีย)\n\n" +
               "=== 3. ผังขาไฟเลี้ยงและสื่อสารกล่อง ECU ยอดฮิต (ต่อบนโต๊ะ) ===\n" +
               "▶ กล่อง Mitsubishi Triton / Pajero 4D56 (เบอร์ 1860A523 ปลั๊ก 4 ช่อง):\n" +
               "  - ไฟบวกตรง (B+): ขา 65 (ปลั๊ก C)\n" +
               "  - ไฟสวิตช์กุญแจ (IG-ON): ขา 73 (ปลั๊ก C)\n" +
               "  - ไฟเลี้ยงรีเลย์หลัก (Main Relay): ขา 55, 56 (ปลั๊ก C)\n" +
               "  - กราวด์ (GND): ขา 49, 50, 61, 62 (ปลั๊ก C)\n" +
               "  - CAN-High: ขา 85 (ปลั๊ก D) -> ต่อเข้า OBD ขา 6\n" +
               "  - CAN-Low: ขา 86 (ปลั๊ก D) -> ต่อเข้า OBD ขา 14\n\n" +
               "▶ กล่อง Toyota Vigo / Revo (1KD / 2KD / 1GD):\n" +
               "  - B+ (ไฟตรง): ขา B1 / E5-1\n" +
               "  - IGSW (ไฟกุญแจ ON): ขา IG2 / E5-9\n" +
               "  - +B / +B2 (ไฟเมนรีเลย์): ขา E5-2, E5-3\n" +
               "  - E1 (กราวด์): ขา E6-1, E6-2\n" +
               "  - CAN-High (CANH): ขา E5-18 -> OBD ขา 6\n" +
               "  - CAN-Low (CANL): ขา E5-19 -> OBD ขา 14\n" +
               "  - K-Line (SIL - เฉพาะ Vigo รุ่นแรก): ขา E5-13 -> OBD ขา 7\n\n" +
               "▶ กล่อง Isuzu D-Max (4JK1 / 4JJ1 / 1.9 RZ4E):\n" +
               "  - ไฟตรง (B+): ขา 1 (ปลั๊กเล็ก)\n" +
               "  - ไฟกุญแจ (IG-ON): ขา 28 (ปลั๊กเล็ก)\n" +
               "  - ไฟเมน (Main Power): ขา 3, 5 (ปลั๊กเล็ก)\n" +
               "  - กราวด์ (GND): ขา 2, 4, 6 (ปลั๊กเล็ก)\n" +
               "  - CAN-High: ขา 43 -> OBD ขา 6 | CAN-Low: ขา 44 -> OBD ขา 14";
    }

    public static Map<String, String> getDtcDictionary() {
        Map<String, String> dtc = new LinkedHashMap<>();
        dtc.put("P0300", "ECU: ตรวจพบการจุดระเบิดผิดพลาดหลายสูบ (Random Misfire) - เช็กคอยล์และหัวเทียน");
        dtc.put("P0301", "ECU: สูบที่ 1 จุดระเบิดผิดพลาด (Cylinder 1 Misfire)");
        dtc.put("P0302", "ECU: สูบที่ 2 จุดระเบิดผิดพลาด (Cylinder 2 Misfire)");
        dtc.put("P0303", "ECU: สูบที่ 3 จุดระเบิดผิดพลาด (Cylinder 3 Misfire)");
        dtc.put("P0304", "ECU: สูบที่ 4 จุดระเบิดผิดพลาด (Cylinder 4 Misfire)");
        dtc.put("P0351", "ECU: วงจรควบคุมคอยล์จุดระเบิด A (สูบ 1) ขัดข้อง");
        dtc.put("P0380", "ECU: วงจรหัวเผา (Glow Plug) ขัดข้อง - เช็กรีเลย์หัวเผาและสายไฟ");
        dtc.put("P0087", "ECU: แรงดันน้ำมันในรางคอมมอนเรลต่ำเกินไป (Rail Pressure Low)");
        dtc.put("P0088", "ECU: แรงดันน้ำมันในรางคอมมอนเรลสูงเกินไป (SCV วาล์วค้าง)");
        dtc.put("P0400", "ECU: ระบบหมุนเวียนไอเสีย EGR ทำงานผิดปกติ");
        dtc.put("P0700", "TCU: กล่องเกียร์อัตโนมัติแจ้งเตือนความผิดปกติ");
        dtc.put("C0031", "ABS: เซ็นเซอร์ความเร็วล้อหน้าซ้าย (FL Wheel Speed Sensor) ขัดข้อง");
        dtc.put("C0034", "ABS: เซ็นเซอร์ความเร็วล้อหน้าขวา (FR Wheel Speed Sensor) ขัดข้อง");
        dtc.put("C0037", "ABS: เซ็นเซอร์ความเร็วล้อหลังซ้าย (RL Wheel Speed Sensor) ขัดข้อง");
        dtc.put("C0040", "ABS: เซ็นเซอร์ความเร็วล้อหลังขวา (RR Wheel Speed Sensor) ขัดข้อง");
        dtc.put("C0200", "ABS: สัญญาณเซ็นเซอร์ล้อหน้าขวาขาดหาย (Toyota/Isuzu) - เช็กสายเซ็นเซอร์และลูกปืนล้อ");
        dtc.put("C0205", "ABS: สัญญาณเซ็นเซอร์ล้อหน้าซ้ายขาดหาย");
        dtc.put("C1241", "ABS: แรงดันไฟเลี้ยงปั๊ม ABS ต่ำหรือสูงผิดปกติ - เช็กฟิวส์ ABS และไดชาร์จ");
        dtc.put("B0100", "SRS: วงจรถุงลมนิรภัยฝั่งคนขับ (Driver Airbag Squib) ขาดหรือช็อต - เช็กสายแพรสไปร่อนคอพวงมาลัย");
        dtc.put("B0101", "SRS: ความต้านทานวงจรถุงลมคนขับสูงเกินไป (ปลั๊กหลวมหรือสายแพรขาด)");
        dtc.put("B0103", "SRS: วงจรถุงลมนิรภัยฝั่งผู้โดยสาร (Passenger Airbag) ขัดข้อง");
        dtc.put("B1200", "BCM: วงจรไฟหน้า / ไฟหรี่ขาดหรือช็อตลงกราวด์");
        dtc.put("U0073", "CAN: สายสื่อสาร CAN Bus ล่ม (Bus Off) - เช็กสาย CAN-H/L และ R 120 โอห์ม");
        dtc.put("U0100", "CAN: ขาดการติดต่อกับกล่อง ECU เครื่องยนต์");
        dtc.put("U0121", "CAN: ขาดการติดต่อกับกล่องเบรก ABS");
        dtc.put("U0151", "CAN: ขาดการติดต่อกับกล่องถุงลมนิรภัย SRS");
        return dtc;
    }

    public static String analyzeObdResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "⚠️ ไม่ได้รับข้อมูลตอบกลับ (Timeout) - กรุณาเช็กการเชื่อมต่อหัวอ่าน";
        }
        String res = rawResponse.toUpperCase();
        if (res.contains("CAN ERROR") || res.contains("FB ERROR") || res.contains("BUS INIT: ERROR")) {
            return "❌ สายสื่อสาร CAN Bus / K-Line มีปัญหา (CAN ERROR)\n" +
                   "• เช็กสาย CAN-H (ขา 6) / CAN-L (ขา 14) ขาด, ช็อต หรือลืมใส่ตัวต้านทาน 120 โอห์ม\n" +
                   "• เกณฑ์มาตรฐานปกติ: CAN-H ~3.2V / CAN-L ~1.8V";
        }
        if (res.contains("NO DATA") || res.contains("STOPPED") || res.contains("?")) {
            return "⚠️ จับสายบัสได้ แต่กล่องเป้าหมายไม่ตอบสนอง (NO DATA)\n" +
                   "• หากสแกนกล่องเครื่องยนต์: เช็กไฟเลี้ยงกล่อง (B+/IG), Immo หรือภาคจ่ายไฟ 5V / MCU เสีย (ไฟค้าง 2.5V เท่ากัน)\n" +
                   "• หากสแกน ABS / SRS: รถรุ่นนี้อาจไม่มีระบบนี้ หรือใช้สาย MS-CAN (ขา 3/11)";
        }
        if (res.contains("7F")) {
            return "ℹ️ กล่องตอบสนองดี (สัญญาณ CAN 3.2V/1.8V ปกติ) แต่ปฏิเสธคำสั่ง (" + res.trim() + ")";
        }
        return "✅ เชื่อมต่อกล่องสำเร็จ (สัญญาณ CAN Active ~3.2V / 1.8V)\nข้อมูลตอบกลับ: " + res.trim();
    }
}
