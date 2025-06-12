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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.android.R
import java.io.File
import com.example.android.MainActivity
import com.example.android.utils.getAnimalImageRes
import com.example.android.utils.getKoreanAnimalName
import androidx.compose.ui.text.font.FontWeight
import com.example.android.utils.getAnimalMessage
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.android.ui.theme.HakgyoFont
import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

@Composable
fun ResultScreen(
    uploadResult: AnimalScore,
    uploadMessage: String?,
    topKResults: List<AnimalScore>,
    sharePageUrl: String?,
    shareCardUrl: String?,
    navController: NavHostController,
    onRetry: () -> Unit,
    uploadedImageUri: String?
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
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
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.imglogo),
            contentDescription = "앱 로고",
            modifier = Modifier
                .size(150.dp)
        )

        // 이미지 표시
        bitmap?.let {
            val width = it.width
            val height = it.height
            val aspectRatio = width.toFloat() / height.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .border(5.dp, Color(0xFF705438), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "업로드한 이미지",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }


        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF3E9)),
            contentAlignment = Alignment.Center
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                val mainAnimal = topKResults.firstOrNull()?.animal ?: "default"
                val koreanName = getKoreanAnimalName(mainAnimal)
                val message = getAnimalMessage(mainAnimal)
                Image(
                    painter = painterResource(id = getAnimalImageRes(mainAnimal)),
                    contentDescription = "$mainAnimal 아이콘",
                    modifier = Modifier
                        .size(130.dp)
                        .padding(bottom = 2.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.lbyour_result),
                    contentDescription = "당신의 결과는",
                    modifier = Modifier
                        .height(50.dp)
                        .width(150.dp)
                )
                Text(
                    text = "$koreanName!!",
                    fontFamily = HakgyoFont,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF705438)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 24.sp
                    ),
                    color = Color(0xFF705438),
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.fillMaxWidth()
                )

                // Top 2 혼합 바 시각화
                if (topKResults.size >= 2) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val first = topKResults[0]
                    val second = topKResults[1]

                    CombinedAnimalBar(first = first, second = second)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.drawable.lbcheck_your_animal),
            contentDescription = "친구들의 동물상도 확인해봐!!",
            modifier = Modifier
                .height(50.dp)
                .width(250.dp)
        )

        // 버튼 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.btrestart),
                contentDescription = "다시하기",
                modifier = Modifier
                    .height(50.dp)
                    .width(150.dp)
                    .clickable {
                        navController.popBackStack("home_screen", inclusive = true)
                        navController.navigate("home_screen")
                    }
            )

            Spacer(modifier = Modifier.width(24.dp))

            
            Image(
                painter = painterResource(id = R.drawable.btsave),
                contentDescription = "저장하기",
                modifier = Modifier
                    .height(53.dp)
                    .width(165.dp)
                    .clickable {
                        shareCardUrl?.let { imageUrl ->
                            // 1. imageId 추출
                            val imageId = extractImageIdFromUrl(imageUrl)

                            // 2. 서버로 결과 전송 (finalize)
                            val resultMap = mapOf(
                                "main_result" to mapOf(
                                    "animal" to uploadResult.animal,
                                    "score" to uploadResult.score
                                ),
                                "top_k" to topKResults
                            )

                            val resultJson = Gson().toJson(resultMap) 
                            Log.d("resultJson", resultJson)  

                            val requestBody = MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("results", resultJson)
                                .addFormDataPart("image_id", imageId)
                                .build()

                            val request = Request.Builder()
                                .url("http://10.0.2.2:8000/upload/finalize")
                                .post(requestBody)
                                .build()

                            val client = OkHttpClient()
                            client.newCall(request).enqueue(object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e("FinalizeUpload", "서버 전송 실패", e)
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        Log.d("FinalizeUpload", "공유 이미지 생성 완료")
                                        // 3. 이미지 저장
                                        saveImageToGallery(context, imageUrl)
                                    }
                                }
                            })
                        }
                    }
            )
            
        }
        Spacer(modifier = Modifier.height(24.dp))
    

        Text(
            text = "⬅ 홈으로 돌아가기",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }
                .padding(bottom = 32.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black 
        )
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
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        AnimalBarRow(
            emoji = getEmoji(first.animal),
            name = first.animal,
            percent = firstRatio,
            barColor = Color(0xFF7AD8F7),
            backgroundColor = Color(0xFFC7F1FF)
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnimalBarRow(
            emoji = getEmoji(second.animal),
            name = second.animal,
            percent = secondRatio,
            barColor = Color(0xFFFFA680),
            backgroundColor = Color(0xFFFFD8C7)
        )
    }
}


@Composable
fun AnimalBarRow(
    emoji: String,
    name: String,
    percent: Float,
    barColor: Color,
    backgroundColor: Color
) {
    val percentLabel = "${(percent * 100).toInt()}%"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$emoji ${getKoreanAnimalName(name)}", modifier = Modifier.weight(1f))
            Text(percentLabel)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(backgroundColor, shape = MaterialTheme.shapes.extraSmall)
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
        "dog" -> "🐶"
        "cat" -> "🐱"
        "bear" -> "🐻"
        "rabbit" -> "🐰"
        "turtle" -> "🐢"
        "deer" -> "🦌"
        "wolf" -> "🐺"
        "tiger" -> "🐯"
        "squirrel" -> "🐿️"
        "dinosaur" -> "🦖"
        "snake" -> "🐍"
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


fun extractImageIdFromUrl(url: String): String {
    return url.substringAfterLast("/").substringBefore("_")
}