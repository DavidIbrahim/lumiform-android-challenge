package com.davidsalama.lumiform.ui.image

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidsalama.lumiform.domain.model.ImageQuestion
import com.davidsalama.lumiform.testutil.FakeImageLoaderRule
import com.davidsalama.lumiform.ui.theme.LumiformTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageDetailScreenTest {

    @get:Rule(order = 0)
    val imageLoaderRule = FakeImageLoaderRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private var backPresses = 0

    private fun show(state: ImageDetailUiState) {
        composeRule.setContent {
            LumiformTheme { ImageDetailScreen(state = state, onBack = { backPresses++ }) }
        }
    }

    @Test
    fun `loaded image shows its title and is described by it`() {
        show(ImageDetailUiState.Loaded(ImageQuestion(9, "https://example.com/ios.png", "Chapter 1 Image")))

        composeRule.onNodeWithText("Chapter 1 Image").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Chapter 1 Image").assertIsDisplayed()
    }

    @Test
    fun `missing image explains it is no longer part of the form`() {
        show(ImageDetailUiState.NotFound)

        composeRule.onNodeWithText("This image is no longer part of the form.").assertIsDisplayed()
    }

    @Test
    fun `back button navigates back`() {
        show(ImageDetailUiState.Loading)

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backPresses)
    }
}
