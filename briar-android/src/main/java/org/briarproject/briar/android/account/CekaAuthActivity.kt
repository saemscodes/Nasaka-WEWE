package org.briarproject.briar.android.account
 
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
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

    override fun injectActivity(component: ActivityComponent) {
        component.inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[SetupViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        val isNetworkAvailable = checkNetworkAvailability()

        setContent {
            NasakaWeweTheme {
                var hasNetwork by remember { mutableStateOf(isNetworkAvailable) }

                if (hasNetwork) {
                    // Online mode: show Supabase auth with session observation
                    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()

                    LaunchedEffect(sessionStatus) {
                        if (sessionStatus is SessionStatus.Authenticated) {
                            val user = (sessionStatus as SessionStatus.Authenticated).session.user
                            val name = user?.userMetadata?.get("full_name")?.toString()
                                ?: user?.email ?: "Nasaka User"
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
                                    .putBoolean("is_ceka_member", true)
                                    .apply()

                                viewModel.createAccount(name, userId, 1)
                            }
                        }
                    }

                    CekaAuthScreen(
                        supabaseClient = supabaseClient,
                        isOnline = true,
                        onOfflineFallback = { hasNetwork = false },
                        onOfflineAccountCreated = { name, password ->
                            viewModel.createAccount(name, password, 0)
                        }
                    )
                } else {
                    // Offline mode: local account creation only
                    CekaAuthScreen(
                        supabaseClient = supabaseClient,
                        isOnline = false,
                        onOfflineFallback = { },
                        onOfflineAccountCreated = { name, password ->
                            viewModel.createAccount(name, password, 0)
                        }
                    )
                }
            }
        }

        viewModel.state.observeEvent(this) { state ->
            if (state == SetupViewModel.State.CREATED) {
                showApp()
            }
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
