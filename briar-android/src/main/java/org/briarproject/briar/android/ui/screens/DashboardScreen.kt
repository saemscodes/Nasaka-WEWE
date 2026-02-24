package org.briarproject.briar.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerSet
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.briarproject.briar.R
import org.briarproject.briar.android.ui.theme.NasakaWeweTheme
import org.briarproject.bramble.api.identity.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userRole: Int,
    peerCount: Int,
    torActive: Boolean,
    btActive: Boolean,
    wifiActive: Boolean,
    onSearch: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    NasakaWeweTheme {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(onNavigate = onNavigate)
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Branded Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        val roleName = when (userRole) {
                            Role.CITIZEN -> stringResource(R.string.role_citizen)
                            Role.OBSERVER -> stringResource(R.string.role_observer)
                            Role.JOURNALIST -> stringResource(R.string.role_journalist)
                            Role.COORDINATOR -> stringResource(R.string.role_coordinator)
                            else -> stringResource(R.string.role_citizen)
                        }
                        Text(
                            text = "Jukumu: $roleName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    // Transport Status Pills
                    Row {
                        StatusIcon(Icons.Default.Cloud, torActive)
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusIcon(Icons.Default.Bluetooth, btActive)
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusIcon(Icons.Default.Wifi, wifiActive)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Iconic Nasaka Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        onSearch(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    placeholder = { Text(stringResource(R.string.find_someone)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats / Proximity Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.peers_connected, peerCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.setup_subtitle),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Actions Grid
                Text(
                    text = "Vitu vya Haraka",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                QuickActionsGrid(onNavigate)
            }
        }
    }
}

@Composable
fun StatusIcon(icon: ImageVector, active: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun QuickActionsGrid(onNavigate: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionCard(Modifier.weight(1f), stringResource(R.string.blogs_button), Icons.Default.Article) { onNavigate("blogs") }
        ActionCard(Modifier.weight(1f), stringResource(R.string.groups_button), Icons.Default.Groups) { onNavigate("groups") }
        ActionCard(Modifier.weight(1f), stringResource(R.string.forums_button), Icons.Default.Forum) { onNavigate("forums") }
    }
}

@Composable
fun ActionCard(modifier: Modifier, title: String, icon: ImageVector, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun BottomNavigationBar(onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = true,
            onClick = { onNavigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Contacts, null) },
            label = { Text(stringResource(R.string.nav_contacts)) },
            selected = false,
            onClick = { onNavigate("contacts") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Map, null) },
            label = { Text(stringResource(R.string.nav_map)) },
            selected = false,
            onClick = { onNavigate("map") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = false,
            onClick = { onNavigate("settings") }
        )
    }
}
