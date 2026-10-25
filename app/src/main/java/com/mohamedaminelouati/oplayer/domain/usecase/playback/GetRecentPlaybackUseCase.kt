package com.mohamedaminelouati.oplayer.domain.usecase.playback

import com.mohamedaminelouati.oplayer.data.RecentPlayback
import com.mohamedaminelouati.oplayer.data.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentPlaybackUseCase @Inject constructor(
    private val repository: PlaybackRepository
) {
    operator fun invoke(): Flow<List<RecentPlayback>> = repository.getRecentPlayback()
}
