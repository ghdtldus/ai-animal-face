package com.example.android

// 여기서 함수 import만 해주면 됨 (utils. 절대 안 씀)
import com.example.android.utils.uriToFile
import com.example.android.utils.uploadImageToServer
import com.example.android.utils.ImageUtils
import com.example.android.data.model.UploadResponse
import androidx.compose.foundation.Image
//File 사용을 위해 필요한 import
import java.io.File

//예외처리를 위해 필요한 import
import java.io.IOException

import com.example.android.ui.theme.AndroidTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import android.os.Build
import android.Manifest
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GalleryImagePicker(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GalleryImagePicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadResult by remember { mutableStateOf<String?>(null) }
    val photoFile = File(context.cacheDir, "camera_temp.jpg")
    // 사진 촬영 결과 저장용 Uri 생성

    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )

    // 카메라 호출 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri
            Log.d("CameraFlow", "카메라 촬영 성공: $photoUri")

            // 이미지 압축
            val compressedBitmap = ImageUtils.getCompressedBitmap(context, photoUri)
            if (compressedBitmap == null) {
                Log.e("CameraFlow", "Bitmap 압축 실패")
                return@rememberLauncherForActivityResult
            }

            val compressedFile = ImageUtils.bitmapToFile(context, compressedBitmap)
            if (compressedFile == null) {
                Log.e("CameraFlow", "Bitmap → File 변환 실패")
                return@rememberLauncherForActivityResult
            }

            uploadImageToServer(compressedFile, "male") { response ->
                if (response != null) {
                    Log.d("CameraFlow", "서버 응답 성공: ${response.main_result.animal}")
                    uploadResult = response.main_result.animal
                } else {
                    Log.e("CameraFlow", "서버 응답 실패")
                    uploadResult = "업로드 실패"
                }
            }
        }
    }

    // 갤러리 호출 런처
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            try {
                val compressedBitmap = ImageUtils.getCompressedBitmap(context, it)
                if (compressedBitmap == null) throw IOException("이미지를 불러올 수 없습니다.")

                val compressedFile = ImageUtils.bitmapToFile(context, compressedBitmap)
                uploadImageToServer(compressedFile, "male") { response ->
                    uploadResult = response?.main_result?.animal ?: "업로드 실패"
                }

            } catch (e: IOException) {
                Log.e("UploadFlow", "이미지 처리 실패: ${e.message}")
                Toast.makeText(context, e.message ?: "이미지 처리 중 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imageLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "이미지 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 갤러리 버튼
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                imageLauncher.launch("image/*")
            }
        }) {
            Text("갤러리에서 이미지 선택")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 카메라 버튼
        Button(onClick = {
            cameraLauncher.launch(photoUri)
        }) {
            Text("카메라로 촬영")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 이미지 미리보기
        selectedImageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "선택된 이미지",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }

        uploadResult?.let {
            Text("예측 결과: $it", modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidTheme {
        Text("Hello!")
    }
}
