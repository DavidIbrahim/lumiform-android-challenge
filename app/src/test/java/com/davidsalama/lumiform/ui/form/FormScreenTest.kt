package com.davidsalama.lumiform.ui.form

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidsalama.lumiform.domain.DataError
import com.davidsalama.lumiform.testutil.FakeImageLoaderRule
import com.davidsalama.lumiform.testutil.sampleFormTree
import com.davidsalama.lumiform.ui.theme.LumiformTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** The screen's states, from the user's point of view (Robolectric). */
@RunWith(AndroidJUnit4::class)
class FormScreenTest {

    // The sample form has two images; without this Coil would try the real network.
    @get:Rule(order = 0)
    val imageLoaderRule = FakeImageLoaderRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private var refreshes = 0

    private fun show(state: FormUiState) {
        composeRule.setContent {
            LumiformTheme {
                FormScreen(
                    state = state,
                    onRefresh = { refreshes++ },
                    onResponseTapped = { _, _ -> },
                    onImageClick = {},
                )
            }
        }
    }

    @Test
    fun `failed refresh over cached data shows the banner above the form`() {
        show(FormUiState(rows = flattenForm(sampleFormTree), error = DataError.NoConnection))

        composeRule.onNodeWithText("You’re offline").assertExists()
        composeRule.onNodeWithText("Main Page").assertExists()

        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, refreshes)
    }

    @Test
    fun `successful state shows no banner`() {
        show(FormUiState(rows = flattenForm(sampleFormTree)))

        composeRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun `top bar shows when the data was last synced`() {
        show(FormUiState(rows = flattenForm(sampleFormTree), lastSyncedAt = Instant.now()))
        composeRule.onNodeWithText("Synced", substring = true).assertExists()
    }

    @Test
    fun `top bar says syncing while a refresh runs`() {
        show(FormUiState(rows = flattenForm(sampleFormTree), isRefreshing = true, lastSyncedAt = Instant.now()))
        composeRule.onNodeWithText("Syncing…").assertExists()
    }

    @Test
    fun `failure without cache takes the whole screen with a retry`() {
        show(FormUiState(error = DataError.Timeout))

        composeRule.onNodeWithText("Connection is slow").assertExists()
        composeRule.onNodeWithText("The server took too long to respond.").assertExists()
        composeRule.onNodeWithText("Main Page").assertDoesNotExist()
        composeRule.onNodeWithText("Try again").performClick()
        assertEquals(1, refreshes)
    }

    @Test
    fun `offline without cache says so and that it will load by itself`() {
        show(FormUiState(error = DataError.NoConnection))

        composeRule.onNodeWithText("You’re offline").assertExists()
        composeRule.onNodeWithText("back online", substring = true).assertExists()
    }
}
