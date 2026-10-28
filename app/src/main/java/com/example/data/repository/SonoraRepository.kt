package com.example.data.repository

import android.content.Context
import com.example.data.local.JobDao
import com.example.data.local.JobEntity
import com.example.data.local.SonoraDatabase
import com.example.data.model.DownloadJob
import com.example.data.model.JobPayload
import com.example.data.model.JobPayloadItem
import com.example.data.model.JobStatus
import com.example.data.model.MusicItem
import com.example.data.provider.MusicMetadataProvider
import com.example.data.provider.ProviderResult
import com.example.data.provider.RemoteBackendMetadataProvider
import com.example.data.provider.TestMockMetadataProvider
import com.example.data.remote.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class BackendConnectionState(
    val isOnline: Boolean = false,
    val latencyMs: Long = -1,
    val statusText: String = "Not checked",
    val workerStatus: String = "Unknown",
    val providerStatus: String = "Unknown"
)

class SonoraRepository(
    context: Context,
    val apiClient: ApiClient = ApiClient()
) {
    private val database: SonoraDatabase = SonoraDatabase.getInstance(context)
    private val jobDao: JobDao = database.jobDao()

    private val remoteProvider = RemoteBackendMetadataProvider(apiClient)
    private val mockProvider = TestMockMetadataProvider()

    // Map of active polling jobs to prevent duplicate pollers
    private val activePollers = ConcurrentHashMap<String, Job>()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())

    var isTestModeActive: Boolean = false
    var pollingIntervalSeconds: Int = 2 // 2 to 10 seconds

    val activeJobsFlow: Flow<List<DownloadJob>> = jobDao.getActiveJobs().map { list ->
        list.map { it.toDomain() }
    }

    val historyJobsFlow: Flow<List<DownloadJob>> = jobDao.getHistoryJobs().map { list ->
        list.map { it.toDomain() }
    }

    val allJobsFlow: Flow<List<DownloadJob>> = jobDao.getAllJobs().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun checkBackendHealth(): BackendConnectionState {
        val start = System.currentTimeMillis()
        return try {
            val response = apiClient.apiService.checkHealth()
            val latency = System.currentTimeMillis() - start
            if (response.isSuccessful) {
                val body = response.body()
                BackendConnectionState(
                    isOnline = true,
                    latencyMs = latency,
                    statusText = "Online (HTTP 200)",
                    workerStatus = body?.workerStatus ?: "Idle",
                    providerStatus = body?.providerStatus ?: "Ready"
                )
            } else {
                BackendConnectionState(
                    isOnline = false,
                    latencyMs = latency,
                    statusText = "Backend HTTP ${response.code()}",
                    workerStatus = "Offline",
                    providerStatus = "Unavailable"
                )
            }
        } catch (e: Exception) {
            BackendConnectionState(
                isOnline = false,
                latencyMs = -1,
                statusText = "Offline: ${e.localizedMessage ?: "Connection refused"}",
                workerStatus = "Offline",
                providerStatus = "Unavailable"
            )
        }
    }

    suspend fun searchMusic(
        query: String,
        filterType: String = "ALL",
        limit: Int = 20
    ): ProviderResult {
        return if (isTestModeActive) {
            mockProvider.searchMusic(query, filterType, limit)
        } else {
            val result = remoteProvider.searchMusic(query, filterType, limit)
            if (result is ProviderResult.Error) {
                // If remote fails and user hasn't explicitly disabled fallback, inform clearly
                ProviderResult.Error("${result.message}. You can enable 'Test / Demo Mode' in Settings to preview Sonora with verified sample ASINs.")
            } else {
                result
            }
        }
    }

    /**
     * Submits a download job with ASIN and Type validation.
     * Generates standard JSON payload and sends to backend or runs test worker if in Test Mode.
     */
    suspend fun submitJob(musicItem: MusicItem): Result<DownloadJob> {
        // 1. ASIN validation
        val validatedAsin = AsinValidator.validateAsin(musicItem.asin).getOrElse {
            return Result.failure(it)
        }

        // 2. Type and URL generation
        val entityTypeStr = musicItem.type.name
        val generatedUrl = AsinValidator.buildAmazonMusicUrl(validatedAsin, entityTypeStr).getOrElse {
            return Result.failure(it)
        }

        // 3. Prepare payload
        val payloadItem = JobPayloadItem(
            asin = validatedAsin,
            type = entityTypeStr,
            url = generatedUrl,
            title = musicItem.title,
            artist = musicItem.artist,
            year = musicItem.year
        )

        val payload = JobPayload(
            schemaVersion = "1.0",
            jobId = null,
            source = "amazon_music",
            items = listOf(payloadItem)
        )

        val now = System.currentTimeMillis()

        if (isTestModeActive) {
            // Mock test execution
            val mockJobId = "mock-${UUID.randomUUID().toString().take(8)}"
            val downloadJob = DownloadJob(
                jobId = mockJobId,
                asin = validatedAsin,
                type = entityTypeStr,
                url = generatedUrl,
                title = musicItem.title,
                artist = musicItem.artist,
                year = musicItem.year,
                status = JobStatus.QUEUED,
                message = "Job queued in Test / Demo Mode",
                createdAt = now,
                updatedAt = now
            )
            jobDao.insertOrUpdate(JobEntity.fromDomain(downloadJob))
            startMockWorkerSimulation(mockJobId, musicItem, generatedUrl)
            return Result.success(downloadJob)
        }

        // Production submission to FastAPI backend
        return try {
            val response = apiClient.apiService.createJob(payload)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val downloadJob = DownloadJob(
                    jobId = body.jobId,
                    asin = validatedAsin,
                    type = entityTypeStr,
                    url = generatedUrl,
                    title = musicItem.title,
                    artist = musicItem.artist,
                    year = musicItem.year,
                    status = JobStatus.fromString(body.status),
                    message = body.message,
                    createdAt = now,
                    updatedAt = now
                )
                jobDao.insertOrUpdate(JobEntity.fromDomain(downloadJob))
                startPolling(body.jobId)
                Result.success(downloadJob)
            } else {
                Result.failure(Exception("Backend rejected job: HTTP ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to backend: ${e.localizedMessage}. Please verify Backend API URL in Settings."))
        }
    }

    /**
     * Starts polling with backoff and jitter as specified in section 10.
     */
    fun startPolling(jobId: String) {
        if (activePollers.containsKey(jobId)) return

        val pollJob = repositoryScope.launch {
            var currentInterval = (pollingIntervalSeconds.coerceAtLeast(2) * 1000).toLong()
            val maxInterval = 10_000L

            try {
                while (true) {
                    val entity = jobDao.getJobById(jobId)
                    if (entity == null) break

                    val currentStatus = JobStatus.fromString(entity.status)
                    if (currentStatus.isTerminal) break

                    try {
                        val response = apiClient.apiService.getJobStatus(jobId)
                        if (response.isSuccessful && response.body() != null) {
                            val statusBody = response.body()!!
                            val newStatus = JobStatus.fromString(statusBody.status)

                            jobDao.updateJobStatus(
                                jobId = jobId,
                                status = newStatus.value,
                                message = statusBody.message ?: entity.message,
                                progress = statusBody.progress ?: entity.progress,
                                directUrl = statusBody.directUrl ?: entity.directUrl,
                                error = statusBody.error ?: entity.error,
                                updatedAt = System.currentTimeMillis(),
                                logs = statusBody.logs ?: entity.logSnippet
                            )

                            if (newStatus.isTerminal) {
                                break
                            }
                        }
                    } catch (e: Exception) {
                        // Network error during poll, do not mark job failed, wait for next attempt
                    }

                    // Add slight jitter (±300ms) to avoid synchronized traffic spikes
                    val jitter = Random.nextLong(-300, 300)
                    delay((currentInterval + jitter).coerceAtLeast(1500))

                    // Gradual backoff up to maxInterval
                    if (currentInterval < maxInterval) {
                        currentInterval = (currentInterval * 1.25).toLong().coerceAtMost(maxInterval)
                    }
                }
            } catch (ce: CancellationException) {
                // Cancelled gracefully
            } finally {
                activePollers.remove(jobId)
            }
        }

        activePollers[jobId] = pollJob
    }

    suspend fun cancelJob(jobId: String): Result<Unit> {
        activePollers[jobId]?.cancel()
        activePollers.remove(jobId)

        return try {
            if (!isTestModeActive) {
                apiClient.apiService.cancelJob(jobId)
            }
            val existing = jobDao.getJobById(jobId)
            if (existing != null) {
                jobDao.updateJobStatus(
                    jobId = jobId,
                    status = JobStatus.CANCELLED.value,
                    message = "Job cancelled by user",
                    progress = existing.progress,
                    directUrl = existing.directUrl,
                    error = null,
                    updatedAt = System.currentTimeMillis(),
                    logs = existing.logSnippet
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun retryJob(jobId: String): Result<Unit> {
        val existing = jobDao.getJobById(jobId) ?: return Result.failure(Exception("Job not found"))

        return try {
            if (!isTestModeActive) {
                apiClient.apiService.retryJob(jobId)
            }
            jobDao.updateJobStatus(
                jobId = jobId,
                status = JobStatus.QUEUED.value,
                message = "Retrying job...",
                progress = null,
                directUrl = null,
                error = null,
                updatedAt = System.currentTimeMillis(),
                logs = null
            )
            if (isTestModeActive) {
                startMockWorkerSimulation(
                    jobId,
                    MusicItem(asin = existing.asin, title = existing.title, artist = existing.artist, year = existing.year),
                    existing.url
                )
            } else {
                startPolling(jobId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteJob(jobId: String) {
        activePollers[jobId]?.cancel()
        activePollers.remove(jobId)
        jobDao.deleteJob(jobId)
    }

    suspend fun clearHistory() {
        jobDao.clearHistory()
    }

    private fun startMockWorkerSimulation(jobId: String, item: MusicItem, url: String) {
        repositoryScope.launch {
            delay(1500)
            jobDao.updateJobStatus(
                jobId = jobId,
                status = JobStatus.STARTING.value,
                message = "Initializing xvfb-run virtual display...",
                progress = 0.1f,
                directUrl = null,
                error = null,
                updatedAt = System.currentTimeMillis(),
                logs = "[Worker] xvfb-run -a lucidadl --target \"$url\"\n[Worker] Virtual X11 display allocated :99"
            )

            delay(2200)
            jobDao.updateJobStatus(
                jobId = jobId,
                status = JobStatus.PROCESSING.value,
                message = "Running lucidadl download worker...",
                progress = 0.45f,
                directUrl = null,
                error = null,
                updatedAt = System.currentTimeMillis(),
                logs = "[Worker] Handshaking Amazon Music provider\n[Worker] Parsing product ASIN ${item.asin}\n[Worker] Fetching media streams..."
            )

            delay(2500)
            jobDao.updateJobStatus(
                jobId = jobId,
                status = JobStatus.WAITING_FOR_RESULT.value,
                message = "Capturing direct download link from stream output...",
                progress = 0.85f,
                directUrl = null,
                error = null,
                updatedAt = System.currentTimeMillis(),
                logs = "[Worker] Stream acquired. Generating direct signed download URI..."
            )

            delay(1800)
            val mockDirectLink = "https://cdn.sonora-audio.internal/downloads/${item.asin}/${item.title.replace(" ", "_")}.flac"
            jobDao.updateJobStatus(
                jobId = jobId,
                status = JobStatus.COMPLETED.value,
                message = "Download completed successfully. Direct link ready.",
                progress = 1.0f,
                directUrl = mockDirectLink,
                error = null,
                updatedAt = System.currentTimeMillis(),
                logs = "[Worker] Exit code 0.\n[Worker] DIRECT_LINK_FOUND: $mockDirectLink"
            )
        }
    }
}
