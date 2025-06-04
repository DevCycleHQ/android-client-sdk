# DevCycle OpenFeature Android Example

This example demonstrates how to use DevCycle's OpenFeature provider with the OpenFeature Android SDK.

## Overview

This example shows how to:
- Initialize the DevCycle OpenFeature provider
- Evaluate different types of feature flags (string, boolean, integer, double)
- Set evaluation context for targeting
- Track events through OpenFeature
- Use the provider in an Android application

## Key Components

### DevCycleProvider
The `DevCycleProvider` class implements the OpenFeature `FeatureProvider` interface, allowing DevCycle to work seamlessly with OpenFeature.

### OpenFeature Integration
- **OpenFeatureApplication.kt**: Shows how to initialize OpenFeature with the DevCycle provider
- **OpenFeatureActivity.kt**: Demonstrates flag evaluation and event tracking using OpenFeature API directly

## Setup

1. Replace `<DEVCYCLE_MOBILE_SDK_KEY>` in `OpenFeatureApplication.kt` with your actual DevCycle SDK key
2. Replace `<YOUR_VARIABLE_KEY>` with an actual variable key from your DevCycle project
3. Optionally, update the flag keys in `OpenFeatureActivity.kt` to match flags in your DevCycle project

## Key Features Demonstrated

### Flag Evaluation
```kotlin
val client = OpenFeatureAPI.getClient()
val stringFlag = client.getStringValue("my-flag", "default")
val booleanFlag = client.getBooleanValue("boolean-flag", false)
val integerFlag = client.getIntegerValue("integer-flag", 42)
val doubleFlag = client.getDoubleValue("double-flag", 3.14)
```

### Setting Context
```kotlin
val evaluationContext = ImmutableContext(
    targetingKey = "user_123",
    attributes = mutableMapOf(
        "email" to Value.String("user@example.com"),
        "country" to Value.String("CA")
    )
)
OpenFeatureAPI.setEvaluationContext(evaluationContext)
```

### Event Tracking
```kotlin
client.track(
    "button_clicked",
    TrackingEventDetails(
        value = 1.0,
        structure = ImmutableStructure(mapOf(
            "button_id" to Value.String("header_cta")
        ))
    )
)
```

## Benefits of OpenFeature Integration

- **Standard API**: Use the same OpenFeature API across different providers
- **Provider Flexibility**: Easy to switch between feature flag providers
- **Ecosystem**: Access to OpenFeature hooks, middleware, and tooling
- **Type Safety**: Strong typing for flag values and contexts
- **Event Tracking**: Unified tracking API across providers
- **Built-in State Management**: OpenFeature API manages client lifecycle automatically

## Running the Example

1. Build and run the `openfeature-example` module
2. The app will initialize OpenFeature with the DevCycle provider
3. Flag values will be displayed on the screen
4. Events will be tracked automatically

## Dependencies

This example uses:
- DevCycle Android SDK (via project dependency)
- OpenFeature Android SDK (0.4.1)
- Standard Android libraries 