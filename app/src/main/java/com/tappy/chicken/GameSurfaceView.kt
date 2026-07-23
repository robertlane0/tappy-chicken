package com.tappy.chicken

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.roundToInt

class GameSurfaceView : SurfaceView, SurfaceHolder.Callback {

    companion object {
        const val VIRTUAL_WIDTH = 1080f
        const val VIRTUAL_HEIGHT = 1920f
        const val CEILING_Y = 100f
        const val GROUND_Y = 1600f
        const val GRAVITY = 3200f
        const val FLAP_IMPULSE = -900f
        const val MAX_FALL_SPEED = 1400f
        const val MAX_RISE_SPEED = -1200f
        const val CHICKEN_START_X = 200f
        const val CHICKEN_START_Y = 900f
        const val GROUND_SCROLL_SPEED = -350f
        const val BACKGROUND_SCROLL_SPEED = -70f
        const val GROUND_HEIGHT = 320f
    }

    private var gameThread: GameThread? = null
    var gameState: GameState = GameState.READY
        private set

    var onStateChanged: ((GameState, Int, Int) -> Unit)? = null
    var onScoreChanged: ((Int) -> Unit)? = null

    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var screenW: Int = 0
    private var screenH: Int = 0

    private var contextRef: Context? = null
    private var saveManager: SaveManager? = null
    private var soundManager: SoundManager? = null
    private val chicken = Chicken()
    private val pipes = mutableListOf<PipePair>()

    private var score: Int = 0
    var highScore: Int = 0
        private set

    private var animationTimer: Float = 0f
    private var groundOffset: Float = 0f
    private var bgOffset: Float = 0f
    private var flashAlpha: Float = 0f
    private var wasPlaying: Boolean = false

    @Volatile
    private var pendingReset: Boolean = false

    private var chickenBitmap: android.graphics.Bitmap? = null

    private val skyPaint = Paint().apply { color = Color.rgb(135, 206, 235) }
    private val grassPaint = Paint().apply { color = Color.rgb(46, 139, 87) }
    private val soilPaint = Paint().apply { color = Color.rgb(210, 180, 140) }
    private val cloudPaint = Paint().apply { color = Color.rgb(245, 245, 245) }
    private val cloudHighlightPaint = Paint().apply { color = Color.WHITE }
    private val flashPaint = Paint().apply { color = Color.WHITE }
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 54f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(8f, 4f, 4f, Color.BLACK)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        contextRef = context
        saveManager = SaveManager(context)
        soundManager = SoundManager(context)
        highScore = saveManager!!.getHighScore()
        holder.addCallback(this)
        chicken.reset(CHICKEN_START_X, CHICKEN_START_Y)
        resetPipes()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread(holder, this).also {
            it.running = true
            it.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenW = width
        screenH = height
        scaleX = width / VIRTUAL_WIDTH
        scaleY = height / VIRTUAL_HEIGHT

        contextRef?.let { ctx ->
            val cw = (chicken.visualWidth * scaleX).roundToInt()
            val ch = (chicken.visualHeight * scaleY).roundToInt()
            chickenBitmap = VectorCache.getBitmap(ctx, R.drawable.ic_chicken, cw, ch)
        }
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
                    chicken.velocityY = FLAP_IMPULSE
                    soundManager?.playFlap()
                    setState(GameState.PLAYING)
                }
                GameState.PLAYING -> {
                    chicken.velocityY = FLAP_IMPULSE
                    soundManager?.playFlap()
                }
                GameState.DEAD -> {}
                GameState.GAME_OVER -> {
                    resetGame()
                }
            }
        }
        return true
    }

    fun update(deltaTime: Float) {
        if (pendingReset) {
            pendingReset = false
            doReset()
        }
        if (flashAlpha > 0f) {
            flashAlpha = (flashAlpha - deltaTime / 0.15f).coerceAtLeast(0f)
        }
        when (gameState) {
            GameState.READY -> {
                chicken.y = CHICKEN_START_Y
                chicken.rotation = 0f
                groundOffset += GROUND_SCROLL_SPEED * deltaTime
                bgOffset += BACKGROUND_SCROLL_SPEED * deltaTime
            }
            GameState.PLAYING -> {
                chicken.velocityY += GRAVITY * deltaTime
                chicken.velocityY = chicken.velocityY.coerceIn(MAX_RISE_SPEED, MAX_FALL_SPEED)
                chicken.y += chicken.velocityY * deltaTime

                if (chicken.y < CEILING_Y) {
                    chicken.y = CEILING_Y
                    chicken.velocityY = 0f
                }

                chicken.rotation = calculateRotation(chicken.velocityY)

                for (pipe in pipes) {
                    pipe.update(deltaTime)
                }

                val iterator = pipes.iterator()
                while (iterator.hasNext()) {
                    val pipe = iterator.next()
                    if (pipe.isOffScreenLeft()) {
                        val maxX = pipes.maxOf { it.x }
                        pipe.recycle(maxX)
                    }
                }

                for (pipe in pipes) {
                    if (!pipe.passed && chicken.x > pipe.centerX()) {
                        pipe.passed = true
                        score++
                        soundManager?.playPoint()
                        onScoreChanged?.invoke(score)
                    }
                }

                if (checkCollisions()) {
                    soundManager?.playHit()
                    flashAlpha = 1f
                    setState(GameState.DEAD)
                }

                groundOffset += GROUND_SCROLL_SPEED * deltaTime
                bgOffset += BACKGROUND_SCROLL_SPEED * deltaTime
            }
            GameState.DEAD -> {
                chicken.velocityY += GRAVITY * deltaTime
                chicken.velocityY = chicken.velocityY.coerceAtMost(MAX_FALL_SPEED)
                chicken.y += chicken.velocityY * deltaTime
                chicken.rotation = calculateRotation(chicken.velocityY)

                if (chicken.y + chicken.collisionHeight / 2f >= GROUND_Y) {
                    chicken.y = GROUND_Y - chicken.collisionHeight / 2f
                    soundManager?.playFall()
                    saveManager?.saveScore(score)
                    if (score > highScore) highScore = score
                    setState(GameState.GAME_OVER)
                }
            }
            GameState.GAME_OVER -> {}
        }
    }

    fun render(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), skyPaint)

        drawClouds(canvas)
        drawPipes(canvas)
        drawChicken(canvas)
        drawGround(canvas)

        if (flashAlpha > 0f) {
            flashPaint.alpha = (flashAlpha * 255).roundToInt()
            canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), flashPaint)
        }
    }

    private fun checkCollisions(): Boolean {
        if (chicken.y + chicken.collisionHeight / 2f >= GROUND_Y) return true

        val chickenBox = chicken.getBoundingBox()
        for (pipe in pipes) {
            if (chickenBox.intersects(pipe.topPipeBox()) || chickenBox.intersects(pipe.bottomPipeBox())) {
                return true
            }
        }
        return false
    }

    private fun drawClouds(canvas: Canvas) {
        val cloudY = 300f * scaleY
        val cloudW = 200f * scaleX
        val cloudH = 80f * scaleY

        val offset = bgOffset % (VIRTUAL_WIDTH * scaleX)
        val baseX = offset

        canvas.drawOval(baseX - cloudW / 2f, cloudY - cloudH / 2f, baseX + cloudW / 2f, cloudY + cloudH / 2f, cloudPaint)
        canvas.drawOval(baseX + cloudW * 0.3f - cloudW / 2f, cloudY - cloudH / 2f, baseX + cloudW * 0.3f + cloudW / 2f, cloudY + cloudH / 2f, cloudHighlightPaint)

        val secondX = baseX + VIRTUAL_WIDTH * scaleX
        canvas.drawOval(secondX - cloudW / 2f, cloudY - cloudH / 2f, secondX + cloudW / 2f, cloudY + cloudH / 2f, cloudPaint)
        canvas.drawOval(secondX + cloudW * 0.3f - cloudW / 2f, cloudY - cloudH / 2f, secondX + cloudW * 0.3f + cloudW / 2f, cloudY + cloudH / 2f, cloudHighlightPaint)
    }

    private fun drawGround(canvas: Canvas) {
        val groundVirtualY = GROUND_Y * scaleY

        soilPaint.color = Color.rgb(210, 180, 140)
        canvas.drawRect(0f, groundVirtualY, screenW.toFloat(), screenH.toFloat(), soilPaint)

        grassPaint.color = Color.rgb(46, 139, 87)
        canvas.drawRect(0f, groundVirtualY, screenW.toFloat(), groundVirtualY + 30f * scaleY, grassPaint)

        val grassStripW = 60f * scaleX
        val scrollPx = -(groundOffset * scaleX)
        val offset = scrollPx % grassStripW
        val darkGrassPaint = Paint().apply { color = Color.rgb(34, 120, 60) }
        var gx = -offset
        while (gx < screenW) {
            canvas.drawRect(gx, groundVirtualY, gx + 3f * scaleX, groundVirtualY + 30f * scaleY, darkGrassPaint)
            gx += grassStripW
        }
    }

    private fun drawPipes(canvas: Canvas) {
        val pipePaint = Paint()
        val capPaint = Paint()
        val borderPaint = Paint().apply {
            color = Color.rgb(0x11, 0x33, 0x11)
            strokeWidth = 4f * scaleX
            style = Paint.Style.STROKE
        }

        for (pipe in pipes) {
            val px = pipe.x * scaleX
            val pw = PipePair.PIPE_WIDTH * scaleX
            val capW = 200f * scaleX
            val capH = 60f * scaleY
            val capOverhang = (capW - pw) / 2f

            val topPipeBottom = pipe.topPipeBottom * scaleY
            val bottomPipeTop = pipe.bottomPipeTop * scaleY

            pipePaint.shader = android.graphics.LinearGradient(
                px, 0f, px + pw, 0f,
                intArrayOf(
                    Color.rgb(0x00, 0x64, 0x00),
                    Color.rgb(0x32, 0xCD, 0x32),
                    Color.rgb(0x00, 0x4d, 0x00)
                ),
                floatArrayOf(0f, 0.3f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )

            if (topPipeBottom > 0f) {
                canvas.drawRect(px, 0f, px + pw, topPipeBottom, pipePaint)
                canvas.drawRect(px, 0f, px + pw, topPipeBottom, borderPaint)

                capPaint.shader = android.graphics.LinearGradient(
                    px - capOverhang, 0f, px - capOverhang + capW, 0f,
                    intArrayOf(
                        Color.rgb(0x00, 0x64, 0x00),
                        Color.rgb(0x32, 0xCD, 0x32),
                        Color.rgb(0x00, 0x4d, 0x00)
                    ),
                    floatArrayOf(0f, 0.3f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                canvas.drawRect(px - capOverhang, topPipeBottom - capH, px - capOverhang + capW, topPipeBottom, capPaint)
                canvas.drawRect(px - capOverhang, topPipeBottom - capH, px - capOverhang + capW, topPipeBottom, borderPaint)
            }

            if (bottomPipeTop < GROUND_Y * scaleY) {
                pipePaint.shader = android.graphics.LinearGradient(
                    px, bottomPipeTop, px + pw, bottomPipeTop,
                    intArrayOf(
                        Color.rgb(0x00, 0x64, 0x00),
                        Color.rgb(0x32, 0xCD, 0x32),
                        Color.rgb(0x00, 0x4d, 0x00)
                    ),
                    floatArrayOf(0f, 0.3f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                canvas.drawRect(px, bottomPipeTop, px + pw, GROUND_Y * scaleY, pipePaint)
                canvas.drawRect(px, bottomPipeTop, px + pw, GROUND_Y * scaleY, borderPaint)

                capPaint.shader = android.graphics.LinearGradient(
                    px - capOverhang, bottomPipeTop, px - capOverhang + capW, bottomPipeTop,
                    intArrayOf(
                        Color.rgb(0x00, 0x64, 0x00),
                        Color.rgb(0x32, 0xCD, 0x32),
                        Color.rgb(0x00, 0x4d, 0x00)
                    ),
                    floatArrayOf(0f, 0.3f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                canvas.drawRect(px - capOverhang, bottomPipeTop, px - capOverhang + capW, bottomPipeTop + capH, capPaint)
                canvas.drawRect(px - capOverhang, bottomPipeTop, px - capOverhang + capW, bottomPipeTop + capH, borderPaint)
            }
        }
    }

    private fun drawChicken(canvas: Canvas) {
        val bmp = chickenBitmap ?: return
        val px = chicken.x * scaleX
        val py = chicken.y * scaleY
        val halfW = (chicken.visualWidth * scaleX) / 2f
        val halfH = (chicken.visualHeight * scaleY) / 2f

        canvas.save()
        canvas.rotate(chicken.rotation, px, py)
        canvas.drawBitmap(bmp, px - halfW, py - halfH, null)
        canvas.restore()
    }

    private fun setState(newState: GameState) {
        gameState = newState
        onStateChanged?.invoke(newState, score, highScore)
    }

    fun resetGame() {
        pendingReset = true
    }

    private fun doReset() {
        chicken.reset(CHICKEN_START_X, CHICKEN_START_Y)
        resetPipes()
        groundOffset = 0f
        bgOffset = 0f
        score = 0
        onScoreChanged?.invoke(0)
        setState(GameState.READY)
    }

    private fun resetPipes() {
        pipes.clear()
        pipes.addAll(createInitialPipes())
    }

    private fun calculateRotation(vy: Float): Float {
        if (vy <= -400f) return -25f
        if (vy >= 1000f) return 70f
        return -25f + (vy + 400f) / 1400f * 95f
    }
}
