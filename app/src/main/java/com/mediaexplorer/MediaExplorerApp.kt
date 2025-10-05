package com.mediaexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 主应用界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaExplorerApp(viewModel: MediaViewModel) {
    val mediaItems by viewModel.mediaItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val isAdbServerRunning by viewModel.isAdbServerRunning.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val useIncrementalTransfer by viewModel.useIncrementalTransfer.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()
    
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showTransferModeDialog by remember { mutableStateOf(false) }
    var showTransferHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var selectedTransferMode by remember { mutableStateOf("wifi") }
    
    LaunchedEffect(Unit) {
        viewModel.loadMedia()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isSelectionMode) {
                            "已选择 ${selectedItems.size} 项"
                        } else {
                            "媒体浏览器"
                        }
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出选择模式")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // 选择模式的操作按钮
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                        IconButton(onClick = { viewModel.invertSelection() }) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "反选")
                        }
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        
                        // 下拉菜单
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("传输到电脑") },
                                onClick = {
                                    showOptionsMenu = false
                                    showTransferModeDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Send, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("批量删除") },
                                onClick = {
                                    showOptionsMenu = false
                                    showDeleteConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("分享") },
                                onClick = {
                                    showOptionsMenu = false
                                    showShareDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            )
                        }
                    } else {
                        // 普通模式的操作按钮
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "选择模式")
                        }
                        IconButton(onClick = { showTransferHistoryDialog = true }) {
                            Icon(Icons.Default.History, contentDescription = "传输历史")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 过滤器标签
                if (!isSelectionMode) {
                    FilterTabs(
                        currentFilter = currentFilter,
                        onFilterChanged = { viewModel.setFilter(it) }
                    )
                }
                
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
                        isSelectionMode = isSelectionMode,
                        selectedItems = selectedItems,
                        onItemClick = { item ->
                            if (isSelectionMode) {
                                viewModel.toggleItemSelection(item.id)
                            } else {
                                selectedItem = item
                            }
                        }
                    )
                }
            }
            
            // 传输进度指示器（浮动在底部）
            if (transferProgress.isTransferring) {
                TransferProgressIndicator(
                    progress = transferProgress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
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
        
        // 传输模式选择对话框
        if (showTransferModeDialog) {
            TransferModeDialog(
                viewModel = viewModel,
                selectedCount = selectedItems.size,
                useIncrementalTransfer = useIncrementalTransfer,
                onModeSelected = { mode ->
                    selectedTransferMode = mode
                    showTransferModeDialog = false
                    showTransferDialog = true
                },
                onDismiss = { showTransferModeDialog = false }
            )
        }
        
        // 传输对话框
        if (showTransferDialog) {
            TransferDialog(
                viewModel = viewModel,
                transferMode = selectedTransferMode,
                isWifiServerRunning = isServerRunning,
                isAdbServerRunning = isAdbServerRunning,
                serverUrl = serverUrl,
                selectedCount = selectedItems.size,
                onDismiss = { showTransferDialog = false }
            )
        }
        
        // 传输历史对话框
        if (showTransferHistoryDialog) {
            TransferHistoryDialog(
                viewModel = viewModel,
                onDismiss = { showTransferHistoryDialog = false }
            )
        }
        
        // 批量删除确认对话框
        if (showDeleteConfirmDialog) {
            BatchDeleteDialog(
                viewModel = viewModel,
                selectedCount = selectedItems.size,
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }
        
        // 分享对话框
        if (showShareDialog) {
            val context = LocalContext.current
            ShareFilesDialog(
                viewModel = viewModel,
                context = context,
                onDismiss = { showShareDialog = false }
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
    isSelectionMode: Boolean = false,
    selectedItems: Set<Long> = emptySet(),
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
                isSelectionMode = isSelectionMode,
                isSelected = selectedItems.contains(item.id),
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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
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
        if (mediaItem.isVideo && !isSelectionMode) {
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
        
        // 选择模式下的覆盖层和复选框
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) {
                            Color(0x600066CC)
                        } else {
                            Color(0x30000000)
                        }
                    )
            )
            
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "已选择" else "未选择",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            )
        }
    }
}

/**
 * 传输模式选择对话框
 */
@Composable
fun TransferModeDialog(
    viewModel: MediaViewModel,
    selectedCount: Int,
    useIncrementalTransfer: Boolean,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf("wifi") }
    val (newCount, totalCount) = viewModel.getTransferStats()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Settings, contentDescription = null)
        },
        title = {
            Text("选择传输方式")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "已选择 $selectedCount 个文件",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 增量传输选项
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (useIncrementalTransfer) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleIncrementalTransfer() }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "增量传输",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (useIncrementalTransfer) {
                                    "只传输 $newCount 个新文件"
                                } else {
                                    "传输所有 $totalCount 个文件"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useIncrementalTransfer,
                            onCheckedChange = { viewModel.setIncrementalTransfer(it) }
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "传输方式：",
                    style = MaterialTheme.typography.titleSmall
                )
                
                // WiFi传输选项
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = "wifi" },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == "wifi") {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (selectedMode == "wifi") {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == "wifi",
                            onClick = { selectedMode = "wifi" }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "📶 WiFi传输",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "无需数据线，浏览器访问\n适合少量文件，速度较慢",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // ADB传输选项
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = "adb" },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == "adb") {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (selectedMode == "adb") {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == "adb",
                            onClick = { selectedMode = "adb" }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🔌 USB/ADB传输",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "需要USB连接和ADB工具\n适合大量文件，速度更快",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onModeSelected(selectedMode) }
            ) {
                Text("下一步")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 文件传输对话框
 */
@Composable
fun TransferDialog(
    viewModel: MediaViewModel,
    transferMode: String,
    isWifiServerRunning: Boolean,
    isAdbServerRunning: Boolean,
    serverUrl: String?,
    selectedCount: Int,
    onDismiss: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var isTransferring by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = { 
            if (!isTransferring) {
                viewModel.stopFileTransfer()
                onDismiss()
            }
        },
        icon = {
            Icon(Icons.Default.Send, contentDescription = null)
        },
        title = {
            Text("剪切传输文件")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    // 未启动服务器
                    !isWifiServerRunning && !isAdbServerRunning -> {
                        Text(
                            text = "将文件传输到电脑",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "⚠️ 注意：剪切传输将在传输完成后删除手机上的文件！",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    // WiFi传输模式
                    isWifiServerRunning -> {
                        Text(
                            text = "📶 WiFi传输服务器已启动",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        serverUrl?.let { url ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "在电脑浏览器中访问：",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = url,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "📱 确保手机和电脑在同一WiFi网络下\n💻 在电脑浏览器中打开上述地址\n📥 点击下载按钮保存文件到电脑",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // ADB传输模式
                    isAdbServerRunning -> {
                        Text(
                            text = "🔌 ADB传输服务器已启动",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "端口：12345",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Text(
                            text = "📱 在电脑上执行以下步骤：\n\n" +
                                    "1️⃣ 确保手机已通过USB连接\n" +
                                    "2️⃣ 打开命令行/终端\n" +
                                    "3️⃣ 运行: python3 adb_transfer_client.py\n" +
                                    "4️⃣ 等待文件传输完成",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.contains("成功")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        },
        confirmButton = {
            when {
                // 未启动服务器 - 显示启动按钮
                !isWifiServerRunning && !isAdbServerRunning -> {
                    Button(
                        onClick = {
                            // 检查增量传输
                            if (viewModel.useIncrementalTransfer.value) {
                                val (newCount, _) = viewModel.getTransferStats()
                                if (newCount == 0) {
                                    message = "所有文件都已传输过"
                                    return@Button
                                }
                            }
                            
                            // 根据选择的模式启动相应服务器
                            when (transferMode) {
                                "wifi" -> {
                                    viewModel.startWifiTransfer(
                                        onSuccess = { url ->
                                            message = "服务器已启动"
                                        },
                                        onError = { error ->
                                            message = error
                                        }
                                    )
                                }
                                "adb" -> {
                                    viewModel.startAdbTransfer(
                                        onSuccess = {
                                            message = "ADB服务器已启动"
                                        },
                                        onError = { error ->
                                            message = error
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Text(if (transferMode == "wifi") "启动WiFi传输" else "启动ADB传输")
                    }
                }
                // 服务器已启动 - 显示完成按钮
                else -> {
                    Button(
                        onClick = {
                            isTransferring = true
                            viewModel.cutTransferFiles { result ->
                                message = result
                                isTransferring = false
                                // 延迟关闭对话框
                                coroutineScope.launch {
                                    delay(2000)
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isTransferring,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isTransferring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isTransferring) "删除中..." else "完成传输并删除")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.stopFileTransfer()
                    onDismiss()
                },
                enabled = !isTransferring
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 传输历史对话框
 */
@Composable
fun TransferHistoryDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit
) {
    val transferRecords = remember { viewModel.getAllTransferRecords() }
    val transferCount = transferRecords.size
    val lastTransferTime = viewModel.getLastTransferTime()
    var showClearConfirmation by remember { mutableStateOf(false) }
    
    // 格式化时间
    fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "从未传输"
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(date)
    }
    
    // 格式化文件大小
    fun formatSize(size: Long): String {
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.History, contentDescription = null)
        },
        title = {
            Text("传输历史")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 统计信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "已传输文件",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$transferCount 个",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (lastTransferTime > 0) {
                            Text(
                                text = "上次传输：${formatTime(lastTransferTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 历史记录列表
                if (transferRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "暂无传输历史",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "启用增量传输后，已传输的文件\n将会显示在这里",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = "传输记录",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transferRecords.size) { index ->
                            val record = transferRecords[index]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = record.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatSize(record.fileSize),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatTime(record.transferTime),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (transferRecords.isNotEmpty()) {
                Button(
                    onClick = { showClearConfirmation = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清除历史")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
    
    // 清除确认对话框
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null)
            },
            title = {
                Text("确认清除历史")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("确定要清除所有传输历史吗？")
                    Text(
                        text = "这将删除 $transferCount 条记录。\n下次使用增量传输时，所有文件都会被重新传输。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearTransferHistory()
                        showClearConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 传输进度指示器
 */
@Composable
fun TransferProgressIndicator(
    progress: TransferProgressState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "正在传输",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "${progress.completedFiles}/${progress.totalFiles}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 当前文件名
            if (progress.currentFileName.isNotEmpty()) {
                Text(
                    text = progress.currentFileName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 进度条
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = progress.overallProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${progress.progressPercentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "${progress.formattedTransferredSize} / ${progress.formattedTotalSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 速度和剩余时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = progress.formattedSpeed,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = progress.formattedTimeRemaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 批量删除确认对话框
 */
@Composable
fun BatchDeleteDialog(
    viewModel: MediaViewModel,
    selectedCount: Int,
    onDismiss: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = {
            Text("批量删除")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "确定要删除选中的 $selectedCount 个文件吗？",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "⚠️ 警告：删除操作不可撤销！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.contains("成功")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isDeleting = true
                    viewModel.deleteSelectedFiles { result ->
                        message = result
                        isDeleting = false
                        coroutineScope.launch {
                            delay(1500)
                            onDismiss()
                        }
                    }
                },
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isDeleting) "删除中..." else "确认删除")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 分享文件对话框
 */
@Composable
fun ShareFilesDialog(
    viewModel: MediaViewModel,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    val selectedFiles = viewModel.getSelectedMediaItems()
    
    LaunchedEffect(Unit) {
        // 创建分享Intent
        val shareIntent = android.content.Intent().apply {
            if (selectedFiles.size == 1) {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_STREAM, selectedFiles[0].uri)
                type = selectedFiles[0].mimeType
            } else {
                action = android.content.Intent.ACTION_SEND_MULTIPLE
                val uris = ArrayList(selectedFiles.map { it.uri })
                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                type = "*/*"
            }
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            val chooserIntent = android.content.Intent.createChooser(shareIntent, "分享到")
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "分享失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        // 关闭对话框
        onDismiss()
    }
    
    // 显示加载提示
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("正在准备分享...") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {}
    )
}


