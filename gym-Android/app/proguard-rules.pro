# ===============================================
# GYM APP - PROGUARD RULES
# Optimized for security and code protection
# ===============================================

# -----------------------------------------------
# BASIC SETTINGS
# -----------------------------------------------

# Preserve line number information for debugging
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# Keep generic signatures (required for Retrofit, Gson)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes Exceptions

# -----------------------------------------------
# AGGRESSIVE OBFUSCATION
# -----------------------------------------------

# Use mixed case class names for better obfuscation
-repackageclasses ''

# Allow access modification for better optimization
-allowaccessmodification

# More aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# -----------------------------------------------
# KOTLIN SPECIFIC
# -----------------------------------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep data classes serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# -----------------------------------------------
# RETROFIT & NETWORKING
# -----------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep Retrofit service interfaces
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# -----------------------------------------------
# GSON
# -----------------------------------------------
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep GSON serialized fields
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# -----------------------------------------------
# DATA MODELS (Keep for serialization)
# -----------------------------------------------
-keep class com.lc9th5.gym.data.model.** { *; }
-keep class com.lc9th5.gym.data.remote.dto.** { *; }

# -----------------------------------------------
# COMPOSE
# -----------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# -----------------------------------------------
# SECURITY - Keep security classes unobfuscated for proper function
# -----------------------------------------------
-keep class com.lc9th5.gym.utils.SecurityUtils { *; }
-keep class com.lc9th5.gym.utils.SecurePreferences { *; }

# -----------------------------------------------
# KTOR
# -----------------------------------------------
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# -----------------------------------------------
# SUPABASE
# -----------------------------------------------
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }

# -----------------------------------------------
# MEDIA3 / EXOPLAYER
# -----------------------------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# -----------------------------------------------
# SLF4J LOGGING
# -----------------------------------------------
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder
-keep class org.slf4j.** { *; }

# -----------------------------------------------
# REMOVE LOGGING IN RELEASE
# -----------------------------------------------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# -----------------------------------------------
# STRING OBFUSCATION HELPER
# Strings containing sensitive data should use encrypted storage
# -----------------------------------------------
# Note: For advanced string encryption, consider DexGuard or similar tools

# -----------------------------------------------
# ANTI-DEBUGGING (Release only)
# -----------------------------------------------
# Remove debug information
-assumenosideeffects class java.lang.System {
    public static void out;
    public static void err;
}
