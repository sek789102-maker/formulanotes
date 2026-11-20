# LIFEKEEP 1.0.0 — รายงานผลการตรวจสอบก่อน Release

วันที่: 2026-11-20 · Package: `com.sek789102.lifekeep` · versionName 1.0.0 · versionCode 1
ไฟล์อัปโหลด: `signed/LIFEKEEP-1.0.0-1-release.aab` (SHA-256 `a85fcf2c3478565ac08131f9687ca880101f318a307ee12bb01bedb6c3784fe3`)
Build จาก commit `fc32a6d` (CI) · เซ็นด้วย Upload key (jarsigner SHA256withRSA) · cert SHA-256 `EE:4D:D7:29:A4:FE:89:8F:D4:6E:D4:BC:F7:E7:E8:5C:C5:00:B9:68:8D:A2:07:C6:E6:98:D1:95:00:71:3C:E9`

## 1. คุณภาพโค้ดและการทดสอบ

| การตรวจ | ผล | ที่มา |
|---|---|---|
| `flutter analyze` | ✅ No issues | ในเครื่อง + CI |
| Unit/Widget tests | ✅ 69/69 ผ่าน | ในเครื่อง + CI (`build/unit-tests.log`) |
| Database CRUD, Search, Categories | ✅ | `test/database_test.dart` |
| Reminders (คำนวณ/ยกเลิก/ไม่ซ้ำ/เพดาน 400) | ✅ | `test/reminder_scheduler_test.dart`, `dates_test.dart` |
| Backup/Restore (checksum, ไฟล์เสีย, เวอร์ชันใหม่กว่า, merge ไม่ซ้ำ, rollback) | ✅ | `test/backup_test.dart` |
| ข้อมูลจำนวนมาก (3,000 รายการ + backup→restore) | ✅ | `test/large_data_test.dart` |
| PIN (PBKDF2 test vectors, lockout) | ✅ | `test/security_test.dart` |
| UI flows + overflow (320dp/ตัวอักษร 130% ถึงแท็บเล็ต) | ✅ | `test/widget_flow_test.dart`, `layout_stress_test.dart` |
| Dependencies | ✅ direct/dev ล่าสุดทั้งหมด; transitive อัปเดตตามที่ทำได้ | `flutter pub outdated` |

## 2. ทดสอบบน Android จริง (Emulator Android 15 / API 35, x86_64)

| การตรวจ | ผล |
|---|---|
| **Release build (R8)** functional check — phase 1: บันทึกเอกสาร/งาน/กิจกรรม, นำเข้าไฟล์แนบ, ตั้งแจ้งเตือน (ผ่าน Gson/R8), ค้นหา, backup + ตรวจ checksum, merge restore ไม่ซ้ำ, PIN ใน secure storage | ✅ 10/10 |
| การแจ้งเตือนที่ตั้งเวลาไว้ **แสดงจริง** โดย Android | ✅ |
| Kill process แล้วเปิดใหม่ — phase 2: ข้อมูล ไฟล์แนบ การแจ้งเตือนที่ค้าง ไฟล์ backup ยังอยู่; ลบเอกสารแล้วไฟล์แนบถูกลบ | ✅ 9/9 |
| Production release APK: ติดตั้ง, เปิด, ทำงาน 10 วินาที, ไม่มี crash/error, เปิดใหม่หลัง process death | ✅ (cold start ~1.1 s) |
| **AAB ที่เซ็นแล้ว**: bundletool validate, jarsigner verify, fingerprint ตรง, ไม่มีสิทธิ์ INTERNET, ติดตั้ง/เปิดบน emulator ไม่มี crash | ✅ (`signed/verification/`) |
| UI integration tests บนอุปกรณ์ (debug) | ⚠️ **ยังไม่ผ่านครบ** — รอบก่อน: test "สร้าง/เสร็จ/ค้นหา/คงอยู่" ✅ ผ่าน, test "สำรองจากหน้าตั้งค่า" ❌ (ปัญหาในตัวเทสต์: คีย์บอร์ดค้าง) → แก้แล้วรันใหม่ แต่รอบใหม่ค้างใน CI ยังไม่มีผล ณ เวลาที่เขียนรายงาน |

## 3. Android Release Configuration

| รายการ | ผล |
|---|---|
| applicationId / namespace | `com.sek789102.lifekeep` (ถาวรหลังอัปโหลดครั้งแรก) |
| versionName / versionCode | 1.0.0 / 1 |
| minSdk / targetSdk / compileSdk | 24 / **36** (ตามข้อกำหนด Play ปัจจุบัน) / 37 |
| R8 code + resource shrinking | เปิด (ค่าเริ่มต้น Flutter); keep.xml สำหรับไอคอนแจ้งเตือน/splash; Gson rules มากับ dependency — ยืนยันด้วย release-mode test บนอุปกรณ์ |
| Signing | Upload keystore (RSA 4096, ถึงปี 2056) อยู่นอก repo; Gradle อ่านจาก `key.properties` หรือ env; `-PrequireUploadKey=true` บังคับ |
| Secrets ใน Git | ✅ ไม่มี — `.gitignore` ครอบคลุม `*.jks`, `*.keystore`, `key.properties`; ตรวจ source ZIP แล้วไม่มีไฟล์ลับ |
| Icon / Splash / ชื่อแอป | Adaptive icon + monochrome; Android 12+ system splash + splash ก่อน 12 (สว่าง/มืด); ชื่อ "LIFEKEEP" |
| Permissions | POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, VIBRATE, USE_BIOMETRIC (+USE_FINGERPRINT จาก local_auth) — **ไม่มี INTERNET, ไม่มีสิทธิ์รูปภาพ/ไฟล์กว้าง, ไม่มี CAMERA** |

## 4. ความปลอดภัยและข้อมูลผู้ใช้

| รายการ | ผล |
|---|---|
| ส่งข้อมูลออกนอกเครื่อง | ✅ ไม่มี — ไม่มีโค้ดเครือข่าย, ไม่มีสิทธิ์ INTERNET (CI fail ถ้าพบ) |
| Android Auto Backup / device transfer | ✅ ปิด (`allowBackup=false`, `dataExtractionRules`) |
| การเข้ารหัส | ⚠️ **ฐานข้อมูล ไฟล์แนบ และไฟล์สำรองไม่ได้เข้ารหัสโดยแอป** (พึ่ง app sandbox + device encryption) — เอกสารทั้งหมดระบุตรงตามนี้ ไม่อ้างว่าเข้ารหัส |
| PIN | ✅ เก็บเฉพาะ PBKDF2-SHA256 + salt ใน Keystore-backed secure storage, lockout เมื่อใส่ผิด |
| Recent apps / screenshot | ✅ FLAG_SECURE เมื่อเปิดล็อกแอป |
| Logs | ✅ `debugPrint` ทั้งหมดอยู่ใต้ `kDebugMode`; ไม่ log ข้อมูลผู้ใช้; ข้อความ error ที่ผู้ใช้เห็นไม่มี stack trace |
| ไฟล์แนบ | ✅ คัดลอกแบบ stream, จำกัด 50 MB/ไฟล์, ป้องกัน path traversal, ลบเมื่อลบเอกสาร (ยืนยันบนอุปกรณ์) |
| Notification บนหน้าล็อก | ✅ visibility = private |

## 5. ปัญหาที่พบและแก้แล้วในรอบนี้
1. **ชื่อหมวดหมู่ไม่ตรงภาษา** — เครื่องที่ตั้งภาษาอังกฤษได้ UI ไทยแต่หมวดหมู่อังกฤษ → แก้ให้ตามภาษาแอป + เปลี่ยนตามเมื่อสลับภาษา (มีเทสต์)
2. **Package name ไม่ผูกกับเจ้าของ** (`com.lifekeep.app` อาจซ้ำกับคนอื่นบน Play) → `com.sek789102.lifekeep`; namespace ปรับให้ตรงกัน
3. Splash เดิมเป็นพื้นขาวเปล่า → splash แบรนด์ทั้ง Android 12+ และก่อนหน้า, สว่าง/มืด
4. Release เดิมเซ็นด้วย debug key โดยไม่มีทางกันพลาด → ระบบ upload key + strict mode
5. ไม่มีการทดสอบ release build (R8) บนอุปกรณ์ → เพิ่ม release-mode functional check + การแจ้งเตือนจริง + restart

## 6. ปัญหาที่ยังเหลือ / ข้อจำกัด
- ⚠️ UI integration test "สำรองจากหน้าตั้งค่า" บนอุปกรณ์ยังไม่ได้ผลยืนยัน (ฟังก์ชันสำรองบนอุปกรณ์จริงผ่านแล้วใน release-mode check)
- ⚠️ **ยังไม่ได้ทดสอบบนมือถือจริง** — เฉพาะ emulator: กล้อง, แกลเลอรี/Photo Picker, ลายนิ้วมือ, การบันทึกไฟล์สำรองผ่าน "บันทึกไปยัง…", การแจ้งเตือนหลังรีบูตเครื่อง, ผลของโหมดประหยัดแบตเตอรี่ของผู้ผลิตต่าง ๆ
- APK สำหรับทดสอบเซ็นด้วย debug key (ไม่มี apksigner ในสภาพแวดล้อมที่เซ็น) — ใช้ทดสอบเท่านั้น
- Play Console อาจเตือนเรื่อง native debug symbols (ไม่ขวางการเผยแพร่)
- ภาพหน้าจอ Store เรนเดอร์จาก UI จริงด้วยข้อมูลตัวอย่าง ไม่ใช่ภาพจากมือถือ
- ฐานข้อมูล/ไฟล์สำรองไม่เข้ารหัส (ตามที่ระบุในเอกสาร)

## 7. ข้อสรุปความพร้อม
**ไฟล์ AAB พร้อมอัปโหลดเข้า Internal testing ได้** (เซ็นถูกต้อง, targetSdk 36, ผ่านการตรวจบนอุปกรณ์ในโหมด release)
**ยังไม่ควรถือว่าพร้อมเผยแพร่ Production** จนกว่า: ทดสอบบนมือถือจริงผ่าน Internal testing, โฮสต์ Privacy Policy, กรอกข้อมูลใน Console และผ่าน Closed testing (ถ้าบัญชีบังคับ)
