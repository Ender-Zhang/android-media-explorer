package com.mediaexplorer

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * 文件传输服务器
 * 使用简单的HTTP协议传输文件到电脑
 */
class FileTransferServer(
    private val context: Context,
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 待传输的文件列表
    private var filesToTransfer = mutableListOf<MediaItem>()
    
    /**
     * 获取本机IP地址
     */
    fun getIPAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
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
                        client?.let { handleClient(it) }
                    } catch (e: Exception) {
                        if (isRunning) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("服务器启动失败: ${e.message}")
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
    private fun handleClient(client: Socket) {
        try {
            val input = client.getInputStream().bufferedReader()
            val output = client.getOutputStream()
            
            // 读取HTTP请求
            val requestLine = input.readLine()
            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendResponse(output, 400, "Bad Request", "")
                client.close()
                return
            }
            
            // 读取所有请求头
            while (true) {
                val line = input.readLine()
                if (line.isNullOrEmpty()) break
            }
            
            // 解析请求路径
            val path = requestLine.split(" ")[1]
            
            when {
                path == "/" -> {
                    // 返回文件列表页面
                    sendFileListPage(output)
                }
                path.startsWith("/download/") -> {
                    // 下载文件
                    val index = path.substringAfter("/download/").toIntOrNull()
                    if (index != null && index < filesToTransfer.size) {
                        sendFile(output, filesToTransfer[index])
                    } else {
                        sendResponse(output, 404, "Not Found", "File not found")
                    }
                }
                path == "/list" -> {
                    // 返回JSON格式的文件列表
                    sendFileListJson(output)
                }
                else -> {
                    sendResponse(output, 404, "Not Found", "Page not found")
                }
            }
            
            client.close()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                client.close()
            } catch (ignored: Exception) {}
        }
    }
    
    /**
     * 发送文件列表页面
     */
    private fun sendFileListPage(output: OutputStream) {
        val html = buildString {
            append("<!DOCTYPE html>\n")
            append("<html><head><meta charset='UTF-8'><title>媒体文件传输</title>")
            append("<style>")
            append("body { font-family: Arial, sans-serif; max-width: 1200px; margin: 50px auto; padding: 20px; }")
            append("h1 { color: #333; }")
            append(".file-list { list-style: none; padding: 0; }")
            append(".file-item { background: #f5f5f5; margin: 10px 0; padding: 15px; border-radius: 5px; display: flex; justify-content: space-between; align-items: center; }")
            append(".file-info { flex: 1; }")
            append(".file-name { font-weight: bold; color: #0066cc; }")
            append(".file-details { color: #666; font-size: 0.9em; margin-top: 5px; }")
            append(".btn { background: #0066cc; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin-left: 10px; }")
            append(".btn:hover { background: #0052a3; }")
            append(".btn-all { background: #28a745; }")
            append(".btn-all:hover { background: #218838; }")
            append(".summary { background: #e8f4f8; padding: 15px; border-radius: 5px; margin-bottom: 20px; }")
            append("</style></head><body>")
            append("<h1>📱 媒体文件传输</h1>")
            append("<div class='summary'>")
            append("<p><strong>待传输文件数量：</strong>${filesToTransfer.size}</p>")
            append("<a href='#' onclick='downloadAll(); return false;' class='btn btn-all'>全部下载</a>")
            append("</div>")
            append("<ul class='file-list'>")
            
            filesToTransfer.forEachIndexed { index, item ->
                append("<li class='file-item'>")
                append("<div class='file-info'>")
                append("<div class='file-name'>${item.displayName}</div>")
                append("<div class='file-details'>")
                append("类型: ${if (item.isVideo) "视频" else "图片"} | ")
                append("大小: ${item.formattedSize} | ")
                if (item.resolution.isNotEmpty()) {
                    append("分辨率: ${item.resolution} | ")
                }
                if (item.formattedDuration.isNotEmpty()) {
                    append("时长: ${item.formattedDuration} | ")
                }
                append("修改时间: ${item.formattedDate}")
                append("</div></div>")
                append("<a href='/download/$index' class='btn'>下载</a>")
                append("</li>")
            }
            
            append("</ul>")
            append("<script>")
            append("function downloadAll() {")
            append("  for (let i = 0; i < ${filesToTransfer.size}; i++) {")
            append("    setTimeout(() => { window.location.href = '/download/' + i; }, i * 500);")
            append("  }")
            append("}")
            append("</script>")
            append("</body></html>")
        }
        
        sendResponse(output, 200, "OK", html, "text/html; charset=UTF-8")
    }
    
    /**
     * 发送JSON格式的文件列表
     */
    private fun sendFileListJson(output: OutputStream) {
        val json = buildString {
            append("{\"files\":[")
            filesToTransfer.forEachIndexed { index, item ->
                if (index > 0) append(",")
                append("{")
                append("\"index\":$index,")
                append("\"name\":\"${item.displayName}\",")
                append("\"size\":${item.size},")
                append("\"type\":\"${if (item.isVideo) "video" else "image"}\",")
                append("\"url\":\"/download/$index\"")
                append("}")
            }
            append("]}")
        }
        
        sendResponse(output, 200, "OK", json, "application/json; charset=UTF-8")
    }
    
    /**
     * 发送文件
     */
    private fun sendFile(output: OutputStream, mediaItem: MediaItem) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(mediaItem.uri)
            
            if (inputStream == null) {
                sendResponse(output, 500, "Internal Server Error", "Cannot open file")
                return
            }
            
            // 发送HTTP响应头
            val headers = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: ${mediaItem.mimeType}\r\n")
                append("Content-Length: ${mediaItem.size}\r\n")
                append("Content-Disposition: attachment; filename=\"${mediaItem.displayName}\"\r\n")
                append("\r\n")
            }
            output.write(headers.toByteArray())
            
            // 发送文件内容
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            
            inputStream.close()
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 发送HTTP响应
     */
    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusMessage: String,
        body: String,
        contentType: String = "text/plain; charset=UTF-8"
    ) {
        val response = buildString {
            append("HTTP/1.1 $statusCode $statusMessage\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.toByteArray().size}\r\n")
            append("\r\n")
            append(body)
        }
        output.write(response.toByteArray())
        output.flush()
    }
    
    /**
     * 删除已传输的文件
     */
    suspend fun deleteTransferredFiles(files: List<MediaItem>): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var successCount = 0
        var failCount = 0
        
        files.forEach { item ->
            try {
                val deleted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+
                    context.contentResolver.delete(item.uri, null, null) > 0
                } else {
                    // Android 10及以下
                    val file = File(item.path)
                    file.exists() && file.delete()
                }
                
                if (deleted) {
                    successCount++
                } else {
                    failCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
                failCount++
            }
        }
        
        Pair(successCount, failCount)
    }
}

