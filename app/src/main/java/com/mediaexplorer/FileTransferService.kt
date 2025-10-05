package com.mediaexplorer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

/**
 * 后台文件传输服务
 * 支持断点续传和后台传输
 */
class FileTransferService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var transferJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private lateinit var notificationManager: NotificationManager
    private lateinit var transferStateManager: TransferStateManager
    private lateinit var adbTransferServer: AdbTransferServer
    
    companion object {
        private const val CHANNEL_ID = "file_transfer_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_TRANSFER = "com.mediaexplorer.START_TRANSFER"
        const val ACTION_STOP_TRANSFER = "com.mediaexplorer.STOP_TRANSFER"
        const val ACTION_PAUSE_TRANSFER = "com.mediaexplorer.PAUSE_TRANSFER"
        const val ACTION_RESUME_TRANSFER = "com.mediaexplorer.RESUME_TRANSFER"
        
        const val EXTRA_TRANSFER_MODE = "transfer_mode"
        const val EXTRA_FILE_LIST = "file_list"
        
        const val MODE_ADB = "adb"
        const val MODE_WIFI = "wifi"
        
        /**
         * 启动传输服务
         */
        fun startTransfer(context: Context, mode: String, files: List<MediaItem>) {
            val intent = Intent(context, FileTransferService::class.java).apply {
                action = ACTION_START_TRANSFER
                putExtra(EXTRA_TRANSFER_MODE, mode)
                // 注意：这里应该传递可序列化的数据，实际使用时可能需要调整
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 停止传输服务
         */
        fun stopTransfer(context: Context) {
            val intent = Intent(context, FileTransferService::class.java).apply {
                action = ACTION_STOP_TRANSFER
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        transferStateManager = TransferStateManager(this)
        adbTransferServer = AdbTransferServer(this)
        
        createNotificationChannel()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRANSFER -> {
                val mode = intent.getStringExtra(EXTRA_TRANSFER_MODE) ?: MODE_ADB
                startForeground(NOTIFICATION_ID, createNotification("准备传输...", 0))
                startTransferProcess(mode)
            }
            ACTION_STOP_TRANSFER -> {
                stopTransferProcess()
                stopSelf()
            }
            ACTION_PAUSE_TRANSFER -> {
                pauseTransferProcess()
            }
            ACTION_RESUME_TRANSFER -> {
                resumeTransferProcess()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopTransferProcess()
        releaseWakeLock()
        scope.cancel()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "文件传输",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "文件传输进度通知"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(text: String, progress: Int): android.app.Notification {
        val stopIntent = Intent(this, FileTransferService::class.java).apply {
            action = ACTION_STOP_TRANSFER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("文件传输中")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "停止",
                stopPendingIntent
            )
            .build()
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification(text: String, progress: Int) {
        val notification = createNotification(text, progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 获取唤醒锁
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MediaExplorer::TransferWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10分钟超时
    }
    
    /**
     * 释放唤醒锁
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
    
    /**
     * 开始传输过程
     */
    private fun startTransferProcess(mode: String) {
        transferJob = scope.launch {
            try {
                when (mode) {
                    MODE_ADB -> startAdbTransfer()
                    MODE_WIFI -> startWifiTransfer()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateNotification("传输失败: ${e.message}", 0)
                delay(3000)
                stopSelf()
            }
        }
    }
    
    /**
     * 停止传输过程
     */
    private fun stopTransferProcess() {
        transferJob?.cancel()
        adbTransferServer.stop()
    }
    
    /**
     * 暂停传输
     */
    private fun pauseTransferProcess() {
        // 暂停逻辑：保存当前状态
        updateNotification("传输已暂停", 0)
    }
    
    /**
     * 恢复传输
     */
    private fun resumeTransferProcess() {
        // 恢复逻辑：从保存的状态恢复
        updateNotification("恢复传输...", 0)
    }
    
    /**
     * ADB传输
     */
    private suspend fun startAdbTransfer() {
        updateNotification("等待电脑连接...", 0)
        
        // 检查是否有未完成的传输
        val hasIncomplete = transferStateManager.hasIncompleteTransfer()
        if (hasIncomplete) {
            val progress = (transferStateManager.getTransferProgress() * 100).toInt()
            updateNotification("从断点恢复传输...", progress)
        }
        
        // ADB服务器会在后台运行，等待客户端连接
        // 这里只需要保持服务活跃
        while (transferJob?.isActive == true) {
            delay(1000)
            
            // 更新进度（如果有的话）
            val progress = (transferStateManager.getTransferProgress() * 100).toInt()
            val incompleteFiles = transferStateManager.getIncompleteFiles()
            val text = if (incompleteFiles.isNotEmpty()) {
                "正在传输... (${incompleteFiles.size} 个文件待传输)"
            } else {
                "等待电脑连接..."
            }
            updateNotification(text, progress)
        }
    }
    
    /**
     * WiFi传输
     */
    private suspend fun startWifiTransfer() {
        updateNotification("WiFi传输服务已启动", 0)
        
        // WiFi服务器在后台运行
        while (transferJob?.isActive == true) {
            delay(1000)
        }
    }
}

