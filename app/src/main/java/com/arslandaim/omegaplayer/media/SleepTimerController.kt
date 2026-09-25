/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepTimerController(
    private val scope: CoroutineScope,
    private val onExpire: suspend () -> Unit
) {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _timeLeftMillis = MutableStateFlow(0L)
    val timeLeftMillis: StateFlow<Long> = _timeLeftMillis.asStateFlow()

    private var countdownJob: Job? = null

    fun start(minutes: Int) {
        countdownJob?.cancel()
        if (minutes <= 0) {
            deactivate()
            return
        }
        _active.value = true
        _timeLeftMillis.value = minutes * 60 * 1000L
        countdownJob = scope.launch {
            while (_timeLeftMillis.value > 0) {
                delay(1000)
                _timeLeftMillis.value -= 1000
            }
            onExpire()
            _active.value = false
        }
    }

    fun activateWithoutCountdown() {
        countdownJob?.cancel()
        _active.value = true
        _timeLeftMillis.value = 0
    }

    fun deactivate() {
        countdownJob?.cancel()
        _active.value = false
        _timeLeftMillis.value = 0
    }
}
