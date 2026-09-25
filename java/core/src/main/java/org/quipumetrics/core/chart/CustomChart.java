package org.quipumetrics.core.chart;

import org.quipumetrics.core.JsonObject;

import java.util.regex.Pattern;

public abstract class CustomChart {

    static final int MAX_KEYS = 64;
    static final int MAX_LABEL_LENGTH = 128;

    private static final Pattern VALID_ID = Pattern.compile("^[a-z0-9_]{1,64}$");

    private final String chartId;

    protected CustomChart(String chartId) {
        if (!VALID_ID.matcher(chartId).matches())
            throw new IllegalArgumentException("chartId must match [a-z0-9_]{1,64}, got: " + chartId);

        this.chartId = chartId;
    }

    /**
     * @return the chart's data, or null when the value is unavailable. A chart
     * with no value is omitted from the submission rather than sent with a
     * placeholder, which would be counted as real data.
     */
    protected abstract JsonObject data() throws Exception;

    public final JsonObject toJson() {
        JsonObject data;
        try {
            data = data();
        } catch (Exception e) {
            return null;
        }

        return data != null && !data.isEmpty() ?
                new JsonObject().add("chartId", chartId).add("data", data) :
                null;
    }

    static boolean validLabel(String label) {
        return label != null && !label.isEmpty() && label.length() <= MAX_LABEL_LENGTH;
    }
}
