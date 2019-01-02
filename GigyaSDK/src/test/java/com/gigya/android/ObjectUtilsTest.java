package com.gigya.android;

import com.gigya.android.sdk.utils.ObjectUtils;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unchecked")
public class ObjectUtilsTest {

    @Test
    public void testDifference() {
        final Map<String, Object> original = new HashMap<String, Object>() {{
            put("one", 1);
            put("two", "Two");
            put("three", 3);
            put("four", 7);
        }};

        Map<String, Object> updated = new HashMap<String, Object>() {{
            put("one", 1);
            put("two", "Updated");
            put("three", 3);
            put("four", 8);
        }};

        Map<String, Object> difference = ObjectUtils.difference(original, updated);
        System.out.println(difference.toString());

        TestCase.assertEquals(2, difference.size());
        assertNotNull(difference.get("two"));
        assertEquals(difference.get("two"), "Updated");
        assertNotNull(difference.get("four"));
        assertEquals(difference.get("four"), 8);
    }

    @Test
    public void testDifferenceWithInnerMapping() {
        final Map<String, Object> original = new HashMap<String, Object>() {{
            put("one", 1);
            put("two", new HashMap<String, Object>() {{
                put("innerOne", 11);
                put("innerTwo", 12);
            }});
            put("three", 3);
        }};

        Map<String, Object> updated = new HashMap<String, Object>() {{
            put("one", 1);
            put("two", new HashMap<String, Object>() {{
                put("innerOne", 11);
                put("innerTwo", 13);
            }});
            put("three", 3);
        }};
        Map<String, Object> difference = ObjectUtils.difference(original, updated);
        System.out.println(difference.toString());

        TestCase.assertEquals(1, difference.size());
        assertNotNull(difference.get("two"));
        assertEquals(((Map<String, Object>) difference.get("two")).get("innerTwo"), 13);
    }
}
