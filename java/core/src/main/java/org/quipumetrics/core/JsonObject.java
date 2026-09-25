package org.quipumetrics.core;

import lombok.Getter;

/**
 * Minimal JSON object writer.
 * <p>
 * Hand written so that core carries no dependencies. Shading a JSON library into
 * every plugin would bloat each jar and risk clashing with the copy the server
 * already loaded.
 */
public final class JsonObject {

    private final StringBuilder builder = new StringBuilder("{");
    @Getter
    private boolean empty = true;

    public JsonObject add(String key, String value) {
        return raw(key, escape(value));
    }

    public JsonObject add(String key, long value) {
        return raw(key, Long.toString(value));
    }

    public JsonObject add(String key, JsonObject value) {
        return raw(key, value.toString());
    }

    public JsonObject add(String key, JsonArray value) {
        return raw(key, value.toString());
    }

    private JsonObject raw(String key, String encodedValue) {
        if (!empty) builder.append(',');
        builder.append(escape(key)).append(':').append(encodedValue);
        empty = false;
        return this;
    }

    @Override
    public String toString() {
        return builder + "}";
    }

    static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20 || (c >= 0x7f && c <= 0x9f)) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }
}
