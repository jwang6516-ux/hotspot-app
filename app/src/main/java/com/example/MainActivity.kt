package com.example

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var launchFailedReason by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Attempt automatic redirect immediately upon launch
        val success = launchHotspotSettings()
        if (success) {
            // Immediately close activity so no residual UI stays in background/recents
            finishAndRemoveTask()
            return
        }

        // If redirect fails (e.g., non-OriginOS system or simulator), show fallback UI
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FallbackScreen(
                        modifier = Modifier.padding(innerPadding),
                        reason = launchFailedReason ?: "无法自动跳转设置，请使用下方按钮手动重试",
                        onRetryVivo = {
                            if (launchVivoTetherSettings()) {
                                finishAndRemoveTask()
                            }
                        },
                        onRetryGeneric = {
                            if (launchGenericTetherSettings()) {
                                finishAndRemoveTask()
                            }
                        },
                        onClose = {
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    /**
     * Priority strategy for launching tethering settings.
     * 1. Vivo / OriginOS dedicated component: com.android.settings.Settings$VivoTetherSettingsActivity
     * 2. Alternative Vivo component variants
     * 3. System standard tether settings: Settings.ACTION_TETHER_SETTINGS
     * 4. Generic wireless settings: Settings.ACTION_WIRELESS_SETTINGS
     */
    private fun launchHotspotSettings(): Boolean {
        return launchVivoTetherSettings() || launchGenericTetherSettings()
    }

    private fun launchVivoTetherSettings(): Boolean {
        val vivoComponents = listOf(
            ComponentName("com.android.settings", "com.android.settings.Settings\$VivoTetherSettingsActivity"),
            ComponentName("com.vivo.setting", "com.vivo.setting.network.TetherSettings"),
            ComponentName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity")
        )

        for (component in vivoComponents) {
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            try {
                startActivity(intent)
                return true
            } catch (e: Exception) {
                launchFailedReason = "vivo专属组件跳转失败: ${e.localizedMessage}"
            }
        }
        return false
    }

    private fun launchGenericTetherSettings(): Boolean {
        val actions = listOf(
            "android.settings.TETHER_SETTINGS",
            "android.settings.HOTSPOT_SETTINGS",
            "android.settings.WIFI_AP_SETTINGS",
            Settings.ACTION_WIRELESS_SETTINGS
        )

        for (action in actions) {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            try {
                startActivity(intent)
                return true
            } catch (e: Exception) {
                launchFailedReason = "通用热点设置跳转失败: ${e.localizedMessage}"
            }
        }
        return false
    }
}

@Composable
fun FallbackScreen(
    modifier: Modifier = Modifier,
    reason: String,
    onRetryVivo: () -> Unit,
    onRetryGeneric: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = "Hotspot",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = "iQOO 个人热点设置快捷助手",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = reason,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Button(
                    onClick = onRetryVivo,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("尝试 iQOO / vivo 专属热点跳转")
                }

                OutlinedButton(
                    onClick = onRetryGeneric,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("尝试系统通用热点设置")
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("退出应用")
                }
            }
        }
    }
}
