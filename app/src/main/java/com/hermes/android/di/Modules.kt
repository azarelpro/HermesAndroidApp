package com.hermes.android.di

import com.hermes.android.data.db.AppDatabase
import com.hermes.android.data.preferences.SettingsDataStore
import com.hermes.android.data.repository.MessageRepository
import com.hermes.android.data.repository.SessionRepository
import com.hermes.android.network.HermesHttpClient
import com.hermes.android.network.HermesWebSocketClient
import com.hermes.android.ui.chat.ChatViewModel
import com.hermes.android.ui.login.LoginViewModel
import com.hermes.android.ui.sessionlist.SessionListViewModel
import com.hermes.android.ui.settings.SettingsViewModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
            .build()
    }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    single { HermesHttpClient(get(), get()) }
    single { HermesWebSocketClient(get(), get(), get(), get()) }
}

val databaseModule = module {
    single { AppDatabase.getInstance(androidContext()) }
    single { SessionRepository(get()) }
    single { MessageRepository(get()) }
}

val preferencesModule = module {
    single { SettingsDataStore.create(androidContext()) }
}

val viewModelModule = module {
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { ChatViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { SessionListViewModel(get(), get()) }
}

val appModule = module {
    includes(networkModule, databaseModule, preferencesModule, viewModelModule)
}
