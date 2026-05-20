package com.autosort.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Sign-In and authorization for Drive access.
 * SINGLETON — shared between AutoSortService and SettingsViewModel
 * so sign-in state is consistent everywhere.
 */
class GoogleAuthManager private constructor(private val context: Context) {

    companion object {
        // Fix 1: Client ID injected via BuildConfig from local.properties (gitignored)
        val WEB_CLIENT_ID: String = com.autosort.BuildConfig.WEB_CLIENT_ID

        @Volatile
        private var INSTANCE: GoogleAuthManager? = null

        fun getInstance(context: Context): GoogleAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleAuthManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .requestServerAuthCode(WEB_CLIENT_ID)
        .build()

    private val googleSignInClient: GoogleSignInClient =
        GoogleSignIn.getClient(context, gso)

    private val _signedInAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val signedInAccount: StateFlow<GoogleSignInAccount?> = _signedInAccount.asStateFlow()

    val isSignedIn: Boolean
        get() = _signedInAccount.value != null

    init {
        // Check if already signed in from a previous session
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && !lastAccount.isExpired) {
            _signedInAccount.value = lastAccount
        }
    }

    /**
     * Returns the Intent to launch the Google Sign-In UI.
     */
    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    /**
     * Call this from the ActivityResult callback after sign-in completes.
     */
    fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            _signedInAccount.value = account
            android.util.Log.i("GoogleAuth", "Sign-in SUCCESS: ${account.email}")
        } catch (e: com.google.android.gms.common.api.ApiException) {
            android.util.Log.e("GoogleAuth", "Sign-in FAILED: statusCode=${e.statusCode} message=${e.message}")
            _signedInAccount.value = null
        } catch (e: Exception) {
            android.util.Log.e("GoogleAuth", "Sign-in FAILED: ${e.message}", e)
            _signedInAccount.value = null
        }
    }

    /**
     * Sign out and clear state.
     */
    fun signOut() {
        googleSignInClient.signOut()
        _signedInAccount.value = null
    }

    /**
     * Returns a GoogleAccountCredential suitable for initializing the
     * Drive service. If email is provided, creates a credential for that specific account.
     */
    fun getCredential(email: String? = null): GoogleAccountCredential? {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        
        if (email != null && email.isNotBlank()) {
            credential.selectedAccountName = email
            return credential
        } else {
            val account = _signedInAccount.value ?: return null
            credential.selectedAccount = account.account
            return credential
        }
    }
}
