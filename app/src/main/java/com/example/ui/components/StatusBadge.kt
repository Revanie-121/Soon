package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JobStatus
import com.example.ui.theme.StatusCancelled
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusProcessing
import com.example.ui.theme.StatusQueued
import com.example.ui.theme.StatusStarting
import com.example.ui.theme.StatusTimedOut
import com.example.ui.theme.StatusWaiting
import com.example.ui.theme.TypeAlbum
import com.example.ui.theme.TypeTrack

@Composable
fun StatusBadge(
    status: JobStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderCol) = when (status) {
        JobStatus.QUEUED -> Triple(StatusQueued.copy(alpha = 0.15f), StatusQueued, StatusQueued.copy(alpha = 0.4f))
        JobStatus.STARTING -> Triple(StatusStarting.copy(alpha = 0.15f), StatusStarting, StatusStarting.copy(alpha = 0.4f))
        JobStatus.PROCESSING -> Triple(StatusProcessing.copy(alpha = 0.15f), StatusProcessing, StatusProcessing.copy(alpha = 0.4f))
        JobStatus.WAITING_FOR_RESULT -> Triple(StatusWaiting.copy(alpha = 0.15f), StatusWaiting, StatusWaiting.copy(alpha = 0.4f))
        JobStatus.COMPLETED -> Triple(StatusCompleted.copy(alpha = 0.15f), StatusCompleted, StatusCompleted.copy(alpha = 0.4f))
        JobStatus.FAILED -> Triple(StatusFailed.copy(alpha = 0.15f), StatusFailed, StatusFailed.copy(alpha = 0.4f))
        JobStatus.CANCELLED -> Triple(StatusCancelled.copy(alpha = 0.15f), StatusCancelled, StatusCancelled.copy(alpha = 0.4f))
        JobStatus.TIMED_OUT -> Triple(StatusTimedOut.copy(alpha = 0.15f), StatusTimedOut, StatusTimedOut.copy(alpha = 0.4f))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.value.replace("_", " ").uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun TypeBadge(
    type: String,
    modifier: Modifier = Modifier
) {
    val isAlbum = type.equals("ALBUM", ignoreCase = true)
    val color = if (isAlbum) TypeAlbum else TypeTrack

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isAlbum) "ALBUM" else "TRACK",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
