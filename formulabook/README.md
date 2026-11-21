# สมุดโน้ตสูตร (FormulaBook)

แอป Android สำหรับจดโน้ตและสูตร ทบทวนแบบ SM-2 นับถอยหลังวันสอบ และเครื่องคิดเลข
ทำงานออฟไลน์ทั้งหมด ไม่มีบัญชี ไม่มีโฆษณา ไม่มีการส่งข้อมูลออกจากเครื่อง (แอปไม่ขอสิทธิ์ INTERNET)

- Package: `com.sek789102.formulabook`
- Kotlin + Jetpack Compose, Room, DataStore, WorkManager, Glance widget
- minSdk 24, targetSdk 36

## การ build
ต้องใช้ JDK 17 และ Android SDK (Android Studio เปิดโฟลเดอร์นี้ได้เลย) โปรเจคไม่มีไฟล์ `gradlew`
ให้ใช้ Gradle 9.3.1 หรือให้ Android Studio สร้าง wrapper ให้

```
gradle :app:testDebugUnitTest      # เทสต์
gradle :app:assembleDebug          # APK สำหรับทดสอบ
gradle :app:bundleRelease          # AAB (ไม่เซ็น ถ้าไม่ได้ตั้งค่า keystore)
```

### การเซ็น release
ตั้งค่าผ่าน environment variables เท่านั้น — ห้าม commit keystore หรือรหัสผ่านลง Git
`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS` (ค่าเริ่มต้น `upload`)

## ข้อมูลและการสำรอง
- ข้อมูลทั้งหมดอยู่ในเครื่อง ไม่ถูกส่งขึ้น Google cloud backup (ดู `res/xml/data_extraction_rules.xml`)
- ปุ่มสำรองข้อมูลในแอปสร้างไฟล์ `.zip` (`backup.json` + รูปภาพแยกไฟล์) และยังกู้คืนไฟล์ `.json` แบบเก่าได้
- รูปที่ไม่มีโน้ตไหนใช้แล้วจะถูกลบอัตโนมัติวันละครั้ง (เว้นไฟล์ที่อายุน้อยกว่า 24 ชั่วโมง)

## การแจ้งเตือนวันสอบ
แจ้งเตือนล่วงหน้า 7, 3, 1 วัน (08:00) และเช้าวันสอบ (07:00)
Android 14+ ไม่อนุญาต “การปลุกและการช่วยเตือน” เป็นค่าเริ่มต้น แอปจะแสดงปุ่มให้อนุญาตในหน้าตั้งค่า
ถ้าไม่อนุญาตก็ยังแจ้งเตือนได้ แต่เวลาอาจคลาดเคลื่อน
