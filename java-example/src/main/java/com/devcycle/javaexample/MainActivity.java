package com.devcycle.javaexample;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.devcycle.sdk.android.api.DVCCallback;
import com.devcycle.sdk.android.api.DVCClient;
import com.devcycle.sdk.android.model.DVCEvent;
import com.devcycle.sdk.android.model.DVCUser;
import com.devcycle.sdk.android.model.Variable;
import com.devcycle.sdk.android.util.LogLevel;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Variable<String> variable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DVCClient client = DVCClient.builder()
                .withContext(getApplicationContext())
                .withUser(
                        DVCUser.builder()
                                .withUserId("test_user")
                                .build()
                )
                .withEnvironmentKey("<ADD-MOBILE-KEY-HERE>")
                .withLogLevel(LogLevel.DEBUG)
                .build();

        // Use your own demo variable here to see the value change from the defaultValue when the client is initialized
        variable = client.variable("<YOUR_VARIABLE_KEY>", "my string variable is not initialized yet");
        Toast.makeText(MainActivity.this, Objects.requireNonNull(variable.getValue()), Toast.LENGTH_SHORT).show();

        client.onInitialized(new DVCCallback<String>() {
            @Override
            public void onSuccess(String result) {
                //Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();

                client.flushEvents(new DVCCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull Throwable t) {
                        Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                DVCEvent event = DVCEvent.builder()
                        .withType("custom_event_type")
                        .withTarget("custom_event_target")
                        .withValue(BigDecimal.valueOf(10.00))
                        .withMetaData(Collections.singletonMap("test", "value"))
                        .build();
                client.track(event);

                // This toast onInitialized will show the value has changed
                Toast.makeText(MainActivity.this, variable.getValue(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Client did not initialize: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
