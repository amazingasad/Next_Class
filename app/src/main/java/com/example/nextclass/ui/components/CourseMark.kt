package com.example.nextclass.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small monochrome glyph that identifies a course without colour. The same course always gets
 * the same one of eight shapes, so a glance at the timeline is enough to tell courses apart.
 */
@Composable
fun CourseMark(
    key: String,
    modifier: Modifier = Modifier,
    markSize: Dp = 14.dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val pattern = key.fold(7) { acc, c -> acc * 31 + c.code }.mod(8)
    Canvas(modifier.size(markSize)) {
        val w = size.width
        val h = size.height
        val line = 1.2.dp.toPx()
        val inset = line / 2f
        val box = Size(w - line, h - line)
        val origin = Offset(inset, inset)
        drawRect(color, topLeft = origin, size = box, style = Stroke(line))
        when (pattern) {
            0 -> drawRect(color, topLeft = origin, size = box)
            1 -> Unit
            2 -> drawRect(color, topLeft = origin, size = Size(box.width / 2f, box.height))
            3 -> drawRect(
                color,
                topLeft = Offset(inset, h / 2f),
                size = Size(box.width, box.height / 2f),
            )
            4 -> {
                val path = Path().apply {
                    moveTo(inset, h - inset)
                    lineTo(w - inset, inset)
                    lineTo(w - inset, h - inset)
                    close()
                }
                drawPath(path, color)
            }
            5 -> {
                drawLine(color, Offset(inset, h / 2f), Offset(w - inset, h / 2f), line)
                drawLine(color, Offset(w / 2f, inset), Offset(w / 2f, h - inset), line)
            }
            6 -> {
                val bar = h / 7f
                for (i in 0..2) {
                    drawRect(
                        color,
                        topLeft = Offset(inset, bar * (1 + i * 2)),
                        size = Size(box.width, bar),
                    )
                }
            }
            else -> drawCircle(color, radius = w / 5f, center = Offset(w / 2f, h / 2f))
        }
    }
}
