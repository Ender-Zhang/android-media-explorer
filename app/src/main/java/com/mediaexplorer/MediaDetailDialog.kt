package com.mediaexplorer

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * 媒体详情对话框 - 全新设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailDialog(
    mediaItem: MediaItem,
    onDismiss: () -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 全屏媒体内容
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (mediaItem.isVideo) {
                    // 视频播放器
                    VideoPlayer(
                        uri = mediaItem.uri,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 图片查看器
                    AsyncImage(
                        model = mediaItem.uri,
                        contentDescription = mediaItem.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // 顶部浮动控制栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 关闭按钮
                FloatingActionButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
                
                // 信息按钮
                FloatingActionButton(
                    onClick = { showInfo = !showInfo },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (showInfo) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "信息"
                    )
                }
            }
            
            // 底部信息面板（可展开/收起）
            AnimatedVisibility(
                visible = showInfo,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MediaInfoPanel(mediaItem)
            }
        }
    }
}

/**
 * 媒体信息面板 - 新设计
 */
@Composable
fun MediaInfoPanel(mediaItem: MediaItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // 标题指示器
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 文件名
            Text(
                text = mediaItem.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 关键信息卡片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 类型
                InfoChip(
                    label = if (mediaItem.isVideo) "视频" else "图片",
                    icon = if (mediaItem.isVideo) "🎬" else "🖼️",
                    modifier = Modifier.weight(1f)
                )
                
                // 大小
                InfoChip(
                    label = mediaItem.formattedSize,
                    icon = "💾",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 分辨率和时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mediaItem.resolution.isNotEmpty()) {
                    InfoChip(
                        label = mediaItem.resolution,
                        icon = "📐",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (mediaItem.isVideo && mediaItem.formattedDuration.isNotEmpty()) {
                    InfoChip(
                        label = mediaItem.formattedDuration,
                        icon = "⏱️",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            
            // 详细信息列表
            DetailInfoRow("创建时间", mediaItem.formattedDate)
            DetailInfoRow("文件路径", mediaItem.path)
            DetailInfoRow("MIME 类型", mediaItem.mimeType)
        }
    }
}

/**
 * 信息芯片卡片
 */
@Composable
fun InfoChip(
    label: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * 详细信息行
 */
@Composable
fun DetailInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}



