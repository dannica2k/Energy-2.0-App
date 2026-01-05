package com.example.energy20.ui.qrscanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.energy20.databinding.ActivityQrScannerBinding
import com.example.energy20.utils.QrCodeParser
import com.example.energy20.utils.ScannedDeviceData
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var isProcessing = false
    private var hasScannedSuccessfully = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        checkCameraPermission()
    }

    private fun setupUI() {
        binding.closeButton.setOnClickListener {
            finish()
        }

        binding.flashlightButton.setOnClickListener {
            toggleFlashlight()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Snackbar.make(
                binding.root,
                "Camera permission is required to scan QR codes",
                Snackbar.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // Image analysis for QR code detection
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
                        processQrCode(qrCode)
                    })
                }

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                // Enable flashlight button only if flash is available
                binding.flashlightButton.isEnabled = camera?.cameraInfo?.hasFlashUnit() == true

            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Snackbar.make(
                    binding.root,
                    "Failed to start camera: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processQrCode(qrContent: String) {
        if (isProcessing || hasScannedSuccessfully) {
            return
        }

        isProcessing = true

        runOnUiThread {
            Log.d(TAG, "QR Code detected: $qrContent")

            // Parse and validate QR code
            val result = QrCodeParser.parse(qrContent)

            result.onSuccess { scannedData ->
                handleSuccessfulScan(scannedData)
            }.onFailure { error ->
                handleScanError(error.message ?: "Invalid QR code")
            }
        }
    }

    private fun handleSuccessfulScan(scannedData: ScannedDeviceData) {
        hasScannedSuccessfully = true

        // Vibrate to provide haptic feedback
        vibrate()

        // Show success message
        binding.instructionText.text = "✓ Device ID scanned successfully!"
        binding.instructionText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

        // Return result to calling activity
        val resultIntent = Intent().apply {
            putExtra(EXTRA_DEVICE_ID, scannedData.deviceId)
        }
        setResult(Activity.RESULT_OK, resultIntent)

        // Close activity after short delay
        binding.root.postDelayed({
            finish()
        }, 500)
    }

    private fun handleScanError(errorMessage: String) {
        Log.w(TAG, "QR scan error: $errorMessage")

        // Show error briefly, then allow retry
        runOnUiThread {
            binding.instructionText.text = errorMessage
            binding.instructionText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

            // Reset after 2 seconds to allow retry
            binding.root.postDelayed({
                if (!hasScannedSuccessfully) {
                    binding.instructionText.text = "Point camera at device QR code"
                    binding.instructionText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    isProcessing = false
                }
            }, 2000)
        }
    }

    private fun toggleFlashlight() {
        camera?.let {
            val currentState = it.cameraInfo.torchState.value == TorchState.ON
            it.cameraControl.enableTorch(!currentState)
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "QrScannerActivity"
        const val EXTRA_DEVICE_ID = "device_id"
    }
}

/**
 * Analyzer for detecting QR codes using ML Kit
 */
private class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT || barcode.valueType == Barcode.TYPE_URL) {
                            barcode.rawValue?.let { qrContent ->
                                onQrCodeDetected(qrContent)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("QrCodeAnalyzer", "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
