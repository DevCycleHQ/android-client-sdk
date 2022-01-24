package com.devcycle.javaexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.devcycle.sdk.android.api.DVCCallback;
import com.devcycle.sdk.android.api.DVCClient;
import com.devcycle.sdk.android.model.DVCEvent;
import com.devcycle.sdk.android.model.DVCUser;
import com.devcycle.sdk.android.model.Variable;
import com.devcycle.sdk.android.util.LogLevel;

import java.util.HashMap;
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
                                .withUserId("nic_test")
                                .build()
                )
                .withEnvironmentKey("add-mobile-key")
                .withLogLevel(LogLevel.DEBUG)
                .build();

        variable = client.variable("activate-flag", "not activated");
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

                HashMap<String, Object> metadata = new HashMap<>();
                metadata.put("test", "value");
                client.track(DVCEvent.builder()
                        .withType("testEvent")
                        .withMetaData(metadata)
                        .build());

                Toast.makeText(MainActivity.this, variable.getValue(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Client did not initialize: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
