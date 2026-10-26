package com.davidsalama.lumiform.data.mapper

import com.davidsalama.lumiform.data.remote.FormApi
import com.davidsalama.lumiform.data.remote.dto.NodeDto
import com.davidsalama.lumiform.data.remote.dto.UnknownNodeDto
import com.davidsalama.lumiform.domain.model.UnknownNode
import com.davidsalama.lumiform.testutil.Fixtures
import com.davidsalama.lumiform.testutil.sampleFormTree
import org.junit.Assert.assertEquals
import org.junit.Test

class DtoMapperTest {

    @Test
    fun `real response maps to the expected domain tree`() {
        val dtos: List<NodeDto> = FormApi.json.decodeFromString(Fixtures.sampleForm)

        assertEquals(sampleFormTree, dtos.map(NodeDto::toDomain))
    }

    @Test
    fun `unknown node keeps its raw type`() {
        assertEquals(UnknownNode(5, "signature"), UnknownNodeDto(5, "signature").toDomain())
    }
}
