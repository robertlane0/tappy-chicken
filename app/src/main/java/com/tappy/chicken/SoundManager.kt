package com.tappy.chicken

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private var flapId: Int = 0
    private var pointId: Int = 0
    private var hitId: Int = 0
    private var fallId: Int = 0

    private val loaded: Boolean

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        val res = context.resources
        val pkg = context.packageName

        flapId = res.getIdentifier("sound_flap", "raw", pkg)
        pointId = res.getIdentifier("sound_point", "raw", pkg)
        hitId = res.getIdentifier("sound_hit", "raw", pkg)
        fallId = res.getIdentifier("sound_fall", "raw", pkg)

        loaded = flapId != 0 || pointId != 0 || hitId != 0 || fallId != 0

        if (loaded) {
            if (flapId != 0) soundPool.load(context, flapId, 1)
            if (pointId != 0) soundPool.load(context, pointId, 1)
            if (hitId != 0) soundPool.load(context, hitId, 1)
            if (fallId != 0) soundPool.load(context, fallId, 1)
        }
    }

    fun playFlap() {
        if (flapId != 0) soundPool.play(flapId, 1f, 1f, 1, 0, 1f)
    }

    fun playPoint() {
        if (pointId != 0) soundPool.play(pointId, 1f, 1f, 1, 0, 1f)
    }

    fun playHit() {
        if (hitId != 0) soundPool.play(hitId, 1f, 1f, 2, 0, 1f)
    }

    fun playFall() {
        if (fallId != 0) soundPool.play(fallId, 1f, 1f, 2, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
