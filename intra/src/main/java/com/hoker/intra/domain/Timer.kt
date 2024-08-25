package com.hoker.intra.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class Timer @Inject constructor() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun repeatEverySecond(action: suspend () -> Unit): Job {
        return scope.launch {
            while (isActive) {
                action()
                delay(1000L)
            }
        }
    }

    private fun startCoroutineTimer(delayMillis: Long = 0, repeatMillis: Long = 0, action: () -> Unit) = scope.launch(Dispatchers.IO) {
        delay(delayMillis)
        if(repeatMillis > 0) {
            while(isActive) {
                action()
                delay(repeatMillis)
            }
        } else {
            action()
        }
    }

    fun getTimer(delayMillis: Long = 0, repeatMillis: Long = 0, action: () -> Unit): Job {
        return startCoroutineTimer(repeatMillis = repeatMillis, delayMillis = delayMillis) {
            scope.launch(Dispatchers.Main) {
                action()
            }
        }
    }
}