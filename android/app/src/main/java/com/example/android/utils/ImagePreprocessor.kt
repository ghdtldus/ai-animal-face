// ImagePreprocessor.kt
package com.example.android.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.core.graphics.scale
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object ImagePreprocessor {
    private lateinit var faceDetector: FaceDetector
    private const val TAG = "ImagePreprocessor"

    fun initialize(context: Context) {
        Log.d(TAG, "initialize() 실행됨")

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("blaze_face_short_range.tflite")
            .setDelegate(Delegate.CPU)
            .build()

        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinDetectionConfidence(0.6f)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        try {
            faceDetector = FaceDetector.createFromOptions(context, options)
            Log.d(TAG, "FaceDetector 생성 성공")
        } catch (e: Exception) {
            Log.e(TAG, "FaceDetector 생성 실패: ${e.message}")
        }
    }

    fun detectFace(bitmap: Bitmap): Bitmap? {
        Log.d(TAG, "detectFace() 시작됨. 입력 이미지 크기: ${bitmap.width}x${bitmap.height}")

        if (!::faceDetector.isInitialized) {
            Log.e(TAG, "FaceDetector가 초기화되지 않았습니다.")
            return null
        }

        val mpImage = BitmapImageBuilder(bitmap).build()
        val result: FaceDetectorResult = faceDetector.detect(mpImage) ?: run {
            Log.w(TAG, "FaceDetectorResult == null")
            return null
        }

        Log.d(TAG, "FaceDetector 감지 수: ${result.detections().size}")

        if (result.detections().isEmpty()) {
            Log.w(TAG, "얼굴이 감지되지 않음")
            return null
        }

        val bbox = result.detections().first().boundingBox()
        Log.d(TAG, "감지된 얼굴 bbox: $bbox")

        val left = bbox.left.toInt().coerceIn(0, bitmap.width)
        val top = bbox.top.toInt().coerceIn(0, bitmap.height)
        val right = bbox.right.toInt().coerceIn(0, bitmap.width)
        val bottom = bbox.bottom.toInt().coerceIn(0, bitmap.height)

        val width = right - left
        val height = bottom - top

        return try {
            Bitmap.createBitmap(bitmap, left, top, width, height)
        } catch (e: Exception) {
            Log.e(TAG, "얼굴 자르기 실패: ${e.message}")
            null
        }
    }

    fun preprocess(bitmap: Bitmap): ByteBuffer {
        val faceBitmap = detectFace(bitmap)
            ?: throw Exception("얼굴이 감지되지 않았습니다. 정면 얼굴 사진을 다시 업로드해주세요.")

        val squareCropped = cropToSquareFace(faceBitmap)
        val resized = squareCropped.scale(224, 224)
        return convertBitmapToByteBuffer(resized)
    }

    private fun cropToSquareFace(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, left, top, size, size)
    }

    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).order(ByteOrder.nativeOrder())
        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixel in intValues) {
            buffer.putFloat((pixel shr 16 and 0xFF) / 255.0f)
            buffer.putFloat((pixel shr 8 and 0xFF) / 255.0f)
            buffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        buffer.rewind()
        return buffer
    }
}