package com.google.mediapipe.examples.poselandmarker

import android.util.Log

class PushUpDetector(
    private val onPushUpRepetitionDetected: ((Int) -> Unit)? = null
) {

    private enum class State {
        WAITING_FOR_START,
        WAITING_FOR_END
    }

    private var state = State.WAITING_FOR_START
    private var repetitionCount = 0

    private val shoulderHipAnkleTolerance = 30f

    // This tolerance value could vary based on the camera angle. Can be between [0, 45]
    private val shoulderAnkleHorizontalTolerance = 45f

    private val elbowWristVerticalTolerance = 20f
    private val startAngleTolerance = 20f

    // This tolerance value could vary based on the difficulty of the push up exercise
    // Can be between [0, 21]
    private val endAngleTolerance = 21f

    /**
     * Evaluates angles for a single frame to track full push-up repetitions.
     */
    fun evaluatePoseFrame(
        shoulderHipAnkleAngle: Float,
        shoulderAnkleAngleWithHorizontal: Float,
        elbowWristAngleWithHorizontal: Float,
        shoulderElbowWristAngle: Float
    ) {
        val isShoulderHipAnkleAligned = isApproximately(shoulderHipAnkleAngle, 0f, shoulderHipAnkleTolerance) ||
                isApproximately(shoulderHipAnkleAngle, 180f, shoulderHipAnkleTolerance)

        val isShoulderAnkleHorizontal = isApproximately(shoulderAnkleAngleWithHorizontal, 0f, shoulderAnkleHorizontalTolerance) ||
                isApproximately(shoulderAnkleAngleWithHorizontal, 180f, shoulderAnkleHorizontalTolerance)

        val isElbowWristVertical = isApproximately(elbowWristAngleWithHorizontal, 90f, elbowWristVerticalTolerance)

        Log.d("tuancoltech", "evaluatePoseFrame detector: ${PushUpDetector2@this} state: $state. " +
                "shoulderHipAnkleAngle: $shoulderHipAnkleAngle. " +
                "shoulderAnkleAngleWithHorizontal: $shoulderAnkleAngleWithHorizontal. " +
                "elbowWristAngleWithHorizontal: $elbowWristAngleWithHorizontal. " +
                "shoulderElbowWristAngle: $shoulderElbowWristAngle")

        when (state) {
            State.WAITING_FOR_START -> {
                val isStartAngle =
                    isApproximately(shoulderElbowWristAngle, 0f, startAngleTolerance) ||
                            isApproximately(shoulderElbowWristAngle, 180f, startAngleTolerance)

                Log.d("tuancoltech", "WAITING_FOR_START isShoulderHipAnkleAligned: $isShoulderHipAnkleAligned " +
                        "isShoulderAnkleHorizontal: $isShoulderAnkleHorizontal " +
                        "isElbowWristVertical: $isElbowWristVertical " +
                        "isStartAngle: $isStartAngle")
                if (isShoulderHipAnkleAligned && isShoulderAnkleHorizontal && isElbowWristVertical && isStartAngle) {
                    Log.i("tuancoltech", "Setting state to WAITING_FOR_END")
                    state = State.WAITING_FOR_END
                }
            }

            State.WAITING_FOR_END -> {
                val isEndAngle = isApproximately(shoulderElbowWristAngle, 90f, endAngleTolerance)

                if (isShoulderHipAnkleAligned && isShoulderAnkleHorizontal && isElbowWristVertical && isEndAngle) {
                    repetitionCount++
                    onPushUpRepetitionDetected?.invoke(repetitionCount)
                    Log.i("tuancoltech", "Setting state to WAITING_FOR_START. repetitionCount: $repetitionCount")
                    state = State.WAITING_FOR_START // reset for next cycle
                }
            }
        }
    }

    private fun isApproximately(value: Float, target: Float, tolerance: Float): Boolean {
        return value in (target - tolerance)..(target + tolerance)
    }

    fun getRepetitionCount(): Int = repetitionCount
}

