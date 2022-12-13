package com.juraj.securitycamera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.PIXEL_4
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.juraj.securitycamera.ui.theme.SecurityCameraTheme
import org.webrtc.SurfaceViewRenderer

@Composable
fun CameraScreen(
    surfaceViewRenderer: SurfaceViewRenderer,
    appScreen: AppScreen,
    onClick: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(appScreen.toString()) }, navigationIcon = {
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Close, null)
            }
        })
        Column(
            Modifier
                .fillMaxSize(), verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (appScreen == AppScreen.SERVER) "• Broadcasting" else "Remote Camera Stream",
                color = if (appScreen == AppScreen.SERVER) Color.Red else Color.DarkGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold
            )
            AndroidView(
                factory = { surfaceViewRenderer },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .aspectRatio(0.5625f)
            )
        }
    }
}

@Preview(device = PIXEL_4)
@Composable
fun CameraScreen_Preview1() {
    SecurityCameraTheme {
        Surface {
            CameraScreen(SurfaceViewRenderer(LocalContext.current), AppScreen.SERVER) {}
        }
    }
}

@Preview(device = PIXEL_4)
@Composable
fun CameraScreen_Preview2() {
    SecurityCameraTheme {
        Surface {
            CameraScreen(SurfaceViewRenderer(LocalContext.current), AppScreen.CLIENT) {}
        }
    }
}