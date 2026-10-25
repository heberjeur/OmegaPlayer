package com.mohamedaminelouati.oplayer.di

import com.mohamedaminelouati.oplayer.data.repository.MediaRepository
import com.mohamedaminelouati.oplayer.data.repository.MediaRepositoryImpl
import com.mohamedaminelouati.oplayer.data.repository.PlaybackRepository
import com.mohamedaminelouati.oplayer.data.repository.PlaybackRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        mediaRepositoryImpl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(
        playbackRepositoryImpl: PlaybackRepositoryImpl
    ): PlaybackRepository
}
