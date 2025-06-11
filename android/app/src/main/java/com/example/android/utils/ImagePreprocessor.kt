package com.example.android.utils
//오프라인에서의 이미지 전처리
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImagePreprocessor {

    // 얼굴 검출기 옵션 설정
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    suspend fun preprocess(context: Context, bitmap: Bitmap): ByteBuffer {
        // 1. 얼굴 검출
        val face = detectFace(bitmap) ?: throw Exception("얼굴이 감지되지 않았습니다. 정면 얼굴 사진을 다시 업로드해주세요.")

        // 2. 얼굴 영역 크롭
        val faceBitmap = cropToSquareFace(bitmap, face.boundingBox)

        // 3. 224x224 리사이즈 + 정규화
        val resized = Bitmap.createScaledBitmap(faceBitmap, 224, 224, true)

        return convertBitmapToByteBuffer(resized)
    }

    private suspend fun detectFace(bitmap: Bitmap): com.google.mlkit.vision.face.Face? {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(image).await()
        return faces.firstOrNull()
    }

    private fun cropToSquareFace(bitmap: Bitmap, rect: android.graphics.Rect): Bitmap {
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()
        val size = maxOf(rect.width(), rect.height())
        val left = (cx - size / 2).toInt().coerceAtLeast(0)
        val top = (cy - size / 2).toInt().coerceAtLeast(0)
        val right = (cx + size / 2).toInt().coerceAtMost(bitmap.width)
        val bottom = (cy + size / 2).toInt().coerceAtMost(bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        byteBuffer.rewind()
        return byteBuffer
    }
}
