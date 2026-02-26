package org.briarproject.briar.android.contact

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.briarproject.bramble.api.connection.ConnectionRegistry
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.contact.event.ContactAddedEvent
import org.briarproject.bramble.api.contact.event.ContactAliasChangedEvent
import org.briarproject.bramble.api.contact.event.ContactRemovedEvent
import org.briarproject.bramble.api.contact.event.PendingContactAddedEvent
import org.briarproject.bramble.api.contact.event.PendingContactRemovedEvent
import org.briarproject.bramble.api.db.DbException
import org.briarproject.bramble.api.db.Transaction
import org.briarproject.bramble.api.db.TransactionManager
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventBus
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.lifecycle.LifecycleManager
import org.briarproject.bramble.api.plugin.event.ContactConnectedEvent
import org.briarproject.bramble.api.plugin.event.ContactDisconnectedEvent
import org.briarproject.briar.api.android.AndroidNotificationManager
import org.briarproject.briar.api.avatar.event.AvatarUpdatedEvent
import org.briarproject.briar.api.conversation.ConversationManager
import org.briarproject.briar.api.conversation.event.ConversationMessageTrackedEvent
import org.briarproject.briar.api.identity.AuthorManager
import java.util.Collections
import javax.inject.Inject

class ComposeContactListViewModel @Inject constructor(
    private val application: Application,
    private val contactManager: ContactManager,
    private val authorManager: AuthorManager,
    private val conversationManager: ConversationManager,
    private val connectionRegistry: ConnectionRegistry,
    private val eventBus: EventBus,
    private val notificationManager: AndroidNotificationManager,
    private val lifecycleManager: LifecycleManager,
    private val db: TransactionManager
) : ViewModel(), EventListener {

    private val _contactListItems = MutableStateFlow<List<ContactListItem>>(emptyList())
    val contactListItems: StateFlow<List<ContactListItem>> = _contactListItems.asStateFlow()

    private val _hasPendingContacts = MutableStateFlow(false)
    val hasPendingContacts: StateFlow<Boolean> = _hasPendingContacts.asStateFlow()

    init {
        eventBus.addListener(this)
        loadContacts()
        checkForPendingContacts()
    }

    override fun onCleared() {
        super.onCleared()
        eventBus.removeListener(this)
    }

    fun loadContacts() {
        viewModelScope.launch {
            db.transaction<DbException>(true) { txn: Transaction ->
                val contacts = contactManager.getContacts(txn).map { c ->
                    val authorInfo = authorManager.getAuthorInfo(txn, c)
                    val count = conversationManager.getGroupCount(txn, c.id)
                    val connected = connectionRegistry.isConnected(c.id)
                    ContactListItem(c, authorInfo, connected, count)
                }
                val sortedList = ArrayList(contacts)
                Collections.sort(sortedList)
                _contactListItems.value = sortedList
            }
        }
    }

    fun checkForPendingContacts() {
        viewModelScope.launch {
            db.transaction<DbException>(true) { txn: Transaction ->
                val hasPending = contactManager.getPendingContacts(txn).isNotEmpty()
                _hasPendingContacts.value = hasPending
            }
        }
    }

    override fun eventOccurred(e: Event) {
        when (e) {
            is ContactAddedEvent, is ContactRemovedEvent -> loadContacts()
            is ContactConnectedEvent, is ContactDisconnectedEvent -> loadContacts() // Simplify for now, reactive update
            is ConversationMessageTrackedEvent -> loadContacts()
            is AvatarUpdatedEvent, is ContactAliasChangedEvent -> loadContacts()
            is PendingContactAddedEvent, is PendingContactRemovedEvent -> checkForPendingContacts()
        }
    }

    fun clearNotifications() {
        notificationManager.clearAllContactNotifications()
        notificationManager.clearAllContactAddedNotifications()
    }
}
