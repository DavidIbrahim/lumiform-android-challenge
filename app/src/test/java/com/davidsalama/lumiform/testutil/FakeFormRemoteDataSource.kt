package com.davidsalama.lumiform.testutil

import com.davidsalama.lumiform.data.remote.FormRemoteDataSource
import com.davidsalama.lumiform.domain.model.FormNode

class FakeFormRemoteDataSource(
    var response: () -> List<FormNode> = { sampleFormTree },
) : FormRemoteDataSource {

    var fetchCount = 0
        private set

    fun respondWith(form: List<FormNode>) {
        response = { form }
    }

    fun failWith(error: Throwable) {
        response = { throw error }
    }

    override suspend fun fetchForm(): List<FormNode> {
        fetchCount++
        return response()
    }
}
