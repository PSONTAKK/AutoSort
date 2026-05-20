package com.autosort.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autosort.data.auth.GoogleAuthManager
import com.autosort.data.config.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appConfig: AppConfig,
    val googleAuthManager: GoogleAuthManager
) : ViewModel() {

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

    private val _subscriptionTier = MutableStateFlow(appConfig.subscriptionTier)
    val subscriptionTier: StateFlow<String> = _subscriptionTier.asStateFlow()

    private val _aiCreditsUsed = MutableStateFlow(appConfig.aiCreditsUsed)
    val aiCreditsUsed: StateFlow<Int> = _aiCreditsUsed.asStateFlow()

    private val _connectedAccounts = MutableStateFlow(appConfig.connectedGoogleAccounts.toList())
    val connectedAccounts: StateFlow<List<String>> = _connectedAccounts.asStateFlow()

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
        val account = googleAuthManager.signedInAccount.value
        _googleEmail.value = account?.email ?: ""
        
        if (account?.email != null) {
            appConfig.addGoogleAccount(account.email!!)
            _connectedAccounts.value = appConfig.connectedGoogleAccounts.toList()
        }
    }

    fun signOutGoogle(emailToRemove: String? = null) {
        if (emailToRemove != null) {
            appConfig.removeGoogleAccount(emailToRemove)
            _connectedAccounts.value = appConfig.connectedGoogleAccounts.toList()
        } else {
            googleAuthManager.signOut()
            _googleEmail.value = ""
        }
    }
}
