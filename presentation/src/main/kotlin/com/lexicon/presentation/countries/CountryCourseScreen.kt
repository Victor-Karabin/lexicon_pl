package com.lexicon.presentation.countries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.countries.Country
import com.lexicon.presentation.R
import com.lexicon.presentation.common.BottomActionPanel
import com.lexicon.presentation.common.ScrollToTopClearance
import com.lexicon.presentation.common.ScrollToTopLayout
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.presets.ChecklistRow
import com.lexicon.presentation.presets.VocabularySearchField
import com.lexicon.presentation.theme.Dimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun CountryCourseScreen(
    onStarted: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CountryCourseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.started.collect { onStarted() } }

    CountryCourseContent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onToggled = viewModel::onToggled,
        onSelectAll = viewModel::onSelectAll,
        onClear = viewModel::onClear,
        onStart = viewModel::onStart,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun CountryCourseContent(
    uiState: CountryCourseUiState,
    onQueryChanged: (String) -> Unit,
    onToggled: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.countries_select_title), onClose = onClose) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VocabularySearchField(
                query = uiState.query,
                placeholder = stringResource(R.string.countries_search_hint),
                onQueryChanged = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMedium),
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            SelectionBar(
                count = uiState.chosen.size,
                onSelectAll = onSelectAll,
                onClear = onClear,
            )

            val list = rememberLazyListState()
            LaunchedEffect(uiState.query) { list.scrollToItem(0) }
            ScrollToTopLayout(list = list, modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = ScrollToTopClearance),
                ) {
                    items(uiState.shown, key = Country::code) { country ->
                        ChecklistRow(
                            text = "${country.flag} ${country.english}",
                            isChecked = country.code in uiState.chosen,
                            onToggle = { onToggled(country.code) },
                        )
                    }
                }
            }

            BottomActionPanel {
                Button(
                    onClick = onStart,
                    enabled = uiState.canStart,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.course_start))
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Text(
            text = pluralStringResource(R.plurals.selection_count, count, count),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSelectAll) {
            Text(stringResource(R.string.countries_select_all))
        }
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.countries_clear))
        }
    }
}
