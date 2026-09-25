package com.arslandaim.omegaplayer.domain.usecase.media

import com.arslandaim.omegaplayer.data.repository.MediaRepository
import javax.inject.Inject

class SyncMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke() {
        repository.syncMediaWithSystem()
    }
}
