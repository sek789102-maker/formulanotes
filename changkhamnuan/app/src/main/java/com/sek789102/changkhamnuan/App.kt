package com.sek789102.changkhamnuan

import android.app.Application
import com.sek789102.changkhamnuan.data.AppDb
import com.sek789102.changkhamnuan.data.Repository
import com.sek789102.changkhamnuan.data.SettingsRepository

class App : Application() {
    val settingsRepo by lazy { SettingsRepository(this) }
    val repo by lazy { Repository(AppDb.get(this), settingsRepo) }
}
