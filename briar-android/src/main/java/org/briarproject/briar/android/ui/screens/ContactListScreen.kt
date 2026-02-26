package org.briarproject.briar.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.briarproject.briar.android.contact.ContactListItem
import org.briarproject.briar.android.ui.theme.NasakaWeweTheme

@Composable
fun ContactListScreen(
    items: List<ContactListItem>,
    hasPending: Boolean,
    onContactClick: (ContactListItem) -> Unit,
    onAddContactNearby: () -> Unit,
    onAddContactRemote: () -> Unit,
    onShowPending: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactNearby,
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (hasPending) {
                PendingContactsBanner(onShowPending)
            }
            
            if (items.isEmpty()) {
                EmptyContactsState()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items) { item ->
                        ContactRow(item, onContactClick)
                        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactRow(item: ContactListItem, onClick: (ContactListItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for Avatar
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.medium,
            color = if (item.isConnected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = item.contact.author.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = if (item.isConnected) MaterialTheme.colors.primary else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.contact.author.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            if (item.isConnected) {
                Text(
                    text = "Connected",
                    color = MaterialTheme.colors.primary,
                    fontSize = 13.sp
                )
            }
        }

        if (item.unreadCount > 0) {
            Badge(backgroundColor = MaterialTheme.colors.primary) {
                Text(text = item.unreadCount.toString(), color = Color.White)
            }
        }
    }
}

@Composable
fun PendingContactsBanner(onShowPending: () -> Unit) {
    Surface(
        color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowPending() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pending Contact Requests",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "SHOW",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
fun EmptyContactsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "No contacts yet", color = Color.Gray)
    }
}
