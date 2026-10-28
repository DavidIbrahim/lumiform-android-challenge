package com.davidsalama.lumiform.ui.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import java.time.Instant
import java.time.ZoneId

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
    // Slides away while reading down the form and comes back on the first scroll up.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { FormTitle(isRefreshing = state.isRefreshing, lastSyncedAt = state.lastSyncedAt) },
                scrollBehavior = scrollBehavior,
            )
        },
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
            else -> Column(contentModifier) {
                if (state.isShowingStaleData) {
                    StaleDataBanner(
                        error = checkNotNull(state.error),
                        lastSyncedAt = state.lastSyncedAt,
                        onRetry = onRefresh,
                    )
                }
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
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
}

/** The app name, with when the data was last synced, so freshness is always visible. */
@Composable
private fun FormTitle(isRefreshing: Boolean, lastSyncedAt: Instant?) {
    val locale = LocalConfiguration.current.locales[0]
    val subtitle = when {
        isRefreshing -> stringResource(R.string.syncing)
        lastSyncedAt != null -> {
            val time = remember(lastSyncedAt, locale) {
                formatSyncTime(lastSyncedAt, Instant.now(), ZoneId.systemDefault(), locale)
            }
            stringResource(R.string.synced_at, time)
        }
        else -> null
    }
    Column {
        Text(stringResource(R.string.app_name))
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
    ) {
        itemsIndexed(
            items = rows,
            key = { _, row -> row.id },
            // Lets Compose reuse a scrolled-off row only for a row of the same kind.
            contentType = { _, row -> row::class },
        ) { index, row ->
            FormRowItem(
                row = row,
                isFirst = index == 0,
                typography = hierarchy,
                onResponseTapped = onResponseTapped,
                onImageClick = onImageClick,
            )
        }
    }
}

/** Nothing cached and the fetch failed: an animated picture, what happened, and a way to retry. */
@Composable
private fun ErrorContent(error: DataError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    // Starts hidden and immediately targets visible, so the content eases in once.
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(modifier.verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visibleState = appear,
            enter = fadeIn() + slideInVertically { it / 10 },
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ErrorIllustration(error)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = error.title(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error.message(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = onRetry, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.try_again))
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.form_empty), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DataError.title(): String = when (this) {
    DataError.NoConnection -> stringResource(R.string.error_title_no_connection)
    DataError.Timeout -> stringResource(R.string.error_title_timeout)
    is DataError.Server -> stringResource(R.string.error_title_server)
    DataError.InvalidData -> stringResource(R.string.error_title_invalid_data)
    DataError.Unknown -> stringResource(R.string.error_title_unknown)
}

@Composable
private fun DataError.message(): String = when (this) {
    DataError.NoConnection -> stringResource(R.string.error_no_connection)
    DataError.Timeout -> stringResource(R.string.error_timeout)
    is DataError.Server -> stringResource(R.string.error_server, code)
    DataError.InvalidData -> stringResource(R.string.error_invalid_data)
    DataError.Unknown -> stringResource(R.string.error_unknown)
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfflineErrorPreview() {
    LumiformTheme {
        FormScreen(
            state = FormUiState(error = DataError.NoConnection),
            onRefresh = {},
            onResponseTapped = { _, _ -> },
            onImageClick = {},
        )
    }
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
