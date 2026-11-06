package com.bottercc.offlinepay.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bottercc.offlinepay.data.PayDraft
import com.bottercc.offlinepay.data.UpiQr
import com.bottercc.offlinepay.ui.theme.LocalIos
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanScreen(onBack: () -> Unit, onScanned: (PayDraft) -> Unit) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        var camera by remember { mutableStateOf<Camera?>(null) }
        var torch by remember { mutableStateOf(false) }

        if (granted) {
            CameraPreview(onCamera = { camera = it }, onCode = onScanned)
            ScanFrame()
        } else {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("Camera access needed", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "OfflinePay uses the camera only to read UPI QR codes. Nothing is recorded or uploaded.",
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                IosButton("Allow camera", onClick = { ask.launch(Manifest.permission.CAMERA) })
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(Icons.Filled.Close, "Close", onBack)
            Text(
                "Scan any UPI QR",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(44.dp))
        }

        if (granted) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("Point at any UPI QR code to pay offline", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
                GlassButton(if (torch) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff, "Torch", size = 56) {
                    torch = !torch
                    camera?.cameraControl?.enableTorch(torch)
                }
            }
        }
    }
}

@Composable
private fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) =
    GlassButton(icon, description, 44, onClick)

@Composable
private fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, size: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size((size * 0.5).dp))
    }
}

/** Dimmed screen with a clear rounded window and corner brackets. */
@Composable
private fun ScanFrame() {
    val accent = LocalIos.current.accent
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val side = size.minDimension * 0.68f
        val topLeft = Offset((size.width - side) / 2, (size.height - side) / 2 - size.height * 0.04f)
        val radius = CornerRadius(28.dp.toPx())
        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRoundRect(Color.Transparent, topLeft, Size(side, side), radius, blendMode = BlendMode.Clear)
        drawRoundRect(accent, topLeft, Size(side, side), radius, style = Stroke(width = 4.dp.toPx()))
    }
}

@Composable
private fun CameraPreview(onCamera: (Camera) -> Unit, onCode: (PayDraft) -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    val preview = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())

    DisposableEffect(owner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        val main = Handler(Looper.getMainLooper())
        var done = false
        var lastWarning = 0L
        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val p = Preview.Builder().build().also { it.setSurfaceProvider(preview.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor, QrAnalyzer(scanner) { raw ->
                main.post {
                    if (done) return@post
                    val draft = UpiQr.parse(raw)
                    if (draft != null) {
                        done = true
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onCode(draft)
                    } else if (System.currentTimeMillis() - lastWarning > 3000) {
                        lastWarning = System.currentTimeMillis()
                        Toast.makeText(ctx, "That isn't a UPI payment QR", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            runCatching {
                provider.unbindAll()
                onCamera(provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, p, analysis))
            }
        }, ContextCompat.getMainExecutor(ctx))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
            scanner.close()
        }
    }
}

private class QrAnalyzer(private val scanner: BarcodeScanner, private val onRaw: (String) -> Unit) : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val image = proxy.image ?: return proxy.close()
        scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(onRaw) }
            .addOnCompleteListener { proxy.close() }
    }
}
