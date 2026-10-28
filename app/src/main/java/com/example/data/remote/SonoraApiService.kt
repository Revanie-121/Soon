package com.example.data.remote

import com.example.data.model.GenericActionResponse
import com.example.data.model.HealthResponse
import com.example.data.model.JobCreateResponse
import com.example.data.model.JobPayload
import com.example.data.model.JobStatusResponse
import com.example.data.model.MusicItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SonoraApiService {

    @GET("/api/health")
    suspend fun checkHealth(): Response<HealthResponse>

    @GET("/api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "all",
        @Query("limit") limit: Int = 20
    ): Response<List<MusicItem>>

    @POST("/api/jobs")
    suspend fun createJob(
        @Body payload: JobPayload
    ): Response<JobCreateResponse>

    @GET("/api/jobs")
    suspend fun listJobs(): Response<List<JobStatusResponse>>

    @GET("/api/jobs/{job_id}")
    suspend fun getJobStatus(
        @Path("job_id") jobId: String
    ): Response<JobStatusResponse>

    @POST("/api/jobs/{job_id}/cancel")
    suspend fun cancelJob(
        @Path("job_id") jobId: String
    ): Response<GenericActionResponse>

    @POST("/api/jobs/{job_id}/retry")
    suspend fun retryJob(
        @Path("job_id") jobId: String
    ): Response<GenericActionResponse>

    @GET("/api/jobs/{job_id}/result")
    suspend fun getJobResult(
        @Path("job_id") jobId: String
    ): Response<JobStatusResponse>
}
