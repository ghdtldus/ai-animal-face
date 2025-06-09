package com.example.android.tflite

import android.content.Context
import com.example.android.utils.ImagePreprocessor  // 이미지 전처리 유틸
import org.tensorflow.lite.Interpreter              // TFLite 인터프리터
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteHelper(context: Context) {
    private var interpreter: Interpreter  // TFLite 모델을 실행할 인터프리터 인스턴스

    // 분류할 클래스 이름 목록 (출력 순서와 일치해야 함)
    private val classNames = listOf(
        "bear", "snake", "cat", "dog", "wolf", "dinosaur",
        "squirrel", "rabbit", "tiger", "turtle", "deer"
    )

    // 백엔드와 동일한 금지 조합 설정
    private val forbiddenPairs = setOf(
        "cat" to "bear", "cat" to "dinosaur", "snake" to "bear",
        "rabbit" to "bear", "turtle" to "cat"
    )

    // 성별 기반 필터링: 남성 선호 동물상
    private val malePreference = setOf("bear", "tiger", "wolf")

    // 성별 기반 필터링: 여성 선호 동물상
    private val femalePreference = setOf("rabbit", "cat", "deer")

    init {
        // assets 폴더에서 TFLite 모델을 메모리에 로드
        val assetFileDescriptor = context.assets.openFd("model_unquant.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength

        // 모델을 ByteBuffer로 매핑
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        // Interpreter 생성
        interpreter = Interpreter(modelBuffer)
    }

    // 금지 조합 필터링 함수: topK 중에서 서로 조합이 가능한 2개만 선택
    private fun filterForbiddenPairs(topK: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (animal in topK) {
            // 현재까지 고른 동물들과 조합이 금지되지 않은 경우에만 추가
            if (result.all { (it to animal) !in forbiddenPairs && (animal to it) !in forbiddenPairs }) {
                result.add(animal)
            }
            if (result.size >= 2) break  // 최대 2개까지만 선택
        }
        return result
    }

    // 성별에 따라 점수 맵 필터링 (남성 → 여성 선호 제거 / 여성 → 남성 선호 제거)
    private fun genderFilter(scores: Map<String, Float>, gender: String?): Map<String, Float> {
        return when (gender) {
            "male" -> scores.filterKeys { it !in femalePreference }
            "female" -> scores.filterKeys { it !in malePreference }
            else -> scores
        }
    }

    // 동물상 예측 수행 함수 (비동기)
    suspend fun predictAnimalFace(context: Context, bitmap: android.graphics.Bitmap, gender: String? = null): List<Map<String, Any>> {
        // 입력 이미지 전처리 (크기 변환, 정규화 등)
        val inputBuffer = ImagePreprocessor.preprocess(context, bitmap)

        // 모델 출력용 버퍼 준비 (11 클래스 × 4바이트 float)
        val outputBuffer = ByteBuffer.allocateDirect(11 * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        // TFLite 모델 실행
        interpreter.run(inputBuffer, outputBuffer)

        // 결과 추출을 위해 버퍼를 처음으로 되감기
        outputBuffer.rewind()

        // 결과 값을 FloatArray로 변환
        val scores = FloatArray(11) { outputBuffer.float }

        // 클래스 이름과 예측 점수를 매핑
        val scoreMap = classNames.zip(scores.toList()).toMap()

        // 성별에 따라 불필요한 클래스 필터링
        val filteredScores = genderFilter(scoreMap, gender)

        // 예측 점수 기준 상위 5개 클래스 선택
        val topK = filteredScores.entries.sortedByDescending { it.value }.take(5).map { it.key }

        // 금지 조합 필터링을 통해 최종 2개 클래스 선택
        val selected = filterForbiddenPairs(topK)

        // 선택된 클래스와 점수 추출
        val selectedScores = selected.map { it to (filteredScores[it] ?: 0f) }

        // 소프트맥스를 통한 정규화된 확률 계산
        val maxLogit = selectedScores.maxOfOrNull { it.second } ?: 0f
        val expScores = selectedScores.map { Math.exp((it.second - maxLogit).toDouble()) }
        val sumExp = expScores.sum()
        val probs = expScores.map { (it / sumExp).toFloat() }

        // 결과를 "animal" + "score(%)" 형식의 리스트로 반환
        return selected.zip(probs).map { (animal, prob) ->
            mapOf("animal" to animal, "score" to String.format("%.1f", prob * 100).toFloat())
        }
    }

    // 리소스 정리: interpreter 종료
    fun close() {
        interpreter.close()
    }
}
