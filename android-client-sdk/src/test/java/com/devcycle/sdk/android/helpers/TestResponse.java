package com.devcycle.sdk.android.helpers;

import com.devcycle.sdk.android.model.BucketedUserConfig;
import com.devcycle.sdk.android.model.Variable;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.mock.Calls;

public final class TestResponse {

    private TestResponse() {
    }

    public static Call<BucketedUserConfig> getConfigJson() {
        BucketedUserConfig config = new BucketedUserConfig();
        Map<String, Variable<Object>> variables = new HashMap<>();
        variables.put("activate-flag", createNewVariable("activate-flag", "Flag activated!", Variable.TypeEnum.STRING));

        config.setVariables(variables);

        return Calls.response(config);
    }

    public static Call<BucketedUserConfig> getConfigJsonDifferentPayload() {
        BucketedUserConfig config = new BucketedUserConfig();
        Map<String, Variable<Object>> variables = new HashMap<>();
        variables.put("haz-new-config", createNewVariable("haz-new-config", "So new", Variable.TypeEnum.STRING));

        config.setVariables(variables);

        return Calls.response(config);
    }

    private static <T> Variable createNewVariable(String key, T value, Variable.TypeEnum type) {
        try {
            Constructor constructor = Variable.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            Variable<T> variable = (Variable<T>) constructor.newInstance();
            variable.setId(UUID.randomUUID().toString());
            variable.setType(type);
            variable.setValue(value);
            //variable.setIsDefaulted(false);
            variable.setKey(key);

            constructor.setAccessible(false);
            return variable;
        } catch (Exception e) {
            // meh
        }
        return null;
    }
}
