package com.juraj.securitycamera

import android.Manifest
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.juraj.securitycamera.ui.theme.SecurityCameraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.webrtc.SurfaceViewRenderer

class MainActivity : ComponentActivity() {

    private val webRtcHelper = WebRtcHelper("http://10.0.2.2:6000/")

    private val surfaceViewRendererView by lazy {
        SurfaceViewRenderer(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            init(webRtcHelper.rootEglBase.eglBaseContext, null)
            setEnableHardwareScaler(true)
            setMirror(true)
        }
    }

    private val haveCameraPermissionFlow = MutableStateFlow(false)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> haveCameraPermissionFlow.value = isGranted }

    private fun checkPermissions() {
        if (haveCameraPermission(this)) {
            haveCameraPermissionFlow.value = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        setContent {
            val currentScreen = rememberSaveable { mutableStateOf(AppScreen.MAIN) }

            SecurityCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    when (currentScreen.value) {
                        AppScreen.MAIN -> MainScreen(haveCameraPermissionFlow.collectAsState().value) {
                            webRtcHelper.start(
                                context = this,
                                view = surfaceViewRendererView,
                                isBroadcasting = it == AppScreen.SERVER
                            )
                            currentScreen.value = it
                        }
                        else -> CameraScreen(surfaceViewRendererView, currentScreen.value) {
                            finish()
                        }
                    }
                }
            }
        }
    }
}