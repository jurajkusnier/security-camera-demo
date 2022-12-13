package com.juraj.securitycamera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.PIXEL_4
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juraj.securitycamera.ui.theme.SecurityCameraTheme

@Composable
fun MainScreen(haveCameraPermission: Boolean, onClick: (AppScreen) -> Unit) {
    data class ButtonOptions(
        val screen: AppScreen,
        val title: String,
        val requireCameraPermission: Boolean
    )

    val buttonOptions = listOf(
        ButtonOptions(
            screen = AppScreen.SERVER,
            title = "Create Server",
            requireCameraPermission = true
        ), ButtonOptions(
            screen = AppScreen.CLIENT,
            title = "Connect to Server",
            requireCameraPermission = false
        )
    )

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Security Camera Demo App") }, navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Home, null)
            }
        })
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp), verticalArrangement = Arrangement.Center
        ) {
            buttonOptions.map {
                Button(
                    onClick = { onClick(it.screen) },
                    Modifier.fillMaxWidth(),
                    enabled = !it.requireCameraPermission || haveCameraPermission
                ) {
                    Text(it.title)
                }
            }
        }
    }
}

@Preview(device = PIXEL_4)
@Composable
fun MainScreen_Preview1() {
    SecurityCameraTheme {
        Surface {
            MainScreen(true) {}
        }
    }
}

@Preview(device = PIXEL_4)
@Composable
fun MainScreen_Preview2() {
    SecurityCameraTheme {
        Surface {
            MainScreen(false) {}
        }
    }
}

enum class AppScreen {
    MAIN,
    CLIENT,
    SERVER
}