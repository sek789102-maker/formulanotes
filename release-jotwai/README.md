# จดไว้ (Jotwai) 1.0.0 — ไฟล์สำหรับ Google Play

| ไฟล์ | ใช้ทำอะไร |
|---|---|
| `signed/Jotwai-1.0.1-2-release.aab` | **อัปโหลดขึ้น Play Console** (เซ็นด้วย upload key แล้ว) |
| `signed/upload-certificate-sha256.txt` | ลายนิ้วมือใบรับรอง upload key (เปิดเผยได้) |
| `signed/verification/verification.md` | ผลตรวจจาก CI: bundletool validate, ลายเซ็น, ติดตั้งและเปิดบน emulator |
| `docs/PLAY_CONSOLE_CHECKLIST.md` | ขั้นตอนใน Play Console ทีละข้อ (✍️ = ต้องกรอกเอง) |
| `docs/STORE_LISTING.md` | ชื่อแอป คำอธิบายสั้น/ยาว (ไทย + อังกฤษ) |
| `docs/DATA_SAFETY.md` | คำตอบแบบฟอร์ม Data safety |
| `docs/PRIVACY_POLICY.md`, `docs/privacy-policy.html` | นโยบายความเป็นส่วนตัว — **กรอกชื่อ/อีเมลก่อน แล้วโฮสต์เป็น URL สาธารณะ** |
| `docs/RELEASE_NOTES.md` | ข้อความ “มีอะไรใหม่” |
| `store-listing/` | ไอคอน 512, feature graphic 1024×500, ภาพหน้าจอ 8 ภาพ (1080×1920) |

Upload keystore และรหัสผ่าน **ไม่อยู่ใน GitHub** — ส่งให้เจ้าของแอปแยกต่างหาก
