package com.google.mediapipe.examples.poselandmarker

import com.google.mediapipe.tasks.components.containers.Connection
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.POSE_LANDMARKS

object PoseLandmarks {
    val SHOULDER_HIP_LEFT = POSE_LANDMARKS.elementAt(22)
    val HIP_ANKLE_LEFT = POSE_LANDMARKS.elementAt(27)
}