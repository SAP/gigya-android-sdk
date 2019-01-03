package com.gigya.android;

import com.gigya.android.sdk.utils.ObjectUtils;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unchecked")
public class ObjectUtilsTest {

    @Test
    public void testDifference() {
        // Arrange
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
        // Act
        Map<String, Object> difference = ObjectUtils.difference(original, updated);
        // Assert
        TestCase.assertEquals(2, difference.size());
        assertNotNull(difference.get("two"));
        assertEquals(difference.get("two"), "Updated");
        assertNotNull(difference.get("four"));
        assertEquals(difference.get("four"), 8);
    }

    @Test
    public void testDifferenceWithInnerMapping() {
        // Arrange
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
        // ct
        Map<String, Object> difference = ObjectUtils.difference(original, updated);
        // Assert
        TestCase.assertEquals(1, difference.size());
        assertNotNull(difference.get("two"));
        assertEquals(((Map<String, Object>) difference.get("two")).get("innerTwo"), 13);
    }

    @Test
    public void testDifferenceWithNulls() {
        // Arrange
        final Map<String, Object> original = new HashMap<String, Object>() {{
            put("one", 1);
            put("two", "Two");
            put("three", 3);
            put("four", 7);
        }};
        Map<String, Object> updated = new HashMap<String, Object>() {{
            put("one", 1);
            put("two", null);
            put("three", 3);
            put("four", 8);
        }};
        // Act
        Map<String, Object> difference = ObjectUtils.difference(original, updated);
        // Assert
        TestCase.assertEquals(2, difference.size());
        assertNull(difference.get("two"));
    }

    @Test
    public void testMergeRemovingDuplicates() {
        // Arrange
        List<String> first = Arrays.asList("1", "2", "3", "4");
        List<String> second = Arrays.asList("3", "4", "5", "6", "7");
        // Act
        List<String> merged = ObjectUtils.mergeRemovingDuplicates(first, second);
        // Assert
        List<String> assertion = Arrays.asList("1", "2", "3", "4", "5", "6", "7");
        assertArrayEquals(assertion.toArray(), merged.toArray());
    }
}
