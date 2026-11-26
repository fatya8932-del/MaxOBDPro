package com.maxobd.pro;
import java.util.*;
public class VehicleDatabase {
    public static class CarProfile {
        public String brand, modelName, engineCode, protocolCmd, headerCmd, initCmd, rpmPid, coolantPid, railPid, boostPid, actInjector, actFan, actEgr;
        public CarProfile(String b, String m, String e, String p, String h, String i, String r, String c, String rp, String bp, String ai, String af, String ae) {
            brand=b; modelName=m; engineCode=e; protocolCmd=p; headerCmd=h; initCmd=i; rpmPid=r; coolantPid=c; railPid=rp; boostPid=bp; actInjector=ai; actFan=af; actEgr=ae;
        }
        @Override public String toString() { return brand + " - " + modelName + " [" + engineCode + "]"; }
    }
    public static List<CarProfile> getAllVehicles() {
        List<CarProfile> list = new ArrayList<>();
        list.add(new CarProfile("Mitsubishi", "Triton / Pajero Sport (CAN)", "4D56 / 4M41 (1860A523)", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F030103", "2F050103", "2F060103"));
        list.add(new CarProfile("Mitsubishi", "Triton รุ่นแรก / Strada (K-Line)", "4D56 KWP2000", "ATSP5", "ATSH8110F1", "81", "2109", "2105", "2123", "210B", "300101", "300201", "300301"));
        list.add(new CarProfile("Mitsubishi", "All New Triton / Pajero", "4N15 MIVEC", "ATSP6", "ATSH7E0", "1003", "221001", "221005", "221023", "22100B", "2F040103", "2F050103", "2F060103"));
        list.add(new CarProfile("Isuzu", "D-Max Commonrail (2005-2011)", "4JK1 / 4JJ1 (K-Line)", "ATSP5", "ATSH8111F1", "81", "2101", "2105", "2110", "210B", "300501", "300601", "300701"));
        list.add(new CarProfile("Isuzu", "All New D-Max / MU-X (2012-2019)", "4JK1-TC / 4JJ1-TCX", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F020103", "2F030103"));
        list.add(new CarProfile("Isuzu", "D-Max 1.9 / 3.0 Blue Power", "RZ4E / 4JJ3", "ATSP6", "ATSH7E0", "1003", "22F40C", "22F405", "22F423", "22F40B", "2F010103", "2F020103", "2F030103"));
        list.add(new CarProfile("Toyota", "Hilux Vigo / Fortuner / Commuter", "1KD-FTV / 2KD-FTV", "ATSP6", "ATSH7E0", "1003", "2101", "2105", "2110", "210B", "300101", "300201", "300301"));
        list.add(new CarProfile("Toyota", "Hilux Revo / Fortuner / Altis", "1GD / 2GD / 1ZR", "ATSP6", "ATSH7E0", "1003", "010C", "0105", "0123", "010B", "2F010103", "2F020103", "2F030103"));
        list.add(new CarProfile("Honda", "Civic FD / FB / FC / City / Jazz", "R18 / L15 / K20 (CAN 29-bit)", "ATSP7", "ATSH18DA10F1", "1003", "010C", "0105", "010E", "010B", "2F010103", "2F020103", "2F030103"));
        return list;
    }
    public static Map<String, String> getDtcDictionary() {
        Map<String, String> dtc = new LinkedHashMap<>();
        dtc.put("P0380", "วงจรหัวเผา (Glow Plug) ขัดข้อง - เชกรีเลยหัวเผาและสายไฟ");
        dtc.put("P0087", "แรงดันน้ำมันในรางคอมมอนเรลต่ำเกินไป (Rail Pressure Low)");
        dtc.put("P0088", "แรงดันน้ำมันในรางคอมมอนเรลสงเกินไป (SCV วาลวค้าง)");
        dtc.put("P0400", "ระบบหมุนเวียนไอเสีย EGR ทำงานผิดปกติ");
        dtc.put("P0700", "กล่องเกียรอัตนมัติ (TCU) แจ้งเตือนความผิดปกติ");
        dtc.put("U0073", "สายสื่อสาร CAN Bus ล่ม (Bus Off) - เชกสาย CAN-H/L และ R 120 อหม");
        dtc.put("U0100", "ขาดการติดต่อกับกล่อง ECU เครื่องยนต");
        dtc.put("B1200", "BCM: วงจรไฟหน้า / ไฟหรี่ขาดหรือชอตลงกราวด");
        return dtc;
    }
    public static String analyzeObdResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) return " ไม่ได้รับข้อมลตอบกลับ (Timeout) - เชกการเชื่อมต่อหัวอ่าน";
        String res = rawResponse.toUpperCase();
        if (res.contains("CAN ERROR") || res.contains("FB ERROR") || res.contains("BUS INIT: ERROR")) {
            return " สายสื่อสาร CAN Bus / K-Line มีปัหา (CAN ERROR)\n เชกสาย CAN-H (ขา 6) / CAN-L (ขา 14) ขาด, ชอต หรือลืมใส่ตัวต้านทาน 120 อหม\n เกมาตรานปกติ: CAN-H ~3.2V / CAN-L ~1.8V";
        }
        if (res.contains("NO DATA") || res.contains("STOPPED") || res.contains("?")) {
            return " จับสายบัสได้ แต่กล่อง ECU เงียบสนิท (NO DATA)\n สายสื่อสารไม่ขาด (ไฟอาจค้างที่ CAN-H 2.5V / CAN-L 2.5V เท่ากัน)\n จุดเชก: 1. ไฟเลี้ยงกล่อง (B+/IG) 2. ระบบ Immo 3. หากไฟครบแต่ยังค้าง 2.5V เท่ากัน (ไม่สวิงเปน 3.2V/1.8V) แสดงว่าภาคจ่ายไฟ 5V หรือชิป MCU ในกล่องเสียครับ";
        }
        if (res.contains("7F")) return "ℹ กล่อง ECU ดี (สัา CAN 3.2V/1.8V ปกติ) แต่ปิเสคำสั่ง (" + res.trim() + ")";
        return " เชื่อมต่อกล่อง ECU สำเรจ (สัา CAN Active ~3.2V / 1.8V)\nข้อมลตอบกลับ: " + res.trim();
    }
}
