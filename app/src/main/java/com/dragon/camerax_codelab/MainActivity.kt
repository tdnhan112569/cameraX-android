package com.dragon.camerax_codelab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.theartofdev.edmodo.cropper.CropImage
import java.io.File
import java.util.concurrent.Executors


private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)

        viewFinder = findViewById<TextureView>(R.id.view_finder)
        if (allPermissionGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        val orientationListener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                Log.d("Orientation", orientation.toString())
            }
        }
        orientationListener.enable()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        viewFinder.layoutParams = ConstraintLayout.LayoutParams(width, width * 4 / 3)
        Log.d("String","${height} ${width}")
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder : TextureView

    private fun startCamera() {
        // TODO: Implement CameraX operations

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(viewFinder.height, viewFinder.width))
        }.build()

        //Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the the viewfinder is updated, we must remove it and reload it
        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
        }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
            val file = File(externalMediaDirs.first(),"medaca-${System.currentTimeMillis()}.jpg")

            imageCapture.takePicture(file, executor, object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    val msg = "Photo capture succeeded: ${file.absolutePath}"
                    Log.d("CameraXApp", msg)
                    viewFinder.post {
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                        startActivity(Intent(this@MainActivity, ImageActivity::class.java).apply {
//                            putExtra("url", file.absolutePath)
//                        })
                        val uri = Uri.fromFile(file)
                        CropImage.activity(uri)
                            .start(this@MainActivity);
                    }
                }

                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    val msg = "Photo capture failed: $message"
                    Log.e("CameraXApp", msg)
                    viewFinder.post {
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        // Bind use case to lifecycle
        // If AndroidStudio complains about "this" being not a LifecycleOwner
        // Try to rebuild project or updating the appcompat dependency to version 1.1 or higher
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        // TODO: Implement camera viewfinder transformations
        val matrix = android.graphics.Matrix()

        //Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.width / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        viewFinder.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if(allPermissionGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     * */
    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

}
