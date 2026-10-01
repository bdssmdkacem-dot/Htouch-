package com.dtouch.app

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.roundToInt

data class HandState(val gesture: Gesture, val x: Float, val y: Float)

class HandTracker(context: Context, private val onState: (HandState?) -> Unit) : ImageAnalysis.Analyzer {
    private val landmarker: HandLandmarker
    private var lastTs = 0L
    private var candidate = Gesture.NONE
    private var candidateCount = 0
    private var confirmed = Gesture.NONE
    private var sx = 0.5f
    private var sy = 0.5f

    init {
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.6f)
            .setMinTrackingConfidence(0.6f)
            .setResultListener { result, _ -> handle(result) }
            .build()
        landmarker = HandLandmarker.createFromOptions(context, options)
    }

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.uptimeMillis()
        var interval = (1000f / Prefs.get(Prefs.FPS)).toLong()
        if (EngineState.paused) interval = maxOf(interval, 200L)
        if (now - lastTs < interval) { image.close(); return }
        lastTs = now
        try {
            val rotation = image.imageInfo.rotationDegrees
            val bmp = image.toBitmap()
            val opts = ImageProcessingOptions.builder().setRotationDegrees(rotation).build()
            landmarker.detectAsync(BitmapImageBuilder(bmp).build(), opts, now)
        } catch (e: Exception) {
        } finally { image.close() }
    }

    private fun handle(result: HandLandmarkerResult) {
        val hand = result.landmarks().firstOrNull()
        if (hand == null) {
            candidate = Gesture.NONE; candidateCount = 0; confirmed = Gesture.NONE
            EngineState.handVisible = false
            EngineState.gesture = Gesture.NONE
            onState(null); return
        }
        val frames = if (EngineState.paused) 2 else Prefs.get(Prefs.CONFIRM).roundToInt()
        val raw = GestureClassifier.classify(hand, Prefs.get(Prefs.PINCH))
        if (raw == candidate) candidateCount++ else { candidate = raw; candidateCount = 1 }
        if (candidateCount >= frames) confirmed = candidate
        val alpha = Prefs.get(Prefs.SMOOTHING)
        val tip = hand[8]
        val mx = 1f - tip.x()
        sx += alpha * (mx - sx)
        sy += alpha * (tip.y() - sy)
        EngineState.handVisible = true
        EngineState.gesture = confirmed
        onState(HandState(confirmed, sx, sy))
    }

    fun close() { try { landmarker.close() } catch (e: Exception) { } }
}
