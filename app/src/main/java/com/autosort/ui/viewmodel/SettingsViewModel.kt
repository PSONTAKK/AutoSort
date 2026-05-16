package com.autosort.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autosort.data.auth.GoogleAuthManager
import com.autosort.data.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appConfig = AppConfig(application)
    val googleAuthManager = GoogleAuthManager.getInstance(application)

    val sourceFolder: StateFlow<String> = appConfig.sourceFolderFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = appConfig.sourceFolder
        )

    // ── Google Drive state ────────────────────────────────────────────────

    val isGoogleLinked: StateFlow<Boolean> = googleAuthManager.signedInAccount
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = googleAuthManager.isSignedIn
        )

    private val _googleEmail = MutableStateFlow(
        googleAuthManager.signedInAccount.value?.email ?: ""
    )
    val googleEmail: StateFlow<String> = _googleEmail.asStateFlow()

    fun updateSourceFolder(path: String) {
        appConfig.sourceFolder = path
    }

    fun getGoogleSignInIntent(): Intent = googleAuthManager.getSignInIntent()

    fun handleGoogleSignInResult(data: Intent?) {
        googleAuthManager.handleSignInResult(data)
        _googleEmail.value = googleAuthManager.signedInAccount.value?.email ?: ""
    }

    fun signOutGoogle() {
        googleAuthManager.signOut()
        _googleEmail.value = ""
    }
}
