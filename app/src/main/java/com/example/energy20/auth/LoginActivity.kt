package com.example.energy20.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.energy20.MainActivity
import com.example.energy20.R
import com.example.energy20.api.AuthApiService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

/**
 * LoginActivity - Handles Google Sign-In authentication
 * 
 * Flow:
 * 1. User clicks "Sign in with Google"
 * 2. Google Sign-In dialog appears
 * 3. User selects account and grants permissions
 * 4. Get ID token from Google
 * 5. Send token to backend for verification
 * 6. Backend creates/updates user and returns auth token
 * 7. Save auth data locally
 * 8. Navigate to MainActivity or DeviceOnboardingActivity
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authApiService: AuthApiService
    private lateinit var authManager: AuthManager
    
    private lateinit var signInButton: SignInButton
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView

    companion object {
        private const val TAG = "LoginActivity"
        // TODO: Replace with your actual Web Client ID from Google Cloud Console
        private const val WEB_CLIENT_ID = "242021166104-he2lbj7avj8r6ujlhc79n7i6al087idv.apps.googleusercontent.com"
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize services
        authManager = AuthManager.getInstance(this)
        authApiService = AuthApiService(this)

        // Check if already authenticated
        if (authManager.isAuthenticated()) {
            navigateToMain()
            return
        }

        // Initialize views
        signInButton = findViewById(R.id.signInButton)
        progressBar = findViewById(R.id.progressBar)
        errorTextView = findViewById(R.id.errorTextView)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up sign-in button
        signInButton.setSize(SignInButton.SIZE_WIDE)
        signInButton.setOnClickListener {
            signIn()
        }
    }

    /**
     * Start Google Sign-In flow
     */
    private fun signIn() {
        showLoading(true)
        hideError()
        
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    /**
     * Handle Google Sign-In result
     */
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                Log.d(TAG, "Got ID token, authenticating with backend...")
                authenticateWithBackend(idToken)
            } else {
                showError("Failed to get ID token from Google")
                showLoading(false)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            showError("Google sign-in failed: ${e.statusCode}")
            showLoading(false)
        }
    }

    /**
     * Authenticate with backend using Google ID token
     */
    private fun authenticateWithBackend(idToken: String) {
        lifecycleScope.launch {
            try {
                val result = authApiService.authenticate(idToken)
                
                result.onSuccess { authResponse ->
                    Log.d(TAG, "Authentication successful: ${authResponse.user.email}")
                    
                    // Save auth data
                    authManager.saveAuthData(
                        token = authResponse.token,
                        user = authResponse.user,
                        devices = authResponse.devices
                    )
                    
                    // Navigate based on whether user has devices
                    if (authResponse.hasDevices) {
                        navigateToMain()
                    } else {
                        navigateToDeviceOnboarding()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Backend authentication failed", error)
                    showError("Authentication failed: ${error.message}")
                    showLoading(false)
                    
                    // Sign out from Google on backend failure
                    googleSignInClient.signOut()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Authentication error", e)
                showError("Authentication error: ${e.message}")
                showLoading(false)
                googleSignInClient.signOut()
            }
        }
    }

    /**
     * Navigate to MainActivity
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Navigate to DeviceOnboardingActivity
     */
    private fun navigateToDeviceOnboarding() {
        // TODO: Implement DeviceOnboardingActivity
        // For now, just go to MainActivity
        Log.d(TAG, "User has no devices, should show onboarding")
        navigateToMain()
    }

    /**
     * Show/hide loading indicator
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        signInButton.isEnabled = !show
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
    }

    /**
     * Hide error message
     */
    private fun hideError() {
        errorTextView.visibility = View.GONE
    }
}
