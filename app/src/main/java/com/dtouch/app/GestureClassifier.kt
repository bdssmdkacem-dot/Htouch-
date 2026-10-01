package com.dtouch.app

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.hypot

enum class Gesture { NONE, POINT, PINCH, OPEN_PALM, FIST, PEACE }

object GestureClassifier {
    private const val WRIST = 0
    private const val THUMB_TIP = 4
    private const val INDEX_MCP = 5
    private const val INDEX_TIP = 8
    private const val MIDDLE_TIP = 12
    private const val RING_TIP = 16
    private const val PINKY_TIP = 20
    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float =
        hypot(a.x() - b.x(), a.y() - b.y())
    private fun extended(l: List<NormalizedLandmark>, tip: Int): Boolean =
        dist(l[tip], l[WRIST]) > dist(l[tip - 2], l[WRIST]) * 1.05f
    fun classify(l: List<NormalizedLandmark>): Gesture {
        if (l.size < 21) return Gesture.NONE
        val handSize = dist(l[WRIST], l[INDEX_MCP]).coerceAtLeast(0.01f)
        val pinch = dist(l[THUMB_TIP], l[INDEX_TIP]) / handSize < 0.35f
        val index = extended(l, INDEX_TIP)
        val middle = extended(l, MIDDLE_TIP)
        val ring = extended(l, RING_TIP)
        val pinky = extended(l, PINKY_TIP)
        return when {
            pinch -> Gesture.PINCH
            index && middle && ring && pinky -> Gesture.OPEN_PALM
            index && middle && !ring && !pinky -> Gesture.PEACE
            index && !middle && !ring && !pinky -> Gesture.POINT
            !index && !middle && !ring && !pinky -> Gesture.FIST
            else -> Gesture.NONE
        }
    }
}
