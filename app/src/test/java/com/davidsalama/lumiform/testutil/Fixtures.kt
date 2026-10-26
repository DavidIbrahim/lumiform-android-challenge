package com.davidsalama.lumiform.testutil

object Fixtures {
    /** The real endpoint response, saved on 2026-10-25. */
    val sampleForm: String by lazy { read("sample-form.json") }

    fun read(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }.readText()
}
