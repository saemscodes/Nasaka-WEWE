package org.briarproject.briar.android.account
 
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Github
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email as EmailProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CekaAuthScreen(supabaseClient: SupabaseClient) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val iOSBlue = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Nasaka-WEWE",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Powered by Civic Education Kenya",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent
                        )
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    supabaseClient.auth.signInWith(EmailProvider) {
                                        this.email = email
                                        this.password = password
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Sign In", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text(
                            text = " OR ",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Divider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                supabaseClient.auth.signInWith(Google)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continue with Google")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                supabaseClient.auth.signInWith(Github)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continue with GitHub")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.civiceducationkenya.com/auth?view=sign_up"))
                    context.startActivity(intent)
                }
            ) {
                Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
