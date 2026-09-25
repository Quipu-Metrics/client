package org.quipumetrics.core;

import lombok.Getter;

public final class JsonArray {

    private final StringBuilder builder = new StringBuilder("[");
    @Getter
    private boolean empty = true;

    public JsonArray add(JsonObject value) {
        return raw(value.toString());
    }

    public JsonArray add(long value) {
        return raw(Long.toString(value));
    }

    private JsonArray raw(String encodedValue) {
        if (!empty) builder.append(',');
        builder.append(encodedValue);
        empty = false;
        return this;
    }

    @Override
    public String toString() {
        return builder + "]";
    }
}
