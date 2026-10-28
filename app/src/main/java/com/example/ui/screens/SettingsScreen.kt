package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraCyan
import com.example.ui.theme.SonoraSurfaceCard
import com.example.ui.theme.SonoraSurfaceVariant
import com.example.ui.theme.SonoraViolet
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SonoraViewModel

@Composable
fun SettingsScreen(
    viewModel: SonoraViewModel,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    var urlInput by remember(settingsState.backendUrl) { mutableStateOf(settingsState.backendUrl) }
    var tokenInput by remember(settingsState.apiToken) { mutableStateOf(settingsState.apiToken) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Pengaturan Sonora",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Konfigurasi koneksi backend FastAPI, Cloudflare Tunnel, dan worker lucidadl",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Test / Demo Mode Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SonoraSurfaceCard)
                    .border(1.dp, SonoraBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Test Mode",
                                tint = SonoraCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test / Demo Mode",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Menggunakan data katalog contoh dengan ASIN Amazon Music valid dan simulasi worker lucidadl lokal.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Switch(
                        checked = settingsState.isTestMode,
                        onCheckedChange = { viewModel.toggleTestMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SonoraCyan,
                            uncheckedTrackColor = SonoraSurfaceVariant
                        ),
                        modifier = Modifier.testTag("test_mode_switch")
                    )
                }
            }
        }

        // Backend API Configuration Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SonoraSurfaceCard)
                    .border(1.dp, SonoraBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Backend API",
                            tint = SonoraViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backend API & Cloudflare Tunnel",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Masukkan URL backend FastAPI (IP LAN Termux, Emulator host 10.0.2.2, atau domain Cloudflare Tunnel HTTPS).",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Base URL API", fontSize = 12.sp) },
                        placeholder = { Text("http://10.0.2.2:8000 atau https://xxxx.trycloudflare.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backend_url_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SonoraViolet,
                            unfocusedBorderColor = SonoraBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Quick URL Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { urlInput = "http://10.0.2.2:8000" },
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Emulator (10.0.2.2)", fontSize = 10.sp, color = SonoraCyan)
                        }

                        OutlinedButton(
                            onClick = { urlInput = "http://127.0.0.1:8000" },
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Localhost", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    // Optional API Token
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("API Bearer Token (Opsional)", fontSize = 12.sp) },
                        placeholder = { Text("Rahasia API jika diaktifkan di backend") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SonoraViolet,
                            unfocusedBorderColor = SonoraBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateBackendUrl(urlInput)
                                viewModel.updateApiToken(tokenInput)
                                viewModel.checkConnection()
                                viewModel.notifyMessage("Pengaturan URL disimpan & diuji")
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                                .testTag("save_backend_url_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SonoraViolet)
                        ) {
                            Text("Simpan URL", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.checkConnection() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("ping_health_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (settingsState.isCheckingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SonoraViolet)
                            } else {
                                Icon(Icons.Default.Refresh, "Check", modifier = Modifier.size(14.dp), tint = SonoraCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uji Ping", fontSize = 12.sp, color = SonoraCyan)
                            }
                        }
                    }

                    // Connection Health Feedback
                    val conn = settingsState.connectionState
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SonoraSurfaceVariant)
                            .border(1.dp, SonoraBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Status Server:", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    text = conn.statusText,
                                    color = if (conn.isOnline) StatusCompleted else StatusFailed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (conn.isOnline) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Worker LucidaDL:", color = TextSecondary, fontSize = 11.sp)
                                    Text(conn.workerStatus, color = SonoraCyan, fontSize = 11.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Provider Amazon:", color = TextSecondary, fontSize = 11.sp)
                                    Text(conn.providerStatus, color = SonoraViolet, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Polling & Performance Settings Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SonoraSurfaceCard)
                    .border(1.dp, SonoraBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Polling Interval",
                            tint = SonoraCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interval Polling Antrean",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Interval awal pengecekan status ke backend: ${settingsState.pollingIntervalSeconds} detik (otomatis backoff hingga 10s dengan jitter).",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Slider(
                        value = settingsState.pollingIntervalSeconds.toFloat(),
                        onValueChange = { viewModel.updatePollingInterval(it.toInt()) },
                        valueRange = 2f..10f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = SonoraViolet,
                            activeTrackColor = SonoraViolet,
                            inactiveTrackColor = SonoraSurfaceVariant
                        )
                    )
                }
            }
        }

        // System Architecture & Termux Info Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SonoraSurfaceCard)
                    .border(1.dp, SonoraBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Architecture",
                            tint = SonoraViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Arsitektur Engine lucidadl",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "• Lingkungan: Termux Android + Ubuntu proot-distro\n" +
                               "• Eksekusi: xvfb-run -a lucidadl\n" +
                               "• Concurrency: 1 Worker aktif (FIFO Safe Queue)\n" +
                               "• Tunnel: Cloudflare Tunnel (HTTPS publik tanpa port forward)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
