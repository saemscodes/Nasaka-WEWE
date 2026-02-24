package org.briarproject.briar.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.briarproject.briar.R
import org.briarproject.briar.android.ui.theme.NasakaWeweTheme

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
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.onboarding_begin))
        }
    }
}

@Composable
fun NameStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_name_label),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboarding_name_prompt)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNext,
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
    }
}

@Composable
fun RoleStep(selectedRole: Int, onRoleSelected: (Int) -> Unit, onNext: () -> Unit) {
    val roles = listOf(
        stringResource(R.string.role_citizen),
        stringResource(R.string.role_observer),
        stringResource(R.string.role_journalist),
        stringResource(R.string.role_coordinator)
    )
    
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_role_label),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        roles.forEachIndexed { index, roleName ->
            FilterChip(
                selected = selectedRole == index,
                onClick = { onRoleSelected(index) },
                label = { Text(roleName) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
    }
}

@Composable
fun PinStep(pin: String, onPinChange: (String) -> Unit, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_pin_label),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.onboarding_pin_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboarding_pin_prompt)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboarding_finish))
        }
    }
}
