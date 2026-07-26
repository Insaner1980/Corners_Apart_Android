package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThirdPartyLicensePolicyTest {
    @Test
    fun bundledNunitoFontsHavePackagedCopyrightAndLicense() {
        val root = projectRoot()
        val fontFiles =
            root
                .resolve("app/src/main/res/font")
                .toFile()
                .listFiles { file -> file.name.startsWith("nunito_") && file.extension == "ttf" }
                .orEmpty()
                .map { file -> file.name }
                .sorted()

        assertEquals(
            listOf(
                "nunito_black.ttf",
                "nunito_bold.ttf",
                "nunito_extrabold.ttf",
                "nunito_semibold.ttf",
            ),
            fontFiles,
        )

        val license = root.resolve(NUNITO_LICENSE_PATH).toFile()

        assertTrue("Nunito license must be packaged with the application.", license.isFile)
        assertTrue(license.readText().contains(NUNITO_COPYRIGHT))
        assertTrue(license.readText().contains("SIL OPEN FONT LICENSE Version 1.1"))
    }

    private companion object {
        const val NUNITO_LICENSE_PATH = "app/src/main/resources/META-INF/LICENSE-NUNITO.txt"
        const val NUNITO_COPYRIGHT =
            "Copyright 2014 The Nunito Project Authors (https://github.com/googlefonts/nunito)"
    }
}
