/**
 * MainViewModel.kt
 *
 * Main ViewModel for the MAL Image Downloader app.
 * Manages UI state, handles business logic, and coordinates between different app components.
 *
 * Features:
 * - UI state management for all screens
 * - Age verification state handling
 * - Download progress monitoring
 * - Settings and configuration management
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Show age verification dialog
     */
    fun showAgeVerification() {
        _uiState.value = _uiState.value.copy(showAgeVerification = true)
    }

    /**
     * Hide age verification dialog
     */
    fun hideAgeVerification() {
        _uiState.value = _uiState.value.copy(showAgeVerification = false)
    }

    /**
     * Update loading state
     */
    fun setLoading(isLoading: Boolean, message: String = "") {
        _uiState.value = _uiState.value.copy(
            isLoading = isLoading,
            loadingMessage = message
        )
    }

    /**
     * Show error message
     */
    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            showError = true
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            showError = false
        )
    }

    /**
     * Show success message
     */
    fun showSuccess(message: String) {
        _uiState.value = _uiState.value.copy(
            successMessage = message,
            showSuccess = true
        )
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            showSuccess = false
        )
    }

    /**
     * Update download statistics
     */
    fun updateDownloadStats(stats: DownloadStats) {
        _uiState.value = _uiState.value.copy(downloadStats = stats)
    }

    /**
     * Execute long-running operation with loading state
     */
    fun executeWithLoading(
        loadingMessage: String,
        operation: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                setLoading(true, loadingMessage)
                operation()
            } catch (e: Exception) {
                showError(e.message ?: "An error occurred")
            } finally {
                setLoading(false)
            }
        }
    }
}

/**
 * UI state for the main app
 */
data class MainUiState(
    val showAgeVerification: Boolean = false,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val errorMessage: String? = null,
    val showError: Boolean = false,
    val successMessage: String? = null,
    val showSuccess: Boolean = false,
    val downloadStats: DownloadStats = DownloadStats()
)

/**
 * Download statistics
 */
data class DownloadStats(
    val totalDownloads: Int = 0,
    val completedDownloads: Int = 0,
    val failedDownloads: Int = 0,
    val activeDownloads: Int = 0
) {
    val successRate: Float
        get() = if (totalDownloads > 0) completedDownloads.toFloat() / totalDownloads else 0f
    
    val isCompleted: Boolean
        get() = totalDownloads > 0 && activeDownloads == 0
}