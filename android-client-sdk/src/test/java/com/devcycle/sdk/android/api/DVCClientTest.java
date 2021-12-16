package com.devcycle.sdk.android.api;

import static android.content.Context.MODE_PRIVATE;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.devcycle.sdk.android.helpers.WhiteBox;
import com.devcycle.sdk.android.model.DVCUser;
import com.devcycle.sdk.android.model.Variable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CountDownLatch;

@RunWith(MockitoJUnitRunner.class)
public class DVCClientTest {

    @Mock
    private Context mockContext;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private SharedPreferences.Editor editor;

    @Mock
    private DVCApi apiInterface;

    private final DVCApiMock dvcApiMock = new DVCApiMock();
    private final Request request = new Request();

    private DVCClient client;
    private Variable<String> not_activated;

    @Before
    public void setup() {
        when(mockContext.getString(anyInt())).thenReturn("Some value");
        when(mockContext.getSharedPreferences("Some value", MODE_PRIVATE)).thenReturn(sharedPreferences);

        when(sharedPreferences.edit()).thenReturn(editor);

        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        client = DVCClient.Companion.builder()
                .withContext(mockContext)
                .withUser(new DVCUser(false, "j_test", null, null, null, null, null, null, null, null))
                .withEnvironmentKey("add-client-sdk")
                .build();

        WhiteBox.setInternalState(request, "api", apiInterface);
        WhiteBox.setInternalState(client, "request", request);
    }

    @Test
    public void initialize_callback_contains_config() throws InterruptedException {
        when(apiInterface.getConfigJson(anyString(), anyMap())).thenReturn(dvcApiMock.getConfigJson(anyString(), anyMap()));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        client.initialize(new DVCCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Assert.assertNotNull(client.allVariables());
                Assert.assertTrue(client.allVariables().size() > 0);
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test
    public void getVariableWhenClientInitialized() throws InterruptedException {
        when(apiInterface.getConfigJson(anyString(), anyMap())).thenReturn(dvcApiMock.getConfigJson(anyString(), anyMap()));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        not_activated = client.variable("activate-flag", "Not activated");

        client.initialize(new DVCCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Assert.assertEquals("Flag activated!", not_activated.getValue());
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
//
//    @Test
//    public void doStuff() throws InterruptedException {
//        when(apiInterface.getConfigJson(anyString(), anyMap())).thenReturn(dvcApiMock.getConfigJson(anyString(), anyMap()));
//
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//
//        not_activated = client.variable("activate-flag", "Not activated");
//
//        client.initialize(new DVCCallback<String>() {
//            @Override
//            public void onSuccess(String result) {
//                try {
//                    Thread.sleep(1L);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                Assert.assertEquals("Flag activated!", not_activated.getValue());
//
//                when(apiInterface.getConfigJson(anyString(), anyMap())).thenReturn(dvcApiMock.getConfigJson(anyString(), anyMap()));
//                UserParam userParam = new UserParam(false, "new_id", null, null, null, null, null, null, null, null);
//                client.identifyUser(userParam, new DVCCallback<Map<String, Variable<Object>>>() {
//                    @Override
//                    public void onSuccess(Map<String, Variable<Object>> result) {
//                        Assert.assertTrue(result.containsKey("activate-flag"));
//                    }
//
//                    @Override
//                    public void onError(Throwable t) {
//                        Assert.fail(t.getMessage());
//                    }
//                });
//
//                when(apiInterface.getConfigJson(anyString(), anyMap())).thenReturn(dvcApiMock.getConfigJsonDifferentPayload());
//                client.identifyUser(userParam, new DVCCallback<Map<String, Variable<Object>>>() {
//                    @Override
//                    public void onSuccess(Map<String, Variable<Object>> result) {
//                        Assert.assertTrue(result.containsKey("haz-new-config"));
//                    }
//
//                    @Override
//                    public void onError(Throwable t) {
//                        Assert.fail(t.getMessage());
//                    }
//                });
//
//                countDownLatch.countDown();
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                Assert.fail(t.getMessage());
//                countDownLatch.countDown();
//            }
//        });
//
//        countDownLatch.await();
//    }
}