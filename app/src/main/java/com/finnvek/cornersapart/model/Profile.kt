package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val colorIndex: Int = 0,
    val avatarStyle: LocalAvatarStyle = LocalAvatarStyle.INITIALS,
    val avatarSeed: String = id,
    val active: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val challengeStars: Map<Int, Int> = emptyMap(),
    val achievements: List<String> = emptyList(),
)

@Serializable
enum class LocalAvatarStyle {
    INITIALS,
    GEOMETRIC,
    MOSAIC,
    RINGS,
}

@Serializable
data class ProfilesData(
    val profiles: List<Profile> = emptyList(),
)
