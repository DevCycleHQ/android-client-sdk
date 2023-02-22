# DevCycle Android SDK

The DevCycle Android Client SDK. This SDK uses our Client SDK APIs to perform all user segmentation 
and bucketing for the SDK, providing fast response times using our globally distributed edge workers 
all around the world.

## Requirements

This version of the DevCycle Android Client SDK supports a minimum Android API Version 21.

## Installation

The SDK can be installed into your Android project by adding the following to *build.gradle*:

```yaml
implementation("com.devcycle:android-client-sdk:1.4.0")
```

Versions earlier than 1.0.6 are deprecated and contain either third party CVEs or issues that would prevent the DevCycle client from initializing correctly if the retrieved Config has had new properties added.

## Usage

To find usage documentation, visit out [docs](https://docs.devcycle.com/docs/sdk/client-side-sdks/android#usage).