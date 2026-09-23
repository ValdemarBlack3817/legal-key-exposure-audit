package legaltech.infra;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Json {
    private Json() {}

    static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder result = new StringBuilder("{");
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                result.append(quote(entry.getKey().toString())).append(':').append(encode(entry.getValue()));
                if (iterator.hasNext()) result.append(',');
            }
            return result.append('}').toString();
        }
        if (value instanceof Collection<?> values) {
            StringBuilder result = new StringBuilder("[");
            Iterator<?> iterator = values.iterator();
            while (iterator.hasNext()) {
                result.append(encode(iterator.next()));
                if (iterator.hasNext()) result.append(',');
            }
            return result.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    static boolean booleanField(String json, String name) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(name) + "\\\"\\s*:\\s*(true|false)").matcher(json);
        if (!matcher.find()) throw new IllegalArgumentException("Missing boolean field: " + name);
        return Boolean.parseBoolean(matcher.group(1));
    }

    static String stringField(String json, String name, String fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(name) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
        return matcher.find() ? unescape(matcher.group(1)) : fallback;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r")
                .replace("\\t", "\t").replace("\\\\", "\\");
    }
}
