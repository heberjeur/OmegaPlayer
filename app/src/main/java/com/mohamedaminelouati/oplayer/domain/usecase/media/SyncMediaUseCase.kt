package com.mohamedaminelouati.oplayer.domain.usecase.media

import com.mohamedaminelouati.oplayer.data.repository.MediaRepository
import javax.inject.Inject

class SyncMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke() {
        repository.syncMediaWithSystem()
    }
}
