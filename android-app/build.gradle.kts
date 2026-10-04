// Top-level build file — declares plugins available to all modules.
// Module-specific configuration goes in app/build.gradle.kts.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
