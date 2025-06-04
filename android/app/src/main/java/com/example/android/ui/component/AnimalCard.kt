package com.example.android.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.android.data.model.AnimalScore
@Composable

fun AnimalCard(animalScore: AnimalScore) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "나의 동물상은",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "${animalScore.animal} 상!!",
                style = MaterialTheme.typography.titleLarge
            )

            LinearProgressIndicator(
                progress = animalScore.score.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "유사도: ${String.format("%.1f", animalScore.score * 100)}%",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "나랑 같은 ${animalScore.animal}상은 누구야?\n당신의 동물상도 확인해보세요!",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
