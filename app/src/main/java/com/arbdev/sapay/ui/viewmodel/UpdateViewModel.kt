package com.arbdev.sapay.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbdev.sapay.BuildConfig
import com.arbdev.sapay.data.model.GitHubRelease
import com.arbdev.sapay.data.repository.UpdateRepository
import com.arbdev.sapay.updater.InAppUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val latestRelease: GitHubRelease? = null,
    val showChangelogDialog: Boolean = false,
    val isChecking: Boolean = false,
    val updateAvailable: Boolean = false,
    val infoMessage: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isDownloaded: Boolean = false,
    val downloadedApkFile: File? = null,
    val downloadError: String? = null
)

class UpdateViewModel(
    private val repository: UpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        checkForUpdate(manual = false)
    }

    fun checkForUpdate(manual: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, infoMessage = null)
            val result = repository.checkLatestRelease(BuildConfig.VERSION_NAME)
            result.fold(
                onSuccess = { release ->
                    if (release != null) {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            latestRelease = release,
                            updateAvailable = true,
                            showChangelogDialog = true,
                            infoMessage = "Pembaruan versi ${release.tagName} tersedia!"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            updateAvailable = false,
                            infoMessage = if (manual) "Aplikasi Anda sudah versi terbaru (${BuildConfig.VERSION_NAME})" else null
                        )
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        infoMessage = if (manual) "Gagal memeriksa pembaruan: Periksa koneksi internet." else null
                    )
                }
            )
        }
    }

    fun syncExistingDownload(context: Context) {
        val release = _uiState.value.latestRelease ?: return
        val updater = InAppUpdater(context)
        val existing = updater.getExistingDownloadedApk(release.tagName)
        if (existing != null) {
            _uiState.value = _uiState.value.copy(
                isDownloaded = true,
                downloadedApkFile = existing,
                downloadProgress = 1.0f,
                downloadError = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isDownloaded = false,
                downloadedApkFile = null
            )
        }
    }

    fun showChangelog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showChangelogDialog = show)
    }

    fun startDownloadAndInstall(context: Context) {
        val state = _uiState.value
        val release = state.latestRelease ?: return
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return
        val updater = InAppUpdater(context)

        // 1. Antisipasi Dobel Download jika proses sedang berjalan
        if (state.isDownloading) {
            _uiState.value = state.copy(infoMessage = "Pembaruan sedang diunduh, mohon tunggu...")
            return
        }

        // 2. Jika sudah pernah terunduh dan file valid, langsung instal (tanpa download ulang)
        val existing = state.downloadedApkFile ?: updater.getExistingDownloadedApk(release.tagName)
        if (existing != null && existing.exists() && existing.length() > 5 * 1024 * 1024) {
            _uiState.value = state.copy(
                isDownloaded = true,
                downloadedApkFile = existing,
                downloadProgress = 1.0f
            )
            updater.installApk(existing) { error ->
                _uiState.value = _uiState.value.copy(downloadError = error, infoMessage = error)
            }
            return
        }

        // 3. Jalankan proses pengunduhan via coroutine streaming
        viewModelScope.launch {
            _uiState.value = state.copy(
                isDownloading = true,
                downloadProgress = 0f,
                downloadError = null,
                isDownloaded = false,
                downloadedApkFile = null
            )

            val downloadResult = updater.downloadApk(
                apkUrl = apkAsset.downloadUrl,
                versionTag = release.tagName,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                }
            )

            downloadResult.fold(
                onSuccess = { file ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        isDownloaded = true,
                        downloadedApkFile = file,
                        downloadProgress = 1.0f,
                        downloadError = null
                    )
                    // Langsung picu pemasangan paket
                    updater.installApk(file) { error ->
                        _uiState.value = _uiState.value.copy(downloadError = error, infoMessage = error)
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        isDownloaded = false,
                        downloadError = error.message ?: "Gagal mengunduh pembaruan",
                        infoMessage = error.message ?: "Gagal mengunduh pembaruan"
                    )
                }
            )
        }
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
