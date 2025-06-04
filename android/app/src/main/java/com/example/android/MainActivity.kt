package com.example.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.android.ui.theme.AndroidTheme
import com.example.android.ui.screen.HomeScreen
import com.example.android.ui.screen.ResultScreen
import com.example.android.data.model.UploadResponse
import com.google.gson.Gson
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import java.net.URLEncoder
import com.example.android.ui.screen.SharePreviewScreen
import com.example.android.ui.screen.RecentResultScreen
import androidx.navigation.NavHostController
import com.example.android.data.model.ResultBundle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController: NavHostController = rememberNavController()
            val startDestination = if (
                intent?.data?.host == "sandwich.app" &&
                intent?.data?.pathSegments?.getOrNull(0) == "share"
            ) {
                val imageId = intent.data?.lastPathSegment ?: ""
                "share_preview/${URLEncoder.encode("https://sandwich.app/share/$imageId", "UTF-8")}"
            } else {
                "home"
            }



            AndroidTheme {
                NavHost(navController = navController, startDestination = startDestination) {
                    composable("home") {
                        HomeScreen(navController = navController)
                    }
                    composable(
                        "result/{resultJson}",
                        arguments = listOf(navArgument("resultJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val resultJson = backStackEntry.arguments?.getString("resultJson")
                        val parsed = parseResultJson(resultJson)  // parsed: ResultBundle
                        ResultScreen(
                            uploadResult = parsed.uploadResult,
                            uploadMessage = parsed.uploadMessage,
                            topKResults = parsed.topKResults,
                            shareCardUrl = parsed.shareCardUrl,
                            uploadedImageUri = parsed.uploadedImageUri,
                            navController = navController,
                            onRetry = {
                                navController.popBackStack("home", inclusive = false)
                            }
                        )
                    }
                    composable(
                        "share_preview/{imageUrl}",
                        arguments = listOf(navArgument("imageUrl") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("imageUrl")
                        val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                        SharePreviewScreen(imageUrl = decodedUrl, navController = navController)
                    }
                    composable("recent_results") {
                        RecentResultScreen(navController = navController)
                    }
                }
            }
        }
    }

    // JSON 파싱 함수
    private fun parseResultJson(json: String?): ResultBundle {
        return Gson().fromJson(
            URLDecoder.decode(json, StandardCharsets.UTF_8.name()),
            ResultBundle::class.java
        )
    }
}
