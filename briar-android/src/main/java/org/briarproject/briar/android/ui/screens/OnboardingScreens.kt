package org.briarproject.briar.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.briarproject.briar.R
import org.briarproject.briar.android.ui.theme.*

@Composable
fun OnboardingFlow(
    onFinish: (String, String?, Int) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var displayName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(0) } // 0: Citizen, 1: Observer, 2: Journalist, 3: Coordinator

    NasakaWeweTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "OnboardingStep"
                ) { currentStep ->
                    when (currentStep) {
                        1 -> WelcomeStep(onNext = { step = 2 })
                        2 -> NameStep(
                            name = displayName,
                            onNameChange = { displayName = it },
                            onNext = { step = 3 }
                        )
                        3 -> RoleStep(
                            selectedRole = role,
                            onRoleSelected = { role = it },
                            onNext = { step = 4 }
                        )
                        4 -> PinStep(
                            pin = pin,
                            onPinChange = { pin = it },
                            onFinish = { onFinish(displayName, pin.ifBlank { null }, role) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(NasakaSpacing.large)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-impact iOS Logo/Title
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-1).sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(NasakaSpacing.small))
        Text(
            text = stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(NasakaSpacing.xLarge))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(NasakaSpacing.medium),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(R.string.onboarding_begin),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun NameStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier.padding(NasakaSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_name_label),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(NasakaSpacing.large))
        
        // iOS Glass-tinted input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(NasakaSpacing.medium),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            TextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.onboarding_name_prompt)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(NasakaSpacing.large))
        
        Button(
            onClick = onNext,
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(NasakaSpacing.medium)
        ) {
            Text(
                text = stringResource(R.string.onboarding_continue),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun RoleStep(selectedRole: Int, onRoleSelected: (Int) -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier.padding(NasakaSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_role_label),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(NasakaSpacing.large))
        
        val roles = listOf(
            stringResource(R.string.role_citizen),
            stringResource(R.string.role_observer),
            stringResource(R.string.role_journalist),
            stringResource(R.string.role_coordinator)
        )

        Column(verticalArrangement = Arrangement.spacedBy(NasakaSpacing.small)) {
            roles.forEachIndexed { index, roleName ->
                val isSelected = selectedRole == index
                Surface(
                    onClick = { onRoleSelected(index) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(100.dp), // Premium Pill
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shadowElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null, // Surface handles click
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(NasakaSpacing.small))
                        Text(
                            text = roleName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(NasakaSpacing.large))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(NasakaSpacing.medium)
        ) {
            Text(
                text = stringResource(R.string.onboarding_continue),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun PinStep(pin: String, onPinChange: (String) -> Unit, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.padding(NasakaSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_pin_label),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.onboarding_pin_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = iOSGrayLight,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(NasakaSpacing.large))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(NasakaSpacing.medium),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            TextField(
                value = pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.onboarding_pin_prompt)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(NasakaSpacing.large))
        
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(NasakaSpacing.medium)
        ) {
            Text(
                text = stringResource(R.string.onboarding_finish),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

