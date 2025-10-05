package com.mediaexplorer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 传输进度数据类
 */
data class TransferProgressState(
    val isTransferring: Boolean = false,
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 0,
    val currentFileName: String = "",
    val currentFileProgress: Float = 0f,
    val overallProgress: Float = 0f,
    val transferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val transferSpeed: Double = 0.0, // MB/s
    val estimatedTimeRemaining: Long = 0L, // 秒
    val completedFiles: Int = 0,
    val failedFiles: Int = 0
) {
    val progressPercentage: Int
        get() = (overallProgress * 100).toInt()
    
    val formattedSpeed: String
        get() = String.format("%.2f MB/s", transferSpeed)
    
    val formattedTimeRemaining: String
        get() {
            if (estimatedTimeRemaining <= 0) return "计算中..."
            val minutes = estimatedTimeRemaining / 60
            val seconds = estimatedTimeRemaining % 60
            return when {
                minutes > 60 -> String.format("约 %d 小时", minutes / 60)
                minutes > 0 -> String.format("约 %d 分 %d 秒", minutes, seconds)
                else -> String.format("约 %d 秒", seconds)
            }
        }
    
    val formattedTransferredSize: String
        get() = formatBytes(transferredBytes)
    
    val formattedTotalSize: String
        get() = formatBytes(totalBytes)
    
    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
}

/**
 * 传输进度管理器
 */
class TransferProgressManager {
    private val _progressState = MutableStateFlow(TransferProgressState())
    val progressState: StateFlow<TransferProgressState> = _progressState.asStateFlow()
    
    private var startTime: Long = 0
    private var lastUpdateTime: Long = 0
    private var lastTransferredBytes: Long = 0
    
    /**
     * 开始传输
     */
    fun startTransfer(totalFiles: Int, totalBytes: Long) {
        startTime = System.currentTimeMillis()
        lastUpdateTime = startTime
        lastTransferredBytes = 0
        
        _progressState.value = TransferProgressState(
            isTransferring = true,
            totalFiles = totalFiles,
            totalBytes = totalBytes
        )
    }
    
    /**
     * 更新当前文件进度
     */
    fun updateFileProgress(
        fileIndex: Int,
        fileName: String,
        fileTransferredBytes: Long,
        fileTotalBytes: Long
    ) {
        val currentState = _progressState.value
        val currentTime = System.currentTimeMillis()
        
        // 计算文件进度
        val fileProgress = if (fileTotalBytes > 0) {
            fileTransferredBytes.toFloat() / fileTotalBytes
        } else 0f
        
        // 计算总体已传输字节
        val totalTransferredBytes = calculateTotalTransferredBytes(
            currentState, 
            fileIndex, 
            fileTransferredBytes
        )
        
        // 计算总体进度
        val overallProgress = if (currentState.totalBytes > 0) {
            totalTransferredBytes.toFloat() / currentState.totalBytes
        } else 0f
        
        // 计算传输速度（每秒更新一次）
        val speed = if (currentTime - lastUpdateTime >= 1000) {
            val bytesDiff = totalTransferredBytes - lastTransferredBytes
            val timeDiff = (currentTime - lastUpdateTime) / 1000.0
            lastTransferredBytes = totalTransferredBytes
            lastUpdateTime = currentTime
            (bytesDiff / timeDiff) / (1024 * 1024) // 转换为 MB/s
        } else {
            currentState.transferSpeed
        }
        
        // 估算剩余时间
        val remainingBytes = currentState.totalBytes - totalTransferredBytes
        val estimatedTime = if (speed > 0) {
            (remainingBytes / (speed * 1024 * 1024)).toLong()
        } else 0L
        
        _progressState.value = currentState.copy(
            currentFileIndex = fileIndex,
            currentFileName = fileName,
            currentFileProgress = fileProgress,
            overallProgress = overallProgress,
            transferredBytes = totalTransferredBytes,
            transferSpeed = speed,
            estimatedTimeRemaining = estimatedTime
        )
    }
    
    /**
     * 完成一个文件
     */
    fun completeFile(success: Boolean) {
        val currentState = _progressState.value
        _progressState.value = currentState.copy(
            completedFiles = if (success) currentState.completedFiles + 1 else currentState.completedFiles,
            failedFiles = if (!success) currentState.failedFiles + 1 else currentState.failedFiles
        )
    }
    
    /**
     * 完成传输
     */
    fun completeTransfer() {
        _progressState.value = _progressState.value.copy(
            isTransferring = false,
            overallProgress = 1f
        )
    }
    
    /**
     * 取消传输
     */
    fun cancelTransfer() {
        _progressState.value = TransferProgressState()
    }
    
    /**
     * 重置进度
     */
    fun reset() {
        _progressState.value = TransferProgressState()
    }
    
    /**
     * 计算总体已传输字节数
     */
    private fun calculateTotalTransferredBytes(
        currentState: TransferProgressState,
        currentFileIndex: Int,
        currentFileTransferred: Long
    ): Long {
        // 简化计算：假设前面的文件都已完成
        // 实际应用中可以维护每个文件的大小列表
        return currentState.transferredBytes + currentFileTransferred
    }
}

