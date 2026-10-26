package com.davidsalama.lumiform.data.mapper

import com.davidsalama.lumiform.data.remote.dto.ChoiceDto
import com.davidsalama.lumiform.data.remote.dto.ImageDto
import com.davidsalama.lumiform.data.remote.dto.NodeDto
import com.davidsalama.lumiform.data.remote.dto.PageDto
import com.davidsalama.lumiform.data.remote.dto.ResponseDto
import com.davidsalama.lumiform.data.remote.dto.ResponseSetDto
import com.davidsalama.lumiform.data.remote.dto.SectionDto
import com.davidsalama.lumiform.data.remote.dto.TextDto
import com.davidsalama.lumiform.data.remote.dto.UnknownNodeDto
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.domain.model.ImageQuestion
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.Response
import com.davidsalama.lumiform.domain.model.ResponseSet
import com.davidsalama.lumiform.domain.model.Section
import com.davidsalama.lumiform.domain.model.TextQuestion
import com.davidsalama.lumiform.domain.model.UnknownNode

fun NodeDto.toDomain(): FormNode = when (this) {
    is PageDto -> Page(id, title, items.map(NodeDto::toDomain))
    is SectionDto -> Section(id, title, items.map(NodeDto::toDomain))
    is TextDto -> TextQuestion(id, content)
    is ImageDto -> ImageQuestion(id, src, title)
    is ChoiceDto -> ChoiceQuestion(id, content, responseSet.toDomain())
    is UnknownNodeDto -> UnknownNode(id, type)
}

private fun ResponseSetDto.toDomain() = ResponseSet(
    id = id,
    multipleSelection = multipleSelection,
    responses = responses.map(ResponseDto::toDomain),
)

private fun ResponseDto.toDomain() = Response(id, label, score)
