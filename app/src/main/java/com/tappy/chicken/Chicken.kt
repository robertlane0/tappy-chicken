package com.tappy.chicken

data class Chicken(
    var x: Float = 0f,
    var y: Float = 0f,
    var velocityY: Float = 0f,
    var rotation: Float = 0f,
    var animationFrame: Int = 0
) {
    val collisionWidth: Float = 55f
    val collisionHeight: Float = 55f
    val visualWidth: Float = 70f
    val visualHeight: Float = 70f

    fun getBoundingBox(): Rect {
        val halfW = collisionWidth / 2f
        val halfH = collisionHeight / 2f
        return Rect(x - halfW, y - halfH, x + halfW, y + halfH)
    }

    fun reset(startX: Float, startY: Float) {
        x = startX
        y = startY
        velocityY = 0f
        rotation = 0f
        animationFrame = 0
    }
}

data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun intersects(other: Rect): Boolean {
        return left < other.right && right > other.left
                && top < other.bottom && bottom > other.top
    }
}
