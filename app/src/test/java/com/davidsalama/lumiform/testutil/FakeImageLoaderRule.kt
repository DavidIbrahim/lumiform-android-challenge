package com.davidsalama.lumiform.testutil

import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Makes every Coil request in a UI test resolve instantly to a solid color, so tests never
 * touch the network and image rows render deterministically.
 */
@OptIn(DelicateCoilApi::class)
class FakeImageLoaderRule : TestWatcher() {

    override fun starting(description: Description) {
        val engine = FakeImageLoaderEngine.Builder()
            .default(ColorImage(0xFF7CC4A8.toInt()))
            .build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext())
                .components { add(engine) }
                .build(),
        )
    }

    override fun finished(description: Description) {
        SingletonImageLoader.reset()
    }
}
