# Signed release verification

- Commit: 2f22c40

## App Bundle (Jotwai-1.2.1-7-release.aab)
- ✅ bundletool validate: OK
- ✅ jarsigner -verify: jar verified
- ✅ signed by the upload certificate (SHA-256 matches upload-certificate-sha256.txt)
- android:versionCode="7"
- android:versionName="1.2.1"
- package="com.sek789102.jotwai"
- INTERNET permission entries: 0
- ✅ universal APK built from the signed AAB installed on emulator (API 35)
- ✅ app running 10 s after launch
- crash/error lines in logcat: 0
