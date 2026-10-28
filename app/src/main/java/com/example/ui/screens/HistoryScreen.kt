package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.JobCard
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraSurfaceCard
import com.example.ui.theme.SonoraViolet
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SonoraViewModel

@Composable
fun HistoryScreen(
    viewModel: SonoraViewModel,
    modifier: Modifier = Modifier
) {
    val historyJobs by viewModel.historyJobs.collectAsStateWithLifecycle()
    val statusFilter by viewModel.historyStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SonoraBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Riwayat Unduhan",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${historyJobs.size} catatan tersimpan di database lokal",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (historyJobs.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = StatusFailed
                        )
                    }
                }
            }
        }

        // Search in history
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setHistorySearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Filter riwayat berdasarkan judul atau ASIN...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, "Search", tint = SonoraViolet, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
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

        // Filter chips: ALL, COMPLETED, FAILED, CANCELLED
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Semua",
                    "COMPLETED" to "Selesai",
                    "FAILED" to "Gagal",
                    "CANCELLED" to "Dibatalkan"
                ).forEach { (key, label) ->
                    val isSelected = statusFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setHistoryStatusFilter(key) },
                        label = { Text(label, fontSize = 11.sp) },
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

        if (historyJobs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Empty History",
                            tint = SonoraBorder,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Riwayat",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pekerjaan yang telah selesai atau gagal akan dicatat di sini.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(historyJobs, key = { it.jobId }) { job ->
                JobCard(
                    job = job,
                    onDetailClick = { viewModel.openJobDetail(it) },
                    onCancelClick = { viewModel.cancelJob(it) },
                    onRetryClick = { viewModel.retryJob(it) },
                    onResultClick = { viewModel.openDirectLinkResult(it) }
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Hapus Semua Riwayat?", color = TextPrimary) },
            text = {
                Text(
                    "Tindakan ini hanya akan menghapus catatan riwayat unduhan yang sudah selesai atau gagal. Pekerjaan aktif di antrean tidak akan terhapus.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusFailed)
                ) {
                    Text("Hapus Riwayat")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = SonoraSurfaceCard
        )
    }
}
