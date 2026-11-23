package com.example.data.model

enum class ProviderType(
    val displayName: String,
    val defaultBaseUrl: String
) {
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1"),
    GENERIC("Generic", ""),
    CUSTOM("Custom", "");

    companion object {
        fun fromStored(value: String): ProviderType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CUSTOM
        }
    }
}
