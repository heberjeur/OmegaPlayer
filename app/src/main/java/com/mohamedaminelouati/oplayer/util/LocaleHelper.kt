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

    fun createConfigurationContext(context: Context, languageTag: String): Context {
        if (languageTag.isEmpty()) {
            return context
        }
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun getLocalizedContext(baseContext: Context, languageTag: String): Context {
        if (languageTag.isEmpty()) {
            return baseContext
        }
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return LocalizedContextWrapper(baseContext, config)
    }

    fun getLocalizedConfiguration(baseContext: Context, languageTag: String): Configuration {
        val config = Configuration(baseContext.resources.configuration)
        if (languageTag.isNotEmpty()) {
            val locale = Locale.forLanguageTag(languageTag)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
        }
        return config
    }

    fun getLayoutDirection(languageTag: String): LayoutDirection {
        val locale = if (languageTag.isNotEmpty()) {
            Locale.forLanguageTag(languageTag)
        } else {
            Locale.getDefault()
        }
        val direction = TextUtilsCompat.getLayoutDirectionFromLocale(locale)
        return if (direction == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    }
}

