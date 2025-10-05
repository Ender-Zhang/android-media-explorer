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
    
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(MediaFilter.ALL)
    val currentFilter: StateFlow<MediaFilter> = _currentFilter.asStateFlow()
    
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
}


