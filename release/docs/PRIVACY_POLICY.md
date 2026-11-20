# นโยบายความเป็นส่วนตัว — LIFEKEEP

**มีผลตั้งแต่:** 20 พฤศจิกายน 2569 (2026-11-20) · **เวอร์ชันแอป:** 1.0.0
**ผู้พัฒนา:** `[ชื่อผู้พัฒนา/ชื่อบัญชีนักพัฒนา — เจ้าของแอปกรอก]` · **ติดต่อ:** `[อีเมลติดต่อ — เจ้าของแอปกรอก]`

LIFEKEEP เป็นแอปจัดการเอกสาร งาน และกำหนดการแบบออฟไลน์ นโยบายนี้อธิบายตามการทำงานจริงของแอปเวอร์ชันนี้

## 1. ข้อมูลที่เราเก็บรวบรวม
**เราไม่เก็บรวบรวมข้อมูลใด ๆ ของคุณ** แอปไม่มีเซิร์ฟเวอร์ ไม่มีบัญชีผู้ใช้ ไม่มีระบบวิเคราะห์การใช้งาน (analytics) ไม่มีโฆษณา และ**ไม่มีสิทธิ์เข้าถึงอินเทอร์เน็ต (INTERNET permission)** จึงไม่สามารถส่งข้อมูลออกจากอุปกรณ์ได้ ผู้พัฒนาไม่สามารถเข้าถึงข้อมูลของคุณ

## 2. ข้อมูลที่คุณบันทึกไว้ในเครื่อง
ข้อมูลที่คุณป้อนเอง ได้แก่ เอกสาร (ชื่อ รายละเอียด หมายเหตุ วันหมดอายุ) รูปภาพแนบ งาน กิจกรรม หมวดหมู่ และการตั้งค่า จะถูกเก็บ**เฉพาะในพื้นที่ส่วนตัวของแอปบนอุปกรณ์ของคุณ**
- ข้อมูลอยู่ในฐานข้อมูล SQLite และโฟลเดอร์ไฟล์ของแอป ซึ่งแอปอื่นเข้าถึงไม่ได้ตามระบบความปลอดภัยของ Android
- **แอปไม่ได้เข้ารหัสฐานข้อมูลและรูปภาพแนบเพิ่มเติมด้วยตัวเอง** การป้องกันขึ้นกับระบบแยกพื้นที่ของแอป (app sandbox) และการเข้ารหัสอุปกรณ์ของ Android
- รูปภาพแนบที่คุณเลือกจะถูก**คัดลอก**มาเก็บในพื้นที่ของแอป และจะถูกลบเมื่อคุณลบเอกสารหรือนำรูปออก
- ปิดการสำรองข้อมูลอัตโนมัติไปยังคลาวด์ของ Android (Android Auto Backup) และการโอนข้อมูลระหว่างอุปกรณ์

## 3. PIN และการล็อกแอป
หากเปิดล็อกแอป PIN จะ**ไม่ถูกเก็บเป็นข้อความ** แอปเก็บเพียงค่าแฮช (PBKDF2-HMAC-SHA256 พร้อม salt) ไว้ใน Secure Storage ที่เข้ารหัสด้วย Android Keystore การปลดล็อกด้วยลายนิ้วมือ/ใบหน้าใช้ระบบของ Android โดยแอป**ไม่ได้รับหรือเก็บข้อมูลชีวมิติ**ใด ๆ

## 4. การสำรองข้อมูล (Backup)
- ไฟล์สำรองเป็น ZIP (JSON + รูปภาพ) และ**ไม่ได้เข้ารหัส**
- ไฟล์สำรองอัตโนมัติถูกเก็บในพื้นที่ของแอปบนเครื่องเท่านั้น
- เมื่อคุณเลือก “บันทึกไปยัง…” คุณเป็นผู้เลือกตำแหน่งเอง (เช่น โฟลเดอร์ในเครื่อง, USB, หรือแอปจัดเก็บไฟล์อื่น) หากเลือกบันทึกผ่านแอปหรือบริการของบุคคลที่สาม ไฟล์จะอยู่ภายใต้นโยบายของผู้ให้บริการนั้น

## 5. สิทธิ์ที่แอปขอ
| สิทธิ์ | เหตุผล |
|---|---|
| แสดงการแจ้งเตือน (POST_NOTIFICATIONS) | แจ้งเตือนงาน กิจกรรม และเอกสารใกล้หมดอายุ |
| ตั้งเวลาแบบแม่นยำ (SCHEDULE_EXACT_ALARM) | ให้การแจ้งเตือนตรงเวลามากขึ้น (ผู้ใช้อนุญาตเองได้) |
| เริ่มทำงานหลังเปิดเครื่อง (RECEIVE_BOOT_COMPLETED) | ตั้งการแจ้งเตือนใหม่หลังรีสตาร์ตเครื่อง |
| การสั่น (VIBRATE) | การแจ้งเตือน |
| ชีวมิติ (USE_BIOMETRIC) | ปลดล็อกแอปด้วยลายนิ้วมือ/ใบหน้า (ถ้าเปิดใช้) |

แอปไม่ขอสิทธิ์อ่านคลังรูปภาพทั้งหมด การเลือกรูปใช้ตัวเลือกรูปของระบบ (แอปเข้าถึงเฉพาะรูปที่คุณเลือก) และการถ่ายรูปใช้แอปกล้องของระบบ

## 6. การแบ่งปันข้อมูล
เราไม่แบ่งปัน ขาย หรือส่งข้อมูลของคุณให้บุคคลที่สามใด ๆ

## 7. การลบข้อมูล
คุณลบข้อมูลได้ทุกเมื่อ: ลบรายการในแอป, ใช้ “ตั้งค่า > ลบข้อมูลทั้งหมด”, หรือถอนการติดตั้งแอป (ข้อมูลและไฟล์สำรองในพื้นที่ของแอปจะถูกลบโดย Android) ไฟล์สำรองที่คุณบันทึกไว้นอกแอปต้องลบเอง

## 8. เด็ก
แอปไม่ได้ออกแบบมาสำหรับเด็ก และไม่เก็บข้อมูลจากผู้ใช้รายใด

## 9. การเปลี่ยนแปลงนโยบาย
หากการทำงานของแอปเปลี่ยนไป (เช่น เพิ่มฟีเจอร์ที่ใช้อินเทอร์เน็ต) นโยบายนี้จะได้รับการปรับปรุงก่อนเผยแพร่เวอร์ชันนั้น

---

# Privacy Policy — LIFEKEEP (English)

**Effective:** 2026-11-20 · **App version:** 1.0.0 · **Developer:** `[developer name]` · **Contact:** `[contact email]`

1. **We collect no data.** No servers, accounts, analytics or ads. The app has **no INTERNET permission** and cannot send data off the device. The developer has no access to your data.
2. **On-device data.** Everything you enter (documents, notes, expiry dates, attached images, tasks, events, categories, settings) is stored only in the app's private storage (SQLite + app files). The app does **not** add its own encryption to the database or attachments; protection relies on Android's app sandbox and device encryption. Selected images are copied into app storage and deleted with their document. Android cloud auto-backup and device-to-device transfer are disabled.
3. **PIN / app lock.** The PIN is never stored in plain text — only a salted PBKDF2-HMAC-SHA256 hash in Android Keystore-backed secure storage. Biometric unlock uses Android's system prompt; the app never receives biometric data.
4. **Backups** are unencrypted ZIP files (JSON + images). Automatic backups stay in app storage. When you use "Save to…", you choose the destination; third-party storage providers are governed by their own policies.
5. **Permissions:** notifications, exact alarms, boot-completed (re-schedule reminders), vibrate, biometric. No broad photo/storage access (system photo picker and camera app are used).
6. **Sharing:** none. 7. **Deletion:** delete items, use Settings → Delete all data, or uninstall the app; exported backup files must be deleted by you. 8. **Children:** not directed at children. 9. **Changes:** this policy will be updated before any version that changes this behaviour.
