package org.briarproject.briar.android.navdrawer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import org.briarproject.bramble.api.lifecycle.LifecycleManager
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.briarproject.briar.android.fragment.BaseFragment
import org.briarproject.briar.android.fragment.BaseFragment.BaseFragmentListener
import org.briarproject.briar.android.ui.screens.ContactListScreen
import org.briarproject.briar.android.ui.screens.GroupListScreen
import org.briarproject.bramble.api.plugin.BluetoothConstants
import org.briarproject.bramble.api.plugin.LanTcpConstants
import org.briarproject.bramble.api.plugin.Plugin.State
import org.briarproject.bramble.api.plugin.TorConstants
import org.briarproject.bramble.api.account.AccountManager
import org.briarproject.briar.android.contact.ComposeContactListViewModel
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
    private lateinit var contactListViewModel: ComposeContactListViewModel

    private var currentScreen by mutableStateOf("dashboard")

    override fun injectActivity(component: ActivityComponent) {
        component.inject(this)
        val provider = ViewModelProvider(this, viewModelFactory)
        navDrawerViewModel = provider.get(NavDrawerViewModel::class.java)
        pluginViewModel = provider.get(PluginViewModel::class.java)
        contactListViewModel = provider.get(ComposeContactListViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val torState by pluginViewModel.getPluginState(TorConstants.ID).observeAsState(State.INACTIVE)
            val btState by pluginViewModel.getPluginState(BluetoothConstants.ID).observeAsState(State.INACTIVE)
            val wifiState by pluginViewModel.getPluginState(LanTcpConstants.ID).observeAsState(State.INACTIVE)

            val contacts by contactListViewModel.contactListItems.collectAsState()
            val hasPending by contactListViewModel.hasPendingContacts.collectAsState()

            NasakaWeweTheme {
                Crossfade(targetState = currentScreen) { screen ->
                    when (screen) {
                        "dashboard" -> DashboardScreen(
                            userRole = accountManager.getRole(),
                            peerCount = contacts.size,
                            torActive = torState == State.ACTIVE,
                            btActive = btState == State.ACTIVE,
                            wifiActive = wifiState == State.ACTIVE,
                            onSearch = { /* Handle search */ },
                            onNavigate = { destination -> handleNavigation(destination) }
                        )
                        "contacts" -> ContactListScreen(
                            items = contacts,
                            hasPending = hasPending,
                            onContactClick = { item -> /* Handle contact click */ },
                            onAddContactNearby = { /* Handle add nearby */ },
                            onAddContactRemote = { /* Handle add remote */ },
                            onShowPending = { /* Handle show pending */ }
                        )
                        "groups" -> GroupListScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            onNewIntent(intent)
        }
    }

    private fun handleNavigation(destination: String) {
        when (destination) {
            "contacts" -> currentScreen = "contacts"
            "settings" -> startActivity(Intent(this, org.briarproject.briar.android.settings.SettingsActivity::class.java))
            "blogs" -> startActivity(Intent(this, org.briarproject.briar.android.blog.BlogActivity::class.java))
            "groups" -> currentScreen = "groups"
            "forums" -> startActivity(Intent(this, org.briarproject.briar.android.forum.ForumActivity::class.java))
            "home" -> currentScreen = "dashboard"
            "map" -> { /* Handle map navigation if implemented */ }
        }
    }

    override fun onBackPressed() {
        if (currentScreen != "dashboard") {
            currentScreen = "dashboard"
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        @JvmField
        val CONTACT_URI: Uri = Uri.parse("briar://contacts")
        @JvmField
        val CONTACT_ADDED_URI: Uri = Uri.parse("briar://contacts/added")
        @JvmField
        val GROUP_URI: Uri = Uri.parse("briar://groups")
        @JvmField
        val FORUM_URI: Uri = Uri.parse("briar://forums")
        @JvmField
        val BLOG_URI: Uri = Uri.parse("briar://blogs")
        @JvmField
        val SIGN_OUT_URI: Uri = Uri.parse("briar://sign_out")
    }
}

