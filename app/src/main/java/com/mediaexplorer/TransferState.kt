package com.mediaexplorer

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 传输状态数据类 - 用于断点续传
 */
data class TransferState(
    val transferId: String,
    val files: List<FileTransferInfo>,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

/**
 * 文件传输信息
 */
data class FileTransferInfo(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val transferredBytes: Long = 0,
    val isCompleted: Boolean = false,
    val md5Hash: String? = null
)

/**
 * 断点续传状态管理器
 */
class TransferStateManager(private val context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("transfer_state", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_CURRENT_TRANSFER = "current_transfer"
        private const val KEY_TRANSFER_HISTORY = "transfer_history"
        private const val MAX_HISTORY_SIZE = 50
    }
    
    /**
     * 保存当前传输状态
     */
    fun saveCurrentTransferState(state: TransferState) {
        val json = gson.toJson(state)
        prefs.edit().putString(KEY_CURRENT_TRANSFER, json).apply()
    }
    
    /**
     * 获取当前传输状态
     */
    fun getCurrentTransferState(): TransferState? {
        val json = prefs.getString(KEY_CURRENT_TRANSFER, null) ?: return null
        return try {
            gson.fromJson(json, TransferState::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 更新文件传输进度
     */
    fun updateFileProgress(fileIndex: Int, transferredBytes: Long) {
        val state = getCurrentTransferState() ?: return
        val updatedFiles = state.files.toMutableList()
        
        if (fileIndex < updatedFiles.size) {
            updatedFiles[fileIndex] = updatedFiles[fileIndex].copy(
                transferredBytes = transferredBytes
            )
            saveCurrentTransferState(state.copy(files = updatedFiles))
        }
    }
    
    /**
     * 标记文件为已完成
     */
    fun markFileCompleted(fileIndex: Int) {
        val state = getCurrentTransferState() ?: return
        val updatedFiles = state.files.toMutableList()
        
        if (fileIndex < updatedFiles.size) {
            updatedFiles[fileIndex] = updatedFiles[fileIndex].copy(
                isCompleted = true,
                transferredBytes = updatedFiles[fileIndex].fileSize
            )
            saveCurrentTransferState(state.copy(files = updatedFiles))
        }
    }
    
    /**
     * 标记传输为已完成
     */
    fun markTransferCompleted() {
        val state = getCurrentTransferState() ?: return
        val completedState = state.copy(isCompleted = true)
        saveCurrentTransferState(completedState)
        
        // 将完成的传输添加到历史记录
        addToHistory(completedState)
    }
    
    /**
     * 清除当前传输状态
     */
    fun clearCurrentTransferState() {
        prefs.edit().remove(KEY_CURRENT_TRANSFER).apply()
    }
    
    /**
     * 添加到历史记录
     */
    private fun addToHistory(state: TransferState) {
        val history = getTransferHistory().toMutableList()
        history.add(0, state)
        
        // 限制历史记录大小
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }
        
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_TRANSFER_HISTORY, json).apply()
    }
    
    /**
     * 获取传输历史
     */
    fun getTransferHistory(): List<TransferState> {
        val json = prefs.getString(KEY_TRANSFER_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TransferState>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 清除传输历史
     */
    fun clearTransferHistory() {
        prefs.edit().remove(KEY_TRANSFER_HISTORY).apply()
    }
    
    /**
     * 检查是否有未完成的传输
     */
    fun hasIncompleteTransfer(): Boolean {
        val state = getCurrentTransferState() ?: return false
        return !state.isCompleted
    }
    
    /**
     * 获取未完成的文件列表
     */
    fun getIncompleteFiles(): List<FileTransferInfo> {
        val state = getCurrentTransferState() ?: return emptyList()
        return state.files.filter { !it.isCompleted }
    }
    
    /**
     * 获取传输进度百分比
     */
    fun getTransferProgress(): Float {
        val state = getCurrentTransferState() ?: return 0f
        val totalBytes = state.files.sumOf { it.fileSize }
        if (totalBytes == 0L) return 0f
        
        val transferredBytes = state.files.sumOf { it.transferredBytes }
        return transferredBytes.toFloat() / totalBytes
    }
}

