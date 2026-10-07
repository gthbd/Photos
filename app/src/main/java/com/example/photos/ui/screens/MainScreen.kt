package com.example.photos.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.photos.R
import com.example.photos.data.MediaItem
import com.example.photos.ui.components.MediaUiState

private const val PORTRAIT_COLUMNS = 5
private const val LANDSCAPE_COLUMNS = 10

@Composable
fun MainScreen(
    uiState: MediaUiState,
    onMediaClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        GalleryTitle()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = uiState) {
                MediaUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is MediaUiState.Content -> MediaGrid(
                    mediaItems = state.items,
                    columnCount = if (isLandscape) LANDSCAPE_COLUMNS else PORTRAIT_COLUMNS,
                    onMediaClick = onMediaClick,
                )
            }
        }
    }
}

@Composable
private fun GalleryTitle() {
    Text(
        text = "Photos",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            //horizontal 16dp vertical 12dp
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun MediaGrid(mediaItems: List<MediaItem>, columnCount: Int, onMediaClick: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(items = mediaItems, key = { it.id }) { item ->
            MediaCell(item, onClick = { onMediaClick(item.id) })
        }
    }
}

@Composable
private fun MediaCell(item: MediaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            VideoBadge(
                durationMs = item.durationMs,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}
@Composable
fun VideoBadge(durationMs: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(2.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.filled_play_arrow),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = formatDuration(durationMs),
            color = Color.White,
            fontSize = 10.sp,
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}