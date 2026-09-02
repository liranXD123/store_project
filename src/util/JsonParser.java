package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A small JSON reader written by hand, because the project does not use any external library.
// It reads JSON text and returns plain Java values:
//   an object becomes a Map, an array becomes a List, a string becomes a String,
//   a number becomes a Double, true/false become a Boolean and null becomes null.
public class JsonParser {
    private String text;
    private int pos;

    // The constructor is private, because the class is used only through the static parse method
    private JsonParser(String text) {
        this.text = text;
        this.pos = 0;
    }

    // Reading a whole JSON document and returning the value that was written in it
    public static Object parse(String text) {
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos < parser.text.length()) {
            throw new IllegalArgumentException("Unexpected text after the end of the JSON value, at position "
                    + parser.pos);
        }
        return value;
    }

    // Reading any JSON value, according to the character that opens it
    private Object readValue() {
        if (pos >= text.length()) {
            throw new IllegalArgumentException("The JSON text ended too early");
        }
        char c = text.charAt(pos);
        if (c == '{')
            return readObject();
        if (c == '[')
            return readArray();
        if (c == '"')
            return readString();
        if (c == 't' || c == 'f')
            return readBoolean();
        if (c == 'n') {
            expect("null");
            return null;
        }
        return readNumber();
    }

    // Reading an object: pairs of a name and a value, separated by commas, inside { }
    private Map<String, Object> readObject() {
        Map<String, Object> result = new HashMap<String, Object>();
        pos++; // skipping the {
        skipWhitespace();

        if (pos < text.length() && text.charAt(pos) == '}') {
            pos++;
            return result;
        }

        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            if (pos >= text.length() || text.charAt(pos) != ':') {
                throw new IllegalArgumentException("Expected ':' after the name " + key);
            }
            pos++; // skipping the :
            skipWhitespace();
            result.put(key, readValue());
            skipWhitespace();

            if (pos >= text.length()) {
                throw new IllegalArgumentException("The object was not closed");
            }
            char c = text.charAt(pos);
            if (c == ',') {
                pos++;
            } else if (c == '}') {
                pos++;
                return result;
            } else {
                throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
            }
        }
    }

    // Reading an array: values separated by commas, inside [ ]
    private List<Object> readArray() {
        List<Object> result = new ArrayList<Object>();
        pos++; // skipping the [
        skipWhitespace();

        if (pos < text.length() && text.charAt(pos) == ']') {
            pos++;
            return result;
        }

        while (true) {
            skipWhitespace();
            result.add(readValue());
            skipWhitespace();

            if (pos >= text.length()) {
                throw new IllegalArgumentException("The array was not closed");
            }
            char c = text.charAt(pos);
            if (c == ',') {
                pos++;
            } else if (c == ']') {
                pos++;
                return result;
            } else {
                throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
            }
        }
    }

    // Reading a string, translating the characters that are written with a backslash
    private String readString() {
        if (pos >= text.length() || text.charAt(pos) != '"') {
            throw new IllegalArgumentException("Expected a string at position " + pos);
        }
        pos++; // skipping the opening quote
        StringBuilder sb = new StringBuilder();

        while (pos < text.length()) {
            char c = text.charAt(pos);
            pos++;

            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }

            // A backslash means that the next character has a special meaning
            if (pos >= text.length()) {
                throw new IllegalArgumentException("The string was not closed");
            }
            char escaped = text.charAt(pos);
            pos++;
            switch (escaped) {
                case '"':
                    sb.append('"');
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case '/':
                    sb.append('/');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'u':
                    // A character written as four hexadecimal digits
                    if (pos + 4 > text.length()) {
                        throw new IllegalArgumentException("A \\u sequence is too short");
                    }
                    String hex = text.substring(pos, pos + 4);
                    sb.append((char) Integer.parseInt(hex, 16));
                    pos += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown escape character: \\" + escaped);
            }
        }
        throw new IllegalArgumentException("The string was not closed");
    }

    // Reading a number and returning it as a Double
    private Double readNumber() {
        int start = pos;
        if (pos < text.length() && (text.charAt(pos) == '-' || text.charAt(pos) == '+')) {
            pos++;
        }
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                pos++;
            } else {
                break;
            }
        }
        String number = text.substring(start, pos);
        try {
            return Double.valueOf(number);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + number + "' is not a valid number");
        }
    }

    private Boolean readBoolean() {
        if (text.charAt(pos) == 't') {
            expect("true");
            return Boolean.TRUE;
        }
        expect("false");
        return Boolean.FALSE;
    }

    // Making sure that the expected word really appears at the current position
    private void expect(String word) {
        if (!text.startsWith(word, pos)) {
            throw new IllegalArgumentException("Expected '" + word + "' at position " + pos);
        }
        pos += word.length();
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }
}
