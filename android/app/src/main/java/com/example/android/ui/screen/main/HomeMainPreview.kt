package com.example.android.ui.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Preview(showBackground = true)
@Composable
fun HomeMainPreview() {
    val navController = rememberNavController()
    HomeMain(navController = navController)
}