# Keep MIUI X (miuix) library — it uses reflection-free Compose, but the icon ImageVector
# properties are resolved by name in some tooling chains; keep them anyway.
-keep class top.yukonga.miuix.kmp.** { *; }

# Compose keep rules (bundled via consumer rules in newer Compose; kept for safety).
-dontwarn androidx.compose.**

# Keep runtime metadata used by the compose compiler.
-keep class kotlin.metadata.** { *; }
