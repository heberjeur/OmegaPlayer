package com.mohamedaminelouati.oplayer.di

import android.content.Context
import com.mohamedaminelouati.oplayer.data.AppDao
import com.mohamedaminelouati.oplayer.data.AppDatabase
import com.mohamedaminelouati.oplayer.data.ThemePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAppDao(database: AppDatabase): AppDao {
        return database.appDao()
    }

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }
}
