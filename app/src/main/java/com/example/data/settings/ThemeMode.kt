package com.example.data.settings

enum class ThemeMode {
    LIGHT,
    DARK,
    AUTO;

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTO
    }
}
