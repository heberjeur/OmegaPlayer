package com.mohamedaminelouati.oplayer.domain.usecase.playback

import com.mohamedaminelouati.oplayer.data.Playlist
import com.mohamedaminelouati.oplayer.data.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaylistsUseCase @Inject constructor(
    private val repository: PlaybackRepository
) {
    operator fun invoke(): Flow<List<Playlist>> = repository.getPlaylists()
}
