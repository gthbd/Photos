package com.example.photos.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.photos.R
import com.example.photos.data.MediaItem
import com.example.photos.ui.components.MediaUiState
import androidx.media3.common.MediaItem as PlayerMediaItem
import kotlinx.coroutines.Job
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch

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

private const val NO_MEDIA_ID = -1L

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
    var resumeMediaId by rememberSaveable { mutableLongStateOf(NO_MEDIA_ID) }
    var resumePositionMs by rememberSaveable { mutableLongStateOf(0L) }
    var resumePlayWhenReady by rememberSaveable { mutableStateOf(true) }
    var isResumeConsumed by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        resumeMediaId = if (currentItem?.isVideo == true) currentItem.id else NO_MEDIA_ID
        resumePositionMs = player.currentPosition
        resumePlayWhenReady = player.playWhenReady
    }

    // Khóa theo id thay vì vị trí: refresh() chèn ảnh mới làm vị trí đổi nhưng video đang xem không được phát lại từ đầu
    LaunchedEffect(currentItem?.id) {
        if (currentItem?.isVideo == true) {
            val isRestoring = !isResumeConsumed && resumeMediaId == currentItem.id
            player.setMediaItem(
                PlayerMediaItem.fromUri(currentItem.uri),
                if (isRestoring) resumePositionMs else 0L,
            )
            player.prepare()
            player.playWhenReady = !isRestoring || resumePlayWhenReady
        } else {
            player.stop()
            player.clearMediaItems()
        }
        isResumeConsumed = true
    }

    HorizontalPager(
        state = pagerState,
        key = { items[it].id },
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val item = items[page]
        if (item.isVideo && item.id == currentItem?.id) {
            VideoPlayerView(player, Modifier.fillMaxSize())
        } else {
            MediaImage(item)
        }
    }
}


private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 3f

@Composable
private fun MediaImage(item: MediaItem) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        // Giới hạn theo phần ảnh phóng to vượt khỏi khung, tính lại cả khi thu nhỏ để ảnh không bị kẹt lệch
        val maxOffsetX = containerSize.width * (scale - 1f) / 2f
        val maxOffsetY = containerSize.height * (scale - 1f) / 2f
        offset = Offset(
            x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
            y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY),
        )
    }

    AsyncImage(
        model = item.uri,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tapPosition ->
                    val startScale = scale
                    val startOffset = offset
                    val isZoomed = startScale > 1f
                    val targetScale = if (isZoomed) 1f else DOUBLE_TAP_ZOOM
                    val targetOffset = if (isZoomed) Offset.Zero else zoomOffsetAt(tapPosition, containerSize)

                    // Double-tap liên tiếp không được chạy hai animation cùng ghi scale/offset
                    zoomJob?.cancel()
                    zoomJob = coroutineScope.launch {
                        animate(0f, 1f) { progress, _ ->
                            scale = lerp(startScale, targetScale, progress)
                            offset = lerp(startOffset, targetOffset, progress)
                        }
                    }
                })
            }
            // Chưa phóng to thì không nhận kéo, để HorizontalPager còn lật trang
            .transformable(transformState, canPan = { scale > 1f })
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    )
}

// Đi qua transformState để cử chỉ tay và animation loại trừ nhau, và vẫn dùng lại phần clamp trong lambda
private suspend fun TransformableState.animateTransform(
    fromScale: Float,
    toScale: Float,
    fromOffset: Offset,
    toOffset: Offset,
) = transform {
    var appliedScale = fromScale
    var appliedOffset = fromOffset
    animate(0f, 1f) { progress, _ ->
        val nextScale = lerp(fromScale, toScale, progress)
        val nextOffset = lerp(fromOffset, toOffset, progress)
        transformBy(
            zoomChange = nextScale / appliedScale,
            panChange = nextOffset - appliedOffset,
        )
        appliedScale = nextScale
        appliedOffset = nextOffset
    }
}

// Giữ điểm được tap đứng yên trên màn hình khi phóng quanh tâm khung
private fun zoomOffsetAt(tapPosition: Offset, containerSize: IntSize): Offset {
    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
    val extraScale = DOUBLE_TAP_ZOOM - 1f
    val maxX = containerSize.width * extraScale / 2f
    val maxY = containerSize.height * extraScale / 2f
    val rawOffset = (center - tapPosition) * extraScale
    return Offset(rawOffset.x.coerceIn(-maxX, maxX), rawOffset.y.coerceIn(-maxY, maxY))
}

@Composable
private fun rememberViewerPlayer(): ExoPlayer {
    val context = LocalContext.current
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose { player.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }
    return player
}

@Composable
private fun VideoPlayerView(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                useController = true
            }
        },
        // Pager tạo/hủy view này khi vuốt qua lại; không gỡ player thì PlayerView cũ vẫn giữ listener trên player dùng chung
        onRelease = { it.player = null },
        modifier = modifier,
    )
}