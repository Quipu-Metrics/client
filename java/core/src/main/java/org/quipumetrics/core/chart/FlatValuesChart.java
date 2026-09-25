package org.quipumetrics.core.chart;

import org.quipumetrics.core.JsonObject;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Shared body for the three chart types whose payload is a flat map of label to
 * count: advanced_pie, simple_bar and multi_line.
 */
abstract class FlatValuesChart extends CustomChart {

    private final Callable<Map<String, Integer>> values;

    FlatValuesChart(String chartId, Callable<Map<String, Integer>> values) {
        super(chartId);
        this.values = values;
    }

    @Override
    protected final JsonObject data() throws Exception {
        Map<String, Integer> current = values.call();
        if (current == null || current.isEmpty()) return null;

        JsonObject entries = new JsonObject();
        int count = 0;
        for (Map.Entry<String, Integer> entry : current.entrySet()) {
            if (count == MAX_KEYS) break;

            if (entry.getValue() == null || !validLabel(entry.getKey()))
                continue;

            entries.add(entry.getKey(), entry.getValue());
            count++;
        }

        return entries.isEmpty() ? null : new JsonObject().add("values", entries);
    }
}
