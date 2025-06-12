// TFLiteHelper.kt
package com.example.android.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.android.utils.ImagePreprocessor
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteHelper(context: Context) {
    private var interpreter: Interpreter

    private val classNames = listOf(
        "bear", "snake", "cat", "dog", "wolf", "dinosaur",
        "squirrel", "rabbit", "tiger", "turtle", "deer"
    )

    private val forbiddenPairs = setOf(
        "cat" to "bear", "cat" to "dinosaur", "snake" to "bear",
        "rabbit" to "bear", "turtle" to "cat"
    )

    private val malePreference = setOf("bear", "tiger", "wolf")
    private val femalePreference = setOf("rabbit", "cat", "deer")

    init {
        val assetFileDescriptor = context.assets.openFd("model_unquant.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(modelBuffer)
    }

    suspend fun predictAnimalFace(bitmap: Bitmap, gender: String? = null): List<Map<String, Any>> {
        Log.d("TFLiteHelper", "predictAnimalFace 시작됨")

        val inputBuffer = ImagePreprocessor.preprocess(bitmap)

        Log.d("TFLiteHelper", "preprocess() 완료, 모델 추론 시작")

        val outputBuffer = ByteBuffer.allocateDirect(11 * 4).order(ByteOrder.nativeOrder())
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val scores = FloatArray(11) { outputBuffer.float }
        val scoreMap = classNames.zip(scores.toList()).toMap()
        val filteredScores = genderFilter(scoreMap, gender)
        val topK = filteredScores.entries.sortedByDescending { it.value }.take(5).map { it.key }
        val selected = filterForbiddenPairs(topK)
        val selectedScores = selected.map { it to (filteredScores[it] ?: 0f) }

        val maxLogit = selectedScores.maxOfOrNull { it.second } ?: 0f
        val expScores = selectedScores.map { Math.exp((it.second - maxLogit).toDouble()) }
        val sumExp = expScores.sum()
        val probs = expScores.map { (it / sumExp).toFloat() }

        return selected.zip(probs).map { (animal, prob) ->
            mapOf("animal" to animal, "score" to String.format("%.1f", prob * 100).toFloat())
        }
    }

    private fun filterForbiddenPairs(topK: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (animal in topK) {
            if (result.all { (it to animal) !in forbiddenPairs && (animal to it) !in forbiddenPairs }) {
                result.add(animal)
            }
            if (result.size >= 2) break
        }
        return result
    }

    private fun genderFilter(scores: Map<String, Float>, gender: String?): Map<String, Float> {
        return when (gender) {
            "male" -> scores.filterKeys { it !in femalePreference }
            "female" -> scores.filterKeys { it !in malePreference }
            else -> scores
        }
    }

    fun close() {
        interpreter.close()
    }
}
