package com.sek789102.changkhamnuan.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

private val Context.store by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The contractor's own details, printed on quotes (never the app's name). */
data class BusinessProfile(
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val lineId: String = "",
    val address: String = "",
    val taxId: String = "",
    val terms: String = "ชำระเงินมัดจำ 50% ก่อนเริ่มงาน ส่วนที่เหลือชำระเมื่องานแล้วเสร็จ"
) {
    val isEmpty: Boolean get() = shopName.isBlank() && ownerName.isBlank() && phone.isBlank()
}

data class Settings(
    val theme: ThemeMode = ThemeMode.DARK,
    val profile: BusinessProfile = BusinessProfile(),
    /** User changes to material usage rates (key → value). */
    val rateOverrides: Map<String, Double> = emptyMap(),
    val lastBackupAt: Long = 0L,
    val onboarded: Boolean = false
)

class SettingsRepository(private val context: Context) {
    private object K {
        val THEME = stringPreferencesKey("theme")
        val PROFILE = stringPreferencesKey("profile_json")
        val RATES = stringPreferencesKey("rates_json")
        val LAST_BACKUP = longPreferencesKey("last_backup")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<Settings> = context.store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    private fun Preferences.toSettings() = Settings(
        theme = ThemeMode.entries.firstOrNull { it.name == this[K.THEME] } ?: ThemeMode.DARK,
        profile = profileFromJson(this[K.PROFILE]),
        rateOverrides = ratesFromJson(this[K.RATES]),
        lastBackupAt = this[K.LAST_BACKUP] ?: 0L,
        onboarded = this[K.ONBOARDED] ?: false
    )

    suspend fun setTheme(t: ThemeMode) { context.store.edit { it[K.THEME] = t.name } }
    suspend fun setProfile(p: BusinessProfile) { context.store.edit { it[K.PROFILE] = profileToJson(p) } }
    suspend fun setRates(r: Map<String, Double>) { context.store.edit { it[K.RATES] = ratesToJson(r) } }
    suspend fun setLastBackup(t: Long) { context.store.edit { it[K.LAST_BACKUP] = t } }
    suspend fun setOnboarded() { context.store.edit { it[K.ONBOARDED] = true } }

    companion object {
        fun profileToJson(p: BusinessProfile): String = JSONObject()
            .put("shopName", p.shopName).put("ownerName", p.ownerName).put("phone", p.phone)
            .put("lineId", p.lineId).put("address", p.address).put("taxId", p.taxId).put("terms", p.terms)
            .toString()

        fun profileFromJson(s: String?): BusinessProfile {
            if (s.isNullOrBlank()) return BusinessProfile()
            return runCatching {
                val o = JSONObject(s)
                BusinessProfile(
                    o.optString("shopName"), o.optString("ownerName"), o.optString("phone"), o.optString("lineId"),
                    o.optString("address"), o.optString("taxId"), o.optString("terms", BusinessProfile().terms)
                )
            }.getOrDefault(BusinessProfile())
        }

        fun ratesToJson(r: Map<String, Double>): String = JSONObject().apply { r.forEach { (k, v) -> put(k, v) } }.toString()

        fun ratesFromJson(s: String?): Map<String, Double> {
            if (s.isNullOrBlank()) return emptyMap()
            return runCatching {
                val o = JSONObject(s)
                o.keys().asSequence().mapNotNull { k -> o.optDouble(k).takeIf { it.isFinite() }?.let { k to it } }.toMap()
            }.getOrDefault(emptyMap())
        }
    }
}
