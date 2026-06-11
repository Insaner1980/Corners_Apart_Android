package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.Profile
import com.finnvek.cornersapart.model.ProfilesData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val store: JsonStateStore<ProfilesData>,
) {
    val profiles: Flow<List<Profile>> = store.data.map { data -> data.profiles }
    val activeProfile: Flow<Profile?> =
        profiles.map { profiles -> profiles.firstOrNull { profile -> profile.active } }

    suspend fun upsertProfile(profile: Profile) {
        store.update { data ->
            val withoutExisting = data.profiles.filterNot { existing -> existing.id == profile.id }
            val nextProfile =
                if (profile.active || withoutExisting.none { existing -> existing.active }) {
                    profile.copy(active = true)
                } else {
                    profile
                }
            ProfilesData(
                profiles =
                    (withoutExisting + nextProfile).withSingleActiveProfile(
                        activeProfileId = nextProfile.id.takeIf { nextProfile.active },
                    ),
            )
        }
    }

    suspend fun setActiveProfile(profileId: String) {
        store.update { data ->
            data.copy(
                profiles =
                    data.profiles.withSingleActiveProfile(
                        activeProfileId = profileId,
                    ),
            )
        }
    }

    suspend fun appendHistory(
        profileId: String,
        entry: HistoryEntry,
    ) {
        store.update { data ->
            data.copy(
                profiles =
                    data.profiles.map { profile ->
                        if (profile.id == profileId) {
                            profile.copy(history = profile.history + entry)
                        } else {
                            profile
                        }
                    },
            )
        }
    }

    private fun List<Profile>.withSingleActiveProfile(activeProfileId: String?): List<Profile> {
        val resolvedActiveProfileId = activeProfileId ?: firstOrNull { profile -> profile.active }?.id
        return map { profile ->
            profile.copy(active = profile.id == resolvedActiveProfileId)
        }
    }
}
