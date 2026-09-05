package vn.hocdantranh.guzheng.ui.sheet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.sheet.FirestoreSheetRepository
import vn.hocdantranh.guzheng.data.sheet.SheetCache
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.data.sheet.SheetRepository

/**
 * Everything the Sheet tab renders. Filtering lives here rather than in the composables so
 * search is testable without a device — the same split as `VideoUiState`.
 */
data class SheetUiState(
    val sheets: List<SheetItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * Case-insensitive, diacritic-**sensitive** title match, mirroring the iOS
     * `localizedCaseInsensitiveContains`: `nho` does not find `Nhỏ ơi`. Folding diacritics
     * would suit Vietnamese typists better, but that is a change for both platforms.
     */
    val filtered: List<SheetItem>
        get() {
            val needle = query.trim()
            if (needle.isEmpty()) return sheets
            return sheets.filter { it.title.contains(needle, ignoreCase = true) }
        }

    fun sheetById(id: String): SheetItem? = sheets.firstOrNull { it.id == id }
}

class SheetViewModel(
    repository: SheetRepository,
    private val cache: SheetCache?,
) : ViewModel() {

    private var repository: SheetRepository = repository

    private val _uiState = MutableStateFlow(SheetUiState(isLoading = true))
    val uiState: StateFlow<SheetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Paint the last-known catalog first, so a warm start shows sheets immediately.
            cache?.read()?.takeIf { it.isNotEmpty() }?.let { cached ->
                _uiState.update { it.copy(sheets = cached) }
            }
            load()
        }
    }

    /** Reloads from Firestore, replacing what is on screen. Also backs the retry button. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val result = runCatching { repository.load() }
        _uiState.update { state ->
            result.fold(
                onSuccess = { sheets ->
                    state.copy(sheets = sheets, isLoading = false, errorMessage = null)
                },
                onFailure = { error ->
                    // Stale sheets beat an error screen; only a cold failure is worth showing.
                    state.copy(
                        isLoading = false,
                        errorMessage = if (state.sheets.isEmpty()) {
                            error.message ?: "Unknown error"
                        } else {
                            null
                        },
                    )
                },
            )
        }
        result.getOrNull()?.let { cache?.write(it) }
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /** Test seam: swaps the backing repository so one instance can exercise both outcomes. */
    internal fun replaceRepositoryForTest(next: SheetRepository) {
        repository = next
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SheetViewModel(
                    repository = FirestoreSheetRepository(),
                    cache = SheetCache(context.applicationContext),
                ) as T
            }
    }
}
