package com.tappy.chicken

import kotlin.random.Random

data class PipePair(
    var x: Float,
    var gapCenter: Float,
    var passed: Boolean = false
) {
    companion object {
        const val PIPE_WIDTH = 180f
        const val GAP_HEIGHT = 320f
        const val PIPE_SPACING = 600f
        const val SCROLL_SPEED = -350f
        const val GAP_CENTER_MIN = 350f
        const val GAP_CENTER_MAX = 1250f
        const val INITIAL_SPAWN_X = 1200f
    }

    val topPipeBottom: Float get() = gapCenter - GAP_HEIGHT / 2f
    val bottomPipeTop: Float get() = gapCenter + GAP_HEIGHT / 2f

    fun topPipeBox(): Rect {
        return Rect(x, 0f, x + PIPE_WIDTH, topPipeBottom)
    }

    fun bottomPipeBox(): Rect {
        return Rect(x, bottomPipeTop, x + PIPE_WIDTH, GameSurfaceView.GROUND_Y)
    }

    fun centerX(): Float = x + PIPE_WIDTH / 2f

    fun recycle(furthestX: Float) {
        x = furthestX + PIPE_SPACING
        gapCenter = Random.nextFloat() * (GAP_CENTER_MAX - GAP_CENTER_MIN) + GAP_CENTER_MIN
        passed = false
    }

    fun update(deltaTime: Float) {
        x += SCROLL_SPEED * deltaTime
    }

    fun isOffScreenLeft(): Boolean = x + PIPE_WIDTH < 0f
}

fun createInitialPipes(): MutableList<PipePair> {
    val pipes = mutableListOf<PipePair>()
    var x = PipePair.INITIAL_SPAWN_X
    for (i in 0 until 3) {
        val gapCenter = Random.nextFloat() * (PipePair.GAP_CENTER_MAX - PipePair.GAP_CENTER_MIN) + PipePair.GAP_CENTER_MIN
        pipes.add(PipePair(x, gapCenter))
        x += PipePair.PIPE_SPACING
    }
    return pipes
}
