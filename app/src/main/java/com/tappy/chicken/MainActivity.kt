package com.tappy.chicken

import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameSurfaceView: GameSurfaceView
    private lateinit var layoutReady: View
    private lateinit var txtTapPrompt: TextView
    private lateinit var txtHighScore: TextView
    private lateinit var textLiveScore: TextView
    private lateinit var panelGameOver: View
    private lateinit var txtScore: TextView
    private lateinit var txtBestScore: TextView
    private lateinit var imgMedal: ImageView
    private lateinit var btnRestart: View

    private var tapPulseAnimation: AlphaAnimation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameSurfaceView = findViewById(R.id.game_surface_view)
        layoutReady = findViewById(R.id.layout_ready)
        txtTapPrompt = findViewById(R.id.txt_tap_prompt)
        txtHighScore = findViewById(R.id.txt_high_score)
        textLiveScore = findViewById(R.id.text_live_score)
        panelGameOver = findViewById(R.id.panel_game_over)
        txtScore = findViewById(R.id.txt_score)
        txtBestScore = findViewById(R.id.txt_best_score)
        imgMedal = findViewById(R.id.img_medal)
        btnRestart = findViewById(R.id.btn_restart)

        layoutReady.visibility = View.VISIBLE
        panelGameOver.visibility = View.GONE
        textLiveScore.visibility = View.GONE

        txtHighScore.text = "Best: ${gameSurfaceView.highScore}"

        startTapPulse()

        gameSurfaceView.onStateChanged = { state, score, highScore ->
            runOnUiThread {
                when (state) {
                    GameState.READY -> {
                        layoutReady.visibility = View.VISIBLE
                        panelGameOver.visibility = View.GONE
                        textLiveScore.visibility = View.GONE
                        txtHighScore.text = "Best: $highScore"
                        startTapPulse()
                    }
                    GameState.PLAYING -> {
                        layoutReady.visibility = View.GONE
                        panelGameOver.visibility = View.GONE
                        textLiveScore.visibility = View.VISIBLE
                        stopTapPulse()
                    }
                    GameState.DEAD -> {}
                    GameState.GAME_OVER -> {
                        textLiveScore.visibility = View.GONE
                        panelGameOver.visibility = View.VISIBLE
                        txtScore.text = score.toString()
                        txtBestScore.text = highScore.toString()
                        updateMedal(score)
                    }
                }
            }
        }

        btnRestart.setOnClickListener {
            gameSurfaceView.resetGame()
        }
    }

    private fun updateMedal(score: Int) {
        val medalRes = when {
            score >= 40 -> R.drawable.ic_medal_gold
            score >= 20 -> R.drawable.ic_medal_silver
            score >= 10 -> R.drawable.ic_medal_bronze
            else -> null
        }
        if (medalRes != null) {
            imgMedal.setImageResource(medalRes)
            imgMedal.visibility = View.VISIBLE
        } else {
            imgMedal.visibility = View.GONE
        }
    }

    private fun startTapPulse() {
        if (tapPulseAnimation != null) return
        val anim = AlphaAnimation(0.4f, 1.0f).apply {
            duration = 600
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        txtTapPrompt.startAnimation(anim)
        tapPulseAnimation = anim
    }

    private fun stopTapPulse() {
        txtTapPrompt.clearAnimation()
        tapPulseAnimation = null
    }
}
