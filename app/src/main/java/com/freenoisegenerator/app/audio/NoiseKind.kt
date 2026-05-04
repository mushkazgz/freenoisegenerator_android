package com.freenoisegenerator.app.audio

enum class NoiseKind(val label: String, val description: String) {
    WHITE("Blanco", "Brillante y uniforme"),
    PINK("Rosa", "Suave y equilibrado"),
    BROWN("Marron", "Grave y profundo");

    companion object {
        fun fromName(name: String?): NoiseKind =
            entries.firstOrNull { it.name == name } ?: WHITE
    }
}
