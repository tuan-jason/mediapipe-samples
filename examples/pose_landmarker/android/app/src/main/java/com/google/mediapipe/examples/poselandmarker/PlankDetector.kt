package com.google.mediapipe.examples.poselandmarker

import android.util.Log

class PlankDetector(
    private val onValidPlankDurationUpdated: ((Int) -> Unit)? = null
) {
    private var startTimeMillis: Long? = null
    private var lastValidDurationSec = 0
    private var consecutiveInvalidFrames = 0

    private val invalidThreshold = 3 // number of invalid frames before reset
    private val frameIntervalMillis = 33L // ~30 FPS

    // Angle tolerances
    private val shoulderHipAnkleTolerance = 30f
    private val shoulderAnkleHorizontalTolerance = 20f
    private val elbowWristHorizontalTolerance = 10f
    private val shoulderElbowWristAngleTolerance = 20f

    /**
     * Called once per frame with pose angles calculated externally.
     */
    fun evaluatePoseFrame(
        shoulderHipAnkleAngle: Float,
        shoulderAnkleAngleWithHorizontal: Float,
        elbowWristAngleWithHorizontal: Float,
        shoulderElbowWristAngle: Float
    ) {
        val isAlignedSHA = isApproximately(shoulderHipAnkleAngle, 0f, shoulderHipAnkleTolerance) ||
                isApproximately(shoulderHipAnkleAngle, 180f, shoulderHipAnkleTolerance)

        val isHorizontalSA = isApproximately(shoulderAnkleAngleWithHorizontal, 0f, shoulderAnkleHorizontalTolerance) ||
                isApproximately(shoulderAnkleAngleWithHorizontal, 180f, shoulderAnkleHorizontalTolerance)

        val isHorizontalEW = isApproximately(elbowWristAngleWithHorizontal, 0f, elbowWristHorizontalTolerance) ||
                isApproximately(elbowWristAngleWithHorizontal, 180f, elbowWristHorizontalTolerance)

        val isAngleSEW = isApproximately(shoulderElbowWristAngle, 90f, shoulderElbowWristAngleTolerance)

        val isValidPlank = isAlignedSHA && isHorizontalSA && isHorizontalEW && isAngleSEW

        if (isValidPlank) {
            Log.i("tuancoltech1", "isValidPlank: $isValidPlank isAlignedSHA: $isAlignedSHA isHorizontalSA:$isHorizontalSA isHorizontalEW: $isHorizontalEW isAngleSEW: $isAngleSEW")
        } else {
            Log.w("tuancoltech1", "isValidPlank: $isValidPlank isAlignedSHA: $isAlignedSHA isHorizontalSA:$isHorizontalSA isHorizontalEW: $isHorizontalEW isAngleSEW: $isAngleSEW")
        }

        if (isValidPlank) {
            if (consecutiveInvalidFrames >= invalidThreshold || startTimeMillis == null) {
                // Reset timer and start fresh
                startTimeMillis = System.currentTimeMillis()
                lastValidDurationSec = 0
                onValidPlankDurationUpdated?.invoke(0)
            }
            consecutiveInvalidFrames = 0

            val elapsedMillis = System.currentTimeMillis() - startTimeMillis!!
            val elapsedSeconds = (elapsedMillis / 1000).toInt()
            if (elapsedSeconds > lastValidDurationSec) {
                lastValidDurationSec = elapsedSeconds
                onValidPlankDurationUpdated?.invoke(elapsedSeconds)
            }
        } else {
            consecutiveInvalidFrames++
            if (consecutiveInvalidFrames >= invalidThreshold) {
                reset()
            }
        }
    }

    private fun isApproximately(value: Float, target: Float, tolerance: Float): Boolean {
        return value in (target - tolerance)..(target + tolerance)
    }

    private fun reset() {
        startTimeMillis = null
        lastValidDurationSec = 0
        consecutiveInvalidFrames = 0
        onValidPlankDurationUpdated?.invoke(0)
    }

    fun getCurrentDurationSeconds(): Int = lastValidDurationSec
}
