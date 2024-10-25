package com.example.testanime

import android.view.MotionEvent
import android.view.View
import kotlin.math.pow
import kotlin.math.sqrt

class ResolutionTouchListener : View.OnTouchListener {

    private var isMoving = false
    private var isResizing = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialWidth = 0f
    private var initialHeight = 0f
    private val resizeThreshold = 50f // Порог для определения начала изменения размера

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v ?: return false
        event ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                initialWidth = v.width.toFloat()
                initialHeight = v.height.toFloat()

                // Определяем, начато ли изменение размера
                if (sqrt((event.x - v.width).toDouble().pow(2.0) + (event.y - v.height).toDouble().pow(2.0)) <= resizeThreshold) {
                    isResizing = true
                } else {
                    isMoving = true
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isMoving) {
                    v.translationX = v.translationX + (event.x - initialX)
                    v.translationY = v.translationY + (event.y - initialY)
                } else if (isResizing) {
                    val newWidth = event.x
                    val newHeight = event.y
                    if (newWidth > 100 && newHeight > 100) { // Минимальный размер
                        v.layoutParams.width = newWidth.toInt()
                        v.layoutParams.height = newHeight.toInt()
                        v.requestLayout()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isMoving = false
                isResizing = false
                return true
            }
        }
        return false
    }
}
