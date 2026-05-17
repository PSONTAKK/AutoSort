# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# AutoSort ProGuard / R8 Rules
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

# ── Fix 9: ProGuard Rules ─────────────────────────────────────────
# ── Fix 10: Strip verbose logging from release builds ─────────────

# ── Google API Client (uses reflection for JSON parsing) ──────────
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }

# ── Gson (used by Google HTTP Client) ─────────────────────────────
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── Google Play Services Auth ─────────────────────────────────────
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# ── Room Database ─────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── SQLCipher (JNI native code requires all fields/methods) ───────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ── Kotlin Coroutines ─────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Compose ───────────────────────────────────────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.navigation.** { *; }

# ── Strip debug/verbose logging from release (Fix 10) ─────────────
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

# ── Ignore warnings for optional Java/Apache classes used by Google API Client ──
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**

# ── Keep BuildConfig (needed for WEB_CLIENT_ID) ───────────────────
-keep class com.autosort.BuildConfig { *; }

# ── General hardening ─────────────────────────────────────────────
-repackageclasses 'a'
-allowaccessmodification
-optimizationpasses 2
