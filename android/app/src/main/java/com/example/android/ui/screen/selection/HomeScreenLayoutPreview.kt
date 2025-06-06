package com.example.android.ui.screen.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreenLayoutPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("성별을 선택해주세요:")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}) { Text("남성") }
            Button(onClick = {}) { Text("여성") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {}) { Text("갤러리에서 이미지 선택") }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {}) { Text("카메라로 촬영") }

        Spacer(modifier = Modifier.height(20.dp))

        // 임의의 이미지 미리보기
//        Image(
//            painter = rememberAsyncImagePainter("https://via.placeholder.com/300"),
//            contentDescription = "선택된 이미지",
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(300.dp)
//        )
    }
}
