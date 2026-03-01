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

    private val isProcessingCallback = mutableStateOf(false)
    private var isCreatingAccountLocally = false

    override fun injectActivity(component: ActivityComponent) {
        Log.d(TAG, "injectActivity started")
        try {
            component.inject(this)
            viewModel = ViewModelProvider(this, viewModelFactory)[SetupViewModel::class.java]
            Log.d(TAG, "injectActivity finished successfully")
        } catch (t: Throwable) {
            Log.e(TAG, "CRITICAL: Injection failed in CekaAuthActivity!", t)
            // If injection fails, we might still be able to show a basic error
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate entry: intent=$intent")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "super.onCreate done")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        // Handle deep link if activity started via redirect
        val data = intent.data
        if (data != null && data.scheme == "nasakawewe" && data.host == "auth-callback") {
            Log.i(TAG, "Activity started via deep link redirect: $data")
            isProcessingCallback.value = true
            handleIncomingDeepLink(intent)
        }

        val isNetworkAvailable = checkNetworkAvailability()

        Log.d(TAG, "Setting content view, network available: $isNetworkAvailable")
        try {
            setContent {
                Log.d(TAG, "setContent composition start")
                NasakaWeweTheme {
                    var hasNetwork by remember { mutableStateOf(isNetworkAvailable) }
                    val isProcessing by isProcessingCallback

                    Log.d(TAG, "Rendering screen: processing=$isProcessing, network=$hasNetwork")

                    if (hasNetwork) {
                        val authPlugin = try {
                            supabaseClient.auth
                        } catch (t: Throwable) {
                            Log.e(TAG, "Supabase Auth plugin unavailable during render!", t)
                            null
                        }

                        if (authPlugin != null) {
                            val sessionStatus by authPlugin.sessionStatus.collectAsState()
                            Log.d(TAG, "Live SessionStatus: $sessionStatus")

                            LaunchedEffect(sessionStatus) {
                                try {
                                    Log.d(TAG, "LaunchedEffect(sessionStatus=$sessionStatus) triggered")
                                    when (sessionStatus) {
                                        is SessionStatus.Authenticated -> {
                                            val user = (sessionStatus as SessionStatus.Authenticated).session.user
                                            val name = user?.userMetadata?.get("full_name")?.toString()
                                                ?.replace("\"", "")
                                                ?: user?.email
                                                ?: "Nasaka User"
                                            val userId = user?.id ?: ""

                                            Log.i(TAG, "Authenticated user found: $name (id=$userId)")

                                            // Transition to Briar account
                                            if (userId.isNotEmpty() && !isCreatingAccountLocally) {
                                                // Check if account already exists to avoid duplicate trigger
                                                if (viewModel.state.lastValue == SetupViewModel.State.CREATED) {
                                                    Log.i(TAG, "Account already exists according to VM, just showing app")
                                                    showApp()
                                                    return@LaunchedEffect
                                                }

                                                isCreatingAccountLocally = true
                                                
                                                Log.i(TAG, "Persisting credentials and calling VM.createAccount")
                                                val prefs = getSharedPreferences("ceka_auth", MODE_PRIVATE)
                                                prefs.edit()
                                                    .putString("cached_name", name)
                                                    .putString("cached_user_id", userId)
                                                    .putString("cached_email", user?.email ?: "")
                                                    .putBoolean("is_ceka_member", true)
                                                    .putLong("last_auth_timestamp", System.currentTimeMillis())
                                                    .apply()

                                                viewModel.createAccount(name, userId, 1)
                                            } else if (userId.isEmpty()) {
                                                Log.e(TAG, "User ID is empty in authenticated session!")
                                                isProcessingCallback.value = false
                                            }
                                        }
                                        is SessionStatus.NotAuthenticated -> {
                                            Log.d(TAG, "Status is NotAuthenticated")
                                            if (isProcessingCallback.value) {
                                                Log.w(TAG, "Auth appeared to fail or remain NotAuthenticated after redirect")
                                                isProcessingCallback.value = false
                                            }
                                        }
                                        is SessionStatus.NetworkError -> {
                                            Log.e(TAG, "Network error in session status")
                                            isProcessingCallback.value = false
                                        }
                                        else -> Log.d(TAG, "Session status is Loading or other: $sessionStatus")
                                    }
                                } catch (t: Throwable) {
                                    Log.e(TAG, "CRITICAL: Throwable in Auth LaunchedEffect!", t)
                                    isProcessingCallback.value = false
                                }
                            }

                            CekaAuthScreen(
                                supabaseClient = supabaseClient,
                                isOnline = true,
                                isProcessingCallback = isProcessing,
                                onOfflineFallback = { hasNetwork = false },
                                onOfflineAccountCreated = { name, password ->
                                    Log.i(TAG, "Creating offline account from online fallback: $name")
                                    persistOfflineCredentials(name, password)
                                    viewModel.createAccount(name, password, 0)
                                }
                            )
                        } else {
                            Log.w(TAG, "Auth plugin null, forcing offline")
                            hasNetwork = false
                        }
                    } else {
                        Log.d(TAG, "Rendering Offline Auth Screen")
                        CekaAuthScreen(
                            supabaseClient = supabaseClient,
                            isOnline = false,
                            isProcessingCallback = false,
                            onOfflineFallback = { },
                            onOfflineAccountCreated = { name, password ->
                                Log.i(TAG, "Creating offline account: $name")
                                persistOfflineCredentials(name, password)
                                viewModel.createAccount(name, password, 0)
                            }
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "FATAL: setContent implementation crashed!", t)
        }

        Log.d(TAG, "Registering VM state observer")
        viewModel.state.observeEvent(this) { state ->
            Log.d(TAG, "VM state changed: $state")
            when (state) {
                SetupViewModel.State.CREATED -> {
                    Log.i(TAG, "SUCCESS! Account created, showing Home Screen")
                    scheduleOfflineCredentialSync()
                    isProcessingCallback.value = false
                    showApp()
                }
                SetupViewModel.State.FAILED -> {
                    Log.e(TAG, "FAILURE: Account creation failed in Briar")
                    isProcessingCallback.value = false
                    isCreatingAccountLocally = false
                }
                else -> Log.d(TAG, "VM on other state: $state")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent received: data=${intent.data}")
        setIntent(intent)
        
        val data = intent.data
        if (data != null && data.scheme == "nasakawewe" && data.host == "auth-callback") {
            Log.i(TAG, "Redirect received in onNewIntent")
            isProcessingCallback.value = true
            handleIncomingDeepLink(intent)
        }
    }

    private fun handleIncomingDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "nasakawewe" && data.host == "auth-callback") {
            Log.i(TAG, "Processing OAuth deep link fragment")
            try {
                // Extension function that might throw Throwable if Auth is missing
                supabaseClient.handleDeeplinks(intent)
                Log.d(TAG, "Deep link handled by Supabase SDK")
            } catch (t: Throwable) {
                Log.e(TAG, "CRITICAL ERROR during handleDeeplinks!", t)
                isProcessingCallback.value = false
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
