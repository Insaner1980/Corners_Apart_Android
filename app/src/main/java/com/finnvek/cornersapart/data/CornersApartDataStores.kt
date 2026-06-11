package com.finnvek.cornersapart.data

import android.content.Context
import androidx.datastore.dataStore
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.ProfilesData
import com.finnvek.cornersapart.model.SavedGameData

private val Context.savedGameDataStore by dataStore(
    fileName = "saved-game.json",
    serializer =
        JsonDataStoreSerializer(
            defaultValue = SavedGameData(),
            serializer = SavedGameData.serializer(),
        ),
)

private val Context.profilesDataStore by dataStore(
    fileName = "profiles.json",
    serializer =
        JsonDataStoreSerializer(
            defaultValue = ProfilesData(),
            serializer = ProfilesData.serializer(),
        ),
)

private val Context.settingsDataStore by dataStore(
    fileName = "settings.json",
    serializer =
        JsonDataStoreSerializer(
            defaultValue = GameSettings(),
            serializer = GameSettings.serializer(),
        ),
)

fun Context.gameRepository(): GameRepository = GameRepository(DataStoreJsonStateStore(savedGameDataStore))

fun Context.profileRepository(): ProfileRepository = ProfileRepository(DataStoreJsonStateStore(profilesDataStore))

fun Context.settingsRepository(): SettingsRepository = SettingsRepository(DataStoreJsonStateStore(settingsDataStore))
