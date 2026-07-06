package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.extractKotlinBlock
import com.finnvek.cornersapart.projectFiles
import com.finnvek.cornersapart.withoutLineComments
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidLintPolicyTest {
    @Test
    fun releaseLintFailsWhenTargetSdkFallsBehindCurrentStablePlatform() {
        val activeBuildFile = projectFiles().appBuildFile.withoutLineComments()
        val lintBlock = activeBuildFile.extractKotlinBlock("lint")

        assertTrue(activeBuildFile.contains("compileSdk = 37"))
        assertTrue(activeBuildFile.contains("targetSdk = 37"))
        assertTrue(lintBlock.contains("checkReleaseBuilds = true"))
        assertTrue(
            "OldTargetApi must stay fatal so release lint fails when targetSdk lags behind Android 17.",
            Regex("""fatal\s*\+=\s*setOf\([\s\S]*"OldTargetApi"""")
                .containsMatchIn(lintBlock),
        )
    }
}
