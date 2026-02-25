package org.briarproject.briar.android.navdrawer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import org.briarproject.bramble.api.lifecycle.LifecycleManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import org.briarproject.bramble.api.plugin.BluetoothConstants
import org.briarproject.bramble.api.plugin.LanTcpConstants
import org.briarproject.bramble.api.plugin.Plugin.State
import org.briarproject.bramble.api.plugin.TorConstants
import org.briarproject.bramble.api.account.AccountManager
import org.briarproject.briar.R
import org.briarproject.briar.android.activity.ActivityComponent
import org.briarproject.briar.android.activity.BriarActivity
import org.briarproject.briar.android.contact.ContactListFragment
import org.briarproject.briar.android.ui.screens.DashboardScreen
import org.briarproject.briar.android.ui.theme.NasakaWeweTheme
import javax.inject.Inject

class NavDrawerActivity : BriarActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var lifecycleManager: LifecycleManager

    @Inject
    lateinit var accountManager: AccountManager

    private lateinit var navDrawerViewModel: NavDrawerViewModel
    private lateinit var pluginViewModel: PluginViewModel

    override fun injectActivity(component: ActivityComponent) {
        component.inject(this)
        val provider = ViewModelProvider(this, viewModelFactory)
        navDrawerViewModel = provider[NavDrawerViewModel::class.java]
        pluginViewModel = provider[PluginViewModel::class.java]
    }

    companion object {
        @JvmField
        val CONTACTS_URI: Uri = Uri.parse("briar://contacts")
        @JvmField
        val CONTACT_URI: Uri = Uri.parse("briar://contacts") // Legacy alias for service
        @JvmField
        val CONTACT_ADDED_URI: Uri = Uri.parse("briar://contact_added")
        @JvmField
        val GROUP_URI: Uri = Uri.parse("briar://groups")
        @JvmField
        val FORUM_URI: Uri = Uri.parse("briar://forums")
        @JvmField
        val BLOG_URI: Uri = Uri.parse("briar://blogs")
        @JvmField
        val PRIVATE_MESSAGE_URI: Uri = Uri.parse("briar://private_messages")
        @JvmField
        val SIGN_OUT_URI: Uri = Uri.parse("briar://sign_out")
        @JvmField
        val SETTINGS_URI: Uri = Uri.parse("briar://settings")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val torState by pluginViewModel.getPluginState(TorConstants.ID).observeAsState(State.INACTIVE)
            val btState by pluginViewModel.getPluginState(BluetoothConstants.ID).observeAsState(State.INACTIVE)
            val wifiState by pluginViewModel.getPluginState(LanTcpConstants.ID).observeAsState(State.INACTIVE)

            NasakaWeweTheme {
                DashboardScreen(
                    userRole = accountManager.getRole(),
                    peerCount = 0, // We'll hook this up to the contact manager later
                    torActive = torState == State.ACTIVE,
                    btActive = btState == State.ACTIVE,
                    wifiActive = wifiState == State.ACTIVE,
                    onSearch = { query -> /* Handle search */ },
                    onNavigate = { destination -> handleNavigation(destination) }
                )
            }
        }

        if (savedInstanceState == null) {
            onNewIntent(intent)
        }
    }

    private fun handleNavigation(destination: String) {
        when (destination) {
            "contacts" -> showContacts()
            "settings" -> startActivity(Intent(this, org.briarproject.briar.android.settings.SettingsActivity::class.java))
            // Add more navigation cases
        }
    }

    private fun showContacts() {
        // For now, we still use the legacy fragment for the contact list to preserve "Everything Full Implementation"
        // We will eventually convert this to Compose too
        setContentView(R.layout.activity_fragment_container) // Temporary fallback for fragment host
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ContactListFragment.newInstance())
            .commit()
    }
}

