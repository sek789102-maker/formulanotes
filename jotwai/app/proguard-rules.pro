# Keep line numbers & source files for Play Console crash symbolication
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room Database & Entities / DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.sek789102.formulabook.data.local.AppDatabase_Impl { *; }
-keep class com.sek789102.formulabook.data.model.** { *; }
-dontwarn androidx.room.paging.**

# DataStore Preferences & Protobuf lite
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-keep class androidx.datastore.preferences.** { *; }
-dontwarn androidx.datastore.preferences.**

# Alarm & BroadcastReceivers & WorkManager
-keep class com.sek789102.formulabook.alarm.ExamAlarmReceiver { *; }
-keep class com.sek789102.formulabook.alarm.BootCompletedReceiver { *; }
-keep class com.sek789102.formulabook.worker.** { *; }

# Jetpack Glance AppWidget
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
-keep class com.sek789102.formulabook.widget.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**
