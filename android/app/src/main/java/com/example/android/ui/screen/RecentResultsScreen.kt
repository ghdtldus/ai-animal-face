package com.example.android.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.android.utils.ResultStorage
import com.example.android.data.model.ResultLog

@Composable
fun RecentResultScreen(navController: NavController) {
    val context = LocalContext.current
    var resultList by remember { mutableStateOf(ResultStorage.loadRecentResults(context)) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("최근 판별 결과 확인하기", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        if (resultList.isEmpty()) {
            Text("기록은 30일 동안만 저장됩니다.")
        } else {
            resultList.forEachIndexed { index, result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${index + 1}. ${result.animal} : ${String.format("%.1f", result.score * 100)}%")

                    Text(
                        text = "🗑️",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp).clickable {
                            ResultStorage.deleteResult(context, result)
                            resultList = ResultStorage.loadRecentResults(context)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("⬅ 홈으로 돌아가기")
        }
    }
}
