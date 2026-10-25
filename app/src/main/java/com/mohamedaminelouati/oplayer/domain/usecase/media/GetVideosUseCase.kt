package com.mohamedaminelouati.oplayer.domain.usecase.media

import com.mohamedaminelouati.oplayer.data.VideoModel
import com.mohamedaminelouati.oplayer.data.repository.MediaRepository
import com.mohamedaminelouati.oplayer.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVideosUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<Resource<List<VideoModel>>> = repository.getVideos()
}
