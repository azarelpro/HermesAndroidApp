package com.hermes.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.android.R
import com.hermes.android.data.model.AppSettings
import com.hermes.android.network.HermesWebSocketClient
import com.hermes.android.ui.theme.HermesTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CardDefaults
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    wsClient: HermesWebSocketClient,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState
    var localIp by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var autoConnect by remember { mutableStateOf(false) }

    when (state) {
        is SettingsUiState.Current -> {
            val settings = state.settings
            localIp = settings.localIp
            username = settings.username
            password = settings.password
            autoConnect = settings.autoConnect
            
            SettingsForm(
                localIp = localIp,
                onLocalIpChange = { localIp = it },
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onShowPasswordChange = { showPassword = it },
                autoConnect = autoConnect,
                onAutoConnectChange = { autoConnect = it },
                isConnected = state.isConnected,
                onSaveClick = {
                    viewModel.saveSettings(AppSettings(
                        localIp = localIp,
                        username = username,
                        password = password,
                        autoConnect = autoConnect
                    ))
                },
                onDisconnectClick = { viewModel.disconnect() }
            )
        }
        is SettingsUiState.Saved -> {
            SettingsForm(
                localIp = localIp,
                onLocalIpChange = { localIp = it },
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onShowPasswordChange = { showPassword = it },
                autoConnect = autoConnect,
                onAutoConnectChange = { autoConnect = it },
                isConnected = false,
                onSaveClick = { },
                onDisconnectClick = { },
                savedMessage = stringResource(R.string.copied)
            )
        }
        is SettingsUiState.Error -> {
            SettingsForm(
                localIp = localIp,
                onLocalIpChange = { localIp = it },
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onShowPasswordChange = { showPassword = it },
                autoConnect = autoConnect,
                onAutoConnectChange = { autoConnect = it },
                isConnected = false,
                onSaveClick = { },
                onDisconnectClick = { },
                errorMessage = state.message
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsForm(
    localIp: String,
    onLocalIpChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordChange: (Boolean) -> Unit,
    autoConnect: Boolean,
    onAutoConnectChange: (Boolean) -> Unit,
    isConnected: Boolean,
    onSaveClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    savedMessage: String? = null,
    errorMessage: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                androidx.compose.material3.IconButton(onClick = { /* handled by parent */ }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Connection Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = localIp,
                    onValueChange = onLocalIpChange,
                    label = { Text(stringResource(R.string.local_ip_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.username_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { onShowPasswordChange(!showPassword) }) {
                            Icon(
                                imageVector = if (showPassword)
                                    androidx.compose.material.icons.Icons.Default.VisibilityOff
                                else
                                    androidx.compose.material.icons.Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-connect on launch", fontSize = 16.sp)
                    Switch(
                        checked = autoConnect,
                        onCheckedChange = onAutoConnectChange,
                        enabled = localIp.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Connection Status",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = if (isConnected) Color.Green else Color.Red,
                                    radius = 6.dp.toPx()
                                )
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isConnected) stringResource(R.string.connection_status_connected) else stringResource(R.string.connection_status_disconnected),
                            fontSize = 16.sp,
                            color = if (isConnected) Color.Green else Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (isConnected) {
                        Button(onClick = onDisconnectClick) {
                            Text(stringResource(R.string.disconnect))
                        }
                    }
                }
            }
        }

        savedMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        errorMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = localIp.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        ) {
            Text(stringResource(R.string.save_settings), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}