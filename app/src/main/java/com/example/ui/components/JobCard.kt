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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadJob
import com.example.data.model.JobStatus
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraCyan
import com.example.ui.theme.SonoraSurfaceCard
import com.example.ui.theme.SonoraViolet
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JobCard(
    job: DownloadJob,
    onDetailClick: (DownloadJob) -> Unit,
    onCancelClick: (String) -> Unit,
    onRetryClick: (String) -> Unit,
    onResultClick: (DownloadJob) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val startTimeStr = timeFormatter.format(Date(job.createdAt))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SonoraSurfaceCard)
            .border(1.dp, SonoraBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("job_card_${job.jobId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Job ID & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ID: ${job.jobId.take(12)}",
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TypeBadge(type = job.type)
                }

                StatusBadge(status = job.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Artist
            Text(
                text = job.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${job.artist} • ASIN: ${job.asin}",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress bar
            if (job.status in listOf(JobStatus.STARTING, JobStatus.PROCESSING, JobStatus.WAITING_FOR_RESULT)) {
                Spacer(modifier = Modifier.height(10.dp))
                if (job.progress != null && job.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { job.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = SonoraViolet,
                        trackColor = SonoraBorder
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = SonoraViolet,
                        trackColor = SonoraBorder
                    )
                }
            }

            // Status message
            if (!job.message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = job.message,
                    color = if (job.status == JobStatus.FAILED) StatusFailed else SonoraCyan,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Timestamp and duration info
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Started: $startTimeStr",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                if (job.status == JobStatus.COMPLETED && !job.directUrl.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Ready",
                            tint = StatusCompleted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Direct Link Ready",
                            color = StatusCompleted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDetailClick(job) },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("job_detail_btn_${job.jobId}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Payload",
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Payload", fontSize = 12.sp, color = TextSecondary)
                }

                if (job.status.isCancellable) {
                    OutlinedButton(
                        onClick = { onCancelClick(job.jobId) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("job_cancel_btn_${job.jobId}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StatusFailed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", fontSize = 12.sp)
                    }
                }

                if (job.status.isRetryable) {
                    Button(
                        onClick = { onRetryClick(job.jobId) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("job_retry_btn_${job.jobId}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonoraViolet
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 12.sp)
                    }
                }

                if (job.status == JobStatus.COMPLETED && !job.directUrl.isNullOrBlank()) {
                    Button(
                        onClick = { onResultClick(job) },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(36.dp)
                            .testTag("job_result_btn_${job.jobId}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusCompleted,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Direct Link",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
