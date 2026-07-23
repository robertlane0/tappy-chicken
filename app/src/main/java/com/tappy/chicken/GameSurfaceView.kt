package com.tappy.chicken

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameSurfaceView : SurfaceView, SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    var gameState: GameState = GameState.READY
        private set

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread(holder, this).also {
            it.running = true
            it.start()
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.let { thread ->
            thread.running = false
            try {
                thread.join()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        gameThread = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (gameState) {
                GameState.READY -> {
                    gameState = GameState.PLAYING
                }
                GameState.PLAYING -> {
                    // flap
                }
                GameState.DEAD -> {
                    // wait for ground impact
                }
                GameState.GAME_OVER -> {
                    // handled by button
                }
            }
        }
        return true
    }

    fun update(deltaTime: Float) {
        when (gameState) {
            GameState.READY -> { /* idle */ }
            GameState.PLAYING -> { /* physics update */ }
            GameState.DEAD -> { /* falling */ }
            GameState.GAME_OVER -> { /* frozen */ }
        }
    }

    fun render(canvas: Canvas) {
        canvas.drawColor(android.graphics.Color.rgb(135, 206, 235))
    }

    fun resetGame() {
        gameState = GameState.READY
    }
}
