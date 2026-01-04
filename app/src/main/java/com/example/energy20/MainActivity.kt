package com.example.energy20

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.energy20.auth.AuthInterceptor
import com.example.energy20.auth.AuthManager
import com.example.energy20.auth.LoginActivity
import com.example.energy20.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "MainActivity"
        // Must match the WEB_CLIENT_ID in LoginActivity
        private const val WEB_CLIENT_ID = "242021166104-he2lbj7avj8r6ujlhc79n7i6al087idv.apps.googleusercontent.com"
    }
    
    /**
     * BroadcastReceiver to handle token expiration events
     */
    private val tokenExpirationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AuthInterceptor.ACTION_TOKEN_EXPIRED) {
                Log.w(TAG, "Token expiration broadcast received - redirecting to login")
                handleTokenExpiration()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AuthManager
        authManager = AuthManager.getInstance(this)

        // Check authentication - redirect to login if not authenticated
        if (!authManager.isAuthenticated()) {
            navigateToLogin()
            return
        }

        // Register broadcast receiver for token expiration
        val filter = IntentFilter(AuthInterceptor.ACTION_TOKEN_EXPIRED)
        registerReceiver(tokenExpirationReceiver, filter, RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "Token expiration receiver registered")

        // Initialize Google Sign-In client for logout
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Set up navigation drawer header with user info
        setupNavigationHeader(navView)
        
        // Handle logout menu item
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    showLogoutConfirmation()
                    true
                }
                else -> {
                    // Let NavController handle other items
                    navController.navigate(menuItem.itemId)
                    drawerLayout.closeDrawers()
                    true
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(tokenExpirationReceiver)
            Log.d(TAG, "Token expiration receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
    
    /**
     * Set up navigation drawer header with user information
     */
    private fun setupNavigationHeader(navView: NavigationView) {
        val headerView = navView.getHeaderView(0)
        val userNameTextView = headerView.findViewById<TextView>(R.id.navHeaderUserName)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.navHeaderUserEmail)
        
        val user = authManager.getCurrentUser()
        if (user != null) {
            userNameTextView.text = user.name ?: "User"
            userEmailTextView.text = user.email
        }
    }
    
    /**
     * Show logout confirmation dialog
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Perform logout
     */
    private fun performLogout() {
        // Sign out from Google to clear cached account selection
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Clear app auth data
            authManager.logout()
            // Navigate to login screen
            navigateToLogin()
        }
    }
    
    /**
     * Handle token expiration - show message and redirect to login
     */
    private fun handleTokenExpiration() {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Session Expired")
                .setMessage("Your session has expired. Please sign in again.")
                .setPositiveButton("Sign In") { _, _ ->
                    navigateToLogin()
                }
                .setCancelable(false)
                .show()
        }
    }
    
    /**
     * Navigate to LoginActivity
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
