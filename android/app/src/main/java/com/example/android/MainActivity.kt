package com.example.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.android.ui.theme.AndroidTheme
import com.example.android.ui.screen.*
import com.example.android.data.model.*
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.android.ui.screen.main.HomeMain
import com.example.android.ui.screen.selection.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            // 딥링크 경로 파싱
            val pathSegments = intent?.data?.pathSegments
            val imageId = intent?.data?.pathSegments?.let { segments ->
                if (segments.size >= 2 && segments[0] == "share") {
                    segments[1].removeSuffix(".html")
                } else ""
            } ?: ""
            Log.d("DeepLink", "🔥 딥링크 imageId: $imageId")

            val imageUrl = "https://animalfaceapp-e67a4.web.app/share/$imageId.png"
            Log.d("DeepLink", "🔥 이동할 URL: $imageUrl")

            val startDestination = if (imageId.isNotEmpty()) {
                "share_preview/${URLEncoder.encode(imageUrl, "UTF-8")}"
            } else {
                "home"
            }

            AndroidTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFDE2CB),
                                    Color(0xFFFFCB9D)
                                )
                            )
                        )
                ) {
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("home") {
                            HomeMain(navController = navController)
                        }
                        composable(
                            "result/{resultJson}",
                            arguments = listOf(navArgument("resultJson") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val resultJson = backStackEntry.arguments?.getString("resultJson")
                            val parsed = parseResultJson(resultJson)
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
                        composable("home_screen") {
                            HomeScreen(navController = navController)
                        }
                        composable("recent_results") {
                            RecentResultScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }

    private fun parseResultJson(json: String?): ResultBundle {
        return Gson().fromJson(
            URLDecoder.decode(json, StandardCharsets.UTF_8.name()),
            ResultBundle::class.java
        )
    }
}
