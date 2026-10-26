package com.davidsalama.lumiform.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of one form item, exactly as the endpoint sends it.
 *
 * Which subclass is used is decided by the `type` field in [NodeDtoSerializer].
 * The `type` field itself isn't declared on the known subclasses: the serializer has already
 * read it, and `ignoreUnknownKeys` skips it.
 */
@Serializable(with = NodeDtoSerializer::class)
sealed interface NodeDto {
    val id: Long
}

@Serializable
data class PageDto(
    override val id: Long,
    val title: String,
    val items: List<NodeDto> = emptyList(),
) : NodeDto

@Serializable
data class SectionDto(
    override val id: Long,
    val title: String,
    val items: List<NodeDto> = emptyList(),
) : NodeDto

@Serializable
data class TextDto(
    override val id: Long,
    val content: String,
) : NodeDto

@Serializable
data class ImageDto(
    override val id: Long,
    val src: String,
    val title: String,
) : NodeDto

@Serializable
data class ChoiceDto(
    override val id: Long,
    val content: String,
    @SerialName("response_set") val responseSet: ResponseSetDto,
) : NodeDto

@Serializable
data class UnknownNodeDto(
    override val id: Long,
    val type: String = "",
) : NodeDto

@Serializable
data class ResponseSetDto(
    val id: Long,
    @SerialName("multiple_selection") val multipleSelection: Boolean = false,
    val responses: List<ResponseDto> = emptyList(),
)

@Serializable
data class ResponseDto(
    val id: Long,
    val label: String,
    // The real data has a number, null, or no field at all.
    val score: Int? = null,
)
