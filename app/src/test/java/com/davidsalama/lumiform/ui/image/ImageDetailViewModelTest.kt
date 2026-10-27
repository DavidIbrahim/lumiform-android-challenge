package com.davidsalama.lumiform.ui.image

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.davidsalama.lumiform.domain.model.ImageQuestion
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.TextQuestion
import com.davidsalama.lumiform.testutil.FakeFormRepository
import com.davidsalama.lumiform.testutil.MainDispatcherRule
import com.davidsalama.lumiform.testutil.sampleFormTree
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric because navigation's toRoute() decodes the arguments through an Android Bundle.
@RunWith(AndroidJUnit4::class)
class ImageDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeFormRepository(cachedForm = sampleFormTree)

    /** The same key the type-safe route ImageDetailDestination(imageId) is stored under. */
    private fun viewModelFor(imageId: Long) =
        ImageDetailViewModel(SavedStateHandle(mapOf("imageId" to imageId)), repository)

    @Test
    fun `shows the cached image for the id in the route`() = runTest {
        viewModelFor(9).uiState.test {
            // The fake emits immediately, so the initial Loading is already replaced.
            assertEquals(
                ImageDetailUiState.Loaded(
                    ImageQuestion(9, "https://raw.githubusercontent.com/github/explore/main/topics/ios/ios.png", "Chapter 1 Image"),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `id of a non-image item is not found`() = runTest {
        viewModelFor(8).uiState.test {
            assertEquals(ImageDetailUiState.NotFound, awaitItem())
        }
    }

    @Test
    fun `image removed by a refresh while open becomes not found`() = runTest {
        viewModelFor(4).uiState.test {
            assertEquals(4L, (awaitItem() as ImageDetailUiState.Loaded).image.id)

            repository.form.value = listOf(Page(1, "Main Page", listOf(TextQuestion(3, "No images any more"))))

            assertEquals(ImageDetailUiState.NotFound, awaitItem())
        }
    }
}
