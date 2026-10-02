package com.example.photos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.photos.data.MediaItem
import com.example.photos.ui.components.MediaUiState

@Composable
fun ViewerScreen(uiState: MediaUiState, mediaId: Long, onBack: () -> Unit) {
    when (uiState) {
        MediaUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        is MediaUiState.Content -> ViewerPager(uiState.items, mediaId, onBack)
    }
}

@Composable
private fun ViewerPager(items: List<MediaItem>, mediaId: Long, onBack: () -> Unit) {
    // Tính một lần: nếu tính lại mỗi lần vẽ, xóa ảnh đang mở sau khi đã vuốt sang ảnh khác sẽ bị đá ra lưới
    val initialPage = remember { items.indexOfFirst { it.id == mediaId } }
    if (initialPage == -1) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val pagerState = rememberPagerState(initialPage = initialPage) { items.size }
    HorizontalPager(
        state = pagerState,
        key = { items[it].id },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) { page ->
        AsyncImage(
            model = items[page].uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}