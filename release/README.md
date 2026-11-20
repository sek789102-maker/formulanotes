# LIFEKEEP 1.0.0 — ชุดไฟล์ Release สำหรับ Google Play

## โครงสร้างโฟลเดอร์

```
release/
├── README.md                     ← ไฟล์นี้ (วิธีติดตั้ง/Build/อัปโหลด)
├── RELEASE_AUDIT_REPORT.md       ← รายงานผลตรวจสอบก่อน Release (อ่านก่อนอัปโหลด)
├── signed/
│   ├── LIFEKEEP-1.0.0-1-release.aab   ← ไฟล์สำหรับอัปโหลด Play Console (เซ็นด้วย Upload key)
│   ├── upload-certificate-sha256.txt  ← ลายนิ้วมือใบรับรอง Upload key (ข้อมูลสาธารณะ)
│   └── verification/                  ← ผลตรวจลายเซ็น + ติดตั้งบน emulator
├── apk/
│   └── LIFEKEEP-1.0.0-1-test.apk      ← APK สำหรับติดตั้งทดสอบ (release/R8, เซ็นด้วย debug key)
├── build/                        ← ผลจาก CI: รายงานทดสอบบนอุปกรณ์ ภาพหน้าจอ log
├── docs/
│   ├── PRIVACY_POLICY.md / privacy-policy.html
│   ├── DATA_SAFETY.md
│   ├── STORE_LISTING.md
│   ├── RELEASE_NOTES.md
│   └── PLAY_CONSOLE_CHECKLIST.md
├── store-listing/                ← ไอคอน 512, Feature graphic, Screenshots
└── LIFEKEEP-1.0.0-source.zip     ← Source code เวอร์ชัน Release
```

> 🔐 **Upload keystore และรหัสผ่านไม่อยู่ใน repo นี้** — ส่งให้เจ้าของแอปแยกต่างหาก เก็บไว้อย่างน้อย 2 ที่

## ติดตั้ง APK สำหรับทดสอบ
1. คัดลอก `apk/LIFEKEEP-1.0.0-1-test.apk` ไปยังมือถือ Android 7.0+
2. เปิดไฟล์ → อนุญาต "ติดตั้งแอปที่ไม่รู้จัก" → ติดตั้ง
3. ⚠️ APK นี้เซ็นด้วย debug key (เพื่อทดสอบเท่านั้น) — ก่อนติดตั้งเวอร์ชันจาก Google Play ต้องถอนตัวนี้ก่อน (สำรองข้อมูลก่อน)
4. ⚠️ Package name ใหม่คือ `com.sek789102.lifekeep` จึงเป็นคนละแอปกับ APK ทดสอบรุ่นก่อน (`com.lifekeep.app`) — ข้อมูลไม่ย้ายมาเอง ใช้ สำรอง→กู้คืน เพื่อย้ายข้อมูล

## อัปโหลดขึ้น Google Play
ดูขั้นตอนเต็มใน `docs/PLAY_CONSOLE_CHECKLIST.md` — โดยย่อ:
1. สร้างแอปใน Play Console → ภาษาเริ่มต้น ไทย → ประเภท App → ฟรี
2. Release → Testing → **Internal testing** → Create release → เปิดใช้ **Play App Signing** (Google-generated key) → อัปโหลด `signed/LIFEKEEP-1.0.0-1-release.aab` → ใส่ Release notes จาก `docs/RELEASE_NOTES.md`
3. กรอก Store listing / App content ตาม Checklist
4. Closed testing (≥12 คน 14 วัน หากเป็นบัญชีส่วนตัวใหม่) → Apply for production

## Build ใหม่จาก Source
```bash
cd lifekeep
flutter pub get
flutter analyze && flutter test
# ตั้งค่า upload key (android/key.properties หรือ env vars — ดู lifekeep/README.md)
flutter build appbundle --release -PrequireUploadKey=true
flutter build apk --release
```
ต้องใช้ Flutter 3.47.9, JDK 17/21, Android SDK (compileSdk 37, targetSdk 36)

ครั้งถัดไปที่อัปเดตแอป: เพิ่มเลข `version:` ใน `lifekeep/pubspec.yaml` (เช่น `1.0.1+2`) — versionCode ต้องเพิ่มขึ้นทุกครั้ง
