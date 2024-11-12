# DevCycle Android SDK

The DevCycle Android Client SDK. This SDK uses our Client SDK APIs to perform all user segmentation
and bucketing for the SDK, providing fast response times using our globally distributed edge workers
all around the world.

## Requirements

This version of the DevCycle Android Client SDK supports a minimum Android API Version 21.

## Installation

The SDK can be installed into your Android project by adding the following to _build.gradle_:

```yaml
implementation("com.devcycle:android-client-sdk:2.1.1")
```

## Usage

To find usage documentation, visit out [docs](https://docs.devcycle.com/docs/sdk/client-side-sdks/android#usage).

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
