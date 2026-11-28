package com.example.nextclass.data.offline

import android.graphics.Bitmap
import com.example.nextclass.domain.offline.Raster

/** Grey-scale copy of a bitmap. The ink threshold follows the paper brightness, so dim photos still work. */
class BitmapRaster(bitmap: Bitmap) : Raster {

    override val width: Int = bitmap.width
    override val height: Int = bitmap.height
    private val grey = ByteArray(width * height)
    override val threshold: Int

    init {
        val row = IntArray(width)
        val histogram = IntArray(256)
        for (y in 0 until height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            for (x in 0 until width) {
                val p = row[x]
                val r = p shr 16 and 0xFF
                val g = p shr 8 and 0xFF
                val b = p and 0xFF
                val luma = (r * 299 + g * 587 + b * 114) / 1000
                grey[y * width + x] = luma.toByte()
                histogram[luma]++
            }
        }
        // Paper level: the median pixel, since most of a page is background.
        var seen = 0
        var paper = 255
        val half = width.toLong() * height / 2
        for (level in 0..255) {
            seen += histogram[level]
            if (seen >= half) {
                paper = level
                break
            }
        }
        threshold = (paper * 0.68f).toInt().coerceIn(90, 190)
    }

    override fun luma(x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= width || y >= height) return 255
        return grey[y * width + x].toInt() and 0xFF
    }
}
