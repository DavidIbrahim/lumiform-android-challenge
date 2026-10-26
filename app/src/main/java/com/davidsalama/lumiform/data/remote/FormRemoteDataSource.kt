package com.davidsalama.lumiform.data.remote

import com.davidsalama.lumiform.data.mapper.toDomain
import com.davidsalama.lumiform.data.remote.dto.NodeDto
import com.davidsalama.lumiform.domain.model.FormNode
import javax.inject.Inject

/** The only place the network is touched. Swapping Retrofit for Ktor would change one class. */
interface FormRemoteDataSource {
    suspend fun fetchForm(): List<FormNode>
}

class RetrofitFormRemoteDataSource @Inject constructor(
    private val api: FormApi,
) : FormRemoteDataSource {

    override suspend fun fetchForm(): List<FormNode> = api.getForm().map(NodeDto::toDomain)
}
