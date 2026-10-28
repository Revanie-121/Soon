package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MusicItem
import com.example.ui.components.AsinManualInputCard
import com.example.ui.components.BackendStatusBar
import com.example.ui.components.MusicItemCard
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraCyan
import com.example.ui.theme.SonoraSurfaceCard
import com.example.ui.theme.SonoraViolet
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SonoraViewModel

@Composable
fun SearchScreen(
    viewModel: SonoraViewModel,
    modifier: Modifier = Modifier
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header: "Discover your music."
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Discover your music.",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Search tracks and albums, then send selected items to your download engine.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item {
            // Backend Status Bar
            BackendStatusBar(
                connectionState = settingsState.connectionState,
                isTestMode = settingsState.isTestMode,
                isChecking = settingsState.isCheckingConnection,
                onRefresh = { viewModel.checkConnection() }
            )
        }

        item {
            // Search Input Field
            OutlinedTextField(
                value = searchState.query,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_input"),
                placeholder = {
                    Text("Cari judul lagu, artis, atau album...", color = TextMuted, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SonoraViolet
                    )
                },
                trailingIcon = {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextMuted
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.triggerSearch(immediate = true)
                }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SonoraViolet,
                    unfocusedBorderColor = SonoraBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SonoraSurfaceCard,
                    unfocusedContainerColor = SonoraSurfaceCard
                )
            )
        }

        item {
            // Category Filter Chips: All, Tracks, Albums
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Semua",
                    "TRACK" to "Tracks",
                    "ALBUM" to "Albums"
                ).forEach { (key, label) ->
                    val isSelected = searchState.categoryFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategoryFilterChanged(key) },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("filter_chip_$key"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonoraViolet.copy(alpha = 0.25f),
                            selectedLabelColor = SonoraViolet,
                            containerColor = SonoraSurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = SonoraBorder,
                            selectedBorderColor = SonoraViolet
                        )
                    )
                }
            }
        }

        item {
            // Manual ASIN Input card
            AsinManualInputCard(
                onEnqueueJob = { item -> viewModel.selectItemForDownload(item) }
            )
        }

        // Mock mode warning badge if active
        if (searchState.isMock) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SonoraCyan.copy(alpha = 0.12f))
                        .border(1.dp, SonoraCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Demo / Mock Mode: Menampilkan katalog uji contoh dengan ASIN Amazon Music nyata. Anda dapat mengirim item ke antrean untuk menguji alur.",
                        color = SonoraCyan,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Error message if any
        if (!searchState.errorMessage.isNullOrBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StatusFailed.copy(alpha = 0.15f))
                        .border(1.dp, StatusFailed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = StatusFailed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = searchState.errorMessage!!,
                            color = StatusFailed,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Loading indicator
        if (searchState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SonoraViolet, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Mencari metadata musik...", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        // Results list or Empty State
        if (!searchState.isLoading && searchState.results.isEmpty() && searchState.errorMessage == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "No music",
                            tint = SonoraBorder,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchState.query.isBlank()) "Mulai pencarian musik" else "Tidak ada hasil ditemukan",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ketik judul lagu atau gunakan form Input ASIN Manual di atas.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        items(searchState.results, key = { it.asin }) { item ->
            MusicItemCard(
                item = item,
                onDownloadClick = { viewModel.selectItemForDownload(it) },
                onDetailClick = { viewModel.selectItemForDownload(it) }
            )
        }
    }
}
