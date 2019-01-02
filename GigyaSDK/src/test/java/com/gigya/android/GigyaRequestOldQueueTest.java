package com.gigya.android;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gigya.android.sdk.network.GigyaRequestOld;
import com.gigya.android.sdk.network.GigyaRequestQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.Queue;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({Volley.class, RequestQueue.class, StringRequest.class, Request.class, Log.class})
public class GigyaRequestOldQueueTest {

    @Mock
    private RequestQueue requestQueue;

    @Mock
    private Context context;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Volley.class, StringRequest.class, Request.class, Log.class);
        PowerMockito.when(Log.i(anyString(), anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.out.println(invocation.getArguments()[0]);
                return 1;
            }
        });
        when(Volley.newRequestQueue(context)).thenReturn(requestQueue);
        doNothing().when(requestQueue).cancelAll(any());
    }

    @Test
    public void testBlock() throws Exception {
        GigyaRequestQueue queue = new GigyaRequestQueue(context);
        Field blocked = GigyaRequestQueue.class.getDeclaredField("blocked");
        blocked.setAccessible(true);
        queue.block();

        final boolean isBlocked = (boolean) blocked.get(queue);
        assertTrue(isBlocked);
    }

    @Test
    public void testRelease() throws NoSuchFieldException, IllegalAccessException {
        GigyaRequestQueue queue = new GigyaRequestQueue(context);
        Field blocked = GigyaRequestQueue.class.getDeclaredField("blocked");
        blocked.setAccessible(true);
        queue.block();
        queue.release();

        final boolean isBlocked = (boolean) blocked.get(queue);
        assertFalse(isBlocked);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelAll() throws Exception {
        GigyaRequestQueue queue = new GigyaRequestQueue(context);
        Field blockingQueue = GigyaRequestQueue.class.getDeclaredField("_blockingQueue");
        blockingQueue.setAccessible(true);

        queue.cancelAll();

        final Queue<GigyaRequestOld> privateBlockingQueue = (Queue<GigyaRequestOld>) blockingQueue.get(queue);
        assertTrue(privateBlockingQueue.isEmpty());
    }

    @Test
    public void testAdd() {
        GigyaRequestQueue queue = new GigyaRequestQueue(context);
        GigyaRequestOld request = new GigyaRequestOld("someUrl", null, null, null, "");
        when(requestQueue.add(any(Request.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Request request = (Request) invocation.getArguments()[0];
                assertEquals(request.getUrl(), "someUrl");
                return request;
            }
        });
        queue.add(request);
    }
}
