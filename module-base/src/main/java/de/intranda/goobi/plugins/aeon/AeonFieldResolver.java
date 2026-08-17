package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Resolves a single field inside an AEON API response.
 *
 * The response is a mostly flat map of key/value pairs, but some fields live inside a nested map. AEON currently keeps
 * the material type and the home site in "customFieldValues". Nested fields are addressed with a slash separated path,
 * the same idiom used to read this plugin's own configuration, for instance "customFieldValues/MaterialType". A path
 * without a slash is an ordinary top level lookup, so existing configurations keep working unchanged.
 *
 * Lookups are case sensitive: the flat AEON keys are camelCase while the nested ones are PascalCase, so matching
 * loosely would only hide a typo behind a silent null value.
 */
public final class AeonFieldResolver {

    private static final String PATH_SEPARATOR = "/";

    private AeonFieldResolver() {
        // utility class
    }

    /**
     * @param response the deserialized AEON response
     * @param path field name, or slash separated path for a field inside a nested map
     * @return the value the path points at, or null if the value itself is null or any segment of the path does not
     *         exist
     */
    public static Object resolve(Map<String, Object> response, String path) {
        String[] segments = splitPath(path);
        Map<String, Object> parent = resolveParent(response, segments);
        if (parent == null) {
            return null;
        }
        return parent.get(segments[segments.length - 1]);
    }

    /**
     * Same as {@link #resolve(Map, String)}, but converts non-string values. AEON delivers most fields as strings, but
     * numbers like the transaction number arrive as integers.
     */
    public static String resolveString(Map<String, Object> response, String path) {
        Object value = resolve(response, path);
        if (value == null) {
            return null;
        }
        return value instanceof String ? (String) value : String.valueOf(value);
    }

    /**
     * Tells a field that is missing from the response apart from one that is present but empty. Callers need that
     * difference to report a renamed field differently from an unfilled one - a plain null value cannot distinguish
     * the two.
     */
    public static boolean containsPath(Map<String, Object> response, String path) {
        String[] segments = splitPath(path);
        Map<String, Object> parent = resolveParent(response, segments);
        return parent != null && parent.containsKey(segments[segments.length - 1]);
    }

    /**
     * @return the map that directly contains the last segment of the path, or null if the path cannot be followed that
     *         far
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveParent(Map<String, Object> response, String[] segments) {
        if (response == null || segments == null) {
            return null;
        }
        Map<String, Object> current = response;
        for (int i = 0; i < segments.length - 1; i++) {
            Object child = current.get(segments[i]);
            // a scalar has no children, so the path simply does not resolve
            if (!(child instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) child;
        }
        return current;
    }

    /**
     * @return the non-empty segments of the path, or null if the path holds no usable segment at all
     */
    private static String[] splitPath(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        for (String segment : StringUtils.split(path, PATH_SEPARATOR)) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }
        return segments.isEmpty() ? null : segments.toArray(new String[0]);
    }
}
