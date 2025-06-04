package com.example.android.ui.component

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.android.utils.ShareManager

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val animalName = intent.getStringExtra("animalName") ?: "결과 없음"
        val similarity = intent.getFloatExtra("similarity", 0f)
        val message = intent.getStringExtra("message") ?: "이미지를 확인해주세요."
        val imageUriStr = intent.getStringExtra("imageUri")
        val imageUri = imageUriStr?.let { Uri.parse(it) }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (animalName == "결과 없음") {
                        Text("결과가 없습니다.", modifier = Modifier.padding(16.dp))
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ResultCardView(
                                animalName = animalName,
                                similarity = similarity,
                                message = message,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Row {
                                Button(onClick = {
                                    if (imageUri != null) {
                                        ShareManager.shareToKakao(this@ResultActivity, "내 동물상 결과: $animalName", imageUri)
                                    } else {
                                        ShareManager.shareToKakao(this@ResultActivity, "내 동물상 결과: $animalName")
                                    }
                                }) {
                                    Text("카카오톡 공유")
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Button(onClick = {
                                    if (imageUri != null) {
                                        ShareManager.shareToInstagram(this@ResultActivity, imageUri)
                                    } else {
                                        // 이미지 없으면 텍스트 공유 불가하므로 Toast 등 알림 추가 가능
                                    }
                                }) {
                                    Text("인스타 공유")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}