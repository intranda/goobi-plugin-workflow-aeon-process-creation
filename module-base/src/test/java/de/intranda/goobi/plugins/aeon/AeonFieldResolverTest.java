package de.intranda.goobi.plugins.aeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * The AEON API returns a mostly flat map of key/value pairs, but some fields live inside a nested map (currently
 * "customFieldValues", which holds "MaterialType" and "HomeSite"). Nested keys are addressed with a slash separated
 * path, matching the XPath idiom already used to read this plugin's configuration.
 */
public class AeonFieldResolverTest {

    private Map<String, Object> response;

    @Before
    public void setUp() {
        Map<String, Object> customFieldValues = new LinkedHashMap<>();
        customFieldValues.put("MaterialType", "Audio Recording (DRMS)");
        customFieldValues.put("HomeSite", "MUS");
        customFieldValues.put("RestrictionCode", null);

        response = new LinkedHashMap<>();
        response.put("transactionNumber", Integer.valueOf(286670));
        response.put("itemInfo2", "MUS");
        response.put("itemInfo3", null);
        response.put("customFieldValues", customFieldValues);
    }

    // ----- flat lookups keep working unchanged -----

    @Test
    public void testResolveFlatKey() {
        assertEquals("MUS", AeonFieldResolver.resolve(response, "itemInfo2"));
    }

    @Test
    public void testResolveFlatKeyKeepsValueType() {
        assertEquals(Integer.valueOf(286670), AeonFieldResolver.resolve(response, "transactionNumber"));
    }

    @Test
    public void testResolveFlatKeyWithNullValue() {
        assertNull(AeonFieldResolver.resolve(response, "itemInfo3"));
    }

    @Test
    public void testResolveAbsentFlatKey() {
        assertNull(AeonFieldResolver.resolve(response, "shippingOption"));
    }

    // ----- nested lookups -----

    @Test
    public void testResolveNestedKey() {
        assertEquals("Audio Recording (DRMS)", AeonFieldResolver.resolve(response, "customFieldValues/MaterialType"));
    }

    @Test
    public void testResolveSecondNestedKey() {
        assertEquals("MUS", AeonFieldResolver.resolve(response, "customFieldValues/HomeSite"));
    }

    @Test
    public void testResolveNestedKeyWithNullValue() {
        assertNull(AeonFieldResolver.resolve(response, "customFieldValues/RestrictionCode"));
    }

    @Test
    public void testResolveAbsentNestedKey() {
        assertNull(AeonFieldResolver.resolve(response, "customFieldValues/DoesNotExist"));
    }

    @Test
    public void testResolveNestedKeyBelowAbsentParent() {
        assertNull(AeonFieldResolver.resolve(response, "noSuchMap/MaterialType"));
    }

    /** Descending into a scalar must not blow up - it simply has no children. */
    @Test
    public void testResolveNestedKeyBelowScalarParent() {
        assertNull(AeonFieldResolver.resolve(response, "itemInfo2/MaterialType"));
    }

    @Test
    public void testResolveDeeplyNestedKey() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("leaf", "found");
        Map<String, Object> middle = new HashMap<>();
        middle.put("inner", inner);
        response.put("outer", middle);

        assertEquals("found", AeonFieldResolver.resolve(response, "outer/inner/leaf"));
    }

    /** Lookups are case sensitive: flat AEON keys are camelCase, nested ones PascalCase. */
    @Test
    public void testResolveIsCaseSensitive() {
        assertNull(AeonFieldResolver.resolve(response, "customFieldValues/materialtype"));
    }

    // ----- path hygiene -----

    @Test
    public void testResolveToleratesSurroundingWhitespaceAndSlashes() {
        assertEquals("Audio Recording (DRMS)", AeonFieldResolver.resolve(response, " /customFieldValues/MaterialType/ "));
    }

    @Test
    public void testResolveBlankPath() {
        assertNull(AeonFieldResolver.resolve(response, ""));
        assertNull(AeonFieldResolver.resolve(response, null));
    }

    @Test
    public void testResolveNullResponse() {
        assertNull(AeonFieldResolver.resolve(null, "itemInfo2"));
    }

    // ----- resolveString: replaces the instanceof String / instanceof Integer casting at the call sites -----

    @Test
    public void testResolveStringFromNestedKey() {
        assertEquals("Audio Recording (DRMS)", AeonFieldResolver.resolveString(response, "customFieldValues/MaterialType"));
    }

    @Test
    public void testResolveStringConvertsNonStringValue() {
        assertEquals("286670", AeonFieldResolver.resolveString(response, "transactionNumber"));
    }

    @Test
    public void testResolveStringOfAbsentKey() {
        assertNull(AeonFieldResolver.resolveString(response, "shippingOption"));
    }

    // ----- containsPath: distinguishes "absent" from "present but null" -----

    @Test
    public void testContainsFlatPath() {
        assertTrue(AeonFieldResolver.containsPath(response, "itemInfo2"));
    }

    /** A key that is present with a null value still counts as present. */
    @Test
    public void testContainsFlatPathWithNullValue() {
        assertTrue(AeonFieldResolver.containsPath(response, "itemInfo3"));
    }

    @Test
    public void testContainsAbsentFlatPath() {
        assertFalse(AeonFieldResolver.containsPath(response, "shippingOption"));
    }

    @Test
    public void testContainsNestedPath() {
        assertTrue(AeonFieldResolver.containsPath(response, "customFieldValues/MaterialType"));
    }

    /** This is what lets the requiredFields check report an empty MaterialType instead of skipping it. */
    @Test
    public void testContainsNestedPathWithNullValue() {
        assertTrue(AeonFieldResolver.containsPath(response, "customFieldValues/RestrictionCode"));
    }

    @Test
    public void testContainsAbsentNestedPath() {
        assertFalse(AeonFieldResolver.containsPath(response, "customFieldValues/DoesNotExist"));
    }

    @Test
    public void testContainsNestedPathBelowAbsentParent() {
        assertFalse(AeonFieldResolver.containsPath(response, "noSuchMap/MaterialType"));
    }

    @Test
    public void testContainsBlankPath() {
        assertFalse(AeonFieldResolver.containsPath(response, ""));
        assertFalse(AeonFieldResolver.containsPath(response, null));
    }
}
