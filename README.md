# DevCycle Android SDK

The DeCycle Android Client SDK.


## Requirements

This version of the DevCycle Android Client SDK supports a minimum Android Version 21.

## Installation

The SDK can be installed into your Android project by adding the following to *build.gradle*:

```yaml
implementation("com.devcycle:android-client-sdk:1.0.0")
```

## Usage

### Initializing the SDK

Using the builder patter we can initialize the DevCycle SDK by providing the `applicationContext`, 
DVCUser, and DevCycle mobile environment key:

```kotlin
val client: DVCClient = DVCClient.builder()
    .withContext(applicationContext)
    .withUser(
        DVCUser.builder()
            .withUserId("test_user")
            .withIsAnonymous(false)
            .build()
    )
    .withEnvironmentKey("<DEVCYCLE_MOBILE_ENVIRONMENT_KEY>")
    .build()

client.initialize(object : DVCCallback<String?> {
    override fun onSuccess(result: String?) {
        // User Configuration loaded successfully from DevCycle
    }

    override fun onError(t: Throwable) {
        // User Configuration failed to load from DevCycle, default values will be used for Variables.
    }
})
```

The user object needs either a `user_id`, or `isAnonymous` set to `true` for an anonymous user.

## Using Variable Values

To get values from your Features, the `variable()` method is used to fetch variable values using 
the variable's identifier `key` coupled with a default value. The default value can be of type 
string, boolean, number, or JSONObject:

```kotlin
var strVariable: Variable<String> = client.variable("str_key", "default")
var boolVariable: Variable<Boolean> = client.variable("bool_key", false)
var numVariable: Variable<Number> = client.variable("bool_key", 0)
var jsonVariable: Variable<JSONObject> = client.variable("json_key", JSONObject("{ \"key\": \"value\" }"))
```

To grab the value, there is a property on the object returned to grab the value:

```kotlin
if (boolVariable.value == true) {
    // Run Feature Flag Code
} else {
    // Run Default Code
}
```

The `Variable` object also contains the following params: 
    - `key`: the key indentifier for the Variable
    - `type`: the type of the Variable, one of: `String` / `Boolean` / `Number` / `JSON`
    - `value`: the Variable's value
    - `defaultValue`: the Variable's default value
    - `isDefaulted`: if the Variable is using the `defaultValue`
    - `evalReason`: evaluation reason for why the variable was bucketed into its value

If the value is not ready, it will return the default value passed in the creation of the variable. 
To get notified when the variable is loaded:

// TODO: do we have something like this?
```
// TODO
```

## Grabbing All Features / Variables

To grab all the Features or Variables returned in the config:

```kotlin
var features: Map<String, Feature>? = client.allFeatures()
var variables: Map<String, Variable<Any>>? = client.allVariables()
```

If the SDK has not finished initializing, these methods will return an empty object.

## Identifying User

To identify a different user, or the same user passed into the initialize with more attributes, 
pass in the entire user attribute object into `identifyUser`:

```
// TODO
```

To wait on Variables that will be returned from the identify call, you can pass in a callback 
or use the Promise returned if no callback is passed in:

```
// TODO
```

## Reset User

To reset the user into an anonymous user, `resetUser` will reset to the anonymous user created before or will create one with an anonymous `user_id`.

```
// TODO
```

To wait on the Features of the anonymous user, you can pass in a callback or use the Promise returned if no callback is passed in:

```
// TODO
```


## Tracking Events

To track events, pass in an object with at least a `type` key:

```
// TODO
```

The SDK will flush events every 10s or `flushEventsMS` specified in the options. To manually flush events, call:

```
// TODO
```