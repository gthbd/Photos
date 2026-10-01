package com.example.photos.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photos.data.MediaItem
import com.example.photos.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MediaUiState {
    data object Loading : MediaUiState
    data class Content(val items: List<MediaItem>) : MediaUiState
}

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application.contentResolver)

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    // ViewModel chỉ được tạo khi AppNavHost hiện, tức là sau khi PermissionScreen xác nhận đã có quyền
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = MediaUiState.Content(repository.loadAll())
        }
    }
}