package com.example.nextclass.data.offline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.nextclass.domain.model.Course
import com.example.nextclass.domain.offline.OcrWord
import com.example.nextclass.domain.offline.OfflineOutcome
import com.example.nextclass.domain.offline.OfflinePage
import com.example.nextclass.domain.offline.TimetableParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/**
 * Reads a routine on the phone with no network. A PDF with a text layer is read straight from
 * that text, so nothing can be misread. Photos, screenshots and scanned PDFs go through the
 * on-device text recogniser. Either way the words and the page pixels go to [TimetableParser].
 */
class OfflineRoutineReader(context: Context) {

    private val appContext = context.applicationContext

    suspend fun read(uri: Uri): OfflineOutcome = withContext(Dispatchers.Default) {
        try {
            TimetableParser.read(pagesOf(uri))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OfflineOutcome.NotATimetable("The file could not be read on this phone.")
        } catch (e: OutOfMemoryError) {
            OfflineOutcome.NotATimetable("The file is too large to read on this phone.")
        }
    }

    /** Reads a course list (code and title per row) on this phone. Empty when nothing matched. */
    suspend fun readCourses(uri: Uri): List<Course> = withContext(Dispatchers.Default) {
        try {
            TimetableParser.readCourses(pagesOf(uri))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        } catch (e: OutOfMemoryError) {
            emptyList()
        }
    }

    private suspend fun pagesOf(uri: Uri): List<OfflinePage> {
        val mime = appContext.contentResolver.getType(uri).orEmpty()
        val isPdf = mime == "application/pdf" || uri.toString().lowercase().endsWith(".pdf")
        return if (isPdf) readPdf(uri) else listOf(readImage(uri))
    }

    // ---- PDF -----------------------------------------------------------------------------

    private suspend fun readPdf(uri: Uri): List<OfflinePage> {
        val textPages = extractPdfText(uri)
        val descriptor = appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Could not open the PDF.")
        val pages = ArrayList<OfflinePage>()
        descriptor.use {
            val renderer = PdfRenderer(descriptor)
            try {
                val count = min(renderer.pageCount, MAX_PDF_PAGES)
                for (index in 0 until count) {
                    val page = renderer.openPage(index)
                    val bitmap: Bitmap
                    try {
                        val longEdge = max(page.width, page.height).toFloat()
                        val scale = min(MAX_SCALE, TARGET_EDGE_PX / longEdge)
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    } finally {
                        page.close()
                    }
                    val text = textPages.getOrNull(index)
                    val words = if (text != null && text.words.size >= MIN_TEXT_WORDS && text.width > 0f && text.height > 0f) {
                        val sx = bitmap.width / text.width
                        val sy = bitmap.height / text.height
                        text.words.map {
                            OcrWord(it.text, it.left * sx, it.top * sy, it.right * sx, it.bottom * sy)
                        }
                    } else {
                        recognise(bitmap)
                    }
                    pages += OfflinePage(words, BitmapRaster(bitmap))
                    bitmap.recycle()
                }
            } finally {
                renderer.close()
            }
        }
        return pages
    }

    private class PageText(val words: List<OcrWord>, val width: Float, val height: Float)

    /** Words of every page from the PDF's own text layer, in points. Empty when the file has none. */
    private fun extractPdfText(uri: Uri): List<PageText> {
        return try {
            PDFBoxResourceLoader.init(appContext)
            val stream = appContext.contentResolver.openInputStream(uri) ?: return emptyList()
            stream.use { input ->
                val document = PDDocument.load(input)
                try {
                    val result = ArrayList<PageText>()
                    val pageCount = min(document.numberOfPages, MAX_PDF_PAGES)
                    for (index in 0 until pageCount) {
                        val stripper = WordStripper()
                        stripper.sortByPosition = true
                        stripper.startPage = index + 1
                        stripper.endPage = index + 1
                        stripper.getText(document)
                        val page = document.getPage(index)
                        val box = page.cropBox
                        val turned = page.rotation % 180 != 0
                        result += PageText(
                            words = stripper.words,
                            width = if (turned) box.height else box.width,
                            height = if (turned) box.width else box.height,
                        )
                    }
                    result
                } finally {
                    document.close()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private class WordStripper : PDFTextStripper() {
        val words = ArrayList<OcrWord>()

        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
            val current = ArrayList<TextPosition>()
            fun flush() {
                if (current.isEmpty()) return
                val value = current.joinToString("") { it.unicode.orEmpty() }.trim()
                if (value.isNotEmpty()) {
                    val left = current.minOf { it.xDirAdj }
                    val right = current.maxOf { it.xDirAdj + it.widthDirAdj }
                    val bottom = current.maxOf { it.yDirAdj }
                    val height = current.maxOf { it.heightDir }
                    words += OcrWord(value, left, bottom - height, right, bottom)
                }
                current.clear()
            }
            for (position in textPositions) {
                if (position.unicode.isNullOrBlank()) flush() else current += position
            }
            flush()
        }
    }

    // ---- Images ----------------------------------------------------------------------------

    private suspend fun readImage(uri: Uri): OfflinePage {
        val bitmap = decodeImage(uri)
        try {
            val words = recognise(bitmap)
            return OfflinePage(words, BitmapRaster(bitmap))
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeImage(uri: Uri): Bitmap {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Could not open the image.")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Not an image.")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > TARGET_EDGE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Not an image.")

        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees != 0f) {
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true,
            )
            if (rotated !== bitmap) bitmap.recycle()
            bitmap = rotated
        }
        if (bitmap.hasAlpha()) {
            val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply {
                drawColor(Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, null)
            }
            bitmap.recycle()
            bitmap = flat
        }
        return bitmap
    }

    /** On-device text recognition. Returns one entry per recognised word with its box in pixels. */
    private suspend fun recognise(bitmap: Bitmap): List<OcrWord> =
        suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result ->
                    val words = ArrayList<OcrWord>()
                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                val box = element.boundingBox ?: continue
                                if (element.text.isBlank()) continue
                                words += OcrWord(
                                    element.text,
                                    box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat(),
                                )
                            }
                        }
                    }
                    recognizer.close()
                    continuation.resume(words)
                }
                .addOnFailureListener { error ->
                    recognizer.close()
                    continuation.resumeWithException(error)
                }
            continuation.invokeOnCancellation { recognizer.close() }
        }

    private companion object {
        const val MAX_PDF_PAGES = 8
        const val TARGET_EDGE_PX = 3000f
        const val MAX_SCALE = 4f
        const val MIN_TEXT_WORDS = 40
    }
}
