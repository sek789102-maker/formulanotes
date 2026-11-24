package com.sek789102.jotwai.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/** App language: follow the phone, Thai, or English. Kept in plain SharedPreferences so it can be read at start-up. */
object Lang {
    const val SYSTEM = "system"
    const val THAI = "th"
    const val ENGLISH = "en"
    private const val PREFS = "jotwai_lang"
    private const val KEY = "lang"

    fun saved(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun save(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value).apply()
    }

    private fun localeOf(value: String): Locale? = when (value) {
        THAI -> Locale.forLanguageTag("th-TH")
        ENGLISH -> Locale.ENGLISH
        else -> null
    }

    /** Context with the chosen language applied (unchanged when following the phone). */
    fun wrap(base: Context): Context {
        val locale = localeOf(saved(base)) ?: return base
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    /** Applies a new choice and restarts the screen so every text changes. */
    fun change(context: Context, value: String) {
        save(context, value)
        val locale = localeOf(value) ?: Locale.forLanguageTag(android.content.res.Resources.getSystem().configuration.locales[0].toLanguageTag())
        Locale.setDefault(locale)
        context.findActivity()?.recreate()
    }

    private fun Context.findActivity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    val isEnglish: Boolean get() = Locale.getDefault().language == "en"
}

/** Thai or English text, by the app language. */
fun t(th: String, en: String): String = if (Lang.isEnglish) en else th
