package com.finnvek.cornersapart.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {
    @Provides
    @Singleton
    fun provideGameRepository(
        @ApplicationContext context: Context,
    ): GameRepository = context.gameRepository()

    @Provides
    @Singleton
    fun provideProfileRepository(
        @ApplicationContext context: Context,
    ): ProfileRepository = context.profileRepository()

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
    ): SettingsRepository = context.settingsRepository()
}
