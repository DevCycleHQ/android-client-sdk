# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# TODO: Update all retrofit rules when retrofit 5 is released.

# Keep essential DevCycle SDK classes and their public APIs
-keep class kotlin.Metadata { *; }
-keep class com.devcycle.sdk.android.model.** { *; }
-keep class com.devcycle.sdk.android.api.DevCycleClient { *; }
-keep class com.devcycle.sdk.android.api.DevCycleClient$* { *; }
-keep class com.devcycle.sdk.android.api.DevCycleOptions { *; }
-keep class com.devcycle.sdk.android.api.** { *; }

# Keep utility classes needed for SDK functionality and testing
-keep class com.devcycle.sdk.android.util.** { *; }

# Keep exception classes
-keep class com.devcycle.sdk.android.exception.** { *; }

# Keep listener interfaces
-keep class com.devcycle.sdk.android.listener.** { *; }

# Keep interceptor classes
-keep class com.devcycle.sdk.android.interceptor.** { *; }

# Jackson and JSON processing
-keep class java.beans.Transient.** {*;}
-keep class java.beans.ConstructorProperties.** {*;}
-keep class java.nio.file.Path.** {*;}
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.core.type.TypeReference
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp and networking
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# Retrofit rules
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class okhttp3.RequestBody
-keep,allowobfuscation,allowshrinking class okhttp3.ResponseBody

# Keep coroutines
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# SLF4J Logger rules
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }
-keep interface org.slf4j.** { *; }

# com.launchdarkly:okhttp-eventsource rules
-keep class com.launchdarkly.eventsource.** { *; }
-dontwarn com.launchdarkly.eventsource.**

# OpenFeature integration classes
-keep class com.devcycle.sdk.android.openfeature.** { *; }
-keep class dev.openfeature.sdk.** { *; }
-dontwarn dev.openfeature.sdk.**
