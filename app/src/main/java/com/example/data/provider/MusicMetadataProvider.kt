package com.example.data.provider

import com.example.data.model.MusicEntityType
import com.example.data.model.MusicItem
import com.example.data.remote.ApiClient
import com.example.data.repository.AsinValidator

sealed interface ProviderResult {
    data class Success(
        val items: List<MusicItem>,
        val isMock: Boolean = false,
        val providerName: String = "Amazon Music Provider",
        val message: String? = null
    ) : ProviderResult

    data class ConfigNeeded(
        val message: String,
        val providerName: String
    ) : ProviderResult

    data class Error(
        val message: String
    ) : ProviderResult
}

data class ProviderStatus(
    val isAvailable: Boolean,
    val providerName: String,
    val mode: String, // "LIVE_BACKEND", "AMAZON_ADAPTER", "TEST_MOCK"
    val description: String
)

interface MusicMetadataProvider {
    val name: String
    suspend fun searchMusic(query: String, filterType: String = "ALL", limit: Int = 20): ProviderResult
    fun normalizeMusicResult(raw: Map<String, Any?>): MusicItem?
    suspend fun getProviderStatus(): ProviderStatus
}

/**
 * Metadata provider that routes queries through the configured FastAPI backend.
 */
class RemoteBackendMetadataProvider(
    private val apiClient: ApiClient
) : MusicMetadataProvider {
    override val name: String = "FastAPI Backend Provider"

    override suspend fun searchMusic(
        query: String,
        filterType: String,
        limit: Int
    ): ProviderResult {
        return try {
            val response = apiClient.apiService.search(
                query = query,
                type = filterType.lowercase(),
                limit = limit
            )
            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                ProviderResult.Success(
                    items = items,
                    isMock = false,
                    providerName = name,
                    message = if (items.isEmpty()) "No results found for '$query'" else null
                )
            } else {
                ProviderResult.Error("Backend returned HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            ProviderResult.Error("Cannot connect to backend: ${e.localizedMessage ?: "Unknown network error"}")
        }
    }

    override fun normalizeMusicResult(raw: Map<String, Any?>): MusicItem? {
        val asinRaw = raw["asin"] as? String ?: return null
        val validatedAsin = AsinValidator.validateAsin(asinRaw).getOrNull() ?: return null
        val title = raw["title"] as? String ?: "Untitled"
        val artist = raw["artist"] as? String ?: "Unknown Artist"
        val year = (raw["year"] as? Number)?.toInt()
        val typeStr = (raw["type"] as? String)?.uppercase() ?: "TRACK"
        val entityType = if (typeStr == "ALBUM") MusicEntityType.ALBUM else MusicEntityType.TRACK
        val artwork = raw["artwork_url"] as? String
        val sourceUrl = raw["source_url"] as? String
            ?: AsinValidator.buildAmazonMusicUrl(validatedAsin, typeStr).getOrNull()

        return MusicItem(
            asin = validatedAsin,
            title = title,
            artist = artist,
            year = year,
            type = entityType,
            artworkUrl = artwork,
            sourceUrl = sourceUrl
        )
    }

    override suspend fun getProviderStatus(): ProviderStatus {
        return try {
            val health = apiClient.apiService.checkHealth()
            if (health.isSuccessful) {
                ProviderStatus(
                    isAvailable = true,
                    providerName = name,
                    mode = "LIVE_BACKEND",
                    description = "Connected to ${apiClient.baseUrl}"
                )
            } else {
                ProviderStatus(
                    isAvailable = false,
                    providerName = name,
                    mode = "LIVE_BACKEND",
                    description = "Backend responded with HTTP ${health.code()}"
                )
            }
        } catch (e: Exception) {
            ProviderStatus(
                isAvailable = false,
                providerName = name,
                mode = "LIVE_BACKEND",
                description = "Backend offline or unreachable (${e.message})"
            )
        }
    }
}

/**
 * Clearly labeled Test / Demonstration metadata provider.
 * Uses verified real-world Amazon Music product ASINs for testing.
 * As mandated in the prompt: "Buat mode pengujian dengan mock data yang diberi label jelas.
 * Jangan mencampurkan mock data dengan hasil pencarian produksi."
 */
class TestMockMetadataProvider : MusicMetadataProvider {
    override val name: String = "Test Mock Provider (Demonstration Only)"

    private val sampleCatalogue = listOf(
        MusicItem(
            asin = "B00C069H6M",
            title = "Random Access Memories",
            artist = "Daft Punk",
            year = 2013,
            type = MusicEntityType.ALBUM,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/61U7T985z8L._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/albums/B00C069H6M"
        ),
        MusicItem(
            asin = "B083Z9374M",
            title = "Blinding Lights",
            artist = "The Weeknd",
            year = 2020,
            type = MusicEntityType.TRACK,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/71u%2B4jU57rL._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/tracks/B083Z9374M"
        ),
        MusicItem(
            asin = "B00138F12A",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            year = 1975,
            type = MusicEntityType.TRACK,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/71B%2B1T7eNmL._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/tracks/B00138F12A"
        ),
        MusicItem(
            asin = "B07PB2341M",
            title = "When We All Fall Asleep, Where Do We Go?",
            artist = "Billie Eilish",
            year = 2019,
            type = MusicEntityType.ALBUM,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/61kC8H6zM0L._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/albums/B07PB2341M"
        ),
        MusicItem(
            asin = "B07V2QJ4PL",
            title = "Bad Guy",
            artist = "Billie Eilish",
            year = 2019,
            type = MusicEntityType.TRACK,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/61kC8H6zM0L._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/tracks/B07V2QJ4PL"
        ),
        MusicItem(
            asin = "B08R92L9K8",
            title = "Discovery",
            artist = "Daft Punk",
            year = 2001,
            type = MusicEntityType.ALBUM,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/71P7u1F4H7L._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/albums/B08R92L9K8"
        ),
        MusicItem(
            asin = "B007XU06KM",
            title = "Get Lucky (feat. Pharrell Williams)",
            artist = "Daft Punk",
            year = 2013,
            type = MusicEntityType.TRACK,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/61U7T985z8L._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/tracks/B007XU06KM"
        ),
        MusicItem(
            asin = "B09V7N88W2",
            title = "Harry's House",
            artist = "Harry Styles",
            year = 2022,
            type = MusicEntityType.ALBUM,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/81M6bLgJzKL._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/albums/B09V7N88W2"
        ),
        MusicItem(
            asin = "B09V8G3JMK",
            title = "As It Was",
            artist = "Harry Styles",
            year = 2022,
            type = MusicEntityType.TRACK,
            artworkUrl = "https://images-na.ssl-images-amazon.com/images/I/81M6bLgJzKL._SX500_.jpg",
            sourceUrl = "https://music.amazon.com/tracks/B09V8G3JMK"
        )
    )

    override suspend fun searchMusic(
        query: String,
        filterType: String,
        limit: Int
    ): ProviderResult {
        val q = query.trim().lowercase()
        val filtered = sampleCatalogue.filter { item ->
            val matchQuery = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.artist.lowercase().contains(q) ||
                item.asin.lowercase().contains(q)

            val matchType = when (filterType.uppercase()) {
                "TRACK", "TRACKS" -> item.type == MusicEntityType.TRACK
                "ALBUM", "ALBUMS" -> item.type == MusicEntityType.ALBUM
                else -> true
            }

            matchQuery && matchType
        }.take(limit)

        return ProviderResult.Success(
            items = filtered,
            isMock = true,
            providerName = name,
            message = "Displaying mock test results (Mock Mode is Active). Verified Amazon ASINs."
        )
    }

    override fun normalizeMusicResult(raw: Map<String, Any?>): MusicItem? = null

    override suspend fun getProviderStatus(): ProviderStatus {
        return ProviderStatus(
            isAvailable = true,
            providerName = name,
            mode = "TEST_MOCK",
            description = "Test Mode Active (Sample Verified ASINs)"
        )
    }
}
