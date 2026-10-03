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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import com.example.photos.R
import androidx.media3.common.MediaItem as PlayerMediaItem

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
                modifier = Modifier.rotate(270f)
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
    val player = rememberViewerPlayer()
    val currentItem = items.getOrNull(pagerState.settledPage)

    // Khóa theo id thay vì vị trí: refresh() chèn ảnh mới làm vị trí đổi nhưng video đang xem không được phát lại từ đầu
    LaunchedEffect(currentItem?.id) {
        if (currentItem?.isVideo == true) {
            player.setMediaItem(PlayerMediaItem.fromUri(currentItem.uri))
            player.prepare()
            player.play()
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }

    HorizontalPager(
        state = pagerState,
        key = { items[it].id },
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val item = items[page]
        if (item.isVideo && item.id == currentItem?.id) {
            ContentFrame(
                player = player,
                modifier = Modifier.fillMaxSize(),
                shutter = { MediaImage(item) },
            )
        } else {
            MediaImage(item)
        }
    }
}

@Composable
private fun MediaImage(item: MediaItem) {
    AsyncImage(
        model = item.uri,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun rememberViewerPlayer(): ExoPlayer {
    val context = LocalContext.current
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose { player.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }
    return player
}