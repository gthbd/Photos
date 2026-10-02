package com.example.photos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.photos.ui.components.AppNavHost
import com.example.photos.ui.components.MediaUiState
import com.example.photos.ui.screens.MainScreen
import com.example.photos.ui.theme.PhotosTheme
import com.example.photos.ui.screens.PermissionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotosTheme {
                    PermissionScreen {
                        AppNavHost()
                    }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PhotosTheme {
        PhotosTheme {
            MainScreen(uiState = MediaUiState.Loading, onMediaClick = {})
        }
    }
}