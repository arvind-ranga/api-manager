package com.example.data.settings

import java.net.URLEncoder

/**
 * Browser search providers used by the "discover" section.
 *
 * The app does not host an API catalogue. Queries are enriched with an
 * endpoint-focused suffix and opened in the user's own browser, so the search
 * provider can return the official documentation or key-management page.
 */
enum class SearchProvider(
    val displayName: String,
    val emoji: String
) {
    GOOGLE_AI("Google AI Mode", "✨"),
    BRAVE_AI("Brave AI", "🦁");

    fun buildSearchUrl(rawQuery: String): String {
        val query = endpointFocusedQuery(rawQuery)
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        return when (this) {
            GOOGLE_AI -> "https://www.google.com/search?q=$encoded&udm=50"
            BRAVE_AI -> "https://search.brave.com/search?q=$encoded&summary=1"
        }
    }

    companion object {
        private const val ENDPOINT_SUFFIX = "API endpoint base URL documentation"

        fun fromStored(value: String?): SearchProvider =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GOOGLE_AI

        internal fun endpointFocusedQuery(rawQuery: String): String {
            val query = rawQuery.trim().ifBlank { "API" }
            val lower = query.lowercase()
            return if (lower.contains("api") && lower.contains("endpoint")) {
                query
            } else {
                "$query $ENDPOINT_SUFFIX"
            }
        }
    }
}
