package org.quipumetrics.core.chart;

import org.quipumetrics.core.JsonArray;
import org.quipumetrics.core.JsonObject;

import java.util.Map;
import java.util.concurrent.Callable;

public final class AdvancedBarChart extends CustomChart {

    private final Callable<Map<String, int[]>> values;

    public AdvancedBarChart(String chartId, Callable<Map<String, int[]>> values) {
        super(chartId);
        this.values = values;
    }

    @Override
    protected JsonObject data() throws Exception {
        Map<String, int[]> current = values.call();
        if (current == null || current.isEmpty()) return null;

        JsonObject entries = new JsonObject();
        int count = 0;
        for (Map.Entry<String, int[]> entry : current.entrySet()) {
            if (count == MAX_KEYS) break;

            int[] series = entry.getValue();
            if (series == null || series.length == 0 || !validLabel(entry.getKey()))
                continue;

            JsonArray array = new JsonArray();
            for (int i = 0; i < series.length && i < MAX_KEYS; i++)
                array.add(series[i]);

            entries.add(entry.getKey(), array);
            count++;
        }
        return entries.isEmpty() ? null : new JsonObject().add("values", entries);
    }
}
