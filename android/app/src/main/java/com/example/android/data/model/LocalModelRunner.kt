//LocalModelRunner.kt

package com.example.android.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.android.data.model.AnimalScore
import com.example.android.data.model.MainResult
import com.example.android.data.model.ResultBundle
import com.example.android.utils.ImagePreprocessor
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object LocalModelRunner {
    private const val MODEL_FILE = "model_unquant.tflite"
    private const val IMAGE_SIZE = 224
    private const val NUM_CLASSES = 11

    private var interpreter: Interpreter? = null
    private var cachedLabels: List<String>? = null

    private val MESSAGES = mapOf(
        "wolf" to "늑대상! 강인하고 자유로운 영혼의 스타일이에요 🐺",
        "turtle" to "거북이상! 느긋하고 차분한 매력을 가진 스타일이에요 🐢",
        "tiger" to "호랑이상! 강인하고 자신감 넘치는 스타일이에요 🐯",
        "squirrel" to "다람쥐상! 활발하고 귀여운 에너지를 가진 스타일이에요 🐿️",
        "dinosaur" to "공룡상! 강력하고 존재감 넘치는 스타일이에요 🦖",
        "deer" to "사슴상! 우아하고 섬세한 느낌의 스타일이에요 🦌",
        "rabbit" to "토끼상! 귀엽고 사랑스러운 이미지를 가진 스타일이에요 🐰",
        "snake" to "뱀상! 신비롭고 매혹적인 분위기를 가진 스타일이에요 🐍",
        "bear" to "곰상! 든든하고 신뢰감을 주는 인상이에요 🐻",
        "cat" to "고양이상! 부드럽고 세련된 매력을 가진 스타일이에요 😺",
        "dog" to "강아지상! 충직하고 친근한 인상을 주는 스타일이에요 🐶"
    )

    private fun loadModel(context: Context) {
        if (interpreter == null) {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val fileInputStream = assetFileDescriptor.createInputStream()
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(modelBuffer)
        }
    }

    private fun loadLabelsFromAsset(context: Context): List<String> {
        cachedLabels?.let { return it }

        val labels = mutableListOf<String>()
        context.assets.open("labels.txt").bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val parts = line.trim().split(" ", limit = 2)
                if (parts.size == 2) {
                    labels.add(parts[1])
                }
            }
        }

        if (labels.size != NUM_CLASSES) {
            throw IllegalStateException("라벨 수(${labels.size})가 모델 클래스 수($NUM_CLASSES)와 일치하지 않습니다.")
        }

        cachedLabels = labels
        return labels
    }

    fun run(context: Context, imageFile: File, gender: String): ResultBundle? {
        try {
            loadModel(context)

            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

            // 얼굴 검출 및 전처리
            val preprocessed = ImagePreprocessor.preprocess(bitmap)
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, NUM_CLASSES), org.tensorflow.lite.DataType.FLOAT32)

            interpreter?.run(preprocessed, outputBuffer.buffer.rewind())

            val scores = outputBuffer.floatArray
            val animalLabels = loadLabelsFromAsset(context)
            val scoreMap = animalLabels.zip(scores.toList()).toMap().toMutableMap()

            // 성별 필터
            val malePreference = setOf("bear", "tiger", "wolf")
            val femalePreference = setOf("rabbit", "cat", "deer")
            val filteredScoreMap = when (gender.lowercase()) {
                "male" -> scoreMap.filterKeys { it !in femalePreference }.toMutableMap()
                "female" -> scoreMap.filterKeys { it !in malePreference }.toMutableMap()
                else -> scoreMap
            }

            // 금지 조합 필터
            val forbiddenPairs = setOf(
                "cat" to "bear", "cat" to "dinosaur", "snake" to "bear",
                "rabbit" to "bear", "turtle" to "cat"
            )

            val top5 = filteredScoreMap.entries.sortedByDescending { it.value }.take(5).map { it.key }

            fun filterForbiddenPairs(animals: List<String>): List<String> {
                val result = mutableListOf<String>()
                for (animal in animals) {
                    val conflict = result.any { existing ->
                        forbiddenPairs.contains(animal to existing) || forbiddenPairs.contains(existing to animal)
                    }
                    if (!conflict) result.add(animal)
                    if (result.size >= 2) break
                }
                return result
            }

            val filteredAnimals = filterForbiddenPairs(top5)
            val filteredScores = filteredAnimals.map { filteredScoreMap[it] ?: 0f }
            val maxScore = filteredScores.maxOrNull() ?: 0f
            val expScores = filteredScores.map { Math.exp((it - maxScore).toDouble()) }
            val sumExpScores = expScores.sum()
            val softmaxScores = expScores.map { (it / sumExpScores) * 100 }

            val topK = filteredAnimals.zip(softmaxScores).map { (animal, score) ->
                AnimalScore(animal = animal, score = score.toFloat())
            }

            val main = if (topK.isNotEmpty()) topK[0] else AnimalScore(animal = "unknown", score = 0f)
            val message = MESSAGES[main.animal] ?: "오프라인 추론 결과입니다."

            return ResultBundle(
                uploadResult = main,
                uploadMessage = message,
                topKResults = topK,
                uploadedImageUri = imageFile.absolutePath,
                shareCardUrl = "",
                sharePageUrl = ""
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }


    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }
}