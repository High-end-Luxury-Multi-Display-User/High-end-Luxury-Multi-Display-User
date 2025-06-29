package android.vendor.carinfo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.math.cos
import kotlin.math.sin

class SeatTiltView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var backrestAngle = 45f  // Angle in degrees

    private val seatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, 200f,
            Color.parseColor("#B388EB"), Color.parseColor("#9F5DE2"), Shader.TileMode.CLAMP)
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.MAGENTA
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    fun setBackrestAngle(angle: Float) {
        backrestAngle = angle.coerceIn(0f, 120f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height * 0.75f

        // --- Draw Seat Base ---
        val basePath = android.graphics.Path().apply {
            moveTo(centerX - 200f, centerY)
            quadTo(centerX, centerY + 60f, centerX + 200f, centerY)
            lineTo(centerX + 180f, centerY + 60f)
            quadTo(centerX, centerY + 100f, centerX - 180f, centerY + 60f)
            close()
        }
        canvas.drawPath(basePath, seatPaint)
        canvas.drawPath(basePath, outlinePaint)

        // --- Draw Backrest (Rotated Path) ---
        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(-backrestAngle, 0f, 0f)

        val backrestPath = android.graphics.Path().apply {
            moveTo(-40f, -10f)
            quadTo(0f, -160f, 40f, -10f)
            lineTo(30f, -20f)
            quadTo(0f, -140f, -30f, -20f)
            close()
        }
        canvas.drawPath(backrestPath, seatPaint)
        canvas.drawPath(backrestPath, outlinePaint)

        canvas.restore()

        // --- Draw Angle Dial ---
        val dialRadius = 150f
        val dialRect = RectF(centerX - dialRadius, centerY - dialRadius, centerX + dialRadius, centerY + dialRadius)
        canvas.drawArc(dialRect, -120f, 120f, false, indicatorPaint)

        // --- Draw Indicator Line ---
        val angleRad = Math.toRadians((backrestAngle - 90).toDouble())
        val lineX = centerX + cos(angleRad) * dialRadius
        val lineY = centerY + sin(angleRad) * dialRadius
        canvas.drawLine(centerX, centerY, lineX.toFloat(), lineY.toFloat(), indicatorPaint)

        // --- Draw Angle Text ---
        canvas.drawText("${backrestAngle.toInt()}°", centerX, centerY - dialRadius - 20f, textPaint)
    }
}