package com.davidsalama.lumiform.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidsalama.lumiform.MainActivity
import com.davidsalama.lumiform.data.local.FormDatabase
import com.davidsalama.lumiform.data.local.dao.FormDao
import com.davidsalama.lumiform.data.local.dao.SelectionDao
import com.davidsalama.lumiform.data.remote.FormApi
import com.davidsalama.lumiform.data.remote.FormRemoteDataSource
import com.davidsalama.lumiform.data.remote.RetrofitFormRemoteDataSource
import com.davidsalama.lumiform.data.remote.formApiHttpClient
import com.davidsalama.lumiform.data.remote.sharedHttpClient
import com.davidsalama.lumiform.di.ConnectivityModule
import com.davidsalama.lumiform.di.DatabaseModule
import com.davidsalama.lumiform.di.NetworkModule
import com.davidsalama.lumiform.domain.ConnectivityObserver
import com.davidsalama.lumiform.testutil.FakeConnectivityObserver
import com.davidsalama.lumiform.testutil.FakeImageLoaderRule
import com.davidsalama.lumiform.testutil.Fixtures
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * The real app from screen to database: activity, navigation, ViewModels, repository, retry,
 * Retrofit, OkHttp and Room all as in production. Only the edges are swapped: the server is a
 * MockWebServer, the database lives in memory, and the test decides when the device is online.
 *
 * Closing the activity and launching it again stands in for reopening the app: the ViewModel is
 * new, while the database (a singleton, like on a device) keeps what was saved.
 */
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@UninstallModules(NetworkModule::class, DatabaseModule::class, ConnectivityModule::class)
@RunWith(AndroidJUnit4::class)
class FullStackAppTest {

    @Module
    @InstallIn(SingletonComponent::class)
    object InMemoryDatabaseModule {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): FormDatabase =
            Room.inMemoryDatabaseBuilder(context, FormDatabase::class.java).build()

        @Provides
        fun provideFormDao(database: FormDatabase): FormDao = database.formDao()

        @Provides
        fun provideSelectionDao(database: FormDatabase): SelectionDao = database.selectionDao()
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val imageLoaderRule = FakeImageLoaderRule()

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    private val server = MockWebServer().apply { start() }
    private val fakeConnectivity = FakeConnectivityObserver(online = true)

    @BindValue
    @JvmField
    val okHttpClient: OkHttpClient = sharedHttpClient()

    @BindValue
    @JvmField
    val remote: FormRemoteDataSource = RetrofitFormRemoteDataSource(
        FormApi.create(server.url("/"), formApiHttpClient(okHttpClient, cache = null, callTimeout = 5.seconds), FormApi.json),
    )

    @BindValue
    @JvmField
    val connectivity: ConnectivityObserver = fakeConnectivity

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        server.close()
    }

    private fun launchApp() {
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun serveForm() = server.enqueue(
        MockResponse.Builder()
            .code(200)
            .addHeader("Content-Type", "text/plain; charset=utf-8")
            .body(Fixtures.sampleForm)
            .build(),
    )

    /** A server that stops answering: every request is refused at the socket. */
    private fun takeServerDown() = server.close()

    private fun waitForText(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun `first launch downloads the form, shows it and records the sync time`() {
        serveForm()
        launchApp()

        waitForText("Main Page")
        composeRule.onNodeWithText("Synced", substring = true).assertIsDisplayed()
        scrollTo("Which areas were inspected?")
        composeRule.onNodeWithText("Which areas were inspected?").assertIsDisplayed()
    }

    @Test
    fun `answers are saved in the database and are still there after rotating and reopening`() {
        serveForm()
        launchApp()
        waitForText("Main Page")
        scrollTo("None selected · multiple choice")
        composeRule.onNodeWithText("Entrance").performClick()
        composeRule.onNodeWithText("Storage").performClick()
        waitForText("2 selected · multiple choice")

        scenario!!.recreate()
        scrollTo("2 selected · multiple choice")
        composeRule.onNodeWithText("2 selected · multiple choice").assertIsDisplayed()

        // A fresh ViewModel can only know the answers from Room. The refresh on launch
        // replaces the form, and the answers must survive that too.
        serveForm()
        launchApp()
        waitForText("Main Page")
        scrollTo("2 selected · multiple choice")
        composeRule.onNodeWithText("2 selected · multiple choice").assertIsDisplayed()
    }

    @Test
    fun `reopening with the server down shows the cached form, its answers and the offline banner`() {
        serveForm()
        launchApp()
        waitForText("Main Page")
        scrollTo("None selected · single choice")
        composeRule.onNodeWithText("Safety procedures").performClick()
        waitForText("1 selected · single choice")

        takeServerDown()
        launchApp()

        waitForText("You’re offline · Showing data from", substring = true)
        composeRule.onNodeWithText("Main Page").assertIsDisplayed()
        scrollTo("1 selected · single choice")
        composeRule.onNodeWithText("1 selected · single choice").assertIsDisplayed()
    }

    @Test
    fun `without a cache a failure takes the whole screen, and Try again loads the form`() {
        repeat(3) { server.enqueue(MockResponse.Builder().code(503).build()) }
        launchApp()

        waitForText("Server problem")
        composeRule.onNodeWithText("Main Page").assertDoesNotExist()

        serveForm()
        composeRule.onNodeWithText("Try again").performClick()
        waitForText("Main Page")
        composeRule.onNodeWithText("Try again").assertDoesNotExist()
    }

    @Test
    fun `the form loads by itself when the connection comes back`() {
        fakeConnectivity.isOnline.value = false
        repeat(3) { server.enqueue(MockResponse.Builder().code(503).build()) }
        launchApp()
        waitForText("Try again")

        serveForm()
        fakeConnectivity.isOnline.value = true

        waitForText("Main Page")
    }

    @Test
    fun `rotating with an image open keeps showing that image`() {
        serveForm()
        launchApp()
        waitForText("Main Page")
        scrollTo("Chapter 1 Image")
        composeRule.onNodeWithText("Chapter 1 Image").performClick()
        composeRule.onNodeWithContentDescription("Chapter 1 Image").assertIsDisplayed()

        scenario!!.recreate()

        composeRule.onNodeWithContentDescription("Chapter 1 Image").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
}
