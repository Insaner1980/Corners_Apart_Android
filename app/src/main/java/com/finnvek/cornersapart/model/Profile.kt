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
    val dailyBestScores: Map<String, Int> = emptyMap(),
    val bestDailyStreak: Int = 0,
    val rivalWins: Map<String, Int> = emptyMap(),
    val rivalLosses: Map<String, Int> = emptyMap(),
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
