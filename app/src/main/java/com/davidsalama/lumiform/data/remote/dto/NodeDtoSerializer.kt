package com.davidsalama.lumiform.data.remote.dto

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Picks the DTO class from the `type` field.
 *
 * Used instead of the default class discriminator because that one throws on a type it
 * doesn't know, which would make one new question type break the whole form.
 */
object NodeDtoSerializer : JsonContentPolymorphicSerializer<NodeDto>(NodeDto::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<NodeDto> =
        when (element.jsonObject["type"]?.jsonPrimitive?.contentOrNull) {
            "page" -> PageDto.serializer()
            "section" -> SectionDto.serializer()
            "text" -> TextDto.serializer()
            "image" -> ImageDto.serializer()
            "choice" -> ChoiceDto.serializer()
            else -> UnknownNodeDto.serializer()
        }
}
