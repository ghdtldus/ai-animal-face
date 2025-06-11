package com.example.android.ui.screen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.android.R
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

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

        Image(
            painter = painterResource(id = R.drawable.imglogo),
            contentDescription = "앱 로고",
            modifier = Modifier
                .size(150.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(4.dp, Color(0xFF705438), RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF3E9))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.lblatest_result),
                    contentDescription = "최근 판별 결과",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(50.dp)
                        .width(170.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (resultList.isEmpty()) {
                    Text("기록은 30일 동안만 저장됩니다.")
                } else {
                    currentItems.forEachIndexed { index, result ->
                        val actualIndex = currentPage * itemsPerPage + index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${actualIndex + 1}. ${result.animal} : ${String.format("%.1f", result.score)}%")

                            Text(
                                text = "🗑️",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clickable {
                                        ResultStorage.deleteResult(context, result.id)
                                        resultList = ResultStorage.loadRecentResults(context)

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
                        PagingButton(
                            text = "이전",
                            enabled = currentPage > 0,
                            onClick = { currentPage-- }
                        )

                        PageIndicator(currentPage, totalPages)

                        PagingButton(
                            text = "다음",
                            enabled = currentPage < totalPages - 1,
                            onClick = { currentPage++ }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "⬅ 홈으로 돌아가기",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { navController.popBackStack() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun PagingButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFF705438), RoundedCornerShape(16.dp))
            .background(if (enabled) Color(0xFFFFE2C8) else Color(0xFFF0F0F0))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) Color.Black else Color.Gray
        )
    }
}

@Composable
fun PageIndicator(currentPage: Int, totalPages: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFF705438), RoundedCornerShape(16.dp))
            .background(Color(0xFFFFE2C8))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${currentPage + 1} / $totalPages",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
    }
}


