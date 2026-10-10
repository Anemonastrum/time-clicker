// The compose compiler plugin already applies org.jetbrains.kotlin.android.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeCompiler) apply false
}
