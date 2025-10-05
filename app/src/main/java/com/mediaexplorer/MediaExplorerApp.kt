package com.mediaexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis

/**
 * 主应用界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaExplorerApp(viewModel: MediaViewModel) {
    val mediaItems by viewModel.mediaItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadMedia()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体浏览器") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 过滤器标签
            FilterTabs(
                currentFilter = currentFilter,
                onFilterChanged = { viewModel.setFilter(it) }
            )
            
            // 媒体网格
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (mediaItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("未找到媒体文件")
                }
            } else {
                MediaGrid(
                    mediaItems = mediaItems,
                    onItemClick = { selectedItem = it }
                )
            }
        }
        
        // 详情对话框
        selectedItem?.let { item ->
            MediaDetailDialog(
                mediaItem = item,
                onDismiss = { selectedItem = null }
            )
        }
    }
}

/**
 * 过滤器标签
 */
@Composable
fun FilterTabs(
    currentFilter: MediaFilter,
    onFilterChanged: (MediaFilter) -> Unit
) {
    TabRow(selectedTabIndex = currentFilter.ordinal) {
        Tab(
            selected = currentFilter == MediaFilter.ALL,
            onClick = { onFilterChanged(MediaFilter.ALL) },
            text = { Text("全部") }
        )
        Tab(
            selected = currentFilter == MediaFilter.IMAGES,
            onClick = { onFilterChanged(MediaFilter.IMAGES) },
            text = { Text("照片") }
        )
        Tab(
            selected = currentFilter == MediaFilter.VIDEOS,
            onClick = { onFilterChanged(MediaFilter.VIDEOS) },
            text = { Text("视频") }
        )
    }
}

/**
 * 媒体网格
 */
@Composable
fun MediaGrid(
    mediaItems: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(mediaItems) { item ->
            MediaGridItem(
                mediaItem = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

/**
 * 媒体网格项
 */
@Composable
fun MediaGridItem(
    mediaItem: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        if (mediaItem.isVideo) {
            // 视频使用自定义缩略图组件
            VideoThumbnailImage(
                uri = mediaItem.uri,
                contentDescription = mediaItem.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 图片使用 Coil
            AsyncImage(
                model = mediaItem.uri,
                contentDescription = mediaItem.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // 视频标识
        if (mediaItem.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "视频",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // 显示时长
            if (mediaItem.formattedDuration.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = mediaItem.formattedDuration,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}


