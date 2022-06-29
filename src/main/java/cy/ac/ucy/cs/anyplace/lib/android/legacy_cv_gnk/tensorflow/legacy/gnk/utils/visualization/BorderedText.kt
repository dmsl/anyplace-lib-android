package cy.ac.ucy.cs.anyplace.lib.android.legacy_cv_gnk.tensorflow.legacy.gnk.utils.visualization

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG

@Deprecated("")
/**
 * A class that encapsulates the tedious bits of rendering legible, bordered text onto a [Canvas].
 */
class BorderedText(
    interiorColor: Int = Color.WHITE,
    exteriorColor: Int = Color.BLACK,
    textSize: Float = 16.0f
) {
    private val interiorPaint: Paint = Paint().also {
        it.textSize = textSize
        it.color = interiorColor
        it.style = Paint.Style.FILL
        it.isAntiAlias = false
        it.alpha = 255
    }

    private val exteriorPaint: Paint = Paint().also {
        it.textSize = textSize
        it.color = exteriorColor
        it.style = Paint.Style.FILL_AND_STROKE
        it.strokeWidth = textSize / 8
        it.isAntiAlias = false
        it.alpha = 255
    }


  @Deprecated("LEGACY GNK")
    fun drawText(
        canvas: Canvas,
        posX: Float,
        posY: Float,
        text: String,
        bgPaint: Paint
    ) {
        val width = exteriorPaint.measureText(text)
        val textSize = exteriorPaint.textSize
        val paint = Paint(bgPaint)

        paint.style = Paint.Style.FILL
        paint.alpha = 160

        canvas.drawRect(posX, posY + textSize.toInt(), posX + width.toInt(), posY, paint)
        canvas.drawText(text, posX, posY + textSize, interiorPaint)
    }
}