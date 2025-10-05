package com.mediaexplorer

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*

/**
 * 媒体文件数据模型
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Long = 0  // 视频时长（毫秒）
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
    
    val isImage: Boolean
        get() = mimeType.startsWith("image/")
    
    val formattedSize: String
        get() = formatFileSize(size)
    
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(dateModified * 1000))
    
    val formattedDuration: String
        get() {
            if (duration == 0L) return ""
            val seconds = duration / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
                else -> String.format("%d:%02d", minutes, seconds % 60)
            }
        }
    
    val resolution: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else ""
    
    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$size B"
        }
    }
}

/**
 * 媒体类型过滤器
 */
enum class MediaFilter {
    ALL,    // 全部
    IMAGES, // 仅图片
    VIDEOS  // 仅视频
}


