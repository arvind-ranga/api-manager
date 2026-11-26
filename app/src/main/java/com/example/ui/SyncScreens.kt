package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.sync.QrEncoder
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        content(Modifier.fillMaxSize().padding(padding).padding(16.dp))
    }
}

@Composable
fun QrSendScreen(
    chunks: List<String>,
    index: Int,
    isBusy: Boolean,
    onPageChanged: (Int) -> Unit,
    onBack: () -> Unit
) {
    SyncScaffold(stringResource(R.string.sync_qr_send_title), onBack) { modifier ->
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isBusy) {
                CircularProgressIndicator()
                return@Column
            }
            if (chunks.isEmpty()) {
                Text(stringResource(R.string.backup_desc))
                return@Column
            }
            val safeIndex = index.coerceIn(0, chunks.lastIndex)
            val bitmap = remember(chunks, safeIndex) { QrEncoder.encode(chunks[safeIndex]) }
            if (bitmap != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.sync_qr_image_desc),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
            } else {
                Text(stringResource(R.string.sync_qr_too_large))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.sync_qr_page, safeIndex + 1, chunks.size),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            RowButtons(
                previousEnabled = safeIndex > 0,
                nextEnabled = safeIndex < chunks.lastIndex,
                onPrevious = { onPageChanged(safeIndex - 1) },
                onNext = { onPageChanged(safeIndex + 1) }
            )
        }
    }
}

@Composable
private fun RowButtons(
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onPrevious, enabled = previousEnabled) {
            Text(stringResource(R.string.action_previous))
        }
        Button(onClick = onNext, enabled = nextEnabled) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
fun QrReceiveScreen(
    message: String?,
    isBusy: Boolean,
    onScanned: (String) -> Unit,
    onAssemble: () -> Unit,
    onBack: () -> Unit
) {
    SyncScaffold(stringResource(R.string.sync_qr_receive_title), onBack) { modifier ->
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message ?: stringResource(R.string.sync_camera_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            QrScanner(onResult = onScanned)
            Button(
                onClick = onAssemble,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_assemble))
            }
        }
    }
}

@Composable
fun LanSendScreen(
    url: String?,
    message: String?,
    isBusy: Boolean,
    onStop: () -> Unit
) {
    SyncScaffold(stringResource(R.string.sync_lan_send_title), onStop) { modifier ->
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isBusy || url == null) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(message ?: "Preparing transfer…")
                return@Column
            }
            val bitmap = remember(url) { QrEncoder.encode(url) }
            if (bitmap != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.sync_qr_image_desc),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .padding(12.dp)
                            .widthIn(max = 320.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.sync_no_code_needed),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_stop))
            }
        }
    }
}

@Composable
fun LanReceiveScreen(
    message: String?,
    isBusy: Boolean,
    onScanned: (String) -> Unit,
    onConnect: (String) -> Unit,
    onBack: () -> Unit
) {
    var address by remember { mutableStateOf("") }
    SyncScaffold(stringResource(R.string.sync_lan_receive_title), onBack) { modifier ->
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message ?: stringResource(R.string.sync_camera_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            QrScanner(onResult = { value ->
                address = value
                onScanned(value)
            })
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.lan_address_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.sync_no_code_needed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onConnect(address) },
                enabled = address.isNotBlank() && !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_connect))
            }
        }
    }
}

@Composable
private fun QrScanner(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnResult by rememberUpdatedState(onResult)
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!permissionGranted) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.sync_camera_permission))
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text(stringResource(R.string.action_scan_qr))
            }
        }
        return
    }

    // Allow scanning many QR chunks; only suppress identical consecutive frames.
    val lastScanned = remember { AtomicReference<String?>(null) }
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
                {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { preview ->
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val reader = MultiFormatReader()
                        val analyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { image ->
                            try {
                                val mediaImage = image.image
                                val plane = mediaImage?.planes?.firstOrNull()
                                if (plane != null) {
                                    val buffer = plane.buffer
                                    val data = ByteArray(buffer.remaining())
                                    buffer.get(data)
                                    val source = PlanarYUVLuminanceSource(
                                        data,
                                        plane.rowStride,
                                        mediaImage.height,
                                        0,
                                        0,
                                        mediaImage.width,
                                        mediaImage.height,
                                        false
                                    )
                                    val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                                    val text = result.text
                                    if (!text.isNullOrBlank() && text != lastScanned.get()) {
                                        lastScanned.set(text)
                                        latestOnResult(text)
                                    }
                                }
                            } catch (_: Exception) {
                                // No QR found in this frame.
                            } finally {
                                image.close()
                            }
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analyzer
                        )
                    } catch (_: Exception) {
                        // Camera unavailable (e.g. desktop emulator).
                    }
                },
                ContextCompat.getMainExecutor(ctx)
            )
            previewView
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}
