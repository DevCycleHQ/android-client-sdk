# DevCycle Android SDK

The DevCycle Android Client SDK. This SDK uses our Client SDK APIs to perform all user segmentation 
and bucketing for the SDK, providing fast response times using our globally distributed edge workers 
all around the world.

## Requirements

This version of the DevCycle Android Client SDK supports a minimum Android API Version 21.

## Installation

The SDK can be installed into your Android project by adding the following to *build.gradle*:

```yaml
implementation("com.devcycle:android-client-sdk:1.0.4")
```

Versions earlier than 1.0.4 are deprecated and contain either third party CVEs or issues that would prevent the DevCycle client from initializing correctly if the retrieved Config has had new properties added.

## Usage

### Initializing the SDK

We recommend initializing the SDK once in `onCreate` of your `Application` class or `MainActivity` to receive features for as soon as possible, and to pass around the client instance around in your app.

Using the builder pattern we can initialize the DevCycle SDK by providing the `applicationContext`, 
DVCUser, and DevCycle mobile environment key:

#### *Kotlin example:*

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {

    ...

    val dvcClient: DVCClient = DVCClient.builder()
        .withContext(applicationContext)
        .withUser(
            DVCUser.builder()
                .withUserId("test_user")
                .build()
        )
        .withEnvironmentKey("<DEVCYCLE_MOBILE_ENVIRONMENT_KEY>")
        .build()
    })
    
    ...

}
```

#### *Java example:*

```java
@Override
protected void onCreate(Bundle savedInstanceState) {

    ...

    DVCClient dvcClient = DVCClient.builder()
        .withContext(getApplicationContext())
        .withUser(
            DVCUser.builder()
                .withUserId("test_user")
                .build()
            )
        .withEnvironmentKey("<DEVCYCLE_MOBILE_ENVIRONMENT_KEY>")
        .build();
    
    ...
}
```

### Notifying when DevCycle features are available

You can attach a callback on the client to determine when your features have been loaded:

#### Java

```
client.onInitialized(new DVCCallback<String>() {
    @Override
    public void onSuccess(String result) {
        // user configuration loaded successfully from DevCycle
    }

    @Override
    public void onError(@NonNull Throwable t) {
        // user configuration failed to load from DevCycle, default values will be used for Variables.
    }
});
```

#### Kotlin

```
client.onInitialized(object : DVCCallback<String> {
    override fun onSuccess(result: String) {
        // successfully initialized
    }

    override fun onError(t: Throwable) {
        // there was an error 
    }
})
```

## Using Variable Values

To get values from your Features, the `variable()` method is used to fetch variable values using 
the variable's identifier `key` coupled with a default value. The default value can be of type 
string, boolean, number, or JSONObject:

```kotlin
var strVariable: Variable<String> = dvcClient.variable("str_key", "default")
var boolVariable: Variable<Boolean> = dvcClient.variable("bool_key", false)
var numVariable: Variable<Number> = dvcClient.variable("num_key", 0)
var jsonVariable: Variable<JSONObject> = dvcClient.variable("json_key", JSONObject("{ \"key\": \"value\" }"))
```

To grab the value, there is a property on the object returned to grab the value:

#### *Kotlin example:*

```kotlin
if (boolVariable.value == true) {
    // run feature flag code
} else {
    // run default code
}
```

#### *Java example:*

```java
if (boolVariable.getValue() == true) {
    // run feature flag code
} else {
    // run default code
}
```

The `Variable` object also contains the following params: 
    - `key`: the key identifier for the Variable
    - `type`: the type of the Variable, one of: `String` / `Boolean` / `Number` / `JSON`
    - `value`: the Variable's value
    - `defaultValue`: the Variable's default value
    - `isDefaulted`: if the Variable is using the `defaultValue`
    - `evalReason`: evaluation reason for why the variable was bucketed into its value

If the value is not ready, it will return the default value passed in the creation of the variable.

## Variable updates

A callback can be registered to be notified when the value changes on a variable.

#### *Kotlin example:*

```kotlin
variable.onUpdate(object: DVCCallback<Variable<String>> {
    override fun onSuccess(result: Variable<String>) {
        // use the new value
    }

    override fun onError(t: Throwable) {
        // optionally handle the error, the previous value will still be used
    }
})
```

#### *Java example:*

```java
variable.onUpdate(new DVCCallback<Variable<String>>() {
    @Override
    public void onSuccess(Variable<String> result) {
        // use the new value
    }

    @Override
    public void onError(@NonNull Throwable t) {
        // optionally handle the error, the previous value will still be used
    }
});
```

## Grabbing All Features / Variables

To grab all the Features or Variables returned in the config:

#### *Kotlin example:*

```kotlin
var features: Map<String, Feature>? = dvcClient.allFeatures()

var variables: Map<String, Variable<Any>>? = dvcClient.allVariables()
```

#### *Java example:*

```java
Map<String, Feature> features = dvcClient.allFeatures();

Map<String, Variable<Object>> variables = dvcClient.allVariables();
```

If the SDK has not finished initializing, these methods will return an empty Map.

## Identifying User

To identify a different user, or the same user passed into the initialize method with more attributes, 
build a DVCUser object and pass it into `identifyUser`:

#### *Kotlin example:*

```kotlin
var user = DVCUser.builder()
                .withUserId("test_user")
                .withEmail("test_user@devcycle.com")
                .withCustomData(mapOf("custom_key" to "value"))
                .build()
dvcClient.identifyUser(user)
```

#### *Java example:*

```kotlin
DVCUser user = DVCUser.builder()
                    .withUserId("test_user")
                    .withEmail("test_user@devcycle.com")
                    .withCustomData(Collections.singletonMap("custom_key", "value"))
                    .build();
client.identifyUser(user);
```

To wait on Variables that will be returned from the identify call, you can pass in a DVCCallback:

#### *Kotlin example:*

```kotlin
dvcClient.identifyUser(user, object: DVCCallback<Map<String, Variable<Any>>> {
    override fun onSuccess(result: Map<String, Variable<Any>>) {
        // new user configuration loaded successfully from DevCycle
    }

    override fun onError(t: Throwable) {
        // user configuration failed to load from DevCycle, existing user's data will persist.
    }
})
```

#### *Java example:*

```java
client.identifyUser(user, new DVCCallback<Map<String, Variable<Object>>>() {
    @Override
    public void onSuccess(Map<String, Variable<Object>> result) {
        // new user configuration loaded successfully from DevCycle
    }

    @Override
    public void onError(@NonNull Throwable t) {
        // user configuration failed to load from DevCycle, existing user's data will persist.
    }
```

If `onError` is called the user's configuration will not be updated and previous user's data will persist.

## Reset User

To reset the user into an anonymous user, `resetUser` will reset to the anonymous user created before 
or will create one with an anonymous `user_id`.

```kotlin
dvcClient.resetUser()
```

#### *Kotlin example:*

To wait on the Features of the anonymous user, you can pass in a DVCCallback:

```kotlin
dvcClient.resetUser(object : DVCCallback<Map<String, Variable<Any>>> {
    override fun onSuccess(result: Map<String, Variable<Any>>) {
        // anonymous user configuration loaded successfully from DevCycle
    }

    override fun onError(t: Throwable) {
        // user configuration failed to load from DevCycle, existing user's data will persist.
    }
})
```

#### *Java example:*

```java
client.resetUser(new DVCCallback<Map<String, Variable<Object>>>() {
    @Override
    public void onSuccess(Map<String, Variable<Object>> result) {
        // anonymous user configuration loaded successfully from DevCycle
    }

    @Override
    public void onError(@NonNull Throwable t) {
        // user configuration failed to load from DevCycle, existing user's data will persist.
    }
});
```


If `onError` is called the user's configuration will not be updated and previous user's data will persist.

## Tracking Events

To track events, pass in an object with at least a `type` key:

#### *Kotlin example:*

```kotlin
var event = DVCEvent.builder()
                .withType("custom_event_type")
                .withTarget("custom_event_target")
                .withValue(BigDecimal(10.0))
                .withMetaData(mapOf("custom_key" to "value"))
                .build()
dvcClient.track(event)
```

#### *Java example:*

```java
DVCEvent event = DVCEvent.builder()
        .withType("custom_event_type")
        .withTarget("custom_event_target")
        .withValue(BigDecimal.valueOf(10.00))
        .withMetaData(Collections.singletonMap("test", "value"))
        .build();
client.track(event);
```

The SDK will flush events every 10s or `flushEventsMS` specified in the options. To manually flush events, call:

```kotlin
dvcClient.flushEvents()
```

A callback can be passed to this method to be notified when the method has completed:

#### *Kotlin example:*

```kotlin
client.flushEvents(object: DVCCallback<String> {
    override fun onSuccess(result: String) {
        // The queue was successfully flushed
    }

    override fun onError(t: Throwable) {
        // The queue could not be flushed and a non-recoverable error was thrown
    }
})
```

#### *Java example:*

```java
client.flushEvents(new DVCCallback<String>() {
    @Override
    public void onSuccess(String result) {
        // The queue was successfully flushed
    }

    @Override
    public void onError(@NonNull Throwable t) {
        // The queue could not be flushed and a non-recoverable error was thrown
    }
});
```
