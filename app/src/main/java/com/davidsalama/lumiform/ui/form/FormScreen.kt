package com.davidsalama.lumiform.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidsalama.lumiform.R
import com.davidsalama.lumiform.domain.DataError
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.Section
import com.davidsalama.lumiform.domain.model.TextQuestion
import com.davidsalama.lumiform.ui.form.rows.FormRowItem
import com.davidsalama.lumiform.ui.theme.HierarchyTypography
import com.davidsalama.lumiform.ui.theme.LumiformTheme

@Composable
fun FormRoute(
    onImageClick: (Long) -> Unit,
    viewModel: FormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FormScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onResponseTapped = viewModel::onResponseTapped,
        onImageClick = onImageClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    state: FormUiState,
    onRefresh: () -> Unit,
    onResponseTapped: (ChoiceQuestion, Long) -> Unit,
    onImageClick: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            state.isInitialLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.isBlockingError -> ErrorContent(
                error = checkNotNull(state.error),
                onRetry = onRefresh,
                modifier = contentModifier,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = contentModifier,
            ) {
                if (state.rows.isEmpty()) {
                    EmptyContent(Modifier.fillMaxSize())
                } else {
                    FormList(state.rows, onResponseTapped, onImageClick)
                }
            }
        }
    }
}

@Composable
private fun FormList(
    rows: List<FormRow>,
    onResponseTapped: (ChoiceQuestion, Long) -> Unit,
    onImageClick: (Long) -> Unit,
) {
    val typography = MaterialTheme.typography
    val hierarchy = remember(typography) { HierarchyTypography(typography) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(
            items = rows,
            key = { it.id },
            // Lets Compose reuse a scrolled-off row only for a row of the same kind.
            contentType = { it::class },
        ) { row ->
            FormRowItem(
                row = row,
                typography = hierarchy,
                onResponseTapped = onResponseTapped,
                onImageClick = onImageClick,
            )
        }
    }
}

@Composable
private fun ErrorContent(error: DataError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = error.message(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.form_empty), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun DataError.message(): String = when (this) {
    DataError.NoConnection -> stringResource(R.string.error_no_connection)
    DataError.Timeout -> stringResource(R.string.error_timeout)
    is DataError.Server -> stringResource(R.string.error_server, code)
    DataError.InvalidData -> stringResource(R.string.error_invalid_data)
    DataError.Unknown -> stringResource(R.string.error_unknown)
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FormScreenPreview() {
    val form = listOf(
        Page(
            1, "Main Page",
            listOf(
                Section(
                    2, "Chapter 1",
                    listOf(
                        TextQuestion(3, "This is the first chapter."),
                        Section(4, "Subsection 1.1", listOf(TextQuestion(5, "Nested question text."))),
                    ),
                ),
            ),
        ),
    )
    LumiformTheme {
        FormScreen(
            state = FormUiState(rows = flattenForm(form)),
            onRefresh = {},
            onResponseTapped = { _, _ -> },
            onImageClick = {},
        )
    }
}
