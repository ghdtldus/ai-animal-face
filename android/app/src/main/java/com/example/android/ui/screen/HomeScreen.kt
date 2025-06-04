package com.example.android.ui.screen

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.android.utils.ImageUtils
import com.example.android.utils.uploadImageToServer
import com.example.android.data.model.AnimalScore
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.android.utils.ResultStorage
import com.example.android.data.model.ResultLog
import com.example.android.data.model.ResultBundle
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // remember 상태들
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadResult by remember { mutableStateOf<String?>(null) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var topKResults by remember { mutableStateOf<List<AnimalScore>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf<String?>(null) }

    // 상태 초기화
    LaunchedEffect(Unit) {
        selectedImageUri = null
        uploadResult = null
        uploadMessage = null
        topKResults = emptyList()
        selectedGender = null
    }

    val uuid = UUID.randomUUID().toString()
    val photoFile = File(context.cacheDir, "$uuid.jpg")

    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )

    fun upload(compressedFile: File) {
        if (selectedGender == null) {
            Toast.makeText(context, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        uploadImageToServer(
            compressedFile,
            selectedGender!!,
            onResult = { response ->
                isLoading = false
                if (response != null) {
                    uploadResult = response.main_result.animal
                    uploadMessage = response.message
                    topKResults = response.top_k

                    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val main = response.main_result
                    ResultStorage.saveResult(
                        context,
                        ResultLog(
                            animal = main.animal,
                            score = main.score,
                            date = today
                        )
                    )

                    val resultBundle = ResultBundle(
                        uploadResult = response.main_result.animal,
                        uploadMessage = response.message,
                        topKResults = response.top_k,
                        shareCardUrl = response.share_card_url,
                        uploadedImageUri = compressedFile.absolutePath
                    )

                    val encodedJson = URLEncoder.encode(Gson().toJson(resultBundle), "UTF-8")
                    navController.navigate("result/$encodedJson")
                }
            },
            onError = { errorMessage ->
                isLoading = false
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    // 카메라 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri
            val compressedBitmap = ImageUtils.getCompressedBitmap(context, photoUri)
            if (compressedBitmap == null) {
                Toast.makeText(context, "사진 처리 실패", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val compressedFile = ImageUtils.bitmapToFile(context, compressedBitmap)
            upload(compressedFile)
        }
    }

    // 갤러리 런처
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            try {
                val compressedBitmap = ImageUtils.getCompressedBitmap(context, it)
                if (compressedBitmap == null) throw IOException("이미지 압축 실패")
                val compressedFile = ImageUtils.bitmapToFile(context, compressedBitmap)
                upload(compressedFile)
            } catch (e: IOException) {
                Toast.makeText(context, e.message ?: "이미지 처리 중 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) imageLauncher.launch("image/*")
        else Toast.makeText(context, "이미지 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch(photoUri)
        else Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        }

        Text("성별을 선택해주세요:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedGender = "male" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedGender == "male") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("남성")
            }
            Button(
                onClick = { selectedGender = "female" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedGender == "female") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("여성")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    imageLauncher.launch("image/*")
                }
            },
            enabled = !isLoading
        ) {
            Text("갤러리에서 이미지 선택")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    cameraLauncher.launch(photoUri)
                }
            },
            enabled = !isLoading
        ) {
            Text("카메라로 촬영")
        }

        Button(onClick = { navController.navigate("recent_results") }) {
            Text("최근 판별 결과 확인하러 가기")
        }

        Spacer(modifier = Modifier.height(20.dp))

        selectedImageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "선택된 이미지",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
    }
}
