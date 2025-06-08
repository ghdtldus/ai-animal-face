package com.example.android

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.compose.material3.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.android.ui.theme.AndroidTheme
import com.example.android.ui.screen.*
import com.example.android.data.model.*
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                1001
            )
        }

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val startDestination = "home" // 항상 홈부터 시작

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
                        val parsed = parseResultJson(resultJson)
                        ResultScreen(
                            uploadResult = parsed.uploadResult,
                            uploadMessage = parsed.uploadMessage,
                            topKResults = parsed.topKResults,
                            sharePageUrl = parsed.sharePageUrl,
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

    private fun parseResultJson(json: String?): ResultBundle {
        return Gson().fromJson(
            URLDecoder.decode(json, StandardCharsets.UTF_8.name()),
            ResultBundle::class.java
        )
    }
}
