package com.mediaexplorer

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 视频缩略图组件
 * 直接使用 Android API 加载缩略图，不依赖 Coil
 */
@Composable
fun VideoThumbnailImage(
    uri: Uri,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(uri) {
        thumbnail = withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("VideoThumbnailImage", "Loading thumbnail for: $uri")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 使用新 API
                    context.contentResolver.loadThumbnail(
                        uri,
                        Size(512, 512),
                        null
                    )
                } else {
                    // Android 9 及以下，使用 ThumbnailUtils
                    val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val pathColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                            val videoPath = cursor.getString(pathColumn)
                            
                            @Suppress("DEPRECATION")
                            android.media.ThumbnailUtils.createVideoThumbnail(
                                videoPath,
                                android.provider.MediaStore.Video.Thumbnails.MINI_KIND
                            )
                        } else null
                    }
                }.also {
                    android.util.Log.d("VideoThumbnailImage", "Thumbnail loaded: ${it != null}")
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoThumbnailImage", "Failed to load thumbnail", e)
                null
            } finally {
                isLoading = false
            }
        }
    }
    
    if (thumbnail != null) {
        Image(
            bitmap = thumbnail!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        // 显示占位符
        androidx.compose.foundation.layout.Box(
            modifier = modifier.background(Color.DarkGray)
        )
    }
}

