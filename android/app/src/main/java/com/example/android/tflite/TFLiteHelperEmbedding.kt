package com.example.android.tflite
//
import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class `TFLiteHelperEmbedding`(context: Context) {
    private val interpreter: Interpreter
    private val meanEmbeddings: Map<String, FloatArray>
    private val inputShape: IntArray
    private val embeddingIndex = 173  // 추론 결과에서 임베딩 벡터가 위치한 텐서 인덱스

    init {
        // TFLite 모델 초기화 및 평균 임베딩 로드
        interpreter = Interpreter(loadModelFile(context, "efficientnet.tflite"))
        meanEmbeddings = loadEmbeddingsFromJson(context, "mean_embeddings.json")
        inputShape = interpreter.getInputTensor(0).shape() // 입력 텐서의 형태 (예: [1, 224, 224, 3])
    }

    // Bitmap 이미지를 입력받아 임베딩 벡터(float 배열) 반환
    fun getEmbeddingFromImage(bitmap: Bitmap): FloatArray {
        val input = convertBitmapToByteBuffer(bitmap) // 전처리된 이미지 입력 생성
        val output = Array(1) { FloatArray(embeddingSize()) } // 출력 공간 확보
        interpreter.run(input, output) // 모델 추론 실행
        return output[0] // 추출된 임베딩 벡터 반환
    }

    // 주어진 임베딩 벡터에 대해 동물상 예측 결과를 반환
    fun predictAnimalFace(embedding: FloatArray, gender: String? = null): List<Map<String, Any>> {
        val sims = meanEmbeddings.mapValues { cosineSimilarity(embedding, it.value) }.toMutableMap()
        val filtered = genderFilter(sims, gender)
        val adjusted = adjustSimilarity(filtered)
        val sorted = adjusted.toList().sortedByDescending { it.second }.take(5).map { it.first }
        val final = filterForbiddenPairs(sorted)

        val selectedScores = final.map { it to (adjusted[it] ?: 0f) }

        // 소프트맥스 적용
        val maxLogit = selectedScores.maxOfOrNull { it.second } ?: 0f
        val expScores = selectedScores.map { Math.exp((it.second - maxLogit).toDouble()) }
        val sumExp = expScores.sum()
        val probs = expScores.map { (it / sumExp).toFloat() }

        // Map 형태로 반환
        return selectedScores.zip(probs).map { (pair, prob) ->
            mapOf("animal" to pair.first, "score" to String.format("%.1f", prob * 100).toFloat())
        }
    }


    // Interpreter 리소스 해제 함수
    // 추론이 끝난 후 리소스 해제를 위해 Interpreter를 명시적으로 닫아주는 함수
    // Interpreter는 네이티브 메모리를 사용하는 객체이므로 앱이 종료되기 전 close()를 호출하는 것이 안전함
    fun close() {
        interpreter.close()
    }

    // 모델의 출력 임베딩 크기 반환
    private fun embeddingSize(): Int = interpreter.getOutputTensor(embeddingIndex).shape()[1]

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val dot = a.zip(b).map { it.first * it.second }.sum() // 내적
        val normA = sqrt(a.map { it * it }.sum().toDouble()).toFloat() // A 벡터 크기
        val normB = sqrt(b.map { it * it }.sum().toDouble()).toFloat() // B 벡터 크기
        return dot / (normA * normB + 1e-10f) // 유사도 계산
    }

    // TFLite 모델을 assets 디렉터리에서 로드하여 ByteBuffer로 반환
    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val assetFile = context.assets.openFd(modelName)
        val input = assetFile.createInputStream()
        val buffer = ByteArray(assetFile.length.toInt())
        input.read(buffer)
        val byteBuffer = ByteBuffer.allocateDirect(buffer.size).order(ByteOrder.nativeOrder())
        byteBuffer.put(buffer)
        return byteBuffer
    }

    // 평균 임베딩을 JSON 파일로부터 로드하여 Map<String, FloatArray> 형태로 반환
    private fun loadEmbeddingsFromJson(context: Context, fileName: String): Map<String, FloatArray> {
        val jsonString = context.assets.open(fileName).bufferedReader().use(BufferedReader::readText)
        val jsonObject = JSONObject(jsonString)
        return jsonObject.keys().asSequence().associateWith { key ->
            val jsonArray = jsonObject.getJSONArray(key)
            FloatArray(jsonArray.length()) { i -> jsonArray.getDouble(i).toFloat() }
        }
    }

    // Bitmap을 ByteBuffer로 변환 (모델 입력 전처리)
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputShape[1], inputShape[2], true) // 모델 입력 크기로 리사이즈
        val buffer = ByteBuffer.allocateDirect(4 * inputShape[1] * inputShape[2] * inputShape[3])
        buffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputShape[1] * inputShape[2])
        resized.getPixels(intValues, 0, resized.width, 0, 0, resized.width, resized.height)
        for (pixel in intValues) {
            // 픽셀에서 RGB 채널 분리 후 0~1 범위로 정규화하여 입력
            buffer.putFloat(((pixel shr 16 and 0xFF) / 255.0f)) // R
            buffer.putFloat(((pixel shr 8 and 0xFF) / 255.0f))  // G
            buffer.putFloat(((pixel and 0xFF) / 255.0f))        // B
        }

        // 모델 입력 전에 ByteBuffer의 position을 0으로 초기화하여 올바른 위치에서 읽도록 함
        buffer.rewind() // 모델 입력 전 포지션 초기화
        return buffer
    }

    // 금지 조합 정의
    private val forbiddenPairs = setOf(
        "cat" to "bear",
        "cat" to "dinosaur",
        "snake" to "bear",
        "rabbit" to "bear",
        "turtle" to "cat"
    )

    // 성별 기반 선호 동물 집합
    private val malePreference = setOf("bear", "tiger", "wolf")
    private val femalePreference = setOf("rabbit", "cat", "deer")

    // 금지 조합 필터링 함수: 동시에 나올 수 없는 동물 조합 제거
    private fun filterForbiddenPairs(topList: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (animal in topList) {
            if (result.all { (it to animal) !in forbiddenPairs && (animal to it) !in forbiddenPairs }) {
                result.add(animal)
            }
            if (result.size >= 2) break // 최대 2개까지만 선택
        }
        return result
    }

    // 성별에 따라 특정 동물 제외
    private fun genderFilter(sim: Map<String, Float>, gender: String?): Map<String, Float> = when (gender) {
        "male" -> sim.filterKeys { it !in femalePreference }
        "female" -> sim.filterKeys { it !in malePreference }
        else -> sim
    }

    // 유사도 상한 보정 및 top1 강조 처리
    private fun adjustSimilarity(sim: Map<String, Float>, maxPercent: Float = 0.7f, boost: Float = 0.05f): Map<String, Float> {
        if (sim.isEmpty()) return sim
        val maxVal = sim.maxOf { it.value }
        val scaled = if (maxVal > maxPercent) {
            val scale = maxPercent / maxVal // 유사도 상한선 초과 시 스케일링 적용
            sim.mapValues { (it.value * scale).coerceAtMost(1.0f) } // 1.0 넘지 않게 제한
        } else sim

        val sorted = scaled.toList().sortedByDescending { it.second }
        if (sorted.size >= 2) {
            val (k1, v1) = sorted[0]
            val (k2, v2) = sorted[1]
            // top1과 top2가 너무 비슷하면 top1 강조, top2 소폭 감소
            if (k1 != k2 && (v1 - v2) < 0.03f) {
                return scaled.toMutableMap().apply {
                    this[k1] = (v1 + boost).coerceAtMost(1.0f)
                    this[k2] = (v2 - boost).coerceAtLeast(0.0f)
                }
            }
        }
        return scaled
    }
}
