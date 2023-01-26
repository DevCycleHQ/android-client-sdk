# Add build specific ProGuard rules here.
# These rules will be pulled when a consumer installs the DevCycle Android SDK.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keepclassmembers enum com.devcycle.sdk.android.model.** { *; }