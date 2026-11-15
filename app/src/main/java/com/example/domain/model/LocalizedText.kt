package com.example.domain.model

enum class TextScript(val id: String) {
    LATIN("latin"),
    CYRILLIC("cyrillic");

    companion object {
        fun fromId(id: String?): TextScript =
            entries.firstOrNull { it.id == id } ?: LATIN
    }
}

data class LText(
    val latin: String,
    val cyrillic: String
) {
    fun get(script: TextScript): String = when (script) {
        TextScript.LATIN -> latin
        TextScript.CYRILLIC -> cyrillic
    }
}

fun lText(latin: String, cyrillic: String): LText = LText(latin, cyrillic)
