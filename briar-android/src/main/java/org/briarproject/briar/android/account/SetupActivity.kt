package org.briarproject.briar.android.account

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.briar.R
import org.briarproject.briar.android.BriarApplication.ENTRY_ACTIVITY
import org.briarproject.briar.android.activity.ActivityComponent
import org.briarproject.briar.android.activity.BaseActivity
import org.briarproject.briar.android.ui.screens.OnboardingFlow
import org.briarproject.briar.android.ui.theme.NasakaWeweTheme
import javax.inject.Inject

class SetupActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    
    private lateinit var viewModel: SetupViewModel

    override fun injectActivity(component: ActivityComponent) {
        component.inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[SetupViewModel::class.java]
        viewModel.state.observeEvent(this) { state ->
            onStateChanged(state)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        
        setContent {
            NasakaWeweTheme {
                OnboardingFlow { name, pin, role ->
                    viewModel.createAccount(name, pin ?: "", role)
                }
            }
        }
    }

    private fun onStateChanged(state: SetupViewModel.State) {
        if (state == SetupViewModel.State.CREATED || state == SetupViewModel.State.FAILED) {
            showApp()
        }
    }

    private fun showApp() {
        val i = Intent(this, ENTRY_ACTIVITY)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_TASK_ON_HOME or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        supportFinishAfterTransition()
        overridePendingTransition(R.anim.screen_new_in, R.anim.screen_old_out)
    }
}
