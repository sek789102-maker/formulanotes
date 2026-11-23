# Project memory (read first)

The owner speaks Thai — always answer in Thai. Work on branch `claude/lifekeep-app-development-8kkme4`.
Never open a pull request unless asked.

## Rules from the owner (always apply)
- Never reveal keystore / key passwords; never commit a keystore or any secret to GitHub.
- Do not claim the app encrypts data (it does not; backups are plain JSON).
- Do not say "Ready for Play Store" while important steps are unverified or still to be done by the owner.
- If something needs the owner's own information (price, contact, privacy-policy URL, tax/bank), say so — never invent it.
- Keep answers short; the owner dislikes long waits — report progress honestly with times.

## Apps in this repo (Android, offline)
| Folder | App | Package | Latest |
|---|---|---|---|
| `lifekeep/` | LIFEKEEP (documents, tasks, calendar) — **Flutter** | com.sek789102.lifekeep | 1.1.0 (versionCode 3) |
| `jotwai/` | จดไว้ (notes) — Kotlin + Compose | com.sek789102.jotwai | 1.0.2 (versionCode 3) |
| `changkhamnuan/` | ช่างคำนวณ (material calculator + contractor documents, paid app, no Pro tier) — Kotlin + Compose | com.sek789102.changkhamnuan | 1.3.1 (versionCode 5) |

Release files (docs, store listing, signed AAB, upload-cert fingerprint) live in `release/`, `release-jotwai/`, `release-changkhamnuan/`.
Signing keys are NOT in the repo — the owner keeps them and attaches them when an AAB must be signed
(ช่างคำนวณ upload key: alias `upload`, SHA-256 A0:51:6F:60:D7:30:58:20:C5:1C:28:AE:1C:11:32:F8:A3:8E:FC:88:54:87:A8:8B:8F:99:BA:A5:B7:39:88:6D).

## How builds work
- No local Android SDK: every build runs in GitHub Actions (`.github/workflows/changkhamnuan-android.yml`, etc.).
- CI runs unit tests (JVM + Robolectric), builds debug APK + unsigned release AAB, runs an emulator smoke test
  (`changkhamnuan/ci/emulator_smoke.sh`) and store screenshots (`ci/store_screenshots.sh`), then commits outputs to
  `changkhamnuan-build/` with message `CI ChangKhamnuan build <sha> (emulator: ...)`.
- Sign the AAB locally with jarsigner (SHA256withRSA) using the owner's key, then verify the fingerprint.
- LIFEKEEP (Flutter 3.47.9): the Flutter SDK can be downloaded from storage.googleapis.com and `flutter analyze` / `flutter test` run locally (CI fails on any analyzer info). Don't run `dart format` on the whole tree.
- Pure-Kotlin domain tests can be compiled and run locally with kotlin-compiler-embeddable from Maven Central (fast check before pushing).

## ช่างคำนวณ — what exists (1.3.1)
- 27 calculators in 7 groups (domain/Calculators.kt, ExtraCalculators.kt, CivilCalculators.kt): concrete, footing, piles,
  stairs · rebar, rebar by spacing, structural steel, formwork · bricks, plaster, drywall, paint · tiles, flooring, ceiling,
  roof · excavation, fill, cut & fill, retaining wall · road, drain, yard, fence · pipes, tanks, slope (+ unit converter).
  Quantity take-off only — never structural design.
- Jobs: quote / invoice / receipt (whole job or per payment stage) and BOQ as image, PDF, CSV (util/QuoteRenderer.kt);
  payment stages, daily labour & site costs, profit per job, combined order list (ui/screens/ContractorScreens.kt, domain/Contractor.kt).
- Room DB v2 with MIGRATION_1_2 (installments, costs) — keep migrations for every schema change.
- Dark yellow UI; card art in res/drawable-nodpi (cropped from the owner's AI images).

## Ideas discussed but NOT wanted yet
PromptPay QR on invoices, shop logo + signature on documents, business overview page, contract template, job templates.
Do not build these unless the owner asks again.
