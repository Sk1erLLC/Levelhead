package club.sk1er.mods.levelhead.core

import club.sk1er.mods.levelhead.Levelhead
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

/**
 * Based on [BucketRateLimiter.kt] from kord
 * Licensed under MIT License
 * https://github.com/kordlib/kord/blob/0.8.x/common/src/main/kotlin/ratelimit/BucketRateLimiter.kt
 */
@OptIn(ExperimentalTime::class)
class RateLimiter constructor(
    private val capacity: Int,
    private val interval: Duration,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val mutex: Mutex = Mutex()

    private var count: Int = 0
    private var nextInterval: Instant = Instant.ofEpochMilli(0)

    private val isNextInterval: Boolean
        get() = nextInterval <= clock.instant()
    private val isAtCapacity: Boolean
        get() = count == capacity

    fun resetState() {
        count = 0
        nextInterval = clock.instant() + interval
        Levelhead.displayManager.checkCacheSizes()
    }

    private suspend fun delayUntilNextInterval() {
        val delay = Duration.between(clock.instant(), nextInterval)
        delay(delay.toKotlinDuration())
    }

    suspend fun consume() = mutex.withLock {
        if (isNextInterval) resetState()

        if (isAtCapacity) {
            delayUntilNextInterval()
            resetState()
        }

        count++
    }
}