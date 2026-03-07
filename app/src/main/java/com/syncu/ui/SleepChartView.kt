package com.syncu.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Professional Hypnogram Sleep Chart
 * Custom-drawn to match high-end health app designs
 * Updated with horizontal grid lines and 24h format
 */
class SleepChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colorAwake = 0xFFD1C4E9.toInt() // Light Purple for Awake/In Bed
    private val colorREM = 0xFF4FC3F7.toInt()
    private val colorLight = 0xFF1E88E5.toInt()
    private val colorDeep = 0xFF154391.toInt()
    private val colorText = 0xFF8E8E93.toInt()
    private val colorGrid = 0x1A8E8E93.toInt() // Subtle grey for horizontal lines

    private val awakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorAwake; style = Paint.Style.FILL }
    private val remPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorREM; style = Paint.Style.FILL }
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorLight; style = Paint.Style.FILL }
    private val deepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorDeep; style = Paint.Style.FILL }
    
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        color = colorText
        textSize = 40f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorGrid
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val transitionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var intervals: List<StageInterval> = emptyList()
    private var startTime: Long = 0
    private var endTime: Long = 0

    data class StageInterval(val start: Long, val end: Long, val type: Int)

    fun setData(dataString: String?, start: Long, end: Long) {
        if (dataString.isNullOrEmpty()) {
            this.intervals = emptyList()
            invalidate()
            return
        }

        try {
            this.intervals = dataString.split(";").filter { it.isNotEmpty() }.map {
                val parts = it.split(",")
                StageInterval(parts[0].toLong(), parts[1].toLong(), parts[2].toInt())
            }.sortedBy { it.start }
            this.startTime = start
            this.endTime = end
            invalidate()
        } catch (e: Exception) {
            this.intervals = emptyList()
            invalidate()
        }
    }

    private fun getCenterY(type: Int, stepHeight: Float): Float {
        return when (type) {
            1, 3, 7 -> stepHeight * 1.0f
            6 -> stepHeight * 2.0f
            4 -> stepHeight * 3.0f
            5 -> stepHeight * 4.0f
            else -> stepHeight * 3.0f
        }
    }

    private fun getColor(type: Int): Int {
        return when (type) {
            1, 3, 7 -> colorAwake
            6 -> colorREM
            4 -> colorLight
            5 -> colorDeep
            else -> colorLight
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (intervals.isEmpty() || startTime >= endTime) return

        val w = width.toFloat()
        val h = height.toFloat()
        val paddingBottom = 42f // Tighter padding for labels
        val chartHeight = h - paddingBottom
        val totalDuration = (endTime - startTime).toDouble()
        
        val stepHeight = chartHeight / 5
        val barThickness = 24f
        val cornerRadius = 8f

        // Draw subtle horizontal grid lines
        for (i in 1..4) {
            val y = stepHeight * i.toFloat()
            canvas.drawLine(0f, y, w, y, gridPaint)
        }
        // Draw baseline
        canvas.drawLine(0f, chartHeight, w, chartHeight, gridPaint)

        var prevX = -1f
        var prevY = -1f
        var prevColor = -1

        for (interval in intervals) {
            val startX = (((interval.start - startTime) / totalDuration) * w).toFloat()
            val endX = (((interval.end - startTime) / totalDuration) * w).toFloat()
            val centerY = getCenterY(interval.type, stepHeight)
            val currentColor = getColor(interval.type)

            if (prevX >= 0) {
                val path = Path()
                path.moveTo(startX, prevY)
                path.lineTo(startX, centerY)
                
                transitionPaint.shader = LinearGradient(
                    startX, prevY, startX, centerY,
                    prevColor, currentColor, Shader.TileMode.CLAMP
                )
                canvas.drawPath(path, transitionPaint)
            }

            val barRect = RectF(startX, centerY - barThickness/2, endX, centerY + barThickness/2)
            val paint = when (interval.type) {
                1, 3, 7 -> awakePaint
                6 -> remPaint
                4 -> lightPaint
                5 -> deepPaint
                else -> lightPaint
            }
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paint)

            prevX = endX
            prevY = centerY
            prevColor = currentColor
        }

        drawTimeLabels(canvas, w, h)
    }

    private fun drawTimeLabels(canvas: Canvas, w: Float, h: Float) {
        val zoneId = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        
        val startLabel = Instant.ofEpochMilli(startTime).atZone(zoneId).format(formatter)
        val endLabel = Instant.ofEpochMilli(endTime).atZone(zoneId).format(formatter)

        // Draw labels at the very bottom
        val labelY = h - 2f
        
        axisPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(startLabel, 0f, labelY, axisPaint)
        
        axisPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(endLabel, w, labelY, axisPaint)
    }
}
