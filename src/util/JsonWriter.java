package util;

import java.util.List;
import java.util.Map;

// Helper methods for writing JSON text by hand.
// The files of the database are built with a StringBuilder so that the fields always
// appear in the same order, which keeps the saved file easy to read and to compare.
public class JsonWriter {

    // Writing a text value as a JSON string, protecting the characters that JSON cannot hold as they are
    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // Control characters have to be written as a \\u sequence
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Helpers for reading values out of the maps that JsonParser returns
    // ------------------------------------------------------------------

    // Returning the object that sits under the given name, or null when it is not there
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getObject(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("The field '" + key + "' is not an object");
        }
        return (Map<String, Object>) value;
    }

    // Returning the array that sits under the given name, or null when it is not there
    @SuppressWarnings("unchecked")
    public static List<Object> getArray(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("The field '" + key + "' is not an array");
        }
        return (List<Object>) value;
    }

    // Returning a text field. A missing field is reported, because every record of the
    // database is written by this program and must be complete
    public static String getString(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            throw new IllegalArgumentException("The field '" + key + "' is missing");
        }
        return value.toString();
    }

    // Returning a number field as a double
    public static double getDouble(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (!(value instanceof Double)) {
            throw new IllegalArgumentException("The field '" + key + "' is not a number");
        }
        return ((Double) value).doubleValue();
    }

    // Returning a number field as a whole number
    public static int getInt(Map<String, Object> parent, String key) {
        return (int) getDouble(parent, key);
    }
}
