/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker.fragment

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.examples.poselandmarker.MainViewModel
import com.google.mediapipe.examples.poselandmarker.Measurement
import com.google.mediapipe.examples.poselandmarker.PlankDetector
import com.google.mediapipe.examples.poselandmarker.PoseLandmarkerHelper
import com.google.mediapipe.examples.poselandmarker.PushUpDetector
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentGalleryBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class GalleryFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    enum class MediaType {
        IMAGE,
        VIDEO,
        UNKNOWN
    }

    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private val fragmentGalleryBinding
        get() = _fragmentGalleryBinding!!
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ScheduledExecutorService

    private var currentMediaUri: Uri? = null

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // Handle the returned Uri
            uri?.let { mediaUri ->
                currentMediaUri = mediaUri
                Log.v("tuancoltech", "getContent uri: $mediaUri")
                when (val mediaType = loadMediaType(mediaUri)) {
                    MediaType.IMAGE -> runDetectionOnImage(mediaUri)
                    MediaType.VIDEO -> runDetectionOnVideo(mediaUri)
                    MediaType.UNKNOWN -> {
                        updateDisplayView(mediaType)
                        Toast.makeText(
                            requireContext(),
                            "Unsupported data type.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding =
            FragmentGalleryBinding.inflate(inflater, container, false)

        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentGalleryBinding.fabGetContent.setOnClickListener {
            getContent.launch(arrayOf("image/*", "video/*"))
        }

        initBottomSheetControls()
    }

    override fun onPause() {
        fragmentGalleryBinding.overlay.clear()
        if (fragmentGalleryBinding.videoView.isPlaying) {
            fragmentGalleryBinding.videoView.stopPlayback()
        }
        fragmentGalleryBinding.videoView.visibility = View.GONE
        super.onPause()
    }

    private fun initBottomSheetControls() {
        // init bottom sheet settings
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseDetectionConfidence
            )
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseTrackingConfidence
            )
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPosePresenceConfidence
            )

        // When clicked, lower detection score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdMinus.setOnClickListener {
            if (viewModel.currentMinPoseDetectionConfidence >= 0.2) {
                viewModel.setMinPoseDetectionConfidence(viewModel.currentMinPoseDetectionConfidence - 0.1f)
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdPlus.setOnClickListener {
            if (viewModel.currentMinPoseDetectionConfidence <= 0.8) {
                viewModel.setMinPoseDetectionConfidence(viewModel.currentMinPoseDetectionConfidence + 0.1f)
                updateControlsUi()
            }
        }

        // When clicked, lower pose tracking score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdMinus.setOnClickListener {
            if (viewModel.currentMinPoseTrackingConfidence >= 0.2) {
                viewModel.setMinPoseTrackingConfidence(
                    viewModel.currentMinPoseTrackingConfidence - 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, raise pose tracking score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdPlus.setOnClickListener {
            if (viewModel.currentMinPoseTrackingConfidence <= 0.8) {
                viewModel.setMinPoseTrackingConfidence(
                    viewModel.currentMinPoseTrackingConfidence + 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, lower pose presence score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdMinus.setOnClickListener {
            if (viewModel.currentMinPosePresenceConfidence >= 0.2) {
                viewModel.setMinPosePresenceConfidence(
                    viewModel.currentMinPosePresenceConfidence - 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, raise pose presence score threshold floor
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdPlus.setOnClickListener {
            if (viewModel.currentMinPosePresenceConfidence <= 0.8) {
                viewModel.setMinPosePresenceConfidence(
                    viewModel.currentMinPosePresenceConfidence + 0.1f
                )
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.setSelection(
            viewModel.currentDelegate,
            false
        )
        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ) {

                    viewModel.setDelegate(p2)
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        // When clicked, change the underlying model used for object detection
        fragmentGalleryBinding.bottomSheetLayout.spinnerModel.setSelection(
            viewModel.currentModel,
            false
        )
        fragmentGalleryBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ) {
                    poseLandmarkerHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        if (fragmentGalleryBinding.videoView.isPlaying) {
            fragmentGalleryBinding.videoView.stopPlayback()
        }
        fragmentGalleryBinding.videoView.visibility = View.GONE
        fragmentGalleryBinding.imageResult.visibility = View.GONE
        fragmentGalleryBinding.overlay.clear()
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseDetectionConfidence
            )
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseTrackingConfidence
            )
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPosePresenceConfidence
            )

        fragmentGalleryBinding.overlay.clear()
        fragmentGalleryBinding.tvPlaceholder.visibility = View.VISIBLE
    }

    // Load and display the image.
    private fun runDetectionOnImage(uri: Uri) {
        setUiEnabled(false)
        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        updateDisplayView(MediaType.IMAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(
                requireActivity().contentResolver,
                uri
            )
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(
                requireActivity().contentResolver,
                uri
            )
        }
            .copy(Bitmap.Config.ARGB_8888, true)
            ?.let { bitmap ->
                fragmentGalleryBinding.imageResult.setImageBitmap(bitmap)

                // Run pose landmarker on the input image
                backgroundExecutor.execute {

                    poseLandmarkerHelper =
                        PoseLandmarkerHelper(
                            context = requireContext(),
                            runningMode = RunningMode.IMAGE,
                            minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                            minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                            minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                            currentDelegate = viewModel.currentDelegate
                        )

                    poseLandmarkerHelper.detectImage(bitmap)?.let { result ->
                        activity?.runOnUiThread {
                            fragmentGalleryBinding.overlay.setResults(
                                result.results[0],
                                bitmap.height,
                                bitmap.width,
                                RunningMode.IMAGE
                            )

                            setUiEnabled(true)
                            fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
                                String.format("%d ms", result.inferenceTime)
                        }
                    } ?: run { Log.e(TAG, "Error running pose landmarker.") }

                    poseLandmarkerHelper.clearPoseLandmarker()
                }
            }
    }

    private fun runDetectionOnVideo(uri: Uri) {
        Log.i("tuancoltech", "runDetectionOnVideo: $uri")
        setUiEnabled(false)
        updateDisplayView(MediaType.VIDEO)

        with(fragmentGalleryBinding.videoView) {
            setVideoURI(uri)
            // mute the audio
            setOnPreparedListener {
//                it.isLooping = true
                it.setVolume(0f, 0f)
            }
            requestFocus()
        }

        analyzePoseJob?.cancel(true)
        detectOnVideoJob?.cancel(true)
        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        detectOnVideoJob = backgroundExecutor.schedule({
            poseLandmarkerHelper =
                PoseLandmarkerHelper(
                    context = requireContext(),
                    runningMode = RunningMode.VIDEO,
                    minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                    minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                    minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                    currentDelegate = viewModel.currentDelegate
                )

            activity?.runOnUiThread {
                fragmentGalleryBinding.videoView.visibility = View.GONE
                fragmentGalleryBinding.progress.visibility = View.VISIBLE
            }

            poseLandmarkerHelper.detectVideoFile(uri, VIDEO_INTERVAL_MS)
                ?.let { resultBundle ->
                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
                }
                ?: run { Log.e(TAG, "Error running pose landmarker.") }

            poseLandmarkerHelper.clearPoseLandmarker()
        }, 0, TimeUnit.MILLISECONDS)
//        backgroundExecutor.execute {
//
//            poseLandmarkerHelper =
//                PoseLandmarkerHelper(
//                    context = requireContext(),
//                    runningMode = RunningMode.VIDEO,
//                    minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
//                    minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
//                    minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
//                    currentDelegate = viewModel.currentDelegate
//                )
//
//            activity?.runOnUiThread {
//                fragmentGalleryBinding.videoView.visibility = View.GONE
//                fragmentGalleryBinding.progress.visibility = View.VISIBLE
//            }
//
//            poseLandmarkerHelper.detectVideoFile(uri, VIDEO_INTERVAL_MS)
//                ?.let { resultBundle ->
//                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
//                }
//                ?: run { Log.e(TAG, "Error running pose landmarker.") }
//
//            poseLandmarkerHelper.clearPoseLandmarker()
//        }

        fragmentGalleryBinding.videoView.setOnCompletionListener {
            Log.i("tuancoltech", "Video playback completed. Looping...")

            poseLandmarkerHelper.setupPoseLandmarker()
//            fragmentGalleryBinding.videoView.start()
//            poseLandmarkerHelper.detectVideoFile(uri, VIDEO_INTERVAL_MS)
//                ?.let { resultBundle ->
//                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
//                }
//                ?: run { Log.e(TAG, "Error running pose landmarker.") }
//            runDetectionOnVideo(uri)
            analyzePoseJob?.cancel(true)
            detectOnVideoJob?.cancel(true)
            if (backgroundExecutor.isShutdown) {
                backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
            }
            backgroundExecutor.execute {
                poseLandmarkerHelper.detectVideoFile(uri, VIDEO_INTERVAL_MS)
                    ?.let { resultBundle ->
                        activity?.runOnUiThread { displayVideoResult(resultBundle) }
                    }
                    ?: run { Log.e(TAG, "Error running pose landmarker.") }
            }
        }
    }

    private var analyzePoseJob: ScheduledFuture<*>? = null
    private var detectOnVideoJob: ScheduledFuture<*>? = null

    // Setup and display the video.
    private fun displayVideoResult(result: PoseLandmarkerHelper.ResultBundle) {

        fragmentGalleryBinding.videoView.visibility = View.VISIBLE
        fragmentGalleryBinding.progress.visibility = View.GONE

        fragmentGalleryBinding.videoView.start()
        val videoStartTimeMs = SystemClock.uptimeMillis()

        Log.d("tuancoltech", "displayVideoResult result.size: ${result.results.size} ")

        if (backgroundExecutor.isShutdown) {
            backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        }

        var pushUpCount = 0
        var lastSampleTs = SystemClock.elapsedRealtime()

        val pushUpDetector = PushUpDetector { count ->
//                            if (isPushUp) {
//                                Log.d("tuancoltech2", "Detected push-up pose!")
//                            }
            Log.d("tuancoltech2", "Detected push-up rep. count: $count")

            activity?.runOnUiThread {
                if (count > 0) {
                    fragmentGalleryBinding.tvPushupOutput.visibility = View.VISIBLE
                    fragmentGalleryBinding.tvPushupOutput.text = "Push-up count: $count"
                }
            }
        }

        val plankDetector = PlankDetector { durationSeconds ->
            Log.d("tuancoltech2", "Detected plank. duration: $durationSeconds seconds")
            activity?.runOnUiThread {

                if (durationSeconds > 0) {
                    fragmentGalleryBinding.tvPlankOutput.visibility = View.VISIBLE
                    fragmentGalleryBinding.tvPlankOutput.text = "Plank duration: $durationSeconds seconds"
                    fragmentGalleryBinding.tvPlankOutput.setTextColor(ContextCompat.getColor(requireActivity(), R.color.mp_color_primary_variant))
                } else {
                    fragmentGalleryBinding.tvPlankOutput.text = "Invalid plank position!"
                    fragmentGalleryBinding.tvPlankOutput.setTextColor(ContextCompat.getColor(requireActivity(), R.color.mp_color_error))
                }
            }
        }

        analyzePoseJob = backgroundExecutor.scheduleWithFixedDelay(
            {
                activity?.runOnUiThread {
                    val videoElapsedTimeMs =
                        SystemClock.uptimeMillis() - videoStartTimeMs
                    val resultIndex =
                        videoElapsedTimeMs.div(VIDEO_INTERVAL_MS).toInt()

                    if (resultIndex >= result.results.size || fragmentGalleryBinding.videoView.visibility == View.GONE) {
                        // The video playback has finished so we stop drawing bounding boxes
                        Log.w("tuancoltech", "shutting down backgroundExecutor as resultIndex: $resultIndex results.size: ${result.results.size} videoView.isGone: ${fragmentGalleryBinding.videoView.visibility == View.GONE}")
                        backgroundExecutor.shutdown()
                    } else {
                        Log.v("tuancoltech", "setResults on index: $resultIndex Size: ${result.results.size} Should be drawing")
                        fragmentGalleryBinding.overlay.setResults(
                            result.results[resultIndex],
                            result.inputImageHeight,
                            result.inputImageWidth,
                            RunningMode.VIDEO
                        )

                        lifecycleScope.launch(Dispatchers.Default) {

                            // Push-up detector: This is current assuming that left side is more visible
                            // than right side of the body. Adjust the algorithm if needed by comparing
                            // visibility of a [NormalizedLandmark]'s between left and right side to
                            // decide which side is better

                            val leftShoulder = result.results[resultIndex].landmarks().get(0).get(11)
                            val leftHip = result.results[resultIndex].landmarks().get(0).get(23)
                            val leftAnkle = result.results[resultIndex].landmarks().get(0).get(27)
                            val leftElbow = result.results[resultIndex].landmarks().get(0).get(13)
                            val leftWrist = result.results[resultIndex].landmarks().get(0).get(15)
                            val leftEar = result.results[resultIndex].landmarks().get(0).get(7)

                            // Should be close to 0° or 180°
                            val shoulderHipAnkleAngle = Measurement.angleOneCommonPoint(leftShoulder, leftHip, leftAnkle)

                            // Should be close to 0°
                            val shoulderAnkleAngleWithHorizontal = Measurement.angleWithHorizontal(leftShoulder, leftAnkle)

                            // Should be close to 90°
                            val elbowWristAngleWithHorizontal = Measurement.angleWithHorizontal(leftElbow, leftWrist)

                            // When angle between left ear and left ankle is near 0°, then it's a
                            // repetition of push-up.
                            val leftEarAnkleAngleWithHorizontal = Measurement.angleWithHorizontal(leftEar, leftAnkle)

                            val shoulderElbowVersusWristAngle = Measurement.angleOneCommonPoint(leftShoulder, leftElbow, leftWrist)

                            Log.w("tuancoltech", "\nshoulderHipAnkleAngle: $shoulderHipAnkleAngle. " +
                                    "shoulderAnkleAngleWithHorizontal: $shoulderAnkleAngleWithHorizontal. " +
                                    "elbowWristAngleWithHorizontal: $elbowWristAngleWithHorizontal")

                            /*val isPushUp = */pushUpDetector.evaluatePoseFrame(
                                abs(shoulderHipAnkleAngle),
                                abs(shoulderAnkleAngleWithHorizontal),
                                abs(elbowWristAngleWithHorizontal),
                                abs(shoulderElbowVersusWristAngle)
                            )

                            plankDetector.evaluatePoseFrame(
                                abs(shoulderHipAnkleAngle),
                                abs(shoulderAnkleAngleWithHorizontal),
                                abs(elbowWristAngleWithHorizontal),
                                abs(shoulderElbowVersusWristAngle))

//                            withContext(Dispatchers.Main) {
//                                val sampleWindowMs = SystemClock.elapsedRealtime() - lastSampleTs
//                                if (isPushUp.first && isPushUp.second && sampleWindowMs > 1000L) {
//                                    pushUpCount ++
//                                    fragmentGalleryBinding.tvOutput.visibility = View.VISIBLE
//                                    fragmentGalleryBinding.tvOutput.text = "Push-up count: $pushUpCount"
//                                    Log.w("tuancoltech2", "\nshoulderHipAnkleAngle: $shoulderHipAnkleAngle. " +
//                                            "shoulderAnkleAngleWithHorizontal: $shoulderAnkleAngleWithHorizontal. " +
//                                            "elbowWristAngleWithHorizontal: $elbowWristAngleWithHorizontal. " +
//                                            "shoulderElbowVersusWristAngle: $shoulderElbowVersusWristAngle")
//                                    lastSampleTs = SystemClock.elapsedRealtime()
//                                }
//                                Log.v("tuancoltech2", "isPushUp: ${isPushUp.first} pushUpCount: $pushUpCount resultIdx: $resultIndex shoulderElbowVersusWristAngle: $shoulderElbowVersusWristAngle")
//                            }
                        }

                        setUiEnabled(true)

                        fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
                            String.format("%d ms", result.inferenceTime)
                    }
                }
            },
            0,
            VIDEO_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun updateDisplayView(mediaType: MediaType) {
        fragmentGalleryBinding.imageResult.visibility =
            if (mediaType == MediaType.IMAGE) View.VISIBLE else View.GONE
        fragmentGalleryBinding.videoView.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
        fragmentGalleryBinding.tvPlaceholder.visibility =
            if (mediaType == MediaType.UNKNOWN) View.VISIBLE else View.GONE
    }

    // Check the type of media that user selected.
    private fun loadMediaType(uri: Uri): MediaType {
        val mimeType = context?.contentResolver?.getType(uri)
        mimeType?.let {
            if (mimeType.startsWith("image")) return MediaType.IMAGE
            if (mimeType.startsWith("video")) return MediaType.VIDEO
        }

        return MediaType.UNKNOWN
    }

    private fun setUiEnabled(enabled: Boolean) {
        fragmentGalleryBinding.fabGetContent.isEnabled = enabled
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdMinus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.detectionThresholdPlus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdMinus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.trackingThresholdPlus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdMinus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.presenceThresholdPlus.isEnabled =
            enabled
        fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.isEnabled =
            enabled
    }

    private fun classifyingError() {
        activity?.runOnUiThread {
            fragmentGalleryBinding.progress.visibility = View.GONE
            setUiEnabled(true)
            updateDisplayView(MediaType.UNKNOWN)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        classifyingError()
        Log.e("tuancoltech", "onError: $error errorCode: $errorCode")
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                fragmentGalleryBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                    PoseLandmarkerHelper.DELEGATE_CPU,
                    false
                )
            }
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        // no-op
        Log.d("tuancoltech", "onResults size: ${resultBundle.results.size}")
    }

    companion object {
        private const val TAG = "GalleryFragment"

        // Value used to get frames at specific intervals for inference (e.g. every 300ms)
        private const val VIDEO_INTERVAL_MS = 150L
    }
}
