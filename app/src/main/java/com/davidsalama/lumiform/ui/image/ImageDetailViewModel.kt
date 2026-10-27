package com.davidsalama.lumiform.ui.image

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.davidsalama.lumiform.domain.FormRepository
import com.davidsalama.lumiform.domain.model.ImageQuestion
import com.davidsalama.lumiform.ui.navigation.ImageDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ImageDetailUiState {
    data object Loading : ImageDetailUiState
    data class Loaded(val image: ImageQuestion) : ImageDetailUiState

    /** The id isn't in the cache, e.g. a refresh removed the image while this screen was open. */
    data object NotFound : ImageDetailUiState
}

/**
 * Only the image id travels through navigation; the image itself is read from the cache.
 * So the screen works offline and is rebuilt correctly after process death.
 */
@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: FormRepository,
) : ViewModel() {

    private val imageId = savedStateHandle.toRoute<ImageDetailDestination>().imageId

    val uiState: StateFlow<ImageDetailUiState> = repository.observeImage(imageId)
        .map { image -> if (image == null) ImageDetailUiState.NotFound else ImageDetailUiState.Loaded(image) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImageDetailUiState.Loading)
}
