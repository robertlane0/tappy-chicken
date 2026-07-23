package com.tappy.chicken

import android.view.SurfaceHolder

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameSurfaceView
) : Thread() {

    @Volatile
    var running: Boolean = false

    override fun run() {
        var lastTime = System.nanoTime()

        while (running) {
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                try {
                    synchronized(surfaceHolder) {
                        val currentTime = System.nanoTime()
                        val deltaTime = (currentTime - lastTime) / 1_000_000_000f
                        lastTime = currentTime

                        val clampedDeltaTime = deltaTime.coerceAtMost(0.1f)

                        gameView.update(clampedDeltaTime)
                        gameView.render(canvas)
                    }
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
