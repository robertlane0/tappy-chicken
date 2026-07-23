package com.tappy.chicken

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

object VectorCache {
    private val bitmapCache = HashMap<Int, Bitmap>()

    fun getBitmap(context: Context, resId: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        val cacheKey = resId * 31 + targetWidth * 17 + targetHeight
        bitmapCache[cacheKey]?.let { return it }

        val drawable = VectorDrawableCompat.create(context.resources, resId, context.theme)
            ?: throw android.content.res.Resources.NotFoundException("Vector asset not found: $resId")

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, targetWidth, targetHeight)
        drawable.draw(canvas)

        bitmapCache[cacheKey] = bitmap
        return bitmap
    }

    fun clear() {
        for (bitmap in bitmapCache.values) {
            bitmap.recycle()
        }
        bitmapCache.clear()
    }
}
