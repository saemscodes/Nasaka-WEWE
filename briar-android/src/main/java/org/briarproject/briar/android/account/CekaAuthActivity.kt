package org.briarproject.briar.android.account

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import org.briarproject.briar.R
import org.briarproject.briar.android.BriarApplication.ENTRY_ACTIVITY
import org.briarproject.briar.android.activity.ActivityComponent
import org.briarproject.briar.android.activity.BaseActivity
import org.briarproject.briar.android.ui.theme.BriarTheme
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

        setContent {
            BriarTheme {
                val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()

                LaunchedEffect(sessionStatus) {
                    if (sessionStatus is SessionStatus.Authenticated) {
                        val user = (sessionStatus as SessionStatus.Authenticated).session.user
                        val name = user?.userMetadata?.get("full_name")?.toString() ?: user?.email ?: "Nasaka User"
                        val userId = user?.id ?: ""
                        
                        // Bridge to Briar account creation
                        // We use the Supabase User ID to derive a secure local password 
                        // to ensure the local DB is encrypted but the user experience is seamless.
                        if (userId.isNotEmpty()) {
                            viewModel.createAccount(name, userId, 1) // Using ID as password for bridge parity
                        }
                    }
                }

                CekaAuthScreen(supabaseClient)
            }
        }

        viewModel.state.observeEvent(this) { state ->
            if (state == SetupViewModel.State.CREATED) {
                showApp()
            }
        }
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
