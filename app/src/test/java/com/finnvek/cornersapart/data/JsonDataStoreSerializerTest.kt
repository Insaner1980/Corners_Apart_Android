package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.Ruleset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
                    reducedMotionEnabled = true,
                    preferredMode = GameMode.COMPACT_DUEL,
                    preferredRuleset = Ruleset.STANDARD,
                )
            val output = ByteArrayOutputStream()

            serializer.writeTo(settings, output)
            val decoded = serializer.readFrom(ByteArrayInputStream(output.toByteArray()))

            assertEquals(settings, decoded)
            assertEquals(GameSettings(), serializer.defaultValue)
        }
}
