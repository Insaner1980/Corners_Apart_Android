plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
}

ktlint {
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
