package com.hermes.android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.android.R
import com.hermes.android.data.model.AppSettings
import com.hermes.android.ui.theme.HermesTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.android.network.HermesWebSocketClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (HermesWebSocketClient) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState
    var localIp by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    when (state) {
        is LoginUiState.Idle -> {
            localIp = state.savedSettings.localIp
            username = state.savedSettings.username
            password = state.savedSettings.password
            LoginForm(
                localIp = localIp,
                onLocalIpChange = { localIp = it },
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onShowPasswordChange = { showPassword = it },
                onLoginClick = { viewModel.login(localIp, username, password) },
                isLoading = false
            )
        }
        is LoginUiState.Loading -> {
            LoginForm(
                localIp = localIp,
                onLocalIpChange = { localIp = it },
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onShowPasswordChange = { showPassword = it },
                onLoginClick = { },
                isLoading = true,
                loadingMessage = state.message
            )
        }
        is LoginUiState.Success -> {
            onLoginSuccess(state.wsClient)
        }
        is LoginUiState.Error -> {
            localIp = state.savedSettings.localIp
            username = state.savedSettings.username
            password = state.savedSettings.password
            LoginForm(
                localIp = localIp,
                onLocalIpChange = { localIp = it },
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onShowPasswordChange = { showPassword = it },
                onLoginClick = { viewModel.login(localIp, username, password) },
                isLoading = false,
                errorMessage = state.message
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginForm(
    localIp: String,
    onLocalIpChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    isLoading: Boolean,
    loadingMessage: String = "",
    errorMessage: String? = null
) {
    val icon = remember { androidx.compose.material.icons.Icons.Default.Visibility }
    val iconOff = remember { androidx.compose.material.icons.Icons.Default.VisibilityOff }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .width(400.dp),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo/Title
                Text(
                    text = stringResource(R.string.login_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.login_subtitle),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))

                // Error message
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                }

                // Local IP field
                OutlinedTextField(
                    value = localIp,
                    onValueChange = onLocalIpChange,
                    label = { Text(stringResource(R.string.local_ip_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    enabled = !isLoading
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.username_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    enabled = !isLoading
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.password_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !isLoading,
                    trailingIcon = {
                        IconButton(onClick = { onShowPasswordChange(!showPassword) }) {
                            Icon(
                                imageVector = if (showPassword) iconOff else icon,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

                // Login button
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loadingMessage,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = localIp.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                    ) {
                        Text(
                            text = stringResource(R.string.login_button),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}