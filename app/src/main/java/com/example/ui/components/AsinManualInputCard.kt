package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MusicEntityType
import com.example.data.model.MusicItem
import com.example.data.repository.AsinValidator
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
import com.example.ui.theme.TypeAlbum
import com.example.ui.theme.TypeTrack

@Composable
fun AsinManualInputCard(
    onEnqueueJob: (MusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var asinInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var artistInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MusicEntityType.TRACK) }

    // Live validation
    val asinValidation = AsinValidator.validateAsin(asinInput)
    val isValidAsin = asinValidation.isSuccess
    val generatedUrlResult = if (isValidAsin) {
        AsinValidator.buildAmazonMusicUrl(asinValidation.getOrNull(), selectedType.name)
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SonoraSurfaceCard)
            .border(1.dp, SonoraBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("asin_manual_input_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Manual ASIN",
                        tint = SonoraCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Input ASIN Manual",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = { isExpanded = !isExpanded },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonoraSurfaceVariant,
                        contentColor = SonoraCyan
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (isExpanded) "Sembunyikan" else "Buka Form", fontSize = 11.sp)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Masukkan tepat 10 karakter ASIN Amazon Music (contoh: B08XXXXXXX)",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    // ASIN text field
                    OutlinedTextField(
                        value = asinInput,
                        onValueChange = { asinInput = it.take(10) },
                        label = { Text("ASIN Produk (10 Karakter)", fontSize = 12.sp) },
                        placeholder = { Text("Contoh: B00C069H6M", fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("asin_text_field"),
                        trailingIcon = {
                            if (asinInput.isNotBlank()) {
                                if (isValidAsin) {
                                    Icon(Icons.Default.Check, "Valid", tint = StatusCompleted)
                                } else {
                                    Icon(Icons.Default.Error, "Invalid", tint = StatusFailed)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SonoraViolet,
                            unfocusedBorderColor = SonoraBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Type selection chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tipe:", color = TextSecondary, fontSize = 12.sp)

                        FilterChip(
                            selected = selectedType == MusicEntityType.TRACK,
                            onClick = { selectedType = MusicEntityType.TRACK },
                            label = { Text("TRACK") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TypeTrack.copy(alpha = 0.25f),
                                selectedLabelColor = TypeTrack
                            )
                        )

                        FilterChip(
                            selected = selectedType == MusicEntityType.ALBUM,
                            onClick = { selectedType = MusicEntityType.ALBUM },
                            label = { Text("ALBUM") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TypeAlbum.copy(alpha = 0.25f),
                                selectedLabelColor = TypeAlbum
                            )
                        )
                    }

                    // Optional Title & Artist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Judul (Opsional)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SonoraViolet,
                                unfocusedBorderColor = SonoraBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = artistInput,
                            onValueChange = { artistInput = it },
                            label = { Text("Artis (Opsional)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SonoraViolet,
                                unfocusedBorderColor = SonoraBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Live generated URL preview
                    if (generatedUrlResult != null && generatedUrlResult.isSuccess) {
                        val url = generatedUrlResult.getOrThrow()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SonoraSurfaceVariant)
                                .border(1.dp, SonoraBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "URL Dihasilkan: $url",
                                color = SonoraCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }

                    // Submit button
                    Button(
                        onClick = {
                            val cleanAsin = asinValidation.getOrNull() ?: return@Button
                            val item = MusicItem(
                                asin = cleanAsin,
                                title = titleInput.ifBlank { "Item $cleanAsin" },
                                artist = artistInput.ifBlank { "Amazon Music Artist" },
                                type = selectedType,
                                sourceUrl = generatedUrlResult?.getOrNull()
                            )
                            onEnqueueJob(item)
                            asinInput = ""
                            titleInput = ""
                            artistInput = ""
                            isExpanded = false
                        },
                        enabled = isValidAsin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("submit_manual_asin_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonoraViolet,
                            disabledContainerColor = SonoraSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Download, "Queue Job", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim ke Antrean Unduhan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
