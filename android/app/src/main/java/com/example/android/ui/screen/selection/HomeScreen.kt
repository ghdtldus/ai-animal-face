package com.example.android.ui.screen.selection

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.android.R
import com.example.android.utils.ImageUtils
import com.example.android.utils.ResultStorage
import com.example.android.utils.uploadImageToServer
import com.example.android.data.model.AnimalScore
import com.example.android.data.model.MainResult
import com.example.android.data.model.OfflineModeManager
import com.example.android.data.model.ResultBundle
import com.example.android.data.model.ResultLog
import com.example.android.model.LocalModelRunner
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 상태 변수들
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadResult by remember { mutableStateOf<String?>(null) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var topKResults by remember { mutableStateOf<List<AnimalScore>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf<String?>(null) }
    var isOffline by remember { mutableStateOf(OfflineModeManager.isOfflineModeEnabled(context)) }


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

    // 로컬 추론 함수
    fun runLocalInference(context: Context, compressedFile: File, gender: String): ResultBundle? {
        return LocalModelRunner.run(context, compressedFile, gender)
    }

    fun upload(compressedFile: File) {
        if (selectedGender == null) {
            Toast.makeText(context, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        if (isOffline) {
            // 오프라인 모드: 로컬 추론 실행
            val localResult = runLocalInference(context, compressedFile, selectedGender!!)
            isLoading = false

            if (localResult != null) {
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                ResultStorage.saveResult(
                    context,
                    ResultLog(
                        animal = localResult.uploadResult,
                        score = localResult.topKResults.firstOrNull()?.score ?: 0f,
                        date = today
                    )
                )

                val encodedJson = URLEncoder.encode(Gson().toJson(localResult), "UTF-8")
                navController.navigate("result/$encodedJson") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            } else {
                Toast.makeText(context, "로컬 추론 실패", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 온라인 모드: 서버 업로드 실행
            uploadImageToServer(
                compressedFile,
                selectedGender!!,
                onResult = { response ->
                    isLoading = false
                    if (response != null) {
                        Log.d("SharePageURL", "받은 URL: ${response.share_page_url}")
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
                            uploadResult = main.animal,
                            uploadMessage = response.message,
                            topKResults = response.top_k,
                            shareCardUrl = response.share_card_url,
                            uploadedImageUri = compressedFile.absolutePath,
                            sharePageUrl = response.share_page_url
                        )

                        val encodedJson = URLEncoder.encode(Gson().toJson(resultBundle), "UTF-8")
                        navController.navigate("result/$encodedJson") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        uploadResult = "업로드 실패"
                        uploadMessage = null
                        topKResults = emptyList()
                        Toast.makeText(context, "서버 업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { errorMessage ->
                    isLoading = false
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Image(
            painter = painterResource(id = R.drawable.imglogo),
            contentDescription = "앱 로고",
            modifier = Modifier
                .size(150.dp)
        )

        OfflineModeToggle(
            isOffline = isOffline,
            onToggle = { newValue ->
                isOffline = newValue
                OfflineModeManager.setOfflineModeEnabled(context, newValue)
            }
        )

        Image(
            painter = painterResource(id = R.drawable.lbselect_gender),
            contentDescription = "성별을 선택해주세요",
            modifier = Modifier
                .height(50.dp)
                .width(300.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val maleSelected = selectedGender == "male"
            Image(
                painter = painterResource(id = if (maleSelected) R.drawable.male_selected else R.drawable.male_unselected),
                contentDescription = "남성 선택",
                modifier = Modifier
                    .height(70.dp)
                    .width(130.dp)
                    .clickable { selectedGender = "male" }
            )

            Spacer(modifier = Modifier.width(24.dp))

            val femaleSelected = selectedGender == "female"
            Image(
                painter = painterResource(id = if (femaleSelected) R.drawable.female_selected else R.drawable.female_unselected),
                contentDescription = "여성 선택",
                modifier = Modifier
                    .height(70.dp)
                    .width(130.dp)
                    .clickable { selectedGender = "female" }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Image(
            painter = painterResource(id = R.drawable.lbselect_picture),
            contentDescription = "사진을 선택해주세요",
            modifier = Modifier
                .height(50.dp)
                .width(300.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.btgallery),
                contentDescription = "갤러리에서 이미지 선택",
                modifier = Modifier
                    .size(width = 300.dp, height = 60.dp)
                    .clickable(enabled = !isLoading) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            imageLauncher.launch("image/*")
                        }
                    }
            )

            Image(
                painter = painterResource(id = R.drawable.btcamera),
                contentDescription = "카메라 열기",
                modifier = Modifier
                    .size(width = 300.dp, height = 60.dp)
                    .clickable(enabled = !isLoading) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            cameraLauncher.launch(photoUri)
                        }
                    }
            )
        }
    }
}

@Composable
fun OfflineModeToggle(isOffline: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.lbmobile_mode),
            contentDescription = "모바일 모드",
            modifier = Modifier
                .height(50.dp)
                .width(130.dp)
        )

        Spacer(modifier = Modifier.width(20.dp))

        CustomSwitch(isOffline = isOffline, onToggle = onToggle)
    }
}

@Composable
fun CustomSwitch(isOffline: Boolean, onToggle: (Boolean) -> Unit) {
    Switch(
        checked = isOffline,
        onCheckedChange = onToggle,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFFFED2AC),
            checkedTrackColor = Color(0xFF705438),
            uncheckedThumbColor = Color(0xFF705438),
            uncheckedTrackColor = Color(0xFFFED2AC)
        )
    )
}
