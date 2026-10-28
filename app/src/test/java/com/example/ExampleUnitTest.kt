package com.example

import com.example.data.model.JobPayload
import com.example.data.model.JobPayloadItem
import com.example.data.model.JobStatus
import com.example.data.repository.AsinValidator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonoraUnitTest {

    @Test
    fun testValidAsinValidation() {
        val validAsin = "B00C069H6M"
        val result = AsinValidator.validateAsin(validAsin)
        assertTrue(result.isSuccess)
        assertEquals("B00C069H6M", result.getOrNull())

        // Test lowercase with whitespace trimming
        val paddedLower = "  b083z9374m  "
        val trimmedResult = AsinValidator.validateAsin(paddedLower)
        assertTrue(trimmedResult.isSuccess)
        assertEquals("B083Z9374M", trimmedResult.getOrNull())
    }

    @Test
    fun testInvalidAsinValidation() {
        // Too short
        assertFalse(AsinValidator.validateAsin("B08123").isSuccess)
        // Too long
        assertFalse(AsinValidator.validateAsin("B08123456789").isSuccess)
        // Empty / blank
        assertFalse(AsinValidator.validateAsin("").isSuccess)
        assertFalse(AsinValidator.validateAsin("   ").isSuccess)
        assertFalse(AsinValidator.validateAsin(null).isSuccess)
        // Special characters
        assertFalse(AsinValidator.validateAsin("B08123456#").isSuccess)
        assertFalse(AsinValidator.validateAsin("B08-123456").isSuccess)
    }

    @Test
    fun testAmazonMusicTrackUrlGeneration() {
        val asin = "B083Z9374M"
        val trackUrlResult = AsinValidator.buildAmazonMusicUrl(asin, "TRACK")
        assertTrue(trackUrlResult.isSuccess)
        assertEquals("https://music.amazon.com/tracks/B083Z9374M", trackUrlResult.getOrNull())

        // Lowercase type
        val lowerTypeResult = AsinValidator.buildAmazonMusicUrl(asin, "track")
        assertTrue(lowerTypeResult.isSuccess)
        assertEquals("https://music.amazon.com/tracks/B083Z9374M", lowerTypeResult.getOrNull())
    }

    @Test
    fun testAmazonMusicAlbumUrlGeneration() {
        val asin = "B00C069H6M"
        val albumUrlResult = AsinValidator.buildAmazonMusicUrl(asin, "ALBUM")
        assertTrue(albumUrlResult.isSuccess)
        assertEquals("https://music.amazon.com/albums/B00C069H6M", albumUrlResult.getOrNull())
    }

    @Test
    fun testInvalidEntityTypeRejected() {
        val asin = "B00C069H6M"
        val invalidTypeResult = AsinValidator.buildAmazonMusicUrl(asin, "PLAYLIST")
        assertFalse(invalidTypeResult.isSuccess)

        val nullTypeResult = AsinValidator.buildAmazonMusicUrl(asin, null)
        assertFalse(nullTypeResult.isSuccess)
    }

    @Test
    fun testJobStatusTransitions() {
        assertTrue(JobStatus.COMPLETED.isTerminal)
        assertTrue(JobStatus.FAILED.isTerminal)
        assertTrue(JobStatus.CANCELLED.isTerminal)
        assertTrue(JobStatus.TIMED_OUT.isTerminal)

        assertFalse(JobStatus.QUEUED.isTerminal)
        assertFalse(JobStatus.PROCESSING.isTerminal)

        assertTrue(JobStatus.PROCESSING.isCancellable)
        assertTrue(JobStatus.FAILED.isRetryable)
    }

    @Test
    fun testJsonPayloadSerialization() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(JobPayload::class.java)

        val payload = JobPayload(
            schemaVersion = "1.0",
            jobId = null,
            source = "amazon_music",
            items = listOf(
                JobPayloadItem(
                    asin = "B00C069H6M",
                    type = "ALBUM",
                    url = "https://music.amazon.com/albums/B00C069H6M",
                    title = "Random Access Memories",
                    artist = "Daft Punk",
                    year = 2013
                )
            )
        )

        val json = adapter.toJson(payload)
        assertTrue(json.contains("\"schema_version\":\"1.0\""))
        assertTrue(json.contains("\"source\":\"amazon_music\""))
        assertTrue(json.contains("\"asin\":\"B00C069H6M\""))
        assertTrue(json.contains("\"type\":\"ALBUM\""))
        assertTrue(json.contains("https://music.amazon.com/albums/B00C069H6M"))
    }
}
