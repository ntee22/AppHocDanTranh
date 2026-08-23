package vn.hocdantranh.guzheng.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.video.FirestoreVideoRepository
import vn.hocdantranh.guzheng.data.video.VideoCategory
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.data.video.VideoRepository

/**
 * Everything the Video tab renders. The derived values live here rather than in the
 * composables so filtering and chip selection can be tested without a device.
 */
data class VideoUiState(
    val videos: List<VideoItem> = emptyList(),
    val selectedCategory: VideoCategory? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
) {
    val filtered: List<VideoItem>
        get() = selectedCategory?.let { c -> videos.filter { it.category == c } } ?: videos

    /**
     * Only categories with at least one loaded video, in enum order. Derived from the data
     * because the live collection has no `tips` video — a hardcoded list would show a chip
     * that filters to nothing.
     */
    val availableCategories: List<VideoCategory>
        get() = VideoCategory.entries.filter { c -> videos.any { it.category == c } }
}

class VideoViewModel(
    private val repository: VideoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState(isLoading = true))
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private var cursor: Any? = null

    init {
        refresh()
    }

    /** Reloads from the first page, replacing what is on screen. Also backs pull-to-refresh. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val page = runCatching { repository.loadPage(null) }.getOrNull()
            cursor = page?.cursor
            _uiState.update {
                it.copy(
                    videos = page?.items ?: it.videos,
                    isLoading = false,
                    hasMore = page?.hasMore ?: false,
                )
            }
        }
    }

    /** Appends the next page. Ignored while a load is in flight or the catalog is exhausted. */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val page = runCatching { repository.loadPage(cursor) }.getOrNull()
            cursor = page?.cursor
            _uiState.update {
                it.copy(
                    videos = it.videos + (page?.items ?: emptyList()),
                    isLoadingMore = false,
                    hasMore = page?.hasMore ?: false,
                )
            }
        }
    }

    /** Null clears the filter. Tapping the active chip clears it too, matching iOS. */
    fun selectCategory(category: VideoCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VideoViewModel(FirestoreVideoRepository()) as T
        }
    }
}
