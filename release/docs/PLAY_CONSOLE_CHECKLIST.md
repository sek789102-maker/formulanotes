# Google Play Console Checklist — LIFEKEEP 1.0.0

สถานะ: ✅ = เตรียมไว้แล้ว/ตรวจแล้ว · ✍️ = **เจ้าของแอปต้องทำ/กรอกเอง** · ⚠️ = ต้องระวัง

## A. ก่อนสร้างแอปใน Console
| รายการ | สถานะ | หมายเหตุ |
|---|---|---|
| บัญชีนักพัฒนา Google Play | ✍️ | ยืนยันตัวตนให้เสร็จ (ค่าธรรมเนียมครั้งเดียว) |
| ประเภทบัญชี | ✍️⚠️ | **บัญชีส่วนตัวที่สร้างหลัง 13 พ.ย. 2023 ต้องทำ Closed testing ≥ 12 คน ต่อเนื่อง ≥ 14 วัน ก่อนขอ Production** |
| Package name | ✅⚠️ | `com.sek789102.lifekeep` — **เปลี่ยนไม่ได้หลังอัปโหลดครั้งแรก** หากต้องการชื่ออื่นให้แจ้งก่อนอัปโหลด |

## B. Store listing (Main store listing)
| รายการ | สถานะ | ไฟล์/ค่า |
|---|---|---|
| App name | ✅ | `LIFEKEEP: เอกสาร งาน ปฏิทิน` (27/30) — ดู STORE_LISTING.md |
| Short description | ✅ | 78/80 ตัวอักษร |
| Full description (ไทย) | ✅ | 1,561/4,000 ตัวอักษร |
| App icon 512×512 | ✅ | `store-listing/app-icon-512.png` |
| Feature graphic 1024×500 | ✅ | `store-listing/feature-graphic-1024x500.png` |
| Phone screenshots (2–8) | ✅ | `store-listing/screenshots/` 8 ภาพ 1080×1920 (เรนเดอร์จาก UI จริงด้วยข้อมูลตัวอย่าง) |
| Tablet screenshots | ✍️ | ไม่บังคับ (แอปรองรับแท็บเล็ต แต่ยังไม่ได้เตรียมภาพ) |
| App category | ✅ | Productivity (แนะนำ) |
| Contact email | ✍️ | **ต้องกรอกเอง** |
| Privacy policy URL | ✍️ | **ต้องโฮสต์ `docs/privacy-policy.html` เป็น URL สาธารณะเอง** (เช่น GitHub Pages / เว็บส่วนตัว) และกรอกชื่อ/อีเมลในไฟล์ก่อน |

## C. App content (Policy)
| แบบฟอร์ม | คำตอบที่แนะนำ (ตามพฤติกรรมจริงของแอป) |
|---|---|
| Privacy policy | URL จากข้อ B ✍️ |
| Ads | **No, my app does not contain ads** |
| App access | **All functionality is available without special access** (ไม่มีล็อกอิน; ล็อกแอปเป็นตัวเลือกที่ผู้ใช้ตั้งเอง) |
| Content rating | ✍️ ตอบแบบสอบถาม IARC เอง — แอปไม่มีความรุนแรง เพศ การพนัน ยาเสพติด ไม่มีการสื่อสารระหว่างผู้ใช้ ไม่มี UGC สาธารณะ ไม่แชร์ตำแหน่ง (คาดว่าได้เรตทั่วไป) |
| Target audience | ✍️ เจ้าของแอปเลือก — **แนะนำ 18+** (หรือ 13+) แอปไม่ได้ออกแบบสำหรับเด็ก หากเลือกช่วงอายุต่ำกว่า 13 จะเข้านโยบาย Families |
| Data safety | ✅ **No data collected or shared** — ดู DATA_SAFETY.md |
| Government app | No |
| Financial features | No (หมวด "การเงิน" เป็นเพียงป้ายจัดเอกสาร ไม่ใช่บริการทางการเงิน) |
| Health | No health features (หมวด "สุขภาพ" เป็นเพียงป้ายจัดเอกสาร) |
| News app | No |
| Permissions declarations | ✅ คาดว่าไม่ต้องยื่น: ใช้ `SCHEDULE_EXACT_ALARM` (ผู้ใช้อนุญาตเอง) ไม่ใช่ `USE_EXACT_ALARM`, ไม่มีสิทธิ์รูปภาพ/ไฟล์กว้าง, ไม่มี foreground service — ⚠️ ให้ดูคำเตือนใน Console หลังอัปโหลด |

## D. Release
| รายการ | สถานะ | หมายเหตุ |
|---|---|---|
| Android App Bundle | ✅ | `LIFEKEEP-1.0.0-2-release.aab` เซ็นด้วย Upload key (ตรวจลายเซ็นแล้ว) |
| Play App Signing | ✍️ | **เลือก "Use Google-generated key" (ค่าเริ่มต้น)** แล้วอัปโหลด AAB ที่เซ็นด้วย upload key |
| Upload key | ✅ | ส่งให้เจ้าของแล้วแยกต่างหาก (ไม่อยู่ใน GitHub) · SHA-256 ใน `signed/upload-certificate-sha256.txt` |
| targetSdk | ✅ | 36 (Android 16) — ตรงตามข้อกำหนด Play ปัจจุบัน |
| versionCode / versionName | ✅ | 2 / 1.0.0 — ครั้งถัดไปต้องเพิ่ม versionCode ใน `pubspec.yaml` (เช่น `version: 1.0.1+3`) |
| Release notes | ✅ | RELEASE_NOTES.md (th-TH, en-US) |
| Native debug symbols | ⚠️ | Console อาจเตือนว่าไม่มี debug symbols — ไม่ขวางการเผยแพร่ |
| Countries / pricing | ✍️ | เลือกประเทศ (เช่น ไทย) · ฟรี |

## E. Testing track ที่แนะนำ
1. **Internal testing** — อัปโหลด AAB, เพิ่มอีเมลผู้ทดสอบ, ติดตั้งจาก Play และตรวจ: แจ้งเตือน, กล้อง/แกลเลอรี, สำรอง→กู้คืนบนมือถือจริง
2. **Closed testing** — ≥ 12 ผู้ทดสอบ ต่อเนื่อง ≥ 14 วัน (บังคับสำหรับบัญชีส่วนตัวใหม่)
3. ตอบแบบสอบถามการทดสอบ แล้ว **Apply for production**
4. **Production** — แนะนำ staged rollout (เช่น 20% → 100%)

> ⚠️ APK ที่ติดตั้งจากไฟล์ (เซ็นด้วย upload key) กับแอปที่ติดตั้งจาก Play (เซ็นด้วย app signing key ของ Google) **มีลายเซ็นต่างกัน** — ต้องถอนการติดตั้งตัวที่ลงจากไฟล์ก่อนติดตั้งจาก Play (สำรองข้อมูลก่อน)
