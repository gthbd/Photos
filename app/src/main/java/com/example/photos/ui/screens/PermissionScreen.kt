package com.example.photos.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.photos.permission.hasMediaAccess
import com.example.photos.permission.requiredMediaPermissions

@Composable
fun PermissionScreen(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(hasMediaAccess(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { hasAccess = hasMediaAccess(context) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAccess = hasMediaAccess(context)
    }

    if (hasAccess) {
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ứng dụng cần quyền truy cập ảnh và video để hiển thị thư viện.")
        Button(
            onClick = { permissionLauncher.launch(requiredMediaPermissions()) },
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("Cấp quyền")
        }
    }
}