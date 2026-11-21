# Signed release verification

- Commit: a3ffa54

## App Bundle (LIFEKEEP-1.0.0-2-release.aab)
- ✅ bundletool validate: OK
- ✅ jarsigner -verify: jar verified
- ✅ signed by the upload certificate (SHA-256 matches upload-certificate-sha256.txt)
- android:versionCode="2"
- android:versionName="1.0.0"
- package="com.sek789102.lifekeep"
- INTERNET permission entries: 0
- ✅ universal APK built from the signed AAB installed on emulator (API 35)
- ✅ app running 10 s after launch
- crash/error lines in logcat: 0
