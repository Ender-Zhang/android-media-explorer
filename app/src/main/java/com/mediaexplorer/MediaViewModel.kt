package com.mediaexplorer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 媒体浏览器的 ViewModel
 */
class MediaViewModel(application: Application) : AndroidViewModel(application) {
    
    private val mediaScanner = MediaScanner(application)
    private val fileTransferServer = FileTransferServer(application)
    private val adbTransferServer = AdbTransferServer(application)
    private val transferHistory = TransferHistory(application)
    private val transferProgressManager = TransferProgressManager()
    private val transferStateManager = TransferStateManager(application)
    
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(MediaFilter.ALL)
    val currentFilter: StateFlow<MediaFilter> = _currentFilter.asStateFlow()
    
    // 选择模式相关状态
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    private val _selectedItems = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItems: StateFlow<Set<Long>> = _selectedItems.asStateFlow()
    
    // 传输服务器状态
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()
    
    private val _isAdbServerRunning = MutableStateFlow(false)
    val isAdbServerRunning: StateFlow<Boolean> = _isAdbServerRunning.asStateFlow()
    
    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()
    
    // 增量传输选项
    private val _useIncrementalTransfer = MutableStateFlow(false)
    val useIncrementalTransfer: StateFlow<Boolean> = _useIncrementalTransfer.asStateFlow()
    
    // 传输进度
    val transferProgress: StateFlow<TransferProgressState> = transferProgressManager.progressState
    
    private var allMediaItems: List<MediaItem> = emptyList()
    
    /**
     * 加载所有媒体文件
     */
    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                allMediaItems = mediaScanner.scanAllMedia()
                applyFilter()
            } catch (e: Exception) {
                e.printStackTrace()
                _mediaItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 设置过滤器
     */
    fun setFilter(filter: MediaFilter) {
        _currentFilter.value = filter
        applyFilter()
    }
    
    /**
     * 应用过滤器
     */
    private fun applyFilter() {
        _mediaItems.value = when (_currentFilter.value) {
            MediaFilter.ALL -> allMediaItems
            MediaFilter.IMAGES -> allMediaItems.filter { it.isImage }
            MediaFilter.VIDEOS -> allMediaItems.filter { it.isVideo }
        }
    }
    
    /**
     * 刷新媒体列表
     */
    fun refresh() {
        loadMedia()
    }
    
    /**
     * 切换选择模式
     */
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            // 退出选择模式时清空选择
            _selectedItems.value = emptySet()
        }
    }
    
    /**
     * 切换单个项目的选择状态
     */
    fun toggleItemSelection(itemId: Long) {
        val currentSelection = _selectedItems.value.toMutableSet()
        if (currentSelection.contains(itemId)) {
            currentSelection.remove(itemId)
        } else {
            currentSelection.add(itemId)
        }
        _selectedItems.value = currentSelection
    }
    
    /**
     * 全选
     */
    fun selectAll() {
        _selectedItems.value = _mediaItems.value.map { it.id }.toSet()
    }
    
    /**
     * 反选
     */
    fun invertSelection() {
        val currentSelection = _selectedItems.value
        val allIds = _mediaItems.value.map { it.id }.toSet()
        _selectedItems.value = allIds - currentSelection
    }
    
    /**
     * 取消全部选择
     */
    fun clearSelection() {
        _selectedItems.value = emptySet()
    }
    
    /**
     * 获取选中的媒体项
     */
    fun getSelectedMediaItems(): List<MediaItem> {
        val selectedIds = _selectedItems.value
        return _mediaItems.value.filter { it.id in selectedIds }
    }
    
    /**
     * 切换增量传输模式
     */
    fun toggleIncrementalTransfer() {
        _useIncrementalTransfer.value = !_useIncrementalTransfer.value
    }
    
    /**
     * 设置增量传输模式
     */
    fun setIncrementalTransfer(enabled: Boolean) {
        _useIncrementalTransfer.value = enabled
    }
    
    /**
     * 获取要传输的文件（考虑增量传输）
     */
    private fun getFilesToTransfer(): List<MediaItem> {
        val selectedFiles = getSelectedMediaItems()
        return if (_useIncrementalTransfer.value) {
            transferHistory.filterNewFiles(selectedFiles)
        } else {
            selectedFiles
        }
    }
    
    /**
     * 获取传输统计信息
     */
    fun getTransferStats(): Pair<Int, Int> {
        val selectedFiles = getSelectedMediaItems()
        val newFiles = transferHistory.filterNewFiles(selectedFiles)
        return Pair(newFiles.size, selectedFiles.size)
    }
    
    /**
     * 启动WiFi文件传输服务器
     */
    fun startWifiTransfer(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val filesToTransfer = getFilesToTransfer()
        if (filesToTransfer.isEmpty()) {
            if (_useIncrementalTransfer.value) {
                onError("所有选中的文件都已传输过")
            } else {
                onError("请先选择要传输的文件")
            }
            return
        }
        
        fileTransferServer.setFilesToTransfer(filesToTransfer)
        fileTransferServer.start { error ->
            onError(error)
        }
        
        val ip = fileTransferServer.getIPAddress()
        if (ip != null) {
            val url = "http://$ip:8080"
            _serverUrl.value = url
            _isServerRunning.value = true
            onSuccess(url)
        } else {
            onError("无法获取IP地址")
        }
    }
    
    /**
     * 启动ADB文件传输服务器
     */
    fun startAdbTransfer(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val filesToTransfer = getFilesToTransfer()
        if (filesToTransfer.isEmpty()) {
            if (_useIncrementalTransfer.value) {
                onError("所有选中的文件都已传输过")
            } else {
                onError("请先选择要传输的文件")
            }
            return
        }
        
        // 保存传输状态以支持断点续传
        saveTransferState(filesToTransfer)
        
        adbTransferServer.setFilesToTransfer(filesToTransfer)
        adbTransferServer.start { error ->
            onError(error)
        }
        
        _isAdbServerRunning.value = true
        onSuccess()
    }
    
    /**
     * 保存传输状态
     */
    private fun saveTransferState(files: List<MediaItem>) {
        val fileInfoList = files.map { item ->
            FileTransferInfo(
                filePath = item.path,
                fileName = item.displayName,
                fileSize = item.size,
                transferredBytes = 0L,
                isCompleted = false
            )
        }
        
        val transferState = TransferState(
            transferId = System.currentTimeMillis().toString(),
            files = fileInfoList
        )
        
        transferStateManager.saveCurrentTransferState(transferState)
    }
    
    /**
     * 检查是否有未完成的传输
     */
    fun hasIncompleteTransfer(): Boolean {
        return transferStateManager.hasIncompleteTransfer()
    }
    
    /**
     * 获取传输进度
     */
    fun getTransferStateProgress(): Float {
        return transferStateManager.getTransferProgress()
    }
    
    /**
     * 清除传输状态
     */
    fun clearTransferState() {
        transferStateManager.clearCurrentTransferState()
    }
    
    /**
     * 停止文件传输服务器
     */
    fun stopFileTransfer() {
        fileTransferServer.stop()
        adbTransferServer.stop()
        _isServerRunning.value = false
        _isAdbServerRunning.value = false
        _serverUrl.value = null
    }
    
    /**
     * 记录传输历史
     */
    suspend fun recordTransferHistory() {
        val files = getFilesToTransfer()
        transferHistory.recordTransfer(files)
    }
    
    /**
     * 剪切传输：传输完成后删除文件
     */
    fun cutTransferFiles(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val filesToDelete = getFilesToTransfer()
            if (filesToDelete.isEmpty()) {
                onResult("没有要删除的文件")
                return@launch
            }
            
            // 记录传输历史
            recordTransferHistory()
            
            // 删除文件
            val (successCount, failCount) = fileTransferServer.deleteTransferredFiles(filesToDelete)
            
            // 停止服务器
            stopFileTransfer()
            
            // 清空选择
            clearSelection()
            
            // 刷新列表
            refresh()
            
            // 返回结果
            val message = if (failCount > 0) {
                "已删除 $successCount 个文件，失败 $failCount 个"
            } else {
                "已成功删除 $successCount 个文件"
            }
            onResult(message)
        }
    }
    
    /**
     * 仅记录传输（不删除文件）
     */
    fun recordTransferOnly(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val files = getFilesToTransfer()
            if (files.isEmpty()) {
                onResult("没有要记录的文件")
                return@launch
            }
            
            // 记录传输历史
            recordTransferHistory()
            
            // 停止服务器
            stopFileTransfer()
            
            // 清空选择
            clearSelection()
            
            onResult("已记录 ${files.size} 个文件的传输历史")
        }
    }
    
    /**
     * 清除传输历史
     */
    fun clearTransferHistory() {
        transferHistory.clearHistory()
    }
    
    /**
     * 获取传输历史统计
     */
    fun getTransferHistoryCount(): Int {
        return transferHistory.getTransferredCount()
    }
    
    /**
     * 获取上次传输时间
     */
    fun getLastTransferTime(): Long {
        return transferHistory.getLastTransferTime()
    }
    
    /**
     * 获取所有传输记录
     */
    fun getAllTransferRecords(): List<TransferHistory.TransferRecord> {
        return transferHistory.getTransferredFiles().values.toList()
            .sortedByDescending { it.transferTime }
    }
    
    /**
     * 批量删除选中的文件
     */
    fun deleteSelectedFiles(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val filesToDelete = getSelectedMediaItems()
            if (filesToDelete.isEmpty()) {
                onResult("没有选中的文件")
                return@launch
            }
            
            val (successCount, failCount) = fileTransferServer.deleteTransferredFiles(filesToDelete)
            
            // 清空选择
            clearSelection()
            
            // 退出选择模式
            _isSelectionMode.value = false
            
            // 刷新列表
            refresh()
            
            val message = if (failCount > 0) {
                "已删除 $successCount 个文件，失败 $failCount 个"
            } else {
                "已成功删除 $successCount 个文件"
            }
            onResult(message)
        }
    }
    
    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        stopFileTransfer()
    }
}


