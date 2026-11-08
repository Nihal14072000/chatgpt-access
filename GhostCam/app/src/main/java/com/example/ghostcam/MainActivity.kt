package com.example.ghostcam

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var ghostOverlay: ImageView
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var opacityLabel: TextView
    private lateinit var lensOptionsContainer: LinearLayout
    private lateinit var flipButton: TextView

    private var imageCapture: ImageCapture? = null
    private var activeCamera: Camera? = null
    private var isFrontCamera = false
    private var selectedLensId: String? = null
    private var rearLenses: List<LensOption> = emptyList()

    private data class LensOption(val cameraId: String, val label: String, val focalLength: Float)

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
        }

    private val pickReferenceImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                ghostOverlay.setImageURI(uri)
                ghostOverlay.visibility = ImageView.VISIBLE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        ghostOverlay = findViewById(R.id.ghostOverlay)
        opacitySeekBar = findViewById(R.id.opacitySeekBar)
        opacityLabel = findViewById(R.id.opacityLabel)
        lensOptionsContainer = findViewById(R.id.lensOptions)
        flipButton = findViewById(R.id.flipButton)

        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        rearLenses = discoverRearLenses()
        renderLensOptions()

        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ghostOverlay.alpha = progress / 100f
                opacityLabel.text = "Overlay  $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        findViewById<TextView>(R.id.imageButton).setOnClickListener {
            pickReferenceImage.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }

        findViewById<TextView>(R.id.captureButton).setOnClickListener { capturePhoto() }
        flipButton.setOnClickListener {
            isFrontCamera = !isFrontCamera
            selectedLensId = null
            renderLensOptions()
            startCamera()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun discoverRearLenses(): List<LensOption> {
        return try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameras = manager.cameraIdList.mapNotNull { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) !=
                    CameraCharacteristics.LENS_FACING_BACK) return@mapNotNull null
                val focal = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?: return@mapNotNull null
                val value = focal.minOrNull() ?: return@mapNotNull null
                LensOption(id, "", value)
            }.distinctBy { it.cameraId }

            if (cameras.isEmpty()) return emptyList()
            val sorted = cameras.sortedBy { it.focalLength }
            val base = sorted[sorted.size / 2].focalLength
            sorted.map { lens ->
                val ratio = lens.focalLength / base
                val label = when {
                    ratio < 0.8f -> "0.5×"
                    ratio > 1.6f -> String.format(Locale.US, "%.1f×", ratio)
                    else -> "1×"
                }
                lens.copy(label = label)
            }.distinctBy { it.label }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun renderLensOptions() {
        lensOptionsContainer.removeAllViews()
        if (isFrontCamera) {
            lensOptionsContainer.visibility = android.view.View.GONE
            return
        }
        lensOptionsContainer.visibility = android.view.View.VISIBLE
        rearLenses.forEachIndexed { index, lens ->
            val chip = TextView(this).apply {
                text = lens.label
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(18, 10, 18, 10)
                isSelected = lens.cameraId == (selectedLensId ?: rearLenses.firstOrNull()?.cameraId)
                setTextColor(getColor(if (isSelected) android.R.color.black else android.R.color.white))
                setBackgroundResource(if (isSelected) R.drawable.lens_chip_selected else R.drawable.lens_chip)
                setOnClickListener {
                    selectedLensId = lens.cameraId
                    renderLensOptions()
                    startCamera()
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 8 }
            lensOptionsContainer.addView(chip, params)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val selector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    val lens = rearLenses.firstOrNull { it.cameraId == selectedLensId }
                        ?: rearLenses.firstOrNull()
                    if (lens == null) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.Builder().addCameraFilter { infos ->
                            infos.filter { Camera2CameraInfo.from(it).cameraId == lens.cameraId }
                        }.build()
                    }
                }

                cameraProvider.unbindAll()
                activeCamera = cameraProvider.bindToLifecycle(this, selector, preview, capture)
                imageCapture = capture
                if (!isFrontCamera && selectedLensId == null) {
                    selectedLensId = rearLenses.firstOrNull()?.cameraId
                    renderLensOptions()
                }
            } catch (exception: Exception) {
                Toast.makeText(
                    this,
                    "Unable to start selected camera lens: ${exception.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val capture = imageCapture
        if (capture == null) {
            Toast.makeText(this, "Camera is not ready yet.", Toast.LENGTH_SHORT).show()
            return
        }

        val name = "GhostCam_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GhostCam")
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        this@MainActivity,
                        "Photo saved to Pictures/GhostCam",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        "Capture failed: ${exception.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
}
