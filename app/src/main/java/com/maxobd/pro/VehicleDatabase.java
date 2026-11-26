package com.maxobd.pro;
import java.util.*;

public class VehicleDatabase {
    public static class CarProfile {
        public String brand, modelYear, engineCode, protocolCmd, headerCmd, initCmd, rpmPid, coolantPid, railPid, boostPid;
        public String actInjector, actCoil, actFan, actEgr;

        public CarProfile(String b, String m, String e, String p, String h, String i, String r, String c, String rp, String bp,
                          String ai, String ac, String af, String ae) {
            brand=b; modelYear=m; engineCode=e; protocolCmd=p; headerCmd=h; initCmd=i;
            rpmPid=r; coolantPid=c; railPid=rp; boostPid=bp;
            actInjector=ai; actCoil=ac; actFan=af; actEgr=ae;
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
        list.add(new CarProfile("Toyota", "Hilux Vigo (ปี 2004-2008 รุ่นแรก K-Line)", "1KD / 2KD-FTV", "ATSP5", "ATSH8110F1", "81", "2101", "2105", "2110", "210B", "300101", "300401", "300201", "300301"));
        list.add(new CarProfile("Toyota", "Hilux Vigo Champ / Fortuner (ปี 2009-2015)", "1KD / 2KD VN-Turbo", "ATSP6", "ATSH7E0", "1003", "2101", "2105", "2110", "210B", "300101", "300401", "300201", "300301"));
        list.add(new CarProfile("Toyota", "Hilux Revo / Fortuner (ปี 2015-2026)", "1GD / 2GD-FTV", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Toyota", "Vios / Yaris (ปี 2003-2012 Gen 1-2)", "1NZ-FE (เบนซิน)", "ATSP4", "ATSH686AF1", "81", "010C", "0105", "010E", "010B", "300101", "300401", "300201", "300301"));
        list.add(new CarProfile("Toyota", "All New Vios / Yaris / Ativ (ปี 2013-2026)", "1NZ / 3NR-FE (เบนซิน)", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Toyota", "Altis / Camry / Corolla Cross (ปี 2008-2026)", "1ZR / 2ZR / Hybrid", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));

        // 2. ISUZU
        list.add(new CarProfile("Isuzu", "D-Max Commonrail รุ่นแรก / Platinum (ปี 2005-2011)", "4JK1 / 4JJ1 (K-Line)", "ATSP5", "ATSH8111F1", "81", "2101", "2105", "2110", "210B", "300501", "300801", "300601", "300701"));
        list.add(new CarProfile("Isuzu", "All New D-Max / MU-X (ปี 2012-2015)", "4JK1-TC / 4JJ1-TCX", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Isuzu", "D-Max / MU-X 1.9 & 3.0 Blue Power (ปี 2016-2019)", "RZ4E-TC / 4JJ1", "ATSP6", "ATSH7E0", "1003", "22F40C", "22F405", "22F423", "22F40B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Isuzu", "All New D-Max / MU-X พลานุภาพ (ปี 2020-2026)", "RZ4E Gen2 / 4JJ3-TCX", "ATSP6", "ATSH7E0", "1003", "22F40C", "22F405", "22F423", "22F40B", "2F010103", "2F040103", "2F020103", "2F030103"));

        // 3. MITSUBISHI
        list.add(new CarProfile("Mitsubishi", "Strada / Triton รุ่นแรก (ปี 2005-2007 K-Line)", "4D56 KWP2000", "ATSP5", "ATSH8110F1", "81", "2109", "2105", "2123", "210B", "300101", "300401", "300201", "300301"));
        list.add(new CarProfile("Mitsubishi", "Triton / Pajero Sport (ปี 2008-2014 CAN)", "4D56 / 4M41 (1860A523)", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F030103", "2F070103", "2F050103", "2F060103"));
        list.add(new CarProfile("Mitsubishi", "All New Triton / Pajero Sport (ปี 2015-2023)", "4N15 MIVEC", "ATSP6", "ATSH7E0", "1003", "221001", "221005", "221023", "22100B", "2F040103", "2F070103", "2F050103", "2F060103"));
        list.add(new CarProfile("Mitsubishi", "All New Triton โฉมใหม่ล่าสุด (ปี 2024-2026)", "4N16 Hyper Power", "ATSP6", "ATSH7E0", "1003", "221001", "221005", "221023", "22100B", "2F040103", "2F070103", "2F050103", "2F060103"));
        list.add(new CarProfile("Mitsubishi", "Attrage / Mirage / Xpander (ปี 2012-2026)", "3A92 / 4A91 (เบนซิน)", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));

        // 4. HONDA
        list.add(new CarProfile("Honda", "Civic ES / Jazz GD / City ZX (ปี 2001-2007)", "D17 / L15A (เบนซิน)", "ATSP5", "ATSH8110F1", "81", "010C", "0105", "010E", "010B", "300101", "300401", "300201", "300301"));
        list.add(new CarProfile("Honda", "Civic FD / FB / Jazz GE / City GM (ปี 2006-2013)", "R18A / L15A (เบนซิน)", "ATSP7", "ATSH18DA10F1", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Honda", "Civic FC-FE / City Turbo / HR-V (ปี 2016-2026)", "L15B7 / P10A2 / e:HEV", "ATSP7", "ATSH18DA10F1", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));

        // 5. NISSAN
        list.add(new CarProfile("Nissan", "Navara D40 / NP300 / Terra (ปี 2007-2026)", "YD25DDTi / YS23", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Nissan", "March / Almera / Note / Kicks (ปี 2010-2026)", "HR12 / HR10 Turbo (เบนซิน)", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));

        // 6. FORD & MAZDA
        list.add(new CarProfile("Ford / Mazda", "Ranger T6-NextGen / Everest / BT-50 Pro (ปี 2012-2026)", "2.0 / 2.2 / 3.2 ดีเซล", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Ford / Mazda", "Mazda 2 / Mazda 3 / CX-5 / CX-30 (ปี 2014-2026)", "Skyactiv-G / Skyactiv-D", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));

        // 7. SUZUKI
        list.add(new CarProfile("Suzuki", "Swift / Carry / Ertiga / XL7 (ปี 2012-2026)", "K12B / K12M / K15B (เบนซิน)", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));

        // 8. รถจีน & รถไฟฟ้า EV
        list.add(new CarProfile("BYD / Denza", "Atto 3 / Dolphin / Seal / M6 (ปี 2022-2026)", "e-Platform 3.0 EV", "ATSP6", "ATSH7E0", "1003", "22F40C", "22F405", "22F423", "22F40B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("MG (SAIC)", "MG4 / ZS / EP / Extender / MG5 (ปี 2017-2026)", "ICE / EV SAIC UDS", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Chery / Aion / Changan / GWM", "Omoda / Jaecoo / Aion Y / Deepal / Ora / Haval", "EV / HEV UDS CAN", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));
        list.add(new CarProfile("Geely / Zeekr / Volvo / Neta", "Zeekr X / Volvo EX30 / Neta V-X", "EV UDS CAN", "ATSP6", "ATSH7E0", "1003", "22F40C", "22F405", "22F423", "22F40B", "2F010103", "2F040103", "2F020103", "2F030103"));

        // 9. รถเกาหลี & รถยุโรป
        list.add(new CarProfile("Hyundai / Kia / BMW / Benz", "H-1 / Staria / Carnival / Series 3-5 / C-Class", "CRDi / B47 / OM651", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F040103", "2F020103", "2F030103"));

        return list;
    }

    public static Map<String, String> getDtcDictionary() {
        Map<String, String> dtc = new LinkedHashMap<>();
        dtc.put("P0300", "ตรวจพบการจุดระเบิดผิดพลาดหลายสูบ (Random/Multiple Cylinder Misfire) - เช็กคอยล์จุดระเบิดและหัวเทียน");
        dtc.put("P0301", "สูบที่ 1 จุดระเบิดผิดพลาด (Cylinder 1 Misfire) - เช็กคอยล์/หัวเทียน/หัวฉีดสูบ 1");
        dtc.put("P0351", "วงจรควบคุมคอยล์จุดระเบิด A (สูบ 1) ขัดข้อง - เช็กปลั๊กคอยล์และสายไฟ");
        dtc.put("P0380", "วงจรหัวเผา (Glow Plug) ขัดข้อง - เช็กรีเลย์หัวเผาและสายไฟ");
        dtc.put("P0087", "แรงดันน้ำมันในรางคอมมอนเรลต่ำเกินไป (Rail Pressure Low)");
        dtc.put("P0088", "แรงดันน้ำมันในรางคอมมอนเรลสูงเกินไป (SCV วาล์วค้าง)");
        dtc.put("P0400", "ระบบหมุนเวียนไอเสีย EGR ทำงานผิดปกติ");
        dtc.put("P0700", "กล่องเกียร์อัตโนมัติ (TCU) แจ้งเตือนความผิดปกติ");
        dtc.put("U0073", "สายสื่อสาร CAN Bus ล่ม (Bus Off) - เช็กสาย CAN-H/L และ R 120 โอห์ม");
        dtc.put("U0100", "ขาดการติดต่อกับกล่อง ECU เครื่องยนต์");
        dtc.put("B1200", "BCM: วงจรไฟหน้า / ไฟหรี่ขาดหรือช็อตลงกราวด์");
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
            return "⚠️ จับสายบัสได้ แต่กล่อง ECU เงียบสนิท (NO DATA)\n" +
