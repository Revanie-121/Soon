package com.example.data.repository

object AsinValidator {

    private val ASIN_REGEX = Regex("^[A-Z0-9]{10}$")

    /**
     * Validates an Amazon ASIN string.
     * Rules:
     * - Must be a non-empty string
     * - Whitespace trimmed
     * - Exactly 10 alphanumeric characters (uppercase normalized)
     * Returns Result.success(normalizedAsin) or Result.failure with a descriptive error.
     */
    fun validateAsin(rawAsin: String?): Result<String> {
        if (rawAsin.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("ASIN cannot be empty"))
        }

        val trimmed = rawAsin.trim().uppercase()
        if (trimmed.length != 10) {
            return Result.failure(IllegalArgumentException("ASIN must be exactly 10 alphanumeric characters (got ${trimmed.length})"))
        }

        if (!ASIN_REGEX.matches(trimmed)) {
            return Result.failure(IllegalArgumentException("ASIN contains invalid characters. Must contain only letters and numbers (A-Z, 0-9)"))
        }

        return Result.success(trimmed)
    }

    /**
     * Builds the Amazon Music canonical product URL based on ASIN and entity type.
     * Rules:
     * - type == "ALBUM" -> https://music.amazon.com/albums/{ASIN}
     * - type == "TRACK" -> https://music.amazon.com/tracks/{ASIN}
     */
    fun buildAmazonMusicUrl(asin: String?, type: String?): Result<String> {
        val validatedAsin = validateAsin(asin).getOrElse {
            return Result.failure(it)
        }

        val normalizedType = type?.trim()?.uppercase()
        return when (normalizedType) {
            "ALBUM" -> Result.success("https://music.amazon.com/albums/$validatedAsin")
            "TRACK" -> Result.success("https://music.amazon.com/tracks/$validatedAsin")
            else -> Result.failure(
                IllegalArgumentException("Invalid entity type '$type'. Type must be either TRACK or ALBUM")
            )
        }
    }
}
