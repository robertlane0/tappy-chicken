package com.tappy.chicken

import android.content.Context

class SaveManager(context: Context) {
    private val prefs = context.getSharedPreferences("tappy_chicken_prefs", Context.MODE_PRIVATE)

    fun getHighScore(): Int {
        return prefs.getInt("high_score", 0)
    }

    fun saveScore(currentScore: Int): Boolean {
        val currentHigh = getHighScore()
        if (currentScore > currentHigh) {
            prefs.edit().putInt("high_score", currentScore).apply()
            return true
        }
        return false
    }
}
