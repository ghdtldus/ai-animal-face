package com.example.android.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.android.data.model.AnimalScore
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import com.example.android.utils.ImageUtils
import com.example.android.utils.ImageUtils.uriToAccessibleFile
import android.net.Uri
import java.io.File

@Composable
fun ResultScreen(
    uploadResult: String,
    uploadMessage: String?,
    topKResults: List<AnimalScore>,
    sharePageUrl: String?,
    navController: NavHostController,
    onRetry: () -> Unit,
    uploadedImageUri: String?
) {
    val context = LocalContext.current

    LaunchedEffect(uploadedImageUri) {
        uploadedImageUri?.let {
            Log.d("URI 확인", "uploadedImageUri = $it")
            when {
                it.startsWith("content://") -> Log.d("URI 확인", "✅ 갤러리 이미지")
                it.startsWith("file://") -> Log.d("URI 확인", "✅ 임시 저장 파일")
                it.startsWith("https://") -> Log.d("URI 확인", "✅ 웹 이미지 URL")
                else -> Log.d( "URI 확인", "❌ 알 수 없는 URI 형식")
            }
        } ?: Log.d("URI 확인", "❌ uploadedImageUri = null")
    }

    val bitmap = remember(uploadedImageUri) {
        uploadedImageUri?.let { path ->
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                Log.e("🛑", "파일 디코딩 실패: ${e.message}")
                null
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 이미지 표시
        bitmap?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "업로드한 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }

        Text(
            text = "나의 동물상은",
            style = MaterialTheme.typography.labelLarge
        )

        // 메시지 출력
        uploadMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Top 2 혼합 바 시각화
        if (topKResults.size >= 2) {
            Text(
                text = "예측 결과:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val first = topKResults[0]
            val second = topKResults[1]

            CombinedAnimalBar(first = first, second = second)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 버튼 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    navController.popBackStack("home", inclusive = true)
                    navController.navigate("home")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("다시하기")
            }

            Spacer(modifier = Modifier.width(16.dp))

            sharePageUrl?.let { url ->
                Button(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, url)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "결과 공유하기"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("공유")
                }
            }
        }
    }
}

@Composable
fun CombinedAnimalBar(
    first: AnimalScore,
    second: AnimalScore
) {
    val total = first.score + second.score
    val firstRatio = first.score / total
    val secondRatio = second.score / total

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text("Top 2 예측 결과", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(12.dp))

        // 첫 번째 동물
        AnimalBarRow(
            emoji = getEmoji(first.animal),
            name = first.animal,
            percent = firstRatio,
            barColor = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 두 번째 동물
        AnimalBarRow(
            emoji = getEmoji(second.animal),
            name = second.animal,
            percent = secondRatio,
            barColor = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun AnimalBarRow(
    emoji: String,
    name: String,
    percent: Float,
    barColor: Color
) {
    val percentLabel = "${(percent * 100).toInt()}%"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$emoji $name", modifier = Modifier.weight(1f))
            Text(percentLabel)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), shape = MaterialTheme.shapes.extraSmall)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent)
                    .background(barColor, shape = MaterialTheme.shapes.extraSmall)
            )
        }
    }
}

// 동물 이름에 따라 이모지 매칭
fun getEmoji(animal: String): String {
    return when (animal) {
        "강아지상" -> "🐶"
        "고양이상" -> "🐱"
        "곰상" -> "🐻"
        "토끼상" -> "🐰"
        "거북이상" -> "🐢"
        "사슴상" -> "🦌"
        "늑대상" -> "🐺"
        "호랑이상" -> "🐯"
        "다람쥐상" -> "🐿️"
        "공룡상" -> "🦖"
        "뱀상" -> "🐍"
        else -> "🐾"
    }
}
