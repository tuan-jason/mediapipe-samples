package com.google.mediapipe.examples.poselandmarker

import com.google.mediapipe.examples.poselandmarker.Measurement.angleOneCommonPoint
import com.google.mediapipe.examples.poselandmarker.Measurement.angleWithHorizontal
import com.google.mediapipe.examples.poselandmarker.Measurement.distance
import com.google.mediapipe.examples.poselandmarker.Measurement.pointPosition
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.*

import org.junit.Test
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MeasurementTest {

    private fun createPoint(x: Float, y: Float) = NormalizedLandmark.create(x, y, 0f)

    private fun angleOneCommonPointRounded(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Int {
        return angleOneCommonPoint(p1, p2, p3).roundToInt()
    }

    /**
     * Test cases for [angleOneCommonPoint] function
     */

    @Test
    fun testAngleOneCommonPointHorizontalLine() {
        val p1 = createPoint(0f, 0f)
        val p2 = createPoint(1f, 0f)
        val p3 = createPoint(2f, 0f)
        val result = angleOneCommonPoint(p1, p2, p3)
        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun testAngleRightAngleOneCommonPoint() {
        val p1 = createPoint(0f, 0f)
        val p2 = createPoint(1f, 0f)
        val p3 = createPoint(1f, 1f)
        val result = angleOneCommonPoint(p1, p2, p3)
        assertEquals(90.0, result, 0.0001)
    }

    @Test
    fun testAngleOneCommonPointObtuse() {
        val p1 = createPoint(0f, 0f)
        val p2 = createPoint(1f, 1f)
        val p3 = createPoint(2f, 0f)
        val result = angleOneCommonPoint(p1, p2, p3)
        assertEquals(90.0, result, 1.0) // approximate
    }

    @Test
    fun testAngleOneCommonPointInvalidPoints() {
        val p1 = createPoint(0f, 0f)
        val p2 = createPoint(0f, 0f)
        val p3 = createPoint(0f, 0f)
        val result = angleOneCommonPoint(p1, p2, p3)
        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun testAngleOneCommonPointStraightLine() {
        val p1 = createPoint(0f, 0f)
        val p2 = createPoint(1f, 1f)
        val p3 = createPoint(2f, 2f)
        val result = angleOneCommonPoint(p1, p2, p3)
        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun testAcuteAngleOneCommonPoint() {
        val p1 = createPoint(0f, 0f)
        val p2 = createPoint(1f, 0f)
        val p3 = createPoint(2f, 1f)
        assertEquals(45, angleOneCommonPointRounded(p1, p2, p3))
    }

    @Test
    fun testObtuseAngleOneCommonPoint() {
        val p1 = createPoint(0f, 0f)
        val p2 = createPoint(1f, 0f)
        val p3 = createPoint(2f, -1f)
        assertEquals(45, angleOneCommonPointRounded(p1, p2, p3))
    }

    /**
     * Test cases for [angleWithHorizontal] function
     */

    @Test
    fun testHorizontalRight() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(1f, 0f, 0f)
        assertEquals(0, angleWithHorizontal(p1, p2).roundToInt())
    }

    @Test
    fun testHorizontalLeft() {
        val p1 = NormalizedLandmark.create(1f, 0f, 0f)
        val p2 = NormalizedLandmark.create(0f, 0f, 0f)
        assertEquals(180, angleWithHorizontal(p1, p2).roundToInt())
    }

    @Test
    fun testVerticalUpward() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(0f, 1f, 0f)
        assertEquals(90, angleWithHorizontal(p1, p2).roundToInt())
    }

    @Test
    fun testVerticalDownward() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(0f, -1f, 0f)
        assertEquals(-90, angleWithHorizontal(p1, p2).roundToInt())
    }

    @Test
    fun testDiagonal45() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(1f, 1f, 0f)
        assertEquals(45, angleWithHorizontal(p1, p2).roundToInt())
    }

    @Test
    fun testDiagonal135() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(-1f, 1f, 0f)
        assertEquals(135, angleWithHorizontal(p1, p2).roundToInt())
    }

    @Test
    fun testDiagonalMinus45() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(1f, -1f, 0f)
        assertEquals(-45, angleWithHorizontal(p1, p2).roundToInt())
    }

    @Test
    fun testDiagonalMinus135() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(-1f, -1f, 0f)
        assertEquals(-135, angleWithHorizontal(p1, p2).roundToInt())
    }

    @Test
    fun testSamePointReturnsZero() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(0f, 0f, 0f)
        assertEquals(0, angleWithHorizontal(p1, p2).roundToInt())
    }

    /**
     * Test cases for [distance] function
     */
    @Test
    fun testHorizontalDistance() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(3f, 0f, 0f)
        assertEquals(3.0, distance(p1, p2), 1e-6)
    }

    @Test
    fun testVerticalDistance() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(0f, 4f, 0f)
        assertEquals(4.0, distance(p1, p2), 1e-6)
    }

    @Test
    fun testDiagonalDistance() {
        val p1 = NormalizedLandmark.create(0f, 0f, 0f)
        val p2 = NormalizedLandmark.create(3f, 4f, 0f)
        assertEquals(5.0, distance(p1, p2), 1e-6) // classic 3-4-5 triangle
    }

    @Test
    fun testSamePoint() {
        val p1 = NormalizedLandmark.create(2.5f, -1.2f, 0f)
        val p2 = NormalizedLandmark.create(2.5f, -1.2f, 0f)
        assertEquals(0.0, distance(p1, p2), 1e-6)
    }

    @Test
    fun testNegativeCoordinates() {
        val p1 = NormalizedLandmark.create(-1f, -1f, 0f)
        val p2 = NormalizedLandmark.create(1f, 1f, 0f)
        assertEquals(sqrt(8.0), distance(p1, p2), 1e-6)
    }

    /**
     * Test cases for [pointPosition] function
     */
    @Test
    fun testPointOnLeft() {
        val linePt1 = NormalizedLandmark.create(0f, 0f, 0f)
        val linePt2 = NormalizedLandmark.create(1f, 0f, 0f)
        val point = NormalizedLandmark.create(0.5f, 1f, 0f)
        assertEquals("left", pointPosition(point, linePt1, linePt2))
    }

    @Test
    fun testPointOnRight() {
        val linePt1 = NormalizedLandmark.create(0f, 0f, 0f)
        val linePt2 = NormalizedLandmark.create(1f, 0f, 0f)
        val point = NormalizedLandmark.create(0.5f, -1f, 0f)
        assertEquals("right", pointPosition(point, linePt1, linePt2))
    }

    @Test
    fun testPointOnLineReturnsLeft() {
        val linePt1 = NormalizedLandmark.create(0f, 0f, 0f)
        val linePt2 = NormalizedLandmark.create(1f, 1f, 0f)
        val point = NormalizedLandmark.create(0.5f, 0.5f, 0f) // exactly on the line
        assertEquals("left", pointPosition(point, linePt1, linePt2)) // per spec: value == 0 => "left"
    }

    @Test
    fun testVerticalLineLeft() {
        val linePt1 = NormalizedLandmark.create(0f, 0f, 0f)
        val linePt2 = NormalizedLandmark.create(0f, 1f, 0f)
        val point = NormalizedLandmark.create(-1f, 0.5f, 0f)
        assertEquals("left", pointPosition(point, linePt1, linePt2))
    }

    @Test
    fun testVerticalLineRight() {
        val linePt1 = NormalizedLandmark.create(0f, 0f, 0f)
        val linePt2 = NormalizedLandmark.create(0f, 1f, 0f)
        val point = NormalizedLandmark.create(1f, 0.5f, 0f)
        assertEquals("right", pointPosition(point, linePt1, linePt2))
    }

    @Test
    fun testPointBeyondLineStillCorrectSide() {
        val linePt1 = NormalizedLandmark.create(0f, 0f, 0f)
        val linePt2 = NormalizedLandmark.create(1f, 0f, 0f)
        val point = NormalizedLandmark.create(2f, 1f, 0f)
        assertEquals("left", pointPosition(point, linePt1, linePt2))
    }
}