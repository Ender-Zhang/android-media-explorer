package com.mediaexplorer

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 传输历史记录管理
 * 用于增量传输功能
 */
class TransferHistory(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "transfer_history",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_TRANSFERRED_FILES = "transferred_files"
        private const val KEY_LAST_TRANSFER_TIME = "last_transfer_time"
    }
    
    /**
     * 传输记录数据类
     */
    data class TransferRecord(
        val fileId: Long,
        val filePath: String,
        val fileName: String,
        val fileSize: Long,
        val dateModified: Long,
        val transferTime: Long
    )
    
    /**
     * 记录已传输的文件
     */
    suspend fun recordTransfer(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val existingRecords = getTransferredFiles().toMutableMap()
        val currentTime = System.currentTimeMillis()
        
        items.forEach { item ->
            val record = TransferRecord(
                fileId = item.id,
                filePath = item.path,
                fileName = item.displayName,
                fileSize = item.size,
                dateModified = item.dateModified,
                transferTime = currentTime
            )
            existingRecords[item.path] = record
        }
        
        saveTransferredFiles(existingRecords)
        prefs.edit().putLong(KEY_LAST_TRANSFER_TIME, currentTime).apply()
    }
    
    /**
     * 获取所有已传输的文件记录
     */
    fun getTransferredFiles(): Map<String, TransferRecord> {
        val jsonString = prefs.getString(KEY_TRANSFERRED_FILES, null) ?: return emptyMap()
        val records = mutableMapOf<String, TransferRecord>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val record = TransferRecord(
                    fileId = jsonObject.getLong("fileId"),
                    filePath = jsonObject.getString("filePath"),
                    fileName = jsonObject.getString("fileName"),
                    fileSize = jsonObject.getLong("fileSize"),
                    dateModified = jsonObject.getLong("dateModified"),
                    transferTime = jsonObject.getLong("transferTime")
                )
                records[record.filePath] = record
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return records
    }
    
    /**
     * 保存已传输的文件记录
     */
    private fun saveTransferredFiles(records: Map<String, TransferRecord>) {
        val jsonArray = JSONArray()
        
        records.values.forEach { record ->
            val jsonObject = JSONObject().apply {
                put("fileId", record.fileId)
                put("filePath", record.filePath)
                put("fileName", record.fileName)
                put("fileSize", record.fileSize)
                put("dateModified", record.dateModified)
                put("transferTime", record.transferTime)
            }
            jsonArray.put(jsonObject)
        }
        
        prefs.edit().putString(KEY_TRANSFERRED_FILES, jsonArray.toString()).apply()
    }
    
    /**
     * 检查文件是否已传输
     */
    fun isFileTransferred(item: MediaItem): Boolean {
        val records = getTransferredFiles()
        val record = records[item.path] ?: return false
        
        // 检查文件是否被修改过
        return record.dateModified == item.dateModified && record.fileSize == item.size
    }
    
    /**
     * 过滤出未传输的文件（用于增量传输）
     */
    fun filterNewFiles(items: List<MediaItem>): List<MediaItem> {
        return items.filter { !isFileTransferred(it) }
    }
    
    /**
     * 获取已传输的文件数量
     */
    fun getTransferredCount(): Int {
        return getTransferredFiles().size
    }
    
    /**
     * 获取上次传输时间
     */
    fun getLastTransferTime(): Long {
        return prefs.getLong(KEY_LAST_TRANSFER_TIME, 0)
    }
    
    /**
     * 清除所有传输历史
     */
    fun clearHistory() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 删除指定文件的传输记录
     */
    fun removeTransferRecord(filePath: String) {
        val records = getTransferredFiles().toMutableMap()
        records.remove(filePath)
        saveTransferredFiles(records)
    }
    
    /**
     * 批量删除传输记录
     */
    fun removeTransferRecords(filePaths: List<String>) {
        val records = getTransferredFiles().toMutableMap()
        filePaths.forEach { records.remove(it) }
        saveTransferredFiles(records)
    }
}

