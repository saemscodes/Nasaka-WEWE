package org.briarproject.briar.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.briarproject.briar.R
import org.briarproject.briar.android.ui.theme.*
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NasakaWeweTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                // iOS Glassmorphic Sticky Top Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shadowElevation = 0.dp
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                val roleName = when (userRole) {
                                    Role.CITIZEN -> stringResource(R.string.role_citizen)
                                    Role.OBSERVER -> stringResource(R.string.role_observer)
                                    Role.JOURNALIST -> stringResource(R.string.role_journalist)
                                    Role.COORDINATOR -> stringResource(R.string.role_coordinator)
                                    else -> stringResource(R.string.role_citizen)
                                }
                                Text(
                                    text = roleName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        },
                        actions = {
                            Row(modifier = Modifier.padding(end = 8.dp)) {
                                StatusIcon(Icons.Default.Cloud, torActive)
                                Spacer(modifier = Modifier.width(4.dp))
                                StatusIcon(Icons.Default.Bluetooth, btActive)
                                Spacer(modifier = Modifier.width(4.dp))
                                StatusIcon(Icons.Default.Wifi, wifiActive)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        scrollBehavior = scrollBehavior
                    )
                }
            },
            bottomBar = {
                // iOS Premium Glass Bottom Navigation
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shadowElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Outlined.Home, null) },
                            label = { Text(stringResource(R.string.nav_home)) },
                            selected = true,
                            onClick = { onNavigate("home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Outlined.People, null) },
                            label = { Text(stringResource(R.string.nav_contacts)) },
                            selected = false,
                            onClick = { onNavigate("contacts") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Outlined.Map, null) },
                            label = { Text(stringResource(R.string.nav_map)) },
                            selected = false,
                            onClick = { onNavigate("map") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Outlined.Settings, null) },
                            label = { Text(stringResource(R.string.nav_settings)) },
                            selected = false,
                            onClick = { onNavigate("settings") }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(NasakaSpacing.medium)
            ) {
                item {
                    // Modern Glass Search Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = NasakaSpacing.small),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                onSearch(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.find_someone)) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = iOSGrayLight) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(NasakaSpacing.large)) }

                item {
                    // Glassmorphic Stats Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(NasakaSpacing.medium),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(NasakaSpacing.medium)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SignalCellularAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(NasakaSpacing.small))
                                Text(
                                    text = "NETWORK STATUS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(Modifier.height(NasakaSpacing.small))
                            Text(
                                text = stringResource(R.string.peers_connected, peerCount),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = stringResource(R.string.setup_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = iOSGrayLight
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(NasakaSpacing.large)) }

                item {
                    Text(
                        text = "Vitu vya Haraka",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                item { Spacer(modifier = Modifier.height(NasakaSpacing.small)) }
                
                item { QuickActionsGrid(onNavigate) }
            }
        }
    }
}

@Composable
fun StatusIcon(icon: ImageVector, active: Boolean) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        border = if (active) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (active) MaterialTheme.colorScheme.primary else iOSGrayLight
            )
        }
    }
}

@Composable
fun QuickActionsGrid(onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.spacedBy(NasakaSpacing.medium)
    ) {
        ActionCard(Modifier.weight(1f), stringResource(R.string.blogs_button), Icons.Outlined.Article) { onNavigate("blogs") }
        ActionCard(Modifier.weight(1f), stringResource(R.string.groups_button), Icons.Outlined.Groups) { onNavigate("groups") }
        ActionCard(Modifier.weight(1f), stringResource(R.string.forums_button), Icons.Outlined.Forum) { onNavigate("forums") }
    }
}

@Composable
fun ActionCard(modifier: Modifier, title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(110.dp),
        onClick = onClick,
        shape = RoundedCornerShape(NasakaSpacing.medium),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height( NasakaSpacing.small))
            Text(
                text = title, 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
        }
    }
}

