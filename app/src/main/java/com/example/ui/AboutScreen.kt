package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.openUrl

private data class LinkItem(val label: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.about_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            AboutParagraph(stringResource(R.string.about_purpose))
            AboutParagraph(stringResource(R.string.about_security))
            AboutParagraph(stringResource(R.string.about_privacy))

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.providers_title), style = MaterialTheme.typography.titleMedium)
            listOf(
                LinkItem(stringResource(R.string.provider_openai), "https://platform.openai.com/api-keys"),
                LinkItem(stringResource(R.string.provider_anthropic), "https://console.anthropic.com/settings/keys"),
                LinkItem(stringResource(R.string.provider_gemini), "https://aistudio.google.com/app/apikey"),
                LinkItem(stringResource(R.string.provider_openrouter), "https://openrouter.ai/keys")
            ).forEach { link ->
                TextButton(onClick = { context.openUrl(link.url) }) {
                    Text(link.label)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.about_links), style = MaterialTheme.typography.titleMedium)
            listOf(
                LinkItem(stringResource(R.string.link_github), "https://github.com/arvind-ranga/api-manager"),
                LinkItem(stringResource(R.string.link_ko_fi), "https://ko-fi.com/arvindranga"),
                LinkItem(stringResource(R.string.link_sponsors), "https://github.com/sponsors/arvind-ranga"),
                LinkItem(
                    stringResource(R.string.link_upi),
                    "upi://pay?pa=electriclegend@yescred&pn=Arvind%20Ranga&cu=INR"
                ),
                LinkItem(stringResource(R.string.link_izzyondroid), "https://apt.izzysoft.de/fdroid/index/apk/com.aistudio.apimanager.vkrxmw"),
                LinkItem(stringResource(R.string.link_fdroid), "https://f-droid.org/")
            ).forEach { link ->
                TextButton(onClick = { context.openUrl(link.url) }) {
                    Text(link.label)
                }
            }
        }
    }
}

@Composable
private fun AboutParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
}
