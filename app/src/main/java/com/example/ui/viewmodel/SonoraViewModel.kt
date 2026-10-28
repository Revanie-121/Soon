package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DownloadJob
import com.example.data.model.JobPayload
import com.example.data.model.JobPayloadItem
import com.example.data.model.JobStatus
import com.example.data.model.MusicEntityType
import com.example.data.model.MusicItem
import com.example.data.provider.ProviderResult
import com.example.data.repository.AsinValidator
import com.example.data.repository.BackendConnectionState
import com.example.data.repository.SonoraRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val categoryFilter: String = "ALL", // ALL, TRACK, ALBUM
    val isLoading: Boolean = false,
    val results: List<MusicItem> = emptyList(),
    val isMock: Boolean = false,
    val providerName: String = "FastAPI Backend Provider",
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

data class SettingsUiState(
    val backendUrl: String = "http://10.0.2.2:8000",
    val apiToken: String = "",
    val pollingIntervalSeconds: Int = 2,
    val isTestMode: Boolean = false,
    val connectionState: BackendConnectionState = BackendConnectionState(),
    val isCheckingConnection: Boolean = false
)

class SonoraViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SonoraRepository(application)

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    // Confirmation dialog item
    private val _downloadTargetItem = MutableStateFlow<MusicItem?>(null)
    val downloadTargetItem: StateFlow<MusicItem?> = _downloadTargetItem.asStateFlow()

    // Detailed bottom sheet job
    private val _detailJob = MutableStateFlow<DownloadJob?>(null)
    val detailJob: StateFlow<DownloadJob?> = _detailJob.asStateFlow()

    // Direct result dialog job
    private val _completedResultJob = MutableStateFlow<DownloadJob?>(null)
    val completedResultJob: StateFlow<DownloadJob?> = _completedResultJob.asStateFlow()

    // Active Jobs Flow
    val activeJobs: StateFlow<List<DownloadJob>> = repository.activeJobsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History filter & query
    private val _historyStatusFilter = MutableStateFlow("ALL")
    val historyStatusFilter: StateFlow<String> = _historyStatusFilter.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // Filtered History Flow
    val historyJobs: StateFlow<List<DownloadJob>> = combine(
        repository.historyJobsFlow,
        _historyStatusFilter,
        _historySearchQuery
    ) { jobs, filter, query ->
        jobs.filter { job ->
            val matchesFilter = when (filter) {
                "COMPLETED" -> job.status == JobStatus.COMPLETED
                "FAILED" -> job.status == JobStatus.FAILED
                "CANCELLED" -> job.status == JobStatus.CANCELLED
                "TIMED_OUT" -> job.status == JobStatus.TIMED_OUT
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                job.title.contains(query, ignoreCase = true) ||
                job.artist.contains(query, ignoreCase = true) ||
                job.asin.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Snackbar event channel
    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage.asSharedFlow()

    private var searchDebounceJob: Job? = null
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    init {
        // Run initial health check & sample catalogue load
        checkConnection()
        triggerSearch(query = "", immediate = true)
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchState.update { it.copy(query = newQuery) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(500)
            executeSearch(newQuery, _searchState.value.categoryFilter)
        }
    }

    fun onCategoryFilterChanged(category: String) {
        _searchState.update { it.copy(categoryFilter = category) }
        executeSearch(_searchState.value.query, category)
    }

    fun triggerSearch(query: String = _searchState.value.query, immediate: Boolean = false) {
        searchDebounceJob?.cancel()
        if (immediate) {
            executeSearch(query, _searchState.value.categoryFilter)
        } else {
            searchDebounceJob = viewModelScope.launch {
                executeSearch(query, _searchState.value.categoryFilter)
            }
        }
    }

    private fun executeSearch(query: String, filter: String) {
        viewModelScope.launch {
            _searchState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.searchMusic(query, filter)
            when (result) {
                is ProviderResult.Success -> {
                    _searchState.update {
                        it.copy(
                            isLoading = false,
                            results = result.items,
                            isMock = result.isMock,
                            providerName = result.providerName,
                            statusMessage = result.message,
                            errorMessage = null
                        )
                    }
                }
                is ProviderResult.ConfigNeeded -> {
                    _searchState.update {
                        it.copy(
                            isLoading = false,
                            results = emptyList(),
                            statusMessage = result.message,
                            providerName = result.providerName,
                            errorMessage = null
                        )
                    }
                }
                is ProviderResult.Error -> {
                    _searchState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            statusMessage = null
                        )
                    }
                }
            }
        }
    }

    fun selectItemForDownload(item: MusicItem) {
        _downloadTargetItem.value = item
    }

    fun dismissDownloadDialog() {
        _downloadTargetItem.value = null
    }

    fun confirmDownload(item: MusicItem) {
        viewModelScope.launch {
            _downloadTargetItem.value = null
            val submitResult = repository.submitJob(item)
            submitResult.onSuccess { job ->
                _snackBarMessage.emit("Job ${job.jobId} submitted to download queue")
            }.onFailure { error ->
                _snackBarMessage.emit("Failed to queue job: ${error.localizedMessage}")
            }
        }
    }

    fun openJobDetail(job: DownloadJob) {
        _detailJob.value = job
    }

    fun dismissJobDetail() {
        _detailJob.value = null
    }

    fun openDirectLinkResult(job: DownloadJob) {
        _completedResultJob.value = job
    }

    fun dismissDirectLinkResult() {
        _completedResultJob.value = null
    }

    fun cancelJob(jobId: String) {
        viewModelScope.launch {
            val result = repository.cancelJob(jobId)
            result.onSuccess {
                _snackBarMessage.emit("Job cancelled")
            }.onFailure { err ->
                _snackBarMessage.emit("Cancel failed: ${err.localizedMessage}")
            }
        }
    }

    fun retryJob(jobId: String) {
        viewModelScope.launch {
            val result = repository.retryJob(jobId)
            result.onSuccess {
                _snackBarMessage.emit("Job requeued")
            }.onFailure { err ->
                _snackBarMessage.emit("Retry failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            repository.deleteJob(jobId)
            _snackBarMessage.emit("Entry removed")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _snackBarMessage.emit("History cleared")
        }
    }

    fun setHistoryStatusFilter(filter: String) {
        _historyStatusFilter.value = filter
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    // Settings actions
    fun updateBackendUrl(newUrl: String) {
        _settingsState.update { it.copy(backendUrl = newUrl) }
        repository.apiClient.baseUrl = newUrl
    }

    fun updateApiToken(newToken: String) {
        _settingsState.update { it.copy(apiToken = newToken) }
        repository.apiClient.updateToken(newToken)
    }

    fun updatePollingInterval(seconds: Int) {
        val safeSeconds = seconds.coerceIn(2, 10)
        _settingsState.update { it.copy(pollingIntervalSeconds = safeSeconds) }
        repository.pollingIntervalSeconds = safeSeconds
    }

    fun toggleTestMode(enabled: Boolean) {
        _settingsState.update { it.copy(isTestMode = enabled) }
        repository.isTestModeActive = enabled
        triggerSearch(immediate = true)
    }

    fun checkConnection() {
        viewModelScope.launch {
            _settingsState.update { it.copy(isCheckingConnection = true) }
            val state = repository.checkBackendHealth()
            _settingsState.update {
                it.copy(
                    isCheckingConnection = false,
                    connectionState = state
                )
            }
        }
    }

    fun generateJobJsonPayload(job: DownloadJob): String {
        val payload = JobPayload(
            schemaVersion = "1.0",
            jobId = job.jobId,
            source = "amazon_music",
            items = listOf(
                JobPayloadItem(
                    asin = job.asin,
                    type = job.type,
                    url = job.url,
                    title = job.title,
                    artist = job.artist,
                    year = job.year
                )
            )
        )
        val adapter = moshi.adapter(JobPayload::class.java).indent("  ")
        return adapter.toJson(payload)
    }

    fun generateItemJsonPayload(item: MusicItem, generatedUrl: String): String {
        val payload = JobPayload(
            schemaVersion = "1.0",
            jobId = null,
            source = "amazon_music",
            items = listOf(
                JobPayloadItem(
                    asin = item.asin,
                    type = item.type.name,
                    url = generatedUrl,
                    title = item.title,
                    artist = item.artist,
                    year = item.year
                )
            )
        )
        val adapter = moshi.adapter(JobPayload::class.java).indent("  ")
        return adapter.toJson(payload)
    }

    fun notifyMessage(msg: String) {
        viewModelScope.launch {
            _snackBarMessage.emit(msg)
        }
    }
}
