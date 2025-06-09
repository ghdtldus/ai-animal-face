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

    val itemsPerPage = 10
    var currentPage by remember { mutableStateOf(0) }

    val totalPages = (resultList.size + itemsPerPage - 1) / itemsPerPage
    val currentItems = resultList.drop(currentPage * itemsPerPage).take(itemsPerPage)

    Column(modifier = Modifier.padding(16.dp)) {
        Text("최근 판별 결과 확인하기", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        if (resultList.isEmpty()) {
            Text("기록은 30일 동안만 저장됩니다.")
        } else {
            currentItems.forEachIndexed { index, result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${currentPage * itemsPerPage + index + 1}. ${result.animal} : ${String.format("%.1f", result.score * 100)}%")

                    Text(
                        text = "🗑️",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable {
                                ResultStorage.deleteResult(context, result)
                                resultList = ResultStorage.loadRecentResults(context)
                                // 현재 페이지에 아이템이 하나도 안 남는다면 이전 페이지로 이동
                                if (currentPage > 0 && currentPage * itemsPerPage >= resultList.size) {
                                    currentPage--
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0
                ) {
                    Text("이전")
                }

                Text("페이지 ${currentPage + 1} / $totalPages")

                TextButton(
                    onClick = { if (currentPage < totalPages - 1) currentPage++ },
                    enabled = currentPage < totalPages - 1
                ) {
                    Text("다음")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("⬅ 홈으로 돌아가기")
        }
    }
}
