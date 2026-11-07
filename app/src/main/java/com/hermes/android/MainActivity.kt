package com.hermes.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hermes.android.network.HermesWebSocketClient
import com.hermes.android.ui.chat.ChatScreen
import com.hermes.android.ui.chat.ChatViewModel
import com.hermes.android.ui.login.LoginScreen
import com.hermes.android.ui.login.LoginViewModel
import com.hermes.android.ui.sessionlist.SessionListScreen
import com.hermes.android.ui.sessionlist.SessionListViewModel
import com.hermes.android.ui.settings.SettingsScreen
import com.hermes.android.ui.settings.SettingsViewModel
import com.hermes.android.ui.theme.HermesTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {

    private val wsClient: HermesWebSocketClient by lazy {
        GlobalContext.get().get<HermesWebSocketClient>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HermesTheme {
                MaterialTheme {
                    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
                    var currentSessionId by remember { mutableStateOf<String?>(null) }

                    Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            AppScreen.Login -> LoginScreen(
                                onLoginSuccess = { client ->
                                    @Suppress("UNUSED_EXPRESSION") client
                                    currentScreen = AppScreen.SessionList
                                },
                                viewModel = koinViewModel<LoginViewModel>()
                            )
                            AppScreen.SessionList -> SessionListScreen(
                                onSessionClick = { session ->
                                    currentSessionId = session.id
                                    currentScreen = AppScreen.Chat
                                },
                                onCreateSession = {
                                    currentScreen = AppScreen.Chat
                                    currentSessionId = null
                                },
                                onSettingsClick = {
                                    currentScreen = AppScreen.Settings
                                },
                                wsClient = wsClient,
                                viewModel = koinViewModel<SessionListViewModel>()
                            )
                            AppScreen.Chat -> ChatScreen(
                                sessionId = currentSessionId ?: "new",
                                onBackClick = {
                                    currentScreen = AppScreen.SessionList
                                    currentSessionId = null
                                },
                                wsClient = wsClient,
                                viewModel = koinViewModel<ChatViewModel>()
                            )
                            AppScreen.Settings -> SettingsScreen(
                                onBackClick = {
                                    currentScreen = AppScreen.SessionList
                                },
                                wsClient = wsClient,
                                viewModel = koinViewModel<SettingsViewModel>()
                            )
                        }
                    }
                }
            }
        }
    }

    enum class AppScreen {
        Login, SessionList, Chat, Settings
    }
}
