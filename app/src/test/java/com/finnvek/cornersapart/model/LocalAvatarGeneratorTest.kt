package com.finnvek.cornersapart.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAvatarGeneratorTest {
    @Test
    fun generatedAvatarsAreLocalDeterministicAndSupportAllV1Styles() {
        val generator = LocalAvatarGenerator()

        LocalAvatarStyle.entries.forEach { style ->
            val profile =
                Profile(
                    id = style.name,
                    name = "Ada Lovelace",
                    colorIndex = 1,
                    avatarStyle = style,
                    avatarSeed = "seed-${style.name}",
                )

            val first = generator.generate(profile)
            val second = generator.generate(profile)

            assertEquals(first, second)
            assertEquals(style, first.style)
            assertTrue(first.localOnly)
            assertTrue(first.palette.isNotEmpty())
        }
    }

    @Test
    fun initialsAvatarUsesNameInitials() {
        val avatar =
            LocalAvatarGenerator().generate(
                Profile(
                    id = "ada",
                    name = "Ada Lovelace",
                    avatarStyle = LocalAvatarStyle.INITIALS,
                    avatarSeed = "ada",
                ),
            )

        assertEquals("AL", avatar.initials)
    }
}
