package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.PlayerScore
import com.finnvek.cornersapart.model.Profile
import com.finnvek.cornersapart.model.ProfilesData
import com.finnvek.cornersapart.model.Ruleset
import com.finnvek.cornersapart.model.SavedGameData
import com.finnvek.cornersapart.model.ScoreBreakdown
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class JsonDataStoreSerializerTest {
    @Test
    fun jsonSerializerRoundTripsSettingsWithDefaultsEncoded() =
        runTest {
            val serializer =
                JsonDataStoreSerializer(
                    defaultValue = GameSettings(),
                    serializer = GameSettings.serializer(),
                )
            val settings =
                GameSettings(
                    preferredDifficulty = 4,
                    soundEnabled = false,
                    hapticsEnabled = false,
                    preferredMode = GameMode.COMPACT_DUEL,
                )

            val decoded = serializer.roundTrip(settings)

            assertEquals(settings, decoded)
            assertEquals(GameSettings(), serializer.defaultValue)
        }

    @Test
    fun jsonSerializerDoesNotPersistDormantSettings() =
        runTest {
            val serializer =
                JsonDataStoreSerializer(
                    defaultValue = GameSettings(),
                    serializer = GameSettings.serializer(),
                )

            val output = ByteArrayOutputStream()
            serializer.writeTo(GameSettings(), output)
            val encoded = output.toString(Charsets.UTF_8.name())

            assertFalse(encoded.contains("reducedMotionEnabled"))
            assertFalse(encoded.contains("preferredRuleset"))
        }

    @Test
    fun jsonSerializerRoundTripsSavedGameDataWithNestedGameState() =
        runTest {
            val serializer =
                JsonDataStoreSerializer(
                    defaultValue = SavedGameData(),
                    serializer = SavedGameData.serializer(),
                )
            val gameState =
                GameEngine().newGame(
                    GameConfig(
                        mode = GameMode.FOUR_PLAYER,
                        randomSeed = 99L,
                        bonusTiles = listOf(BonusTile(row = 4, col = 4)),
                    ),
                )
            val savedGameData =
                SavedGameData(
                    gameState = gameState,
                    savedAtEpochMillis = 1234L,
                    settings =
                        GameSettings(
                            preferredDifficulty = 4,
                            soundEnabled = false,
                            preferredMode = GameMode.SOLO,
                        ),
                )

            val decoded = serializer.roundTrip(savedGameData)

            assertEquals(savedGameData, decoded)
        }

    @Test
    fun jsonSerializerRoundTripsProfilesDataWithHistory() =
        runTest {
            val serializer =
                JsonDataStoreSerializer(
                    defaultValue = ProfilesData(),
                    serializer = ProfilesData.serializer(),
                )
            val profilesData =
                ProfilesData(
                    profiles =
                        listOf(
                            Profile(
                                id = "player-1",
                                name = "Emma",
                                colorIndex = 2,
                                avatarStyle = LocalAvatarStyle.MOSAIC,
                                avatarSeed = "emma-seed",
                                active = true,
                                history =
                                    listOf(
                                        HistoryEntry(
                                            date = "2026-06-21",
                                            rank = 1,
                                            totalScore = 42,
                                            scoreBreakdown =
                                                ScoreBreakdown(
                                                    placedCellPoints = 37,
                                                    bonusTilePoints = 5,
                                                ),
                                            claimedBonusTiles = 1,
                                            piecesPlaced = 12,
                                            difficulty = 4,
                                            ruleset = Ruleset.STANDARD,
                                            gameMode = GameMode.FOUR_PLAYER,
                                            timeSeconds = 600,
                                            scores =
                                                listOf(
                                                    PlayerScore(
                                                        name = "Emma",
                                                        totalScore = 42,
                                                        scoreBreakdown =
                                                            ScoreBreakdown(
                                                                placedCellPoints = 37,
                                                                bonusTilePoints = 5,
                                                            ),
                                                        claimedBonusTiles = 1,
                                                        colorIndex = 2,
                                                        ownerIndex = 0,
                                                    ),
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                )

            val decoded = serializer.roundTrip(profilesData)

            assertEquals(profilesData, decoded)
        }

    private suspend fun <T> JsonDataStoreSerializer<T>.roundTrip(value: T): T {
        val output = ByteArrayOutputStream()
        writeTo(value, output)
        return readFrom(ByteArrayInputStream(output.toByteArray()))
    }
}
