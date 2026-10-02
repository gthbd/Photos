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
import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.photos.R

@Composable
fun ViewerScreen(uiState: MediaUiState, mediaId: Long, onBack: () -> Unit) {
    ImmersiveMode()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (uiState) {
            MediaUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is MediaUiState.Content -> ViewerPager(uiState.items, mediaId, onBack)
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(8.dp)
                // Nền mờ để mũi tên trắng đọc được cả trên ảnh sáng
                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
        ) {
            Icon(
                painter = painterResource(id =R.drawable.arrow_upward),
                contentDescription = "Quay lại",
                tint = Color.White,
                modifier = Modifier.rotate(240f)
            )
        }
    }
}

@Composable
private fun ImmersiveMode() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
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