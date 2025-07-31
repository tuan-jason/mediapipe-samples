package com.google.mediapipe.examples.poselandmarker

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.*

object Measurement {

    /**
     *
     */
    fun angleOneCommonPoint(point1: NormalizedLandmark, point2: NormalizedLandmark, point3: NormalizedLandmark): Float {
        val ax = point2.x() - point1.x()
        val ay = point2.y() - point1.y()
        val bx = point3.x() - point2.x()
        val by = point3.y() - point2.y()

        val dot = ax * bx + ay * by
        val cross = ax * by - ay * bx

        val angleRad = atan2(abs(cross), dot)
        return (angleRad * 180.0 / PI).toFloat()
    }

    fun angleWithHorizontal(point1: NormalizedLandmark, point2: NormalizedLandmark): Float {
        val xDiff = point2.x() - point1.x()
        val yDiff = point2.y() - point1.y()
        return Math.toDegrees(atan2(yDiff, xDiff).toDouble()).toFloat()
    }

    fun distance(point1: NormalizedLandmark, point2: NormalizedLandmark): Float {
        val dx = point1.x() - point2.x()
        val dy = point1.y() - point2.y()
        return sqrt((dx * dx + dy * dy))
    }

    fun pointPosition(point: NormalizedLandmark, linePt1: NormalizedLandmark, linePt2: NormalizedLandmark): String {
        val value = (linePt2.x() - linePt1.x()) * (point.y() - linePt1.y()) -
                (linePt2.y() - linePt1.y()) * (point.x() - linePt1.x())

        return if (value >= 0) "left" else "right"
    }

}