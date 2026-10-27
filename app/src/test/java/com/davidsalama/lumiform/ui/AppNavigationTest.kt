package com.davidsalama.lumiform.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidsalama.lumiform.MainActivity
import com.davidsalama.lumiform.testutil.FakeFormRepository
import com.davidsalama.lumiform.testutil.FakeImageLoaderRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * The whole app on the JVM: real activity, navigation, ViewModels and Compose UI, with only
 * the data layer replaced by an in-memory fake (see TestDataModule).
 */
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val imageLoaderRule = FakeImageLoaderRule()

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: FakeFormRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun `tapping an image opens it full screen with its title, and back returns to the form`() {
        scrollTo("Chapter 1 Image")
        composeRule.onNodeWithText("Chapter 1 Image").performClick()

        // Detail screen: the title is in the top bar and the image is described by it.
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithText("Chapter 1 Image").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Chapter 1 Image").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithText("Lumiform Forms").assertIsDisplayed()
    }

    @Test
    fun `tapping a response goes through the ViewModel and repository back to the screen`() {
        // The status line is the bottom of the question, so the chips above it are on screen too.
        scrollTo("None selected · multiple choice")
        composeRule.onNodeWithText("Entrance").performClick()
        composeRule.onNodeWithText("Storage").performClick()

        scrollTo("2 selected · multiple choice")
        composeRule.onNodeWithText("2 selected · multiple choice").assertIsDisplayed()
        assertEquals(mapOf(14L to setOf(1021L, 1022L)), repository.selections.value)
    }

    @Test
    fun `single choice switches the selection instead of adding to it`() {
        scrollTo("None selected · single choice")
        composeRule.onNodeWithText("Safety procedures").performClick()
        composeRule.onNodeWithText("Equipment checks").performClick()

        scrollTo("1 selected · single choice")
        composeRule.onNodeWithText("1 selected · single choice").assertIsDisplayed()
        assertEquals(mapOf(13L to setOf(1012L)), repository.selections.value)
    }
}
