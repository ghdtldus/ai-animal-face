package com.example.android

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.android.ui.screen.*
import com.example.android.data.model.*
import com.example.android.ui.theme.AndroidTheme
import com.example.android.ui.screen.main.HomeMain
import com.example.android.ui.screen.selection.HomeScreen
import com.example.android.utils.ImagePreprocessor
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ImagePreprocessor.initialize(this)
        
        //앱 시작하자마자 권한 요청하여 이중으로 권한요청하는것을 막기위해 주석처리
        // Android 13+ 권한 요청
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        //    ActivityCompat.requestPermissions(
        //        this,
        //        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
        //        1001
        //    )
       // }

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val startDestination = "home"

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
                ){
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
                                sharePageUrl = parsed.sharePageUrl,
                                shareCardUrl = parsed.shareCardUrl,
                                uploadedImageUri = parsed.uploadedImageUri,
                                navController = navController,
                                onRetry = {
                                    navController.popBackStack("home", inclusive = false)
                                }
                            )
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
