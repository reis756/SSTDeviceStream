/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sst.sstdevicestream.ui.framebyframe

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.sst.sstdevicestream.R
import com.sst.sstdevicestream.databinding.FragmentCameraBinding
import com.sst.sstdevicestream.model.DeviceData
import com.sst.sstdevicestream.ui.permission.PermissionsFragment
import com.sst.sstdevicestream.comom.ObjectDetectorHelper
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.ByteArrayOutputStream
import java.util.LinkedList
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: CameraViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private var showInfo = false

    private var aiEnabled = true

    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            findNavController().navigate(R.id.navigation_permission)
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(), objectDetectorListener = this
        )

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        fragmentCameraBinding.liveButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.activeStreaming(isChecked)
        }

        fragmentCameraBinding.showInfo.setOnClickListener {
            showInfo = !showInfo

            fragmentCameraBinding.info.isVisible = showInfo
        }

        fragmentCameraBinding.aiButton.setOnClickListener {
            aiEnabled = !aiEnabled
            fragmentCameraBinding.overlay.isVisible = aiEnabled
        }

        fragmentCameraBinding.switchCam.setOnClickListener {
            lensFacing =
                if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK

            bindCameraUseCases()
        }

        setupBottomSheetTranscriptList()

        // Attach listeners to UI control widgets
        initBottomSheetControls()

        setupObservers()

        savePrefs()

        viewModel.setupRabbitMq()

        val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)

        fragmentCameraBinding.viewFinder.setOnTouchListener { view, event ->
            view.performClick()
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val timer = Timer()

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        viewModel.deviceData = DeviceData(
                            viewModel.userUuid,
                            "Android Phone",
                            location?.latitude ?: 0.0,
                            location?.longitude ?: 0.0
                        )
                    }
            }

        }, 0, 5000L)
    }

    private fun savePrefs() {
        val sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPreferences?.getString(USER_ID, "")
        if (userId.isNullOrEmpty()) {
            val uuid = UUID.randomUUID().toString()
            val prefEdit = sharedPreferences?.edit()
            prefEdit?.putString(USER_ID, UUID.randomUUID().toString())
            prefEdit?.apply()

            viewModel.setUserId(uuid)
        } else {
            viewModel.setUserId(userId)
        }
    }

    private val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = camera?.cameraInfo?.zoomState?.value?.zoomRatio?.times(detector.scaleFactor)
            if (scale != null) {
                camera?.cameraControl?.setZoomRatio(scale)
            }
            return true
        }
    }

    private fun setupBottomSheetTranscriptList() {
        sheetBehavior =
            BottomSheetBehavior.from(fragmentCameraBinding.bottomSheetLayout.bottomSheetLayout)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                fragmentCameraBinding.camControls.isVisible =
                    newState == BottomSheetBehavior.STATE_COLLAPSED
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    private fun initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        // When clicked, decrease the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {/* no op */
                }
            }

        // When clicked, change the underlying model used for object detection
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {/* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
            objectDetectorHelper.numThreads.toString()

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding.overlay.clear()
    }

    private fun setupObservers() {
        viewModel.error.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }

        viewModel.info.observe(viewLifecycleOwner) {
            fragmentCameraBinding.info.text = it
        }
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation).build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(cameraExecutor) { image ->
                    if (!::bitmapBuffer.isInitialized) {
                        // The image rotation and RGB image buffer are initialized only once
                        // the analyzer has started running
                        bitmapBuffer = Bitmap.createBitmap(
                            image.width, image.height, Bitmap.Config.ARGB_8888
                        )
                    }

                    detectObjects(image)
                }
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection

        if (aiEnabled) {
            objectDetectorHelper.detect(bitmapBuffer, imageRotation)
        } else {
            rotateImage(bitmapBuffer, imageRotation)?.let {
                viewModel.publishMessage(
                    viewModel.rabbitMqQueueStream, bitmapToByteArray(it), viewModel.deviceData
                )
            }
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.preRotate(degree.toFloat())
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int,
        bitmap: Bitmap
    ) {
        activity?.runOnUiThread {
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)

            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(), imageHeight, imageWidth
            )

            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()

            drawBitmap(
                results ?: LinkedList<Detection>(), bitmap
            )
        }
    }

    private fun drawBitmap(
        detectionResults: MutableList<Detection>, bitmap: Bitmap
    ) {
        val canvas = Canvas(bitmap)

        val boxPaint = Paint()
        val textBackgroundPaint = Paint()
        val textPaint = Paint()
        val bounds = Rect()

        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(
            requireContext(),
            R.color.bounding_box_color
        )
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE

        for (result in detectionResults) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top
            val bottom = boundingBox.bottom
            val left = boundingBox.left
            val right = boundingBox.right

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val drawableText =
                result.categories[0].label + " " + String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left, top, left + textWidth + 8, top + textHeight + 8, textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }

        viewModel.publishMessage(
            viewModel.rabbitMqQueueStream, bitmapToByteArray(bitmap), viewModel.deviceData
        )
    }

    private fun bitmapToByteArray(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 80
    ): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val SHARED_PREFS = "sharedPrefs"
        const val USER_ID = "userId"
    }
}
