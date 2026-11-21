package com.example.nextclass.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateContentRequest(
    val systemInstruction: ContentDto? = null,
    val contents: List<ContentDto>,
    val generationConfig: GenerationConfigDto? = null,
)

@Serializable
data class ContentDto(
    val role: String? = null,
    val parts: List<PartDto> = emptyList(),
)

@Serializable
data class PartDto(
    val text: String? = null,
    val inlineData: InlineDataDto? = null,
)

@Serializable
data class InlineDataDto(
    val mimeType: String,
    val data: String,
)

@Serializable
data class GenerationConfigDto(
    val responseMimeType: String? = null,
    val responseSchema: SchemaDto? = null,
    val temperature: Double? = null,
)

@Serializable
data class SchemaDto(
    val type: String,
    val description: String? = null,
    val items: SchemaDto? = null,
    val properties: Map<String, SchemaDto>? = null,
    val required: List<String>? = null,
    @SerialName("enum") val enumValues: List<String>? = null,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<CandidateDto> = emptyList(),
    val promptFeedback: PromptFeedbackDto? = null,
)

@Serializable
data class CandidateDto(
    val content: ContentDto? = null,
    val finishReason: String? = null,
)

@Serializable
data class PromptFeedbackDto(
    val blockReason: String? = null,
)

@Serializable
data class ModelListResponse(
    val models: List<ModelDto> = emptyList(),
)

@Serializable
data class ModelDto(
    val name: String? = null,
)

/** One class object exactly as Gemini returns it under the strict response schema. */
@Serializable
data class ParsedSessionDto(
    val dayOfWeek: Int,
    val courseCode: String,
    val courseTitle: String = "",
    val room: String = "",
    val startTime: String,
    val endTime: String,
    val type: String = "LECTURE",
)
