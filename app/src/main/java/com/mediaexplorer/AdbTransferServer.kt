package com.mediaexplorer

import android.content.Context
import kotlinx.coroutines.*
import java.io.File
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * ADB传输服务器
 * 通过ADB端口转发与电脑端进行高速传输
 * 
 * 使用方法：
 * 1. 在手机上启动此服务器（端口12345）
 * 2. 在电脑上执行：adb forward tcp:12345 tcp:12345
 * 3. 运行电脑端脚本连接 localhost:12345 下载文件
 */
class AdbTransferServer(
    private val context: Context,
    private val port: Int = 12345
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var filesToTransfer = mutableListOf<MediaItem>()
    
    /**
     * 设置待传输的文件
     */
    fun setFilesToTransfer(files: List<MediaItem>) {
        filesToTransfer.clear()
        filesToTransfer.addAll(files)
    }
    
    /**
     * 启动服务器
     */
    fun start(onError: (String) -> Unit = {}) {
        if (isRunning) return
        
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept()
                        client?.let { 
                            launch { handleClient(it) }
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("ADB服务器启动失败: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 停止服务器
     */
    fun stop() {
        isRunning = false
        serverJob?.cancel()
        serverSocket?.close()
        serverSocket = null
    }
    
    /**
     * 处理客户端请求
     */
    private suspend fun handleClient(client: Socket) {
        try {
            val input = client.getInputStream().bufferedReader()
            val output = client.getOutputStream()
            
            // 读取命令
            val command = input.readLine() ?: return
            
            when {
                command == "LIST" -> {
                    // 发送文件列表
                    sendFileList(output)
                }
                command.startsWith("GET ") -> {
                    // 下载文件
                    val index = command.substringAfter("GET ").toIntOrNull()
                    if (index != null && index < filesToTransfer.size) {
                        sendFile(output, filesToTransfer[index])
                    } else {
                        sendError(output, "Invalid file index")
                    }
                }
                command.startsWith("RESUME ") -> {
                    // 断点续传
                    val parts = command.substringAfter("RESUME ").split(" ")
                    if (parts.size >= 2) {
                        val index = parts[0].toIntOrNull()
                        val offset = parts[1].toLongOrNull()
                        if (index != null && offset != null && index < filesToTransfer.size) {
                            resumeFile(output, filesToTransfer[index], offset)
                        } else {
                            sendError(output, "Invalid resume parameters")
                        }
                    } else {
                        sendError(output, "Invalid resume command")
                    }
                }
                command == "COUNT" -> {
                    // 发送文件数量
                    output.write("${filesToTransfer.size}\n".toByteArray())
                }
                else -> {
                    sendError(output, "Unknown command")
                }
            }
            
            output.flush()
            client.close()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                client.close()
            } catch (ignored: Exception) {}
        }
    }
    
    /**
     * 发送文件列表（JSON格式）
     */
    private fun sendFileList(output: OutputStream) {
        val json = buildString {
            append("[")
            filesToTransfer.forEachIndexed { index, item ->
                if (index > 0) append(",")
                append("{")
                append("\"index\":$index,")
                append("\"id\":${item.id},")
                append("\"name\":\"${escapeJson(item.displayName)}\",")
                append("\"path\":\"${escapeJson(item.path)}\",")
                append("\"size\":${item.size},")
                append("\"type\":\"${if (item.isVideo) "video" else "image"}\",")
                append("\"mimeType\":\"${item.mimeType}\",")
                append("\"dateModified\":${item.dateModified}")
                append("}")
            }
            append("]\n")
        }
        
        output.write("OK\n".toByteArray())
        output.write("${json.toByteArray().size}\n".toByteArray())
        output.write(json.toByteArray())
    }
    
    /**
     * 发送文件
     */
    private suspend fun sendFile(output: OutputStream, mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(mediaItem.uri)
            
            if (inputStream == null) {
                sendError(output, "Cannot open file")
                return@withContext
            }
            
            // 发送响应头
            output.write("OK\n".toByteArray())
            output.write("${mediaItem.size}\n".toByteArray())
            output.write("${escapeJson(mediaItem.displayName)}\n".toByteArray())
            
            // 发送文件内容
            val buffer = ByteArray(65536) // 64KB buffer for faster transfer
            var bytesRead: Int
            var totalBytes = 0L
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
            
            inputStream.close()
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            sendError(output, "Transfer error: ${e.message}")
        }
    }
    
    /**
     * 断点续传文件
     */
    private suspend fun resumeFile(output: OutputStream, mediaItem: MediaItem, offset: Long) = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(mediaItem.uri)
            
            if (inputStream == null) {
                sendError(output, "Cannot open file")
                return@withContext
            }
            
            // 跳过已传输的字节
            var skipped = 0L
            while (skipped < offset) {
                val toSkip = minOf(offset - skipped, 65536L)
                val actualSkipped = inputStream.skip(toSkip)
                if (actualSkipped <= 0) break
                skipped += actualSkipped
            }
            
            if (skipped != offset) {
                sendError(output, "Failed to seek to offset")
                inputStream.close()
                return@withContext
            }
            
            // 发送响应头
            output.write("OK\n".toByteArray())
            output.write("${mediaItem.size}\n".toByteArray())
            output.write("$offset\n".toByteArray()) // 告诉客户端从哪里开始
            output.write("${escapeJson(mediaItem.displayName)}\n".toByteArray())
            
            // 发送剩余的文件内容
            val buffer = ByteArray(65536)
            var bytesRead: Int
            var totalBytes = offset
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
            
            inputStream.close()
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            sendError(output, "Resume error: ${e.message}")
        }
    }
    
    /**
     * 发送错误消息
     */
    private fun sendError(output: OutputStream, message: String) {
        try {
            output.write("ERROR\n".toByteArray())
            output.write("$message\n".toByteArray())
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 转义JSON字符串
     */
    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    /**
     * 获取服务器状态
     */
    fun isRunning(): Boolean = isRunning
}

