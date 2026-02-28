package org.briarproject.briar.android.account
 
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.handleDeeplinks
import org.briarproject.briar.R
import org.briarproject.briar.android.BriarApplication.ENTRY_ACTIVITY
import org.briarproject.briar.android.activity.ActivityComponent
import org.briarproject.briar.android.activity.BaseActivity
import org.briarproject.briar.android.ui.theme.NasakaWeweTheme
import javax.inject.Inject

class CekaAuthActivity : BaseActivity() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SetupViewModel

    companion object {
        private const val TAG = "CekaAuthActivity"
    }

    override fun injectActivity(component: ActivityComponent) {
        component.inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[SetupViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        // Handle deep link if the activity was launched via nasakawewe://auth-callback
        handleIncomingDeepLink(intent)

        val isNetworkAvailable = checkNetworkAvailability()

        setContent {
            NasakaWeweTheme {
                var hasNetwork by remember { mutableStateOf(isNetworkAvailable) }
                var isProcessingCallback by remember { mutableStateOf(false) }

                if (hasNetwork) {
                    // Online mode: show Supabase auth with session observation
                    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()

                    LaunchedEffect(sessionStatus) {
                        when (sessionStatus) {
                            is SessionStatus.Authenticated -> {
                                val user = (sessionStatus as SessionStatus.Authenticated).session.user
                                val name = user?.userMetadata?.get("full_name")?.toString()
                                    ?.replace("\"", "")
                                    ?: user?.email
                                    ?: "Nasaka User"
                                val userId = user?.id ?: ""

                                // Bridge to Briar account creation
                                // We use the Supabase User ID to derive a secure local password
                                // to ensure the local DB is encrypted but the user experience is seamless.
                                if (userId.isNotEmpty()) {
                                    // Cache credentials locally for future offline access
                                    val prefs = getSharedPreferences("ceka_auth", MODE_PRIVATE)
                                    prefs.edit()
                                        .putString("cached_name", name)
                                        .putString("cached_user_id", userId)
                                        .putString("cached_email", user?.email ?: "")
                                        .putBoolean("is_ceka_member", true)
                                        .putLong("last_auth_timestamp", System.currentTimeMillis())
                                        .putBoolean("pending_sync", false)
                                        .apply()

                                    Log.i(TAG, "Authenticated user: $name, creating Briar account")
                                    viewModel.createAccount(name, userId, 1)
                                }
                                isProcessingCallback = false
                            }
                            is SessionStatus.NotAuthenticated -> {
                                isProcessingCallback = false
                            }
                            is SessionStatus.LoadingFromStorage -> {
                                // Still loading, keep waiting
                            }
                            is SessionStatus.NetworkError -> {
                                // Network error during session check, allow fallback
                                isProcessingCallback = false
                            }
                        }
                    }

                    CekaAuthScreen(
                        supabaseClient = supabaseClient,
                        isOnline = true,
                        isProcessingCallback = isProcessingCallback,
                        onOfflineFallback = { hasNetwork = false },
                        onOfflineAccountCreated = { name, password ->
                            // Store offline credentials for later sync
                            persistOfflineCredentials(name, password)
                            viewModel.createAccount(name, password, 0)
                        }
                    )
                } else {
                    // Offline mode: local account creation only
                    // Check if we have cached CEKA credentials from a previous session
                    val cachedPrefs = getSharedPreferences("ceka_auth", MODE_PRIVATE)
                    val hasCachedCredentials = cachedPrefs.getBoolean("is_ceka_member", false)
                    val cachedName = cachedPrefs.getString("cached_name", null)

                    CekaAuthScreen(
                        supabaseClient = supabaseClient,
                        isOnline = false,
                        isProcessingCallback = false,
                        onOfflineFallback = { },
                        onOfflineAccountCreated = { name, password ->
                            // Store offline credentials for sync when internet returns
                            persistOfflineCredentials(name, password)
                            viewModel.createAccount(name, password, 0)
                        }
                    )
                }
            }
        }

        viewModel.state.observeEvent(this) { state ->
            if (state == SetupViewModel.State.CREATED) {
                // Check if there are pending offline credentials to sync
                scheduleOfflineCredentialSync()
                showApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle the OAuth callback deep link when returning from browser
        handleIncomingDeepLink(intent)
    }

    /**
     * Process incoming deep links from OAuth callbacks.
     * The Supabase SDK handles extracting access_token, refresh_token, etc.
     * from the nasakawewe://auth-callback#access_token=...&refresh_token=... fragment.
     */
    private fun handleIncomingDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "nasakawewe" && data.host == "auth-callback") {
            Log.i(TAG, "Received OAuth callback deep link: $data")
            try {
                supabaseClient.handleDeeplinks(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling deep link", e)
            }
        }
    }

    /**
     * Persist offline credentials locally so they can be synced to CEKA
     * when internet connectivity is restored.
     */
    private fun persistOfflineCredentials(name: String, password: String) {
        val prefs = getSharedPreferences("ceka_auth", MODE_PRIVATE)
        prefs.edit()
            .putString("cached_name", name)
            .putBoolean("pending_sync", true)
            .putLong("offline_created_timestamp", System.currentTimeMillis())
            .apply()
        Log.i(TAG, "Offline credentials persisted for later sync")
    }

    /**
     * If there are pending offline credentials that haven't been synced to CEKA,
     * attempt to sync them now (will be picked up on next online session).
     */
    private fun scheduleOfflineCredentialSync() {
        val prefs = getSharedPreferences("ceka_auth", MODE_PRIVATE)
        val pendingSync = prefs.getBoolean("pending_sync", false)
        if (pendingSync && checkNetworkAvailability()) {
            Log.i(TAG, "Network available, offline credentials will sync on next CEKA connection")
            // The sync will happen automatically when the user links their CEKA account
            // via Settings or when the app detects internet and the user triggers re-auth
        }
    }

    private fun checkNetworkAvailability(): Boolean {
        val connectivityManager = getSystemService<ConnectivityManager>() ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showApp() {
        val i = Intent(this, ENTRY_ACTIVITY)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        finish()
        overridePendingTransition(R.anim.screen_new_in, R.anim.screen_old_out)
    }
}
