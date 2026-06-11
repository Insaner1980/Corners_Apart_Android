package com.finnvek.cornersapart.model

import kotlin.math.absoluteValue

data class GeneratedAvatar(
    val style: LocalAvatarStyle,
    val initials: String,
    val palette: List<Int>,
    val localOnly: Boolean = true,
)

class LocalAvatarGenerator {
    fun generate(profile: Profile): GeneratedAvatar {
        val seed = profile.avatarSeed.ifBlank { profile.id }
        return GeneratedAvatar(
            style = profile.avatarStyle,
            initials = initialsFor(profile.name),
            palette = paletteFor(seed, profile.colorIndex, profile.avatarStyle),
        )
    }

    private fun initialsFor(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { part -> part.isNotBlank() }
        return parts
            .take(2)
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "CA" }
    }

    private fun paletteFor(
        seed: String,
        colorIndex: Int,
        style: LocalAvatarStyle,
    ): List<Int> {
        val base = (seed.hashCode() + colorIndex * COLOR_STEP + style.ordinal * STYLE_STEP).absoluteValue
        return List(PALETTE_SIZE) { index ->
            val channel = (base + index * CHANNEL_STEP) and RGB_MASK
            RGB_OPAQUE or channel
        }
    }

    private companion object {
        const val COLOR_STEP = 997
        const val STYLE_STEP = 577
        const val CHANNEL_STEP = 0x24_68_AC
        const val RGB_MASK = 0x00_FF_FF_FF
        const val RGB_OPAQUE = -0x1000000
        const val PALETTE_SIZE = 4
    }
}
