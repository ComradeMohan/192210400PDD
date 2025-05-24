package com.simats.univalut

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


class CircularGradeDistributionView (context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint()
    private val percentages = floatArrayOf(45f, 30f, 15f, 10f, 15f) // These percentages are for grades A, B, C, D, E
    private val colors = intArrayOf(
        Color.parseColor("#4CAF50"), // Green for A
        Color.parseColor("#2196F3"), // Blue for B
        Color.parseColor("#FFC107"), // Yellow for C
        Color.parseColor("#F44336"), // Red for D
        Color.parseColor("#FFEB3B")  // Yellow for E
    )

    init {
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Set up paint for circle
        val paint = Paint().apply {
            color = Color.BLUE // Circle color
            style = Paint.Style.FILL
        }

        // Get the width and height (assuming square)
        val radius = Math.min(width, height) / 2f

        // Draw the circle centered on the view
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
    }
}