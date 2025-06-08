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
import com.example.android.data.model.AnimalScore
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import android.net.Uri
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import android.provider.MediaStore
import android.content.ContentValues
import java.net.URL
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResultScreen(
    uploadResult: String,
    uploadMessage: String?,
    topKResults: List<AnimalScore>,
    sharePageUrl: String?,
    shareCardUrl: String?,
    navController: NavHostController,
    onRetry: () -> Unit,
    uploadedImageUri: String?
) {
    val context = LocalContext.current
    Log.d("ShareCardURL", "공유 카드 URL: $shareCardUrl")
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                1001
            )
        }
    }

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
                val realPath = if (path.startsWith("file://")) Uri.parse(path).path else path
                BitmapFactory.decodeFile(realPath)
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
                contentScale = ContentScale.Fit,
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
                    navController.popBackStack("home_screen", inclusive = true)
                    navController.navigate("home_screen")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("다시하기")
            }

            Spacer(modifier = Modifier.width(16.dp))

            sharePageUrl?.let { imageUrl ->
                Button(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, imageUrl)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "결과 공유하기"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("공유")
                }
                Spacer(modifier = Modifier.width(16.dp))

                shareCardUrl?.let { imageUrl ->
                    Button(
                        onClick = {
                            saveImageToGallery(context, imageUrl)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("저장")
                    }
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


fun downloadImage(context: Context, imageUrl: String) {
    try {
        Toast.makeText(context, "이미지 저장을 시작했어요!", Toast.LENGTH_SHORT).show()
        Log.d("ImageSave", "이미지 저장 요청 보냄: $imageUrl")

        val filename = "animal_face_result_${System.currentTimeMillis()}.png"
        val request = DownloadManager.Request(Uri.parse(imageUrl)).apply {
            setTitle("동물상 결과 저장")
            setDescription("이미지를 저장 중입니다.")
            val filename = "animal_face_result_${System.currentTimeMillis()}.png"
            setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, filename)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "이미지 저장을 시작했어요!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "이미지 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}


fun saveImageToGallery(context: Context, imageUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        Log.d("ImageSave", "공유 카드 저장 URL: $imageUrl")
        val contentResolver = context.contentResolver
        val filename = "animal_face_result_${System.currentTimeMillis()}.png"
        val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AnimalFaceApp")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val imageUri = contentResolver.insert(imageCollection, imageDetails)

        if (imageUri != null) {
            try {
                val outputStream = contentResolver.openOutputStream(imageUri)
                val inputStream = URL(imageUrl).openStream()

                inputStream.use { input ->
                    outputStream?.use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }

                imageDetails.clear()
                imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, imageDetails, null, null)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "갤러리에 이미지가 저장되었습니다!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ImageSave", "저장 실패 예외", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "이미지 저장 실패 (URI 생성 실패)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
