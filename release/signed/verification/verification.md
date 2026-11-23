# Signed release verification

- Commit: 7b70b05

## App Bundle (LIFEKEEP-1.1.1-4-release.aab)
- ✅ bundletool validate: OK
- ✅ jarsigner -verify: jar verified
- ✅ signed by the upload certificate (SHA-256 matches upload-certificate-sha256.txt)
- android:versionCode="4"
- android:versionName="1.1.1"
- package="com.sek789102.lifekeep"
- INTERNET permission entries: 0
- ✅ universal APK built from the signed AAB installed on emulator (API 35)
- ✅ app running 10 s after launch
- crash/error lines in logcat: 0
