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

data class UpdateUiState(
    val latestRelease: GitHubRelease? = null,
    val showChangelogDialog: Boolean = false,
    val isChecking: Boolean = false,
    val updateAvailable: Boolean = false,
    val infoMessage: String? = null
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
                            showChangelogDialog = true
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
                        infoMessage = if (manual) "Gagal memeriksa pembaruan" else null
                    )
                }
            )
        }
    }

    fun showChangelog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showChangelogDialog = show)
    }

    fun startDownloadAndInstall(context: Context) {
        val release = _uiState.value.latestRelease ?: return
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return

        val updater = InAppUpdater(context)
        updater.downloadAndInstall(apkAsset.downloadUrl, release.tagName)
        _uiState.value = _uiState.value.copy(showChangelogDialog = false)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
