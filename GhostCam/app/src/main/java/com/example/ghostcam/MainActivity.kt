package com.example.ghostcam

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var ghostOverlay: ImageView
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var opacityLabel: TextView

    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

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

        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ghostOverlay.alpha = progress / 100f
                opacityLabel.text = "Opacity: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        findViewById<Button>(R.id.imageButton).setOnClickListener {
            pickReferenceImage.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }

        findViewById<Button>(R.id.captureButton).setOnClickListener { capturePhoto() }
        findViewById<Button>(R.id.flipButton).setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
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

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, capture)
                imageCapture = capture
            } catch (exception: Exception) {
                Toast.makeText(
                    this,
                    "Unable to start camera: ${exception.localizedMessage}",
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
