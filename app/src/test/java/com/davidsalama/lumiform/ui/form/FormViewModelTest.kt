package com.davidsalama.lumiform.ui.form

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.davidsalama.lumiform.domain.DataError
import com.davidsalama.lumiform.domain.RefreshResult
import com.davidsalama.lumiform.testutil.FakeFormRepository
import com.davidsalama.lumiform.testutil.MainDispatcherRule
import com.davidsalama.lumiform.testutil.sampleFormTree
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val offline = RefreshResult.Failed(DataError.NoConnection)

    /** Skips intermediate emissions until the state matches. */
    private suspend fun ReceiveTurbine<FormUiState>.awaitState(predicate: (FormUiState) -> Boolean): FormUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    @Test
    fun `first launch shows loading, then the fetched form`() = runTest {
        val repository = FakeFormRepository().apply { holdRefresh() }
        val viewModel = FormViewModel(repository)

        viewModel.uiState.test {
            assertTrue(awaitState { it.isInitialLoading }.isInitialLoading)

            repository.releaseRefresh()

            val loaded = awaitState { !it.isRefreshing }
            assertEquals((1L..14L).toList(), loaded.rows.map { it.id })
            assertEquals(null, loaded.error)
        }
    }

    @Test
    fun `cached form shows immediately while the refresh is still running`() = runTest {
        val repository = FakeFormRepository(cachedForm = sampleFormTree).apply { holdRefresh() }
        val viewModel = FormViewModel(repository)

        viewModel.uiState.test {
            val state = awaitState { it.rows.isNotEmpty() }
            assertTrue(state.isRefreshing)
            assertFalse(state.isInitialLoading)
        }
        repository.releaseRefresh()
    }

    @Test
    fun `failed first launch without cache is a blocking error`() = runTest {
        val viewModel = FormViewModel(FakeFormRepository(refreshResult = offline))

        viewModel.uiState.test {
            val state = awaitState { !it.isRefreshing }
            assertTrue(state.isBlockingError)
            assertEquals(DataError.NoConnection, state.error)
        }
    }

    @Test
    fun `failed refresh with a cache keeps showing the cached form`() = runTest {
        val viewModel = FormViewModel(FakeFormRepository(cachedForm = sampleFormTree, refreshResult = offline))

        viewModel.uiState.test {
            val state = awaitState { !it.isRefreshing }
            assertEquals(14, state.rows.size)
            assertEquals(DataError.NoConnection, state.error)
            assertFalse(state.isBlockingError)
        }
    }

    @Test
    fun `retry after a failure loads the form and clears the error`() = runTest {
        val repository = FakeFormRepository(refreshResult = offline)
        val viewModel = FormViewModel(repository)

        viewModel.uiState.test {
            awaitState { it.isBlockingError }

            repository.refreshResult = RefreshResult.Success
            viewModel.refresh()

            val state = awaitState { !it.isRefreshing && it.rows.isNotEmpty() }
            assertEquals(null, state.error)
            assertEquals(2, repository.refreshCount)
        }
    }

    @Test
    fun `refresh while one is running does not start another`() = runTest {
        val repository = FakeFormRepository().apply { holdRefresh() }
        val viewModel = FormViewModel(repository)

        viewModel.refresh()
        viewModel.refresh()
        repository.releaseRefresh()

        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun `tapping a response updates that choice row`() = runTest {
        val viewModel = FormViewModel(FakeFormRepository())

        viewModel.uiState.test {
            val loaded = awaitState { it.rows.isNotEmpty() }
            val multi = loaded.rows.filterIsInstance<ChoiceRow>().single { it.id == 14L }

            viewModel.onResponseTapped(multi.question, 1021)
            viewModel.onResponseTapped(multi.question, 1023)

            val updated = awaitState { state ->
                state.rows.filterIsInstance<ChoiceRow>().single { it.id == 14L }.selectedIds.size == 2
            }
            val row = updated.rows.filterIsInstance<ChoiceRow>().single { it.id == 14L }
            assertEquals(setOf(1021L, 1023L), row.selectedIds)
        }
    }
}
