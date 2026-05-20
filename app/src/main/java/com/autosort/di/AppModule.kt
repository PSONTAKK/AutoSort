package com.autosort.di

import android.content.Context
import com.autosort.data.auth.GoogleAuthManager
import com.autosort.data.config.AppConfig
import com.autosort.data.db.AiInsightDao
import com.autosort.data.db.AppDatabase
import com.autosort.data.db.LogDao
import com.autosort.data.db.RuleDao
import com.autosort.data.model.DestinationType
import com.autosort.data.repository.LogRepository
import com.autosort.data.repository.RuleRepository
import com.autosort.service.FileSortEngine
import com.autosort.service.destination.FileDestination
import com.autosort.service.destination.GoogleDriveDestination
import com.autosort.service.destination.LocalFileDestination
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Central Hilt module that provides all shared dependencies.
 * This replaces manual object creation scattered across ViewModels and Services.
 *
 * IMPORTANT: This file only defines HOW to create objects.
 * It does NOT change any business logic or UI behaviour.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideRuleDao(db: AppDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideLogDao(db: AppDatabase): LogDao = db.logDao()

    @Provides
    @Singleton
    fun provideRuleRepository(ruleDao: RuleDao): RuleRepository {
        return RuleRepository(ruleDao)
    }

    @Provides
    @Singleton
    fun provideLogRepository(logDao: LogDao): LogRepository {
        return LogRepository(logDao)
    }

    @Provides
    @Singleton
    fun provideAppConfig(@ApplicationContext context: Context): AppConfig {
        return AppConfig(context)
    }

    @Provides
    @Singleton
    fun provideGoogleAuthManager(@ApplicationContext context: Context): GoogleAuthManager {
        return GoogleAuthManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFileSortEngine(
        ruleRepository: RuleRepository,
        logRepository: LogRepository,
        googleAuthManager: GoogleAuthManager
    ): FileSortEngine {
        val destinations: Map<DestinationType, FileDestination> = mapOf(
            DestinationType.LOCAL        to LocalFileDestination(),
            DestinationType.CLOUD_GDRIVE to GoogleDriveDestination(googleAuthManager)
        )
        return FileSortEngine(ruleRepository, logRepository, destinations)
    }

    @Provides
    @Singleton
    fun provideAiInsightDao(database: AppDatabase): AiInsightDao {
        return database.aiInsightDao()
    }
}
