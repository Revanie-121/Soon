package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AsinValidator
import com.example.ui.components.ConfirmationDownloadDialog
import com.example.ui.components.DirectLinkResultDialog
import com.example.ui.components.JobDetailBottomSheet
import com.example.ui.components.SonoraTopBar
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraCyan
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SonoraViewModel

enum class SonoraScreen(val label: String, val icon: ImageVector, val tag: String) {
    SEARCH("Search", Icons.Default.Search, "nav_search"),
    QUEUE("Queue", Icons.Default.HourglassTop, "nav_queue"),
    HISTORY("History", Icons.Default.History, "nav_history"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: SonoraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SonoraApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SonoraApp(viewModel: SonoraViewModel) {
    var currentScreen by remember { mutableStateOf(SonoraScreen.SEARCH) }
    val snackbarHostState = remember { SnackbarHostState() }

    val activeJobs by viewModel.activeJobs.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val downloadTargetItem by viewModel.downloadTargetItem.collectAsStateWithLifecycle()
    val detailJob by viewModel.detailJob.collectAsStateWithLifecycle()
    val completedResultJob by viewModel.completedResultJob.collectAsStateWithLifecycle()

    // Handle back button when not on Search screen
    BackHandler(enabled = currentScreen != SonoraScreen.SEARCH) {
        currentScreen = SonoraScreen.SEARCH
    }

    // Collect snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackBarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SonoraBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SonoraTopBar(
                title = "SONORA",
                subtitle = when (currentScreen) {
                    SonoraScreen.SEARCH -> "Music Search & Download"
                    SonoraScreen.QUEUE -> "Active Download Engine"
                    SonoraScreen.HISTORY -> "Download History & Direct Links"
                    SonoraScreen.SETTINGS -> "Configuration & Backend Tunnel"
                },
                isOnline = settingsState.connectionState.isOnline,
                isTestMode = settingsState.isTestMode
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SonoraSurface,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                SonoraScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        modifier = Modifier.testTag(screen.tag),
                        icon = {
                            if (screen == SonoraScreen.QUEUE && activeJobs.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = SonoraViolet,
                                            contentColor = Color.White
                                        ) {
                                            Text("${activeJobs.size}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SonoraViolet,
                            selectedTextColor = SonoraViolet,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = SonoraViolet.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                SonoraScreen.SEARCH -> SearchScreen(viewModel = viewModel)
                SonoraScreen.QUEUE -> QueueScreen(viewModel = viewModel)
                SonoraScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                SonoraScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Modal: Confirmation Download Dialog
    downloadTargetItem?.let { item ->
        val urlResult = AsinValidator.buildAmazonMusicUrl(item.asin, item.type.name)
        val generatedUrl = urlResult.getOrDefault("https://music.amazon.com/...")
        val jsonPayload = viewModel.generateItemJsonPayload(item, generatedUrl)

        ConfirmationDownloadDialog(
            item = item,
            jsonPayload = jsonPayload,
            onConfirm = { viewModel.confirmDownload(it) },
            onDismiss = { viewModel.dismissDownloadDialog() },
            onCopiedPayload = { viewModel.notifyMessage("JSON Payload disalin") }
        )
    }

    // Bottom Sheet: Job Detail
    detailJob?.let { job ->
        val jsonPayload = viewModel.generateJobJsonPayload(job)
        JobDetailBottomSheet(
            job = job,
            jsonPayload = jsonPayload,
            onDismiss = { viewModel.dismissJobDetail() },
            onCancelJob = { viewModel.cancelJob(it) },
            onRetryJob = { viewModel.retryJob(it) },
            onDeleteJob = { viewModel.deleteJob(it) },
            onCopiedText = { viewModel.notifyMessage(it) }
        )
    }

    // Modal: Direct Link Result Dialog
    completedResultJob?.let { job ->
        DirectLinkResultDialog(
            job = job,
            onDismiss = { viewModel.dismissDirectLinkResult() },
            onCopiedLink = { viewModel.notifyMessage("Direct download link disalin ke clipboard") }
        )
    }
}
