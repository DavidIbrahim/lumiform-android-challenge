package com.davidsalama.lumiform.ui.image

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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

    private val image = ImageQuestion(9, "https://example.com/ios.png", "Chapter 1 Image")

    private fun show(state: ImageDetailUiState) {
        composeRule.setContent {
            LumiformTheme { ImageDetailScreen(imageId = 9, state = state, onBack = { backPresses++ }) }
        }
    }

    /** A single tap only counts once the double-tap window has passed, so the test clock moves past it. */
    private fun tapImage() {
        composeRule.onNodeWithContentDescription("Chapter 1 Image").performTouchInput { click() }
        composeRule.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun `loaded image is captioned with its title and described by it`() {
        show(ImageDetailUiState.Loaded(image))

        composeRule.onNodeWithText("Chapter 1 Image").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Chapter 1 Image").assertIsDisplayed()
    }

    @Test
    fun `tapping the image hides the controls, and tapping again brings them back`() {
        show(ImageDetailUiState.Loaded(image))

        tapImage()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithText("Chapter 1 Image").assertDoesNotExist()

        tapImage()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `missing image explains it is no longer part of the form`() {
        show(ImageDetailUiState.NotFound)

        composeRule.onNodeWithText("This image is no longer part of the form.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `back button navigates back`() {
        show(ImageDetailUiState.Loading)

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backPresses)
    }
}
