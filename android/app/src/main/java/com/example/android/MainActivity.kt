package com.example.android

// ✅ 여기서 함수 import만 해주면 됨 (utils. 절대 안 씀)
import com.example.android.utils.uriToFile
import com.example.android.utils.uploadImageToServer
import com.example.android.data.model.UploadResponse
import androidx.compose.foundation.Image

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

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
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
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                imageLauncher.launch("image/*")
            }
        }) {
            Text("갤러리에서 이미지 선택")
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

            // ✅ 여기서 utils 없이 uriToFile 직접 호출
            val file = uriToFile(context, uri)

            if (file != null) {
                uploadImageToServer(file, "female") { result ->
                    if (result != null) {
                        Log.d("UploadResult", "주요 동물상: ${result.main_result.animal}")
                    } else {
                        Log.e("UploadResult", "업로드 실패")
                    }
                }
            }
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
