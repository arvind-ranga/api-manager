package com.example.data.model

enum class ProviderType(
    val displayName: String,
    val defaultBaseUrl: String
) {
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1"),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/"),
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
    CUSTOM("Custom", "");

    companion object {
        fun fromStored(value: String): ProviderType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CUSTOM
        }
    }
}