package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.Profile
import com.finnvek.cornersapart.model.ProfilesData
import com.finnvek.cornersapart.model.toSnapshotCopy
import com.finnvek.cornersapart.model.toSnapshotList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val store: JsonStateStore<ProfilesData>,
) {
    val profiles: Flow<List<Profile>> = store.data.map { data -> data.toSnapshotCopy().profiles }
    val activeProfile: Flow<Profile?> =
        profiles.map { profiles -> profiles.firstOrNull { profile -> profile.active } }

    suspend fun upsertProfile(profile: Profile) {
        val incomingProfile = profile.toSnapshotCopy()
        store.update { data ->
            val storedProfiles = data.toSnapshotCopy().profiles
            val withoutExisting = storedProfiles.filterNot { existing -> existing.id == incomingProfile.id }
            val nextProfile =
                if (incomingProfile.active || withoutExisting.none { existing -> existing.active }) {
                    incomingProfile.copy(active = true)
                } else {
                    incomingProfile
                }
            ProfilesData(
                profiles =
                    (withoutExisting + nextProfile).withSingleActiveProfile(
                        activeProfileId = nextProfile.id.takeIf { nextProfile.active },
                    ),
            ).toSnapshotCopy()
        }
    }

    /** Tallentaa haastetason tähdet; parasta tulosta ei koskaan huononneta. */
    suspend fun recordChallengeStars(
        profileId: String,
        level: Int,
        stars: Int,
    ) {
        store.update { data ->
            val storedProfiles = data.toSnapshotCopy().profiles
            data
                .copy(
                    profiles =
                        storedProfiles.map { profile ->
                            if (profile.id == profileId && stars > (profile.challengeStars[level] ?: 0)) {
                                profile.copy(challengeStars = profile.challengeStars + (level to stars))
                            } else {
                                profile
                            }
                        },
                ).toSnapshotCopy()
        }
    }

    /** Kirjaa Rivals-ottelun tuloksen: voitto- tai tappiolaskuri kasvaa yhdellä. */
    suspend fun recordRivalResult(
        profileId: String,
        rivalId: String,
        won: Boolean,
    ) {
        store.update { data ->
            val storedProfiles = data.toSnapshotCopy().profiles
            data
                .copy(
                    profiles =
                        storedProfiles.map { profile ->
                            if (profile.id == profileId) {
                                if (won) {
                                    profile.copy(rivalWins = profile.rivalWins.incremented(rivalId))
                                } else {
                                    profile.copy(rivalLosses = profile.rivalLosses.incremented(rivalId))
                                }
                            } else {
                                profile
                            }
                        },
                ).toSnapshotCopy()
        }
    }

    suspend fun addAchievements(
        profileId: String,
        achievementIds: List<String>,
    ) {
        if (achievementIds.isEmpty()) return
        store.update { data ->
            val storedProfiles = data.toSnapshotCopy().profiles
            data
                .copy(
                    profiles =
                        storedProfiles.map { profile ->
                            if (profile.id == profileId) {
                                profile.copy(
                                    achievements = (profile.achievements + achievementIds).distinct(),
                                )
                            } else {
                                profile
                            }
                        },
                ).toSnapshotCopy()
        }
    }

    /** Tallentaa päivän haasteen tuloksen; parasta ei huononneta ja vanhimmat karsitaan. */
    suspend fun recordDailyBest(
        profileId: String,
        date: String,
        score: Int,
    ) {
        store.update { data ->
            val storedProfiles = data.toSnapshotCopy().profiles
            data
                .copy(
                    profiles =
                        storedProfiles.map { profile ->
                            if (profile.id == profileId && score > (profile.dailyBestScores[date] ?: -1)) {
                                val updated = profile.dailyBestScores + (date to score)
                                profile.copy(
                                    dailyBestScores =
                                        updated.entries
                                            .sortedByDescending { entry -> entry.key }
                                            .take(MAX_DAILY_BEST_ENTRIES)
                                            .associate { entry -> entry.key to entry.value },
                                )
                            } else {
                                profile
                            }
                        },
                ).toSnapshotCopy()
        }
    }

    /** Poistaa profiilin; viimeistä profiilia ei poisteta ja aktiivisuus siirtyy tarvittaessa. */
    suspend fun deleteProfile(profileId: String) {
        store.update { data ->
            val storedProfiles = data.toSnapshotCopy().profiles
            val remaining = storedProfiles.filterNot { profile -> profile.id == profileId }
            if (remaining.isEmpty()) {
                data.toSnapshotCopy()
            } else {
                val activeId = remaining.firstOrNull { profile -> profile.active }?.id ?: remaining.first().id
                ProfilesData(
                    profiles = remaining.withSingleActiveProfile(activeProfileId = activeId),
                ).toSnapshotCopy()
            }
        }
    }

    suspend fun setActiveProfile(profileId: String) {
        store.update { data ->
            val storedProfiles = data.toSnapshotCopy().profiles
            data
                .copy(
                    profiles =
                        storedProfiles.withSingleActiveProfile(
                            activeProfileId = profileId,
                        ),
                ).toSnapshotCopy()
        }
    }

    suspend fun appendHistory(
        profileId: String,
        entry: HistoryEntry,
    ) {
        val historyEntry = entry.toSnapshotCopy()
        store.update { data ->
            val storedProfiles = data.toSnapshotCopy().profiles
            data
                .copy(
                    profiles =
                        storedProfiles.map { profile ->
                            if (profile.id == profileId) {
                                profile.copy(
                                    history =
                                        (profile.history + historyEntry)
                                            .takeLast(GameConstants.MAX_HISTORY_ENTRIES)
                                            .toSnapshotList(),
                                )
                            } else {
                                profile
                            }
                        },
                ).toSnapshotCopy()
        }
    }

    private fun List<Profile>.withSingleActiveProfile(activeProfileId: String?): List<Profile> {
        val resolvedActiveProfileId = activeProfileId ?: firstOrNull { profile -> profile.active }?.id
        return map { profile ->
            profile.copy(active = profile.id == resolvedActiveProfileId)
        }.toSnapshotList()
    }

    private fun Map<String, Int>.incremented(key: String): Map<String, Int> = this + (key to (this[key] ?: 0) + 1)

    private companion object {
        const val MAX_DAILY_BEST_ENTRIES = 60
    }
}
