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
}
