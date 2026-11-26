package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.backup.VaultBackupManager
import com.example.data.crypto.CryptoManager
import com.example.data.local.AppDatabase
import com.example.data.repository.ApiEndpointRepository
import com.example.data.security.AppLockManager
import com.example.data.security.DataStoreLockPreferencesStore
import com.example.data.settings.DataStoreThemePreferencesStore
import com.example.ui.ApiManagerScreen
import com.example.ui.ApiManagerViewModel
import com.example.ui.theme.ApiManagerTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ApiManagerViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val database = AppDatabase.getInstance(app)
                        val cryptoManager = CryptoManager()
                        val repository = ApiEndpointRepository(
                            dao = database.apiEndpointDao(),
                            cryptoManager = cryptoManager
                        )
                        val lockManager = AppLockManager(
                            store = DataStoreLockPreferencesStore(app)
                        )
                        val backupManager = VaultBackupManager(
                            context = app,
                            appVersion = BuildConfig.VERSION_NAME,
                            cryptoManager = cryptoManager
                        )
                        ApiManagerViewModel(
                            repository = repository,
                            lockManager = lockManager,
                            themeStore = DataStoreThemePreferencesStore(app),
                            backupManager = backupManager
                        )
                    }
                }
            )

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ApiManagerTheme(themeMode = uiState.themeMode, accentColor = uiState.accentColor) {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            viewModel.onAppBackgrounded()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                ApiManagerScreen(viewModel = viewModel)
            }
        }
    }
}
