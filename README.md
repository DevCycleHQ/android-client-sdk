# DevCycle Android SDK

The DevCycle Android Client SDK. This SDK uses our Client SDK APIs to perform all user segmentation
and bucketing for the SDK, providing fast response times using our globally distributed edge workers
all around the world.

## Requirements

This version of the DevCycle Android Client SDK supports a minimum Android API Version 23.

## Installation

The SDK can be installed into your Android project by adding the following to _build.gradle_:

```yaml
implementation("com.devcycle:android-client-sdk:2.6.0")
```

## Usage

To find usage documentation, visit out [docs](https://docs.devcycle.com/docs/sdk/client-side-sdks/android#usage).

## OpenFeature Provider

The DevCycle Android SDK includes support for [OpenFeature](https://openfeature.dev/), a vendor-agnostic feature flag API. This allows you to use DevCycle with the OpenFeature SDK for standardized feature flag evaluation.

### Basic Usage

```kotlin
import com.devcycle.sdk.android.openfeature.DevCycleProvider
import dev.openfeature.sdk.OpenFeatureAPI

// Initialize the DevCycle provider
val provider = DevCycleProvider(
    sdkKey = "<DEVCYCLE_MOBILE_SDK_KEY>",
    context = applicationContext
)

// Set the provider with OpenFeature
OpenFeatureAPI.setProviderAndWait(provider)

// Use OpenFeature client for flag evaluation
val client = OpenFeatureAPI.getClient()
val flagValue = client.getBooleanValue("my-feature-flag", false)
```

The provider automatically handles context mapping between OpenFeature's `EvaluationContext` and DevCycle's user model, supporting standard attributes like `email`, `name`, `country`, and custom data.

## Running the included Example Apps

To run the examples you will need to include your Mobile SDK Key and a Variable Key. The Variable
used should be a 'string' type.

### Java Example

The Java Example apps consist of a simple blank screen that automatically triggers a variable evaluation
on run and then displays a toast notification.

Code locations to update:

- [Mobile SDK Key](https://github.com/DevCycleHQ/android-client-sdk/blob/main/java-example/src/main/java/com/devcycle/javaexample/JavaApplication.java#L33)
- [Variable Key](https://github.com/DevCycleHQ/android-client-sdk/blob/main/java-example/src/main/java/com/devcycle/javaexample/JavaApplication.java#L38)

### Kotlin Example

The Kotlin Example provides a simplified interface to evaluate flags manually, but will also
automatically triggers a variable evaluation on run and then displays a toast notification.

Code locations to update:

- [Mobile SDK Key](https://github.com/DevCycleHQ/android-client-sdk/blob/main/kotlin-example/src/main/java/com/devcycle/example/KotlinApplication.kt#L27)
- [Variable Key](https://github.com/DevCycleHQ/android-client-sdk/blob/main/kotlin-example/src/main/java/com/devcycle/example/KotlinApplication.kt#L33)
