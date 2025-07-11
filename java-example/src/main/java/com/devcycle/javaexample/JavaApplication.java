package com.devcycle.javaexample;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.devcycle.sdk.android.api.DevCycleCallback;
import com.devcycle.sdk.android.api.DevCycleClient;
import com.devcycle.sdk.android.model.DevCycleEvent;
import com.devcycle.sdk.android.model.DevCycleUser;
import com.devcycle.sdk.android.model.Variable;
import com.devcycle.sdk.android.util.LogLevel;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Objects;

public class JavaApplication extends Application {
    Variable<String> variable = null;
    String variableValue = null;

    @Override
    public void onCreate() {
        super.onCreate();

        DevCycleUser user = DevCycleUser.builder()
                .withUserId("test_user")
                .build();

        DevCycleClient client = DevCycleClient.builder()
                .withContext(getApplicationContext())
                .withUser(user)
                .withSDKKey("<DEVCYCLE_MOBILE_SDK_KEY>")
                .logLevel(LogLevel.DEBUG)
                .build();

        // Use your own demo variable here to see the value change from the defaultValue
        // when the client is initialized
        variable = client.variable("<YOUR_VARIABLE_KEY>", "my string variable is not initialized yet");
        variableValue = client.variableValue("<YOUR_VARIABLE_KEY>", "my string variable is not initialized yet");
        Toast.makeText(getApplicationContext(), Objects.requireNonNull(variableValue), Toast.LENGTH_SHORT).show();

        client.onInitialized(new DevCycleCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();

                client.flushEvents(new DevCycleCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull Throwable t) {
                        Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                DevCycleEvent event = DevCycleEvent.builder()
                        .withType("custom_event_type")
                        .withTarget("custom_event_target")
                        .withValue(BigDecimal.valueOf(10.00))
                        .withMetaData(Collections.singletonMap("test", "value"))
                        .build();
                client.track(event);

                // This toast onInitialized will show the value has changed
                Toast.makeText(getApplicationContext(), variable.getValue(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Throwable t) {
                Toast.makeText(getApplicationContext(), "Client did not initialize: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
