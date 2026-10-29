package com.mohamedaminelouati.oplayer.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.text.TextUtilsCompat
import java.util.Locale

class LocalizedContextWrapper(
    private val base: Context,
    private val localizedConfig: Configuration
) : ContextWrapper(base) {
    private val localizedResources: Resources by lazy {
        base.createConfigurationContext(localizedConfig).resources
    }

    override fun getResources(): Resources = localizedResources

    override fun getApplicationContext(): Context = base.applicationContext

    override fun getBaseContext(): Context = base
}

object LocaleHelper {
    private const val PREFS_NAME = "locale_pref"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_INDIAN_NUMERALS = "use_indian_numerals"

    var useIndianNumerals: Boolean = false

    fun getSavedIndianNumerals(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_INDIAN_NUMERALS, false)
    }

    fun saveIndianNumerals(context: Context, enabled: Boolean) {
        useIndianNumerals = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INDIAN_NUMERALS, enabled)
            .apply()
    }

    fun formatDigits(text: String, useIndian: Boolean = useIndianNumerals): String {
        if (!useIndian) return text
        val sb = StringBuilder(text.length)
        for (i in 0 until text.length) {
            val ch = text[i]
            when (ch) {
                '0' -> sb.append('٠')
                '1' -> sb.append('١')
                '2' -> sb.append('٢')
                '3' -> sb.append('٣')
                '4' -> sb.append('٤')
                '5' -> sb.append('٥')
                '6' -> sb.append('٦')
                '7' -> sb.append('٧')
                '8' -> sb.append('٨')
                '9' -> sb.append('٩')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "") ?: ""
    }

    fun saveLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    val systemLocale: Locale by lazy {
        val sysConfig = Resources.getSystem().configuration
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            sysConfig.locales[0]
        } else {
            @Suppress("DEPRECATION")
            sysConfig.locale
        }
    }

    fun resolveLocale(languageTag: String): Locale {
        val baseLocale = if (languageTag.isNotEmpty()) {
            Locale.forLanguageTag(languageTag)
        } else {
            systemLocale
        }
        val lang = baseLocale.language
        val tag = if (lang == "ar") {
            if (useIndianNumerals) "ar-u-nu-arab" else "ar-u-nu-latn"
        } else {
            baseLocale.toLanguageTag()
        }
        return Locale.forLanguageTag(tag)
    }

    fun getEffectiveLocale(languageTag: String): Locale {
        return resolveLocale(languageTag)
    }

    fun isArabic(languageTag: String): Boolean {
        return getEffectiveLocale(languageTag).language == "ar"
    }

    fun createConfigurationContext(context: Context, languageTag: String): Context {
        val locale = getEffectiveLocale(languageTag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun getLocalizedContext(baseContext: Context, languageTag: String): Context {
        val locale = getEffectiveLocale(languageTag)
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return LocalizedContextWrapper(baseContext, config)
    }

    fun getLocalizedConfiguration(baseContext: Context, languageTag: String): Configuration {
        val locale = getEffectiveLocale(languageTag)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return config
    }

    fun getLayoutDirection(languageTag: String): LayoutDirection {
        val locale = getEffectiveLocale(languageTag)
        val direction = TextUtilsCompat.getLayoutDirectionFromLocale(locale)
        return if (direction == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    }
}

