package dev.genyo.installer.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {

    private final String src;
    private int pos;

    private JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static Object parse(String json) {
        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        parser.skipWhitespace();
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object value = parse(json);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new JsonParseException("Expected a JSON object at the root");
    }

    private Object parseValue() {
        skipWhitespace();
        if (pos >= src.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> parseObjectValue();
            case '[' -> parseArrayValue();
            case '"' -> parseStringValue();
            case 't', 'f' -> parseBooleanValue();
            case 'n' -> parseNullValue();
            default -> parseNumberValue();
        };
    }

    private Map<String, Object> parseObjectValue() {
        Map<String, Object> result = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = parseStringValue();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            result.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == '}') {
                pos++;
                break;
            } else {
                throw new JsonParseException("Expected ',' or '}' in object at position " + pos);
            }
        }
        return result;
    }

    private List<Object> parseArrayValue() {
        List<Object> result = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            Object value = parseValue();
            result.add(value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == ']') {
                pos++;
                break;
            } else {
                throw new JsonParseException("Expected ',' or ']' in array at position " + pos);
            }
        }
        return result;
    }

    private String parseStringValue() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw new JsonParseException("Unterminated string");
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (pos >= src.length()) {
                    throw new JsonParseException("Unterminated escape sequence");
                }
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > src.length()) {
                            throw new JsonParseException("Invalid unicode escape");
                        }
                        String hex = src.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> throw new JsonParseException("Invalid escape character: \\" + esc);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Boolean parseBooleanValue() {
        if (src.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new JsonParseException("Invalid literal at position " + pos);
    }

    private Object parseNullValue() {
        if (src.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new JsonParseException("Invalid literal at position " + pos);
    }

    private Double parseNumberValue() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
            pos++;
        }
        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        if (pos == start) {
            throw new JsonParseException("Invalid number at position " + pos);
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new JsonParseException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }

    private char peek() {
        if (pos >= src.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        return src.charAt(pos);
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    public static class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }
    }

}
