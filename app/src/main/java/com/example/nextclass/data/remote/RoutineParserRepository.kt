package com.example.nextclass.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import com.example.nextclass.data.local.ApiKeyManager
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.Course
import com.example.nextclass.domain.model.CourseCodes
import com.example.nextclass.domain.model.SessionType
import com.example.nextclass.domain.model.TimeFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/** A failure with a message that is safe to show to the user. */
class ScanFailure(message: String) : Exception(message)

@OptIn(ExperimentalSerializationApi::class)
class RoutineParserRepository(
    context: Context,
    private val keyManager: ApiKeyManager,
) {
    private val appContext = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = true
    }

    /**
     * Uses the phone's DNS first. If that cannot find Google (Private DNS, ad blockers, a flaky
     * ISP resolver), it asks Google and Cloudflare over HTTPS instead.
     */
    private val resilientDns: Dns by lazy {
        val bootstrap = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
        val backups = listOf(
            DnsOverHttps.Builder()
                .client(bootstrap)
                .url("https://dns.google/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8)),
                    InetAddress.getByAddress(byteArrayOf(8, 8, 4, 4)),
                )
                .build(),
            DnsOverHttps.Builder()
                .client(bootstrap)
                .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)),
                    InetAddress.getByAddress(byteArrayOf(1, 0, 0, 1)),
                )
                .build(),
        )
        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                try {
                    return Dns.SYSTEM.lookup(hostname)
                } catch (original: UnknownHostException) {
                    for (backup in backups) {
                        try {
                            return backup.lookup(hostname)
                        } catch (ignored: Exception) {
                            // try the next resolver
                        }
                    }
                    throw original
                }
            }
        }
    }

    private val service: GeminiApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .dns(resilientDns)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun verifyKey(apiKey: String): Result<Unit> {
        val key = apiKey.trim()
        if (key.isEmpty()) return Result.failure(ScanFailure("Enter your API key first."))
        return try {
            service.listModels(key, 1000)
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = when (e.code()) {
                400, 401, 403 -> "Google rejected this key. Copy it again from AI Studio."
                429 -> "Too many requests right now. Try again in a minute."
                else -> "Google returned an error (HTTP ${e.code()})."
            }
            Result.failure(ScanFailure(message))
        } catch (e: IOException) {
            Result.failure(ScanFailure("Could not reach Google. Check your connection."))
        } catch (e: SerializationException) {
            Result.failure(ScanFailure("Unexpected reply from Google."))
        }
    }

    suspend fun scanRoutine(uri: Uri, sectionFilter: String): Result<List<ClassSession>> =
        try {
            Result.success(doScan(uri, sectionFilter))
        } catch (e: ScanFailure) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(ScanFailure("Could not read that file."))
        } catch (e: SecurityException) {
            Result.failure(ScanFailure("This PDF is protected or cannot be opened."))
        } catch (e: OutOfMemoryError) {
            Result.failure(ScanFailure("That file is too large to process on this phone."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ScanFailure("Unexpected error: ${e.javaClass.simpleName}: ${e.message.orEmpty().take(160)}"))
        }

    /** Reads a photo or PDF of a course list (code and title per row). */
    suspend fun scanCourses(uri: Uri): Result<List<Course>> =
        try {
            Result.success(doScanCourses(uri))
        } catch (e: ScanFailure) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(ScanFailure("Could not read that file."))
        } catch (e: SecurityException) {
            Result.failure(ScanFailure("This PDF is protected or cannot be opened."))
        } catch (e: OutOfMemoryError) {
            Result.failure(ScanFailure("That file is too large to process on this phone."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ScanFailure("Unexpected error: ${e.javaClass.simpleName}: ${e.message.orEmpty().take(160)}"))
        }

    private suspend fun doScanCourses(uri: Uri): List<Course> {
        val apiKey = withContext(Dispatchers.IO) { keyManager.getKey() }
        if (apiKey.isBlank()) throw ScanFailure("Add your Gemini API key in Settings first.")

        val pages = withContext(Dispatchers.IO) { loadPages(uri) }
        if (pages.isEmpty()) throw ScanFailure("No readable pages found in that file.")

        val userParts = buildList {
            add(PartDto(text = "Extract every course code and course title from this list."))
            pages.forEach { add(PartDto(inlineData = InlineDataDto(mimeType = "image/jpeg", data = it))) }
        }
        val request = GenerateContentRequest(
            systemInstruction = ContentDto(parts = listOf(PartDto(text = COURSE_INSTRUCTION))),
            contents = listOf(ContentDto(role = "user", parts = userParts)),
            generationConfig = GenerationConfigDto(
                responseMimeType = "application/json",
                responseSchema = COURSE_SCHEMA,
                temperature = 0.0,
            ),
        )
        val payloadChars = userParts.sumOf { (it.inlineData?.data?.length ?: 0) + (it.text?.length ?: 0) }
        if (payloadChars > MAX_PAYLOAD_CHARS) {
            throw ScanFailure("That file is too large to send. Use a smaller image or fewer pages.")
        }

        val models = candidateModels(apiKey)
        val text = try {
            generateWithFallback(apiKey, models, request)
        } catch (e: ScanFailure) {
            throw ScanFailure(e.message.orEmpty() + "\n\n" + diagnose(apiKey))
        }
        return parseCourses(text)
    }

    private fun parseCourses(raw: String): List<Course> {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start < 0 || end <= start) throw ScanFailure("Gemini did not return a course list.")
        val rows = try {
            json.decodeFromString(ListSerializer(ParsedCourseDto.serializer()), raw.substring(start, end + 1))
        } catch (e: SerializationException) {
            throw ScanFailure("Gemini sent a course list the app could not read.")
        }
        return rows
            .filter { CourseCodes.key(it.courseCode).isNotEmpty() && it.courseTitle.isNotBlank() }
            .map { Course(code = CourseCodes.pretty(it.courseCode), title = it.courseTitle.trim()) }
            .distinctBy { it.key }
    }

    private suspend fun doScan(uri: Uri, sectionFilter: String): List<ClassSession> {
        val apiKey = withContext(Dispatchers.IO) { keyManager.getKey() }
        if (apiKey.isBlank()) throw ScanFailure("Add your Gemini API key in Settings first.")

        val pages = withContext(Dispatchers.IO) { loadPages(uri) }
        if (pages.isEmpty()) throw ScanFailure("No readable pages found in that file.")

        val userParts = buildList {
            add(PartDto(text = buildUserPrompt(sectionFilter)))
            pages.forEach { add(PartDto(inlineData = InlineDataDto(mimeType = "image/jpeg", data = it))) }
        }
        val request = GenerateContentRequest(
            systemInstruction = ContentDto(parts = listOf(PartDto(text = SYSTEM_INSTRUCTION))),
            contents = listOf(ContentDto(role = "user", parts = userParts)),
            generationConfig = GenerationConfigDto(
                responseMimeType = "application/json",
                responseSchema = ROUTINE_SCHEMA,
                temperature = 0.0,
            ),
        )

        val payloadBytes = userParts.sumOf { (it.inlineData?.data?.length ?: 0) + (it.text?.length ?: 0) }
        if (payloadBytes > MAX_PAYLOAD_CHARS) {
            throw ScanFailure("That file is too large to send. Use a smaller image or fewer pages.")
        }

        val models = candidateModels(apiKey)
        val text = try {
            generateWithFallback(apiKey, models, request)
        } catch (e: ScanFailure) {
            throw ScanFailure(e.message.orEmpty() + "\n\n" + diagnose(apiKey))
        }
        return parseSessions(text)
    }

    /** Short plain-language check of the network and the API key, shown under a failure. */
    private suspend fun diagnose(apiKey: String): String = withContext(Dispatchers.IO) {
        val systemDnsOk = try {
            InetAddress.getByName(HOST)
            true
        } catch (e: Exception) {
            false
        }

        var keyLine: String
        var reachable = false
        try {
            val result = withTimeoutOrNull(20_000) { service.listModels(apiKey, 1) }
            if (result == null) {
                keyLine = "API key: could not be checked in time. The connection is very slow."
            } else {
                reachable = true
                keyLine = "API key: working. Google accepted it."
            }
        } catch (e: HttpException) {
            reachable = true
            keyLine = when (e.code()) {
                400, 401, 403 -> "API key: not working. Google rejected it. Create a new key in AI Studio and paste it in Settings."
                429 -> "API key: working, but you have hit the free quota. Wait a minute or try tomorrow."
                else -> "API key: Google answered with error ${e.code()}."
            }
        } catch (e: IOException) {
            keyLine = "API key: could not be checked because the phone could not reach Google."
        } catch (e: Exception) {
            keyLine = "API key: could not be checked (${e.javaClass.simpleName})."
        }

        val networkLine = when {
            systemDnsOk && reachable -> "Network: working. Google is reachable."
            !systemDnsOk && reachable ->
                "Network: your phone's DNS cannot find Google, so the app used a backup DNS. " +
                    "Set Private DNS to Automatic in system settings and turn off any DNS filter or VPN."
            !systemDnsOk ->
                "Network: your phone cannot find Google's servers (DNS). Switch between Wi-Fi and mobile data, " +
                    "set Private DNS to Automatic, and turn off any VPN or ad blocker."
            else -> "Network: Google could not be reached. Check your connection and try again."
        }
        "Check result\n$networkLine\n$keyLine"
    }

    /**
     * Asks Google which models this key can call and returns the best Flash models first, so the
     * app keeps working when Google retires a model name. Falls back to a fixed list if the
     * lookup fails.
     */
    private suspend fun candidateModels(apiKey: String): List<String> {
        val listed = try {
            service.listModels(apiKey, 1000).models
        } catch (e: Exception) {
            emptyList()
        }
        val blocked = listOf("lite", "image", "tts", "live", "audio", "embedding", "thinking", "robotics", "computer", "customtools", "exp")
        val usable = listed
            .filter { "generateContent" in it.supportedGenerationMethods }
            .mapNotNull { it.name?.removePrefix("models/") }
            .filter { name -> name.startsWith("gemini") && "flash" in name && blocked.none { it in name } }
            .distinct()
        val versionOf = { name: String ->
            Regex("gemini-(\\d+(?:\\.\\d+)?)").find(name)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        }
        val ranked = usable.sortedWith(
            compareBy<String>({ if (it == "gemini-flash-latest") 0 else 1 }, { "preview" in it }, { -versionOf(it) }),
        ).take(MAX_MODEL_ATTEMPTS)
        return ranked.ifEmpty { FALLBACK_MODELS }
    }

    private suspend fun generateWithFallback(
        apiKey: String,
        models: List<String>,
        request: GenerateContentRequest,
    ): String {
        var lastError: ScanFailure? = null
        for (model in models) {
            try {
                // Gemini 2.5 spends most of its time thinking; the extraction needs none.
                val modelRequest = if (model.startsWith("gemini-2.5")) {
                    request.copy(
                        generationConfig = request.generationConfig?.copy(
                            thinkingConfig = ThinkingConfigDto(thinkingBudget = 0),
                        ),
                    )
                } else {
                    request
                }
                val response = service.generateContent(model, apiKey, modelRequest)
                response.promptFeedback?.blockReason?.let {
                    throw ScanFailure("Gemini declined to read this file ($it).")
                }
                val text = response.candidates.firstOrNull()?.content?.parts
                    ?.mapNotNull { it.text }
                    ?.joinToString("")
                if (!text.isNullOrBlank()) return text
                lastError = ScanFailure("Gemini returned an empty answer.")
            } catch (e: HttpException) {
                val body = try {
                    e.response()?.errorBody()?.string().orEmpty()
                } catch (io: IOException) {
                    ""
                }
                val keyRejected = e.code() == 401 || e.code() == 403 ||
                    (e.code() == 400 && body.contains("API key", ignoreCase = true))
                if (keyRejected) {
                    throw ScanFailure("Your Gemini API key was rejected. Check it in Settings.")
                }
                if (e.code() == 404) {
                    lastError = ScanFailure("Model $model is not available for this key.")
                    continue
                }
                val detail = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1).orEmpty()
                lastError = ScanFailure(
                    "Gemini error (HTTP ${e.code()}) using $model." +
                        if (detail.isNotBlank()) " ${detail.take(180)}" else "",
                )
            } catch (e: IOException) {
                lastError = ScanFailure(
                    if (e is java.io.InterruptedIOException) {
                        "Gemini took too long to answer. Try a smaller image or try again."
                    } else {
                        "Network problem while contacting Gemini (${e.javaClass.simpleName}: ${e.message.orEmpty().take(110)})."
                    },
                )
            } catch (e: SerializationException) {
                lastError = ScanFailure("Gemini sent a reply the app could not read.")
            }
        }
        throw lastError ?: ScanFailure("Gemini did not answer.")
    }

    private fun parseSessions(raw: String): List<ClassSession> {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val parsed = try {
            json.decodeFromString(ListSerializer(ParsedSessionDto.serializer()), cleaned)
        } catch (e: SerializationException) {
            throw ScanFailure("Gemini's answer was not a valid class list. Try again.")
        } catch (e: IllegalArgumentException) {
            throw ScanFailure("Gemini's answer was not a valid class list. Try again.")
        }

        return parsed.mapNotNull { dto ->
            val start = TimeFormat.normalize(dto.startTime) ?: return@mapNotNull null
            val end = TimeFormat.normalize(dto.endTime) ?: return@mapNotNull null
            if (dto.dayOfWeek !in 1..7) return@mapNotNull null
            if (TimeFormat.toMinutes(end) <= TimeFormat.toMinutes(start)) return@mapNotNull null
            if (dto.courseCode.isBlank()) return@mapNotNull null
            ClassSession(
                dayOfWeek = dto.dayOfWeek,
                courseCode = dto.courseCode.trim().uppercase(),
                courseTitle = dto.courseTitle.trim(),
                room = dto.room.trim(),
                startTime = start,
                endTime = end,
                type = SessionType.fromRaw(dto.type),
            )
        }
            .distinctBy { listOf(it.dayOfWeek, it.courseCode, it.startTime, it.endTime, it.room) }
            .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    }

    private fun buildUserPrompt(sectionFilter: String): String {
        val filter = sectionFilter.trim()
        return if (filter.isEmpty()) {
            "Extract every class session from this routine."
        } else {
            "Extract the class sessions for this section/group only: \"$filter\". " +
                "Skip rows that belong to other sections or groups."
        }
    }

    // ---- File to JPEG pages -------------------------------------------------------------

    private fun loadPages(uri: Uri): List<String> {
        val mime = appContext.contentResolver.getType(uri).orEmpty()
        val isPdf = mime == "application/pdf" || uri.toString().lowercase().endsWith(".pdf")
        return if (isPdf) renderPdf(uri) else listOf(encodeImage(uri))
    }

    private fun renderPdf(uri: Uri): List<String> {
        val descriptor = appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: throw ScanFailure("Could not open the PDF.")
        val pages = ArrayList<String>()
        try {
            val renderer = PdfRenderer(descriptor)
            try {
                val count = minOf(renderer.pageCount, MAX_PDF_PAGES)
                for (index in 0 until count) {
                    val page = renderer.openPage(index)
                    try {
                        val baseScale = RENDER_DPI / 72f
                        val longEdge = max(page.width, page.height) * baseScale
                        val scale = if (longEdge > MAX_EDGE_PX) baseScale * MAX_EDGE_PX / longEdge else baseScale
                        val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                        val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages += toBase64Jpeg(bitmap)
                        bitmap.recycle()
                    } finally {
                        page.close()
                    }
                }
            } finally {
                renderer.close()
            }
        } finally {
            descriptor.close()
        }
        return pages
    }

    private fun encodeImage(uri: Uri): String {
        val resolver = appContext.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: throw ScanFailure("Could not open the image.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw ScanFailure("That file is not a readable image.")
        }

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE_PX) sample *= 2
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: throw ScanFailure("That file is not a readable image.")

        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }

        var bitmap = decoded
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees != 0f) {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
        val encoded = toBase64Jpeg(bitmap)
        bitmap.recycle()
        return encoded
    }

    private fun toBase64Jpeg(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private companion object {
        const val HOST = "generativelanguage.googleapis.com"
        const val BASE_URL = "https://generativelanguage.googleapis.com/"

        /** Used only when the model lookup fails. */
        val FALLBACK_MODELS = listOf("gemini-flash-latest", "gemini-2.5-flash")
        const val MAX_MODEL_ATTEMPTS = 4

        const val RENDER_DPI = 200f
        const val MAX_EDGE_PX = 1900f
        const val MAX_PDF_PAGES = 3
        const val JPEG_QUALITY = 72
        const val MAX_PAYLOAD_CHARS = 14_000_000

        const val SYSTEM_INSTRUCTION =
            "You read university class routine timetables from images and return structured JSON. " +
                "Return only a JSON array of class sessions. Rules: " +
                "dayOfWeek is an integer from 1 (Monday) to 7 (Sunday). " +
                "startTime and endTime are 24-hour HH:mm strings. " +
                "A cell merged across several period columns, such as a multi-period lab or sessional, " +
                "is ONE session that starts at the start time of its first column and ends at the end " +
                "time of its last column. " +
                "Ignore tea, lunch, prayer and other break rows or columns, and ignore empty cells. " +
                "Normalize rooms to a short form such as \"Room 304\" and keep lab names such as " +
                "\"Physics Lab\". " +
                "courseCode is uppercase with a hyphen, for example CSE-2101. " +
                "type is LAB for lab, sessional or practical sessions, TUTORIAL for tutorials, and " +
                "LECTURE otherwise. " +
                "When a section or group is requested, return only sessions for that section or group " +
                "plus sessions shared by all sections. " +
                "Never invent sessions. If nothing is readable, return []."

        const val COURSE_INSTRUCTION =
            "You read lists of university courses from images and return structured JSON. " +
                "Return only a JSON array. Each item has courseCode and courseTitle. " +
                "courseCode is uppercase letters, a hyphen, then the number, for example CSE-2101 " +
                "(write CSE 2101 or CSE2101 as CSE-2101). " +
                "courseTitle is the full course name exactly as printed, without credit hours, " +
                "teacher names or prerequisites. " +
                "Skip header rows, totals and empty rows. Never invent courses. If nothing is readable, return []."

        val COURSE_SCHEMA = SchemaDto(
            type = "ARRAY",
            items = SchemaDto(
                type = "OBJECT",
                properties = mapOf(
                    "courseCode" to SchemaDto(type = "STRING"),
                    "courseTitle" to SchemaDto(type = "STRING"),
                ),
                required = listOf("courseCode", "courseTitle"),
            ),
        )

        val ROUTINE_SCHEMA = SchemaDto(
            type = "ARRAY",
            items = SchemaDto(
                type = "OBJECT",
                properties = mapOf(
                    "dayOfWeek" to SchemaDto(type = "INTEGER", description = "1 = Monday to 7 = Sunday"),
                    "courseCode" to SchemaDto(type = "STRING"),
                    "courseTitle" to SchemaDto(type = "STRING"),
                    "room" to SchemaDto(type = "STRING"),
                    "startTime" to SchemaDto(type = "STRING", description = "24-hour HH:mm"),
                    "endTime" to SchemaDto(type = "STRING", description = "24-hour HH:mm"),
                    "type" to SchemaDto(type = "STRING", enumValues = listOf("LECTURE", "LAB", "TUTORIAL")),
                ),
                required = listOf(
                    "dayOfWeek", "courseCode", "courseTitle", "room", "startTime", "endTime", "type",
                ),
            ),
        )
    }
}
