package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.BackendConnectionState
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraCyan
import com.example.ui.theme.SonoraSurfaceCard
import com.example.ui.theme.SonoraViolet
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun BackendStatusBar(
    connectionState: BackendConnectionState,
    isTestMode: Boolean,
    isChecking: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SonoraSurfaceCard)
            .border(1.dp, SonoraBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status dot
                val dotColor = when {
                    isTestMode -> SonoraCyan
                    connectionState.isOnline -> StatusCompleted
                    else -> StatusFailed
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                if (isTestMode) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Test Mode",
                        tint = SonoraCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Demo / Mock Mode Active",
                        color = SonoraCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        imageVector = if (connectionState.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Connection Status",
                        tint = if (connectionState.isOnline) StatusCompleted else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (connectionState.isOnline) {
                            "Backend Online (${connectionState.latencyMs}ms) • Worker: ${connectionState.workerStatus}"
                        } else {
                            "Backend Offline • Check Settings"
                        },
                        color = if (connectionState.isOnline) TextPrimary else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = SonoraViolet
                )
            } else {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Check Connection",
                        tint = SonoraViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
