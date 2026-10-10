// Top-level build file. Declared once so the app module can alias them without versions.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}
