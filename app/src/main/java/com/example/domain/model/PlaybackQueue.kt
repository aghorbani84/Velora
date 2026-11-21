package com.example.domain.model

import kotlin.random.Random

enum class QueueMode {
    NORMAL,
    SHUFFLE
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class QueueAdvanceResult(
    val queue: PlaybackQueue,
    val nextTrack: Track?,
    val shouldStopPlayback: Boolean,
    val seekToStartOnly: Boolean = false
)

data class PlaybackQueue(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val mode: QueueMode = QueueMode.NORMAL,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffledOrder: List<Int> = emptyList()
) {
    val currentTrack: Track?
        get() = tracks.getOrNull(currentIndex)

    val isEmpty: Boolean
        get() = tracks.isEmpty()

    val activeOrder: List<Int>
        get() = if (mode == QueueMode.SHUFFLE && shuffledOrder.size == tracks.size) {
            shuffledOrder
        } else {
            tracks.indices.toList()
        }

    val orderedQueueTracks: List<Track>
        get() = activeOrder.mapNotNull { tracks.getOrNull(it) }

    fun setQueue(
        newTracks: List<Track>,
        startIndex: Int = 0,
        random: Random = Random.Default
    ): PlaybackQueue {
        if (newTracks.isEmpty()) {
            return copy(
                tracks = emptyList(),
                currentIndex = -1,
                shuffledOrder = emptyList()
            )
        }
        val validStart = startIndex.coerceIn(0, newTracks.lastIndex)
        val order = if (mode == QueueMode.SHUFFLE) {
            buildShuffledOrder(newTracks.size, validStart, random)
        } else {
            newTracks.indices.toList()
        }
        return copy(
            tracks = newTracks,
            currentIndex = validStart,
            shuffledOrder = order
        )
    }

    fun selectTrack(trackId: String, random: Random = Random.Default): PlaybackQueue {
        val index = tracks.indexOfFirst { it.id == trackId }
        if (index < 0) return this
        val order = if (mode == QueueMode.SHUFFLE) {
            buildShuffledOrder(tracks.size, index, random)
        } else {
            tracks.indices.toList()
        }
        return copy(currentIndex = index, shuffledOrder = order)
    }

    fun appendTrack(track: Track, random: Random = Random.Default): PlaybackQueue {
        if (tracks.any { it.id == track.id }) return this
        val updatedTracks = tracks + track
        val updatedCurrent = if (currentIndex < 0) 0 else currentIndex
        val updatedOrder = if (mode == QueueMode.SHUFFLE) {
            val newIndex = updatedTracks.lastIndex
            if (shuffledOrder.size == tracks.size) {
                shuffledOrder + newIndex
            } else {
                buildShuffledOrder(updatedTracks.size, updatedCurrent, random)
            }
        } else {
            updatedTracks.indices.toList()
        }
        return copy(
            tracks = updatedTracks,
            currentIndex = updatedCurrent,
            shuffledOrder = updatedOrder
        )
    }

    fun playNext(track: Track, random: Random = Random.Default): PlaybackQueue {
        if (tracks.isEmpty()) return setQueue(listOf(track), 0, random)
        val filtered = tracks.filterNot { it.id == track.id }
        val insertPos = (currentIndex + 1).coerceIn(0, filtered.size)
        val updatedTracks = filtered.toMutableList().apply { add(insertPos, track) }
        val updatedOrder = if (mode == QueueMode.SHUFFLE) {
            buildShuffledOrder(updatedTracks.size, currentIndex, random)
        } else {
            updatedTracks.indices.toList()
        }
        return copy(
            tracks = updatedTracks,
            shuffledOrder = updatedOrder
        )
    }

    fun removeTrack(trackId: String, random: Random = Random.Default): PlaybackQueue {
        val removeIdx = tracks.indexOfFirst { it.id == trackId }
        if (removeIdx < 0) return this
        val updatedTracks = tracks.toMutableList().apply { removeAt(removeIdx) }
        if (updatedTracks.isEmpty()) {
            return copy(tracks = emptyList(), currentIndex = -1, shuffledOrder = emptyList())
        }
        val updatedCurrent = when {
            currentIndex > removeIdx -> currentIndex - 1
            currentIndex == removeIdx -> removeIdx.coerceAtMost(updatedTracks.lastIndex)
            else -> currentIndex
        }
        val updatedOrder = if (mode == QueueMode.SHUFFLE) {
            buildShuffledOrder(updatedTracks.size, updatedCurrent, random)
        } else {
            updatedTracks.indices.toList()
        }
        return copy(
            tracks = updatedTracks,
            currentIndex = updatedCurrent,
            shuffledOrder = updatedOrder
        )
    }

    fun toggleShuffle(random: Random = Random.Default): PlaybackQueue {
        val nextMode = if (mode == QueueMode.NORMAL) QueueMode.SHUFFLE else QueueMode.NORMAL
        return setMode(nextMode, random)
    }

    fun setMode(newMode: QueueMode, random: Random = Random.Default): PlaybackQueue {
        val anchor = currentIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        val order = if (newMode == QueueMode.SHUFFLE && tracks.isNotEmpty()) {
            buildShuffledOrder(tracks.size, anchor, random)
        } else {
            tracks.indices.toList()
        }
        return copy(mode = newMode, shuffledOrder = order)
    }

    fun cycleRepeatMode(): PlaybackQueue {
        val nextRepeat = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        return copy(repeatMode = nextRepeat)
    }

    fun moveNext(): QueueAdvanceResult {
        if (tracks.isEmpty() || currentIndex !in tracks.indices) {
            return QueueAdvanceResult(
                queue = this,
                nextTrack = null,
                shouldStopPlayback = true
            )
        }

        if (repeatMode == RepeatMode.ONE) {
            return QueueAdvanceResult(
                queue = this,
                nextTrack = tracks[currentIndex],
                shouldStopPlayback = false,
                seekToStartOnly = true
            )
        }

        val order = activeOrder
        val positionInOrder = order.indexOf(currentIndex).takeIf { it >= 0 } ?: 0
        return if (positionInOrder + 1 < order.size) {
            val nextIdx = order[positionInOrder + 1]
            val updated = copy(currentIndex = nextIdx)
            QueueAdvanceResult(
                queue = updated,
                nextTrack = updated.currentTrack,
                shouldStopPlayback = false
            )
        } else {
            when (repeatMode) {
                RepeatMode.ALL -> {
                    val wrappedIdx = order.first()
                    val updated = copy(currentIndex = wrappedIdx)
                    QueueAdvanceResult(
                        queue = updated,
                        nextTrack = updated.currentTrack,
                        shouldStopPlayback = false
                    )
                }
                RepeatMode.OFF -> {
                    QueueAdvanceResult(
                        queue = this,
                        nextTrack = null,
                        shouldStopPlayback = true
                    )
                }
                RepeatMode.ONE -> {
                    QueueAdvanceResult(
                        queue = this,
                        nextTrack = tracks[currentIndex],
                        shouldStopPlayback = false,
                        seekToStartOnly = true
                    )
                }
            }
        }
    }

    fun movePrevious(currentPositionMs: Long = 0L): QueueAdvanceResult {
        if (tracks.isEmpty() || currentIndex !in tracks.indices) {
            return QueueAdvanceResult(
                queue = this,
                nextTrack = null,
                shouldStopPlayback = true
            )
        }

        if (currentPositionMs > 3_000L || repeatMode == RepeatMode.ONE) {
            return QueueAdvanceResult(
                queue = this,
                nextTrack = tracks[currentIndex],
                shouldStopPlayback = false,
                seekToStartOnly = true
            )
        }

        val order = activeOrder
        val positionInOrder = order.indexOf(currentIndex).takeIf { it >= 0 } ?: 0
        return if (positionInOrder > 0) {
            val prevIdx = order[positionInOrder - 1]
            val updated = copy(currentIndex = prevIdx)
            QueueAdvanceResult(
                queue = updated,
                nextTrack = updated.currentTrack,
                shouldStopPlayback = false
            )
        } else {
            if (repeatMode == RepeatMode.ALL) {
                val wrappedIdx = order.last()
                val updated = copy(currentIndex = wrappedIdx)
                QueueAdvanceResult(
                    queue = updated,
                    nextTrack = updated.currentTrack,
                    shouldStopPlayback = false
                )
            } else {
                QueueAdvanceResult(
                    queue = this,
                    nextTrack = tracks[currentIndex],
                    shouldStopPlayback = false,
                    seekToStartOnly = true
                )
            }
        }
    }

    companion object {
        fun clampSeekPosition(requestedPositionMs: Long, durationMs: Long): Long {
            if (durationMs <= 0L) return 0L
            return requestedPositionMs.coerceIn(0L, durationMs)
        }

        fun buildShuffledOrder(size: Int, anchorIndex: Int, random: Random): List<Int> {
            if (size <= 0) return emptyList()
            if (size == 1) return listOf(0)
            val validAnchor = anchorIndex.coerceIn(0, size - 1)
            val remaining = (0 until size).filter { it != validAnchor }.toMutableList()
            remaining.shuffle(random)
            val naturalRemaining = (0 until size).filter { it != validAnchor }
            if (remaining.size >= 2 && remaining == naturalRemaining) {
                val first = remaining.removeAt(0)
                remaining.add(first)
            }
            return listOf(validAnchor) + remaining
        }
    }
}
