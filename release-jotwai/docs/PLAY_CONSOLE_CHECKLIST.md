# Google Play Console Checklist — จดไว้ (Jotwai) 1.0.0

สถานะ: ✅ = เตรียมไว้แล้ว/ตรวจแล้ว · ✍️ = **เจ้าของแอปต้องทำ/กรอกเอง** · ⚠️ = ต้องระวัง

## A. ก่อนสร้างแอปใน Console
| รายการ | สถานะ | หมายเหตุ |
|---|---|---|
| บัญชีนักพัฒนา Google Play | ✍️ | ใช้บัญชีเดียวกับ LIFEKEEP ได้ |
| Closed testing | ✍️⚠️ | **บัญชีส่วนตัวที่สร้างหลัง 13 พ.ย. 2023 ต้องทำ Closed testing ≥ 12 คน ต่อเนื่อง ≥ 14 วัน ต่อแอป ก่อนขอ Production** |
| Package name | ✅⚠️ | `com.sek789102.jotwai` — **เปลี่ยนไม่ได้หลังอัปโหลดครั้งแรก** |

## B. Store listing
| รายการ | สถานะ | ไฟล์/ค่า |
|---|---|---|
| App name | ✅ | `จดไว้: โน้ต รายการ แจ้งเตือน` (28/30) — ดู STORE_LISTING.md |
| Short description | ✅ | 73/80 |
| Full description (ไทย) | ✅ | 1,894/4,000 |
| App icon 512×512 | ✅ | `store-listing/app-icon-512.png` |
| Feature graphic 1024×500 | ✅ | `store-listing/feature-graphic-1024x500.png` |
| Phone screenshots | ✅ | `store-listing/screenshots/` 1080×1920 ถ่ายจากแอปจริงบน emulator (ข้อมูลตัวอย่าง) |
| Category | ✅ | Productivity |
| Contact email | ✍️ | **ต้องกรอกเอง** |
| Privacy policy URL | ✍️ | **ต้องโฮสต์ `docs/privacy-policy.html` เป็น URL สาธารณะเอง** และกรอกชื่อ/อีเมลในไฟล์ก่อน |

## C. App content (Policy)
| แบบฟอร์ม | คำตอบที่แนะนำ (ตามพฤติกรรมจริงของแอป) |
|---|---|
| Privacy policy | URL จากข้อ B ✍️ |
| Ads | **No, my app does not contain ads** |
| App access | **All functionality is available without special access** (ไม่มีล็อกอิน) |
| Content rating | ✍️ ตอบแบบสอบถาม IARC เอง — ไม่มีความรุนแรง เพศ การพนัน ไม่มีการสื่อสารระหว่างผู้ใช้ ไม่แชร์ตำแหน่ง |
| Target audience | ✍️ แนะนำ **13+ หรือ 18+** (ถ้าเลือกต่ำกว่า 13 จะเข้านโยบาย Families) |
| Data safety | ✅ **No data collected or shared** — ดู DATA_SAFETY.md |
| Government / Financial / Health / News | No |
| Permissions declarations | ✅ คาดว่าไม่ต้องยื่น: ใช้ `SCHEDULE_EXACT_ALARM` (ผู้ใช้อนุญาตเอง) ไม่ใช่ `USE_EXACT_ALARM`, ไม่มีสิทธิ์รูปภาพ/ไฟล์กว้าง — ⚠️ ดูคำเตือนใน Console หลังอัปโหลด |
| Foreground service | ⚠️ `FOREGROUND_SERVICE` มาจากไลบรารี WorkManager แอปไม่ได้ประกาศ foreground service type เอง — ถ้า Console ถามแบบฟอร์ม Foreground service ให้แจ้งผม |

## D. Release
| รายการ | สถานะ | หมายเหตุ |
|---|---|---|
| Android App Bundle | ✅ | `signed/Jotwai-1.0.0-1-release.aab` เซ็นด้วย Upload key ของจดไว้ |
| Play App Signing | ✍️ | เลือก **"Use Google-generated key"** (ค่าเริ่มต้น) |
| Upload key | ✅ | ส่งให้เจ้าของแยกต่างหาก (ไม่อยู่ใน GitHub) · SHA-256 ใน `signed/upload-certificate-sha256.txt` · **คนละกุญแจกับ LIFEKEEP** |
| targetSdk | ✅ | 36 |
| versionCode / versionName | ✅ | 1 / 1.0.0 — ครั้งถัดไปต้องเพิ่ม `versionCode` ใน `jotwai/app/build.gradle.kts` |
| Release notes | ✅ | RELEASE_NOTES.md |
| Countries / pricing | ✍️ | เช่น ไทย · ฟรี |

## E. Testing track ที่แนะนำ
1. **Internal testing** — อัปโหลด AAB แล้วติดตั้งจาก Play บนมือถือจริง ตรวจ: การเตือน, ล็อกโน้ต, กล้อง/คลังรูป, สำรอง→กู้คืน, วิดเจ็ต
2. **Closed testing** — ≥ 12 คน ≥ 14 วัน
3. **Production** — แนะนำ staged rollout

> ⚠️ แอปที่ติดตั้งจากไฟล์ APK (debug) กับแอปจาก Play มีลายเซ็นต่างกัน — ต้อง **สำรองข้อมูลแล้วถอนการติดตั้ง** ตัวที่ลงจากไฟล์ก่อน แล้วค่อยกู้คืนในตัวจาก Play
