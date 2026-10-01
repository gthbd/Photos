package com.example.photos.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long
)

class MediaRepository(private val contentResolver: ContentResolver) {

    suspend fun loadAll(): List<MediaItem> = withContext(Dispatchers.IO) {
        val cursor = contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(FileColumns._ID, FileColumns.MEDIA_TYPE, MediaStore.Video.Media.DURATION),
            "${FileColumns.MEDIA_TYPE} IN (?, ?)",
            arrayOf(
                FileColumns.MEDIA_TYPE_IMAGE.toString(),
                FileColumns.MEDIA_TYPE_VIDEO.toString(),
            ),
            "${FileColumns.DATE_ADDED} DESC",
        ) ?: return@withContext emptyList()

        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(FileColumns._ID)
            val typeColumn = it.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            buildList {
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val isVideo = it.getInt(typeColumn) == FileColumns.MEDIA_TYPE_VIDEO
                    val durationMs = it.getLong(durationColumn)
                    add(MediaItem(id, contentUri(id, isVideo), isVideo, durationMs))
                }
            }
        }
    }

    // Uri theo từng loại (Images/Video) là dạng chuẩn để các thư viện tải ảnh mở đúng nội dung
    private fun contentUri(id: Long, isVideo: Boolean): Uri {
        val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return ContentUris.withAppendedId(baseUri, id)
    }
}