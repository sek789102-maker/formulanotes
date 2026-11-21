# นโยบายความเป็นส่วนตัว — จดไว้ (Jotwai)

**มีผลตั้งแต่:** 21 พฤศจิกายน 2569 (2026-11-21) · **เวอร์ชันแอป:** 1.0.0
**ผู้พัฒนา:** `[ชื่อผู้พัฒนา/ชื่อบัญชีนักพัฒนา — เจ้าของแอปกรอก]` · **ติดต่อ:** `[อีเมลติดต่อ — เจ้าของแอปกรอก]`

“จดไว้” เป็นแอปจดโน้ตแบบออฟไลน์ นโยบายนี้อธิบายตามการทำงานจริงของแอปเวอร์ชันนี้

## 1. ข้อมูลที่เราเก็บรวบรวม
**เราไม่เก็บรวบรวมข้อมูลใด ๆ ของคุณ** แอปไม่มีเซิร์ฟเวอร์ ไม่มีบัญชีผู้ใช้ ไม่มีระบบวิเคราะห์การใช้งาน (analytics) ไม่มีโฆษณา ไม่มีระบบรายงานข้อผิดพลาดที่ส่งออกนอกเครื่อง และ**ไม่มีสิทธิ์เข้าถึงอินเทอร์เน็ต (INTERNET permission)** จึงไม่สามารถส่งข้อมูลออกจากอุปกรณ์ได้ ผู้พัฒนาไม่สามารถเข้าถึงข้อมูลของคุณ

## 2. ข้อมูลที่คุณบันทึกไว้ในเครื่อง
โน้ต รายการ (checklist) รูปภาพที่แนบหรือวาด โฟลเดอร์ ป้ายกำกับ เวลาเตือน ประวัติการแก้ไขโน้ต และการตั้งค่า จะถูกเก็บ**เฉพาะในพื้นที่ส่วนตัวของแอปบนอุปกรณ์ของคุณ**
- ข้อมูลอยู่ในฐานข้อมูลและโฟลเดอร์ไฟล์ของแอป ซึ่งแอปอื่นเข้าถึงไม่ได้ตามระบบความปลอดภัยของ Android
- **แอปไม่ได้เข้ารหัสข้อมูลเพิ่มเติมด้วยตัวเอง** การป้องกันขึ้นกับระบบแยกพื้นที่ของแอป (app sandbox) และการเข้ารหัสอุปกรณ์ของ Android
- ปิดการสำรองข้อมูลอัตโนมัติไปยังคลาวด์ของ Android (Android Auto Backup) ยังอนุญาตการย้ายข้อมูลแบบเชื่อมต่อตรงระหว่างโทรศัพท์ (สาย/Wi-Fi ภายใน) ตอนเปลี่ยนเครื่อง
- แอปเก็บเวอร์ชันก่อนหน้าของโน้ตไว้ในเครื่อง (สูงสุด 30 เวอร์ชันต่อโน้ต) เพื่อให้กู้คืนการแก้ไขได้ และจะถูกลบไปพร้อมโน้ตเมื่อลบถาวร

## 3. โน้ตที่ล็อก
การล็อกโน้ตใช้ล็อกหน้าจอของโทรศัพท์ (ลายนิ้วมือ ใบหน้า PIN รูปแบบ หรือรหัสผ่าน) ผ่านระบบของ Android แอป**ไม่ได้รับหรือเก็บข้อมูลชีวมิติหรือรหัสผ่านใด ๆ** โน้ตที่ล็อกจะถูกซ่อนในรายการ การค้นหา วิดเจ็ต และการแจ้งเตือน **แต่ไม่ได้ถูกเข้ารหัส**

## 4. การสำรองข้อมูล (Backup)
- ไฟล์สำรองเป็น ZIP (ข้อมูล JSON + รูปภาพ) และ**ไม่ได้เข้ารหัส** ใครมีไฟล์ก็อ่านได้
- แอปสร้างไฟล์สำรองเฉพาะเมื่อคุณกดสั่ง และคุณเป็นผู้เลือกตำแหน่งบันทึกเอง หากเลือกบันทึกผ่านแอปหรือบริการของบุคคลที่สาม (เช่น Google Drive) ไฟล์จะอยู่ภายใต้นโยบายของผู้ให้บริการนั้น

## 5. สิทธิ์ที่แอปขอ
| สิทธิ์ | เหตุผล |
|---|---|
| แสดงการแจ้งเตือน (POST_NOTIFICATIONS) | แสดงการเตือนตามเวลาที่คุณตั้งไว้ในโน้ต |
| การปลุกและการช่วยเตือน (SCHEDULE_EXACT_ALARM) | ให้การเตือนตรงเวลา (คุณอนุญาตหรือไม่ก็ได้) |
| เริ่มทำงานหลังเปิดเครื่อง (RECEIVE_BOOT_COMPLETED) | ตั้งการเตือนใหม่หลังรีสตาร์ตเครื่อง |
| ชีวมิติ (USE_BIOMETRIC) | เปิดโน้ตที่ล็อกด้วยลายนิ้วมือ/ใบหน้า |

สิทธิ์ `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` ถูกเพิ่มโดยไลบรารี WorkManager ของ Android สำหรับงานบำรุงรักษาในเครื่อง (ล้างถังขยะที่เกิน 30 วัน) และ**ไม่ได้ใช้ส่งข้อมูลใด ๆ** — แอปไม่มีสิทธิ์อินเทอร์เน็ต

แอปไม่ขอสิทธิ์อ่านคลังรูปภาพทั้งหมด การเลือกรูปใช้ตัวเลือกรูปของระบบ (แอปเข้าถึงเฉพาะรูปที่คุณเลือก) และการถ่ายรูปใช้แอปกล้องของระบบ รูปจะถูก**คัดลอก**มาเก็บในพื้นที่ของแอป

## 6. การแบ่งปันข้อมูล
เราไม่แบ่งปัน ขาย หรือส่งข้อมูลของคุณให้บุคคลที่สามใด ๆ ข้อมูลจะออกจากแอปเฉพาะเมื่อคุณสั่งเอง (กด “ส่ง / แชร์” โน้ต หรือบันทึกไฟล์สำรอง)

## 7. การลบข้อมูล
ลบโน้ตได้ทุกเมื่อ (โน้ตในถังขยะจะถูกลบถาวรอัตโนมัติหลัง 30 วัน หรือกด “ล้างถังขยะ”) หรือถอนการติดตั้งแอป ซึ่ง Android จะลบข้อมูลทั้งหมดของแอป ไฟล์สำรองที่คุณบันทึกไว้นอกแอปต้องลบเอง

## 8. เด็ก
แอปไม่ได้ออกแบบมาสำหรับเด็กโดยเฉพาะ และไม่เก็บข้อมูลจากผู้ใช้รายใด

## 9. การเปลี่ยนแปลงนโยบาย
หากการทำงานของแอปเปลี่ยนไป (เช่น เพิ่มฟีเจอร์ที่ใช้อินเทอร์เน็ต) นโยบายนี้จะได้รับการปรับปรุงก่อนเผยแพร่เวอร์ชันนั้น

---

# Privacy Policy — Jotwai (English)

**Effective:** 2026-11-21 · **App version:** 1.0.0 · **Developer:** `[developer name]` · **Contact:** `[contact email]`

1. **We collect no data.** No servers, accounts, analytics, crash reporting or ads. The app has **no INTERNET permission** and cannot send data off the device. The developer has no access to your data.
2. **On-device data.** Notes, checklists, attached or drawn images, folders, labels, reminder times, note version history (up to 30 earlier versions per note) and settings are stored only in the app's private storage. The app does **not** add its own encryption; protection relies on Android's app sandbox and device encryption. Android cloud auto-backup is disabled; direct phone-to-phone transfer is allowed.
3. **Locked notes** use your phone's screen lock (fingerprint, face, PIN, pattern, password) via Android's system prompt. The app never receives biometric data or passwords. Locked notes are hidden from lists, search, widgets and notifications but are **not encrypted**.
4. **Backups** are unencrypted ZIP files (JSON + images), created only when you ask, saved where you choose. Third-party storage providers are governed by their own policies.
5. **Permissions:** notifications, exact alarms (optional), boot-completed (re-schedule reminders), biometric. `WAKE_LOCK`, `ACCESS_NETWORK_STATE` and `FOREGROUND_SERVICE` come from Android's WorkManager library (local trash clean-up) and are not used to send data. No broad photo/storage access (system photo picker and camera app are used).
6. **Sharing:** none. Data leaves the app only when you share a note or save a backup yourself.
7. **Deletion:** delete notes at any time (trash is emptied automatically after 30 days), or uninstall the app. Exported backup files must be deleted by you.
8. **Children:** not directed at children. 9. **Changes:** this policy will be updated before any version that changes this behaviour.
