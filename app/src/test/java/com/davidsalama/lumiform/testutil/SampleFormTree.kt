package com.davidsalama.lumiform.testutil

import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.domain.model.ImageQuestion
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.Response
import com.davidsalama.lumiform.domain.model.ResponseSet
import com.davidsalama.lumiform.domain.model.Section
import com.davidsalama.lumiform.domain.model.TextQuestion

/** [Fixtures.sampleForm] as the domain tree, written out by hand. */
val sampleFormTree: List<FormNode> = listOf(
    Page(
        id = 1, title = "Main Page",
        children = listOf(
            Section(
                id = 2, title = "Introduction",
                children = listOf(
                    TextQuestion(3, "Welcome to the main page!"),
                    ImageQuestion(
                        4,
                        "https://raw.githubusercontent.com/github/explore/main/topics/android/android.png",
                        "Welcome Image",
                    ),
                ),
            ),
            Section(
                id = 5, title = "Chapter 1",
                children = listOf(
                    TextQuestion(6, "This is the first chapter."),
                    Section(
                        id = 7, title = "Subsection 1.1",
                        children = listOf(
                            TextQuestion(8, "This is a subsection under Chapter 1."),
                            ImageQuestion(
                                9,
                                "https://raw.githubusercontent.com/github/explore/main/topics/ios/ios.png",
                                "Chapter 1 Image",
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
    Page(
        id = 10, title = "Second Page",
        children = listOf(
            Section(
                id = 11, title = "Chapter 2",
                children = listOf(
                    TextQuestion(12, "This is the second chapter."),
                    ChoiceQuestion(
                        13, "What is the main topic of Chapter 2?",
                        ResponseSet(
                            id = 101, multipleSelection = false,
                            responses = listOf(
                                Response(1011, "Safety procedures", 1),
                                Response(1012, "Equipment checks", 2),
                                Response(1013, "Not applicable", null),
                            ),
                        ),
                    ),
                    ChoiceQuestion(
                        14, "Which areas were inspected?",
                        ResponseSet(
                            id = 102, multipleSelection = true,
                            responses = listOf(
                                Response(1021, "Entrance", null),
                                Response(1022, "Storage", null),
                                Response(1023, "Loading bay", null),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)
