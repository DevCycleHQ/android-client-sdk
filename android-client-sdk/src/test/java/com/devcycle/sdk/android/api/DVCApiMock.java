package com.devcycle.sdk.android.api;

import com.devcycle.sdk.android.helpers.TestResponse;
import com.devcycle.sdk.android.model.BucketedUserConfig;

import java.util.Map;

import retrofit2.Call;

public class DVCApiMock implements DVCApi {

    @Override
    public Call<BucketedUserConfig> getConfigJson(String envKey, Map<String, String> params) {
        return TestResponse.getConfigJson();
    }

    public Call<BucketedUserConfig> getConfigJsonDifferentPayload() {
        return TestResponse.getConfigJsonDifferentPayload();
    }
}
