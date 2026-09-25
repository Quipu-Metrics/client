package org.quipumetrics.core.chart;

import org.quipumetrics.core.JsonObject;

import java.util.Map;
import java.util.concurrent.Callable;

public final class DrilldownPieChart extends CustomChart {

    private final Callable<Map<String, Map<String, Integer>>> values;

    public DrilldownPieChart(String chartId, Callable<Map<String, Map<String, Integer>>> values) {
        super(chartId);
        this.values = values;
    }

    @Override
    protected JsonObject data() throws Exception {
        Map<String, Map<String, Integer>> current = values.call();
        if (current == null || current.isEmpty()) return null;

        JsonObject outer = new JsonObject();
        int outerCount = 0;
        for (Map.Entry<String, Map<String, Integer>> group : current.entrySet()) {
            if (outerCount == MAX_KEYS) break;

            if (group.getValue() == null || !validLabel(group.getKey()))
                continue;

            JsonObject inner = new JsonObject();
            int innerCount = 0;
            for (Map.Entry<String, Integer> entry : group.getValue().entrySet()) {
                if (innerCount == MAX_KEYS) break;

                if (entry.getValue() == null || !validLabel(entry.getKey()))
                    continue;

                inner.add(entry.getKey(), entry.getValue());
                innerCount++;
            }

            if (inner.isEmpty()) continue;

            outer.add(group.getKey(), inner);
            outerCount++;
        }

        return outer.isEmpty() ? null : new JsonObject().add("values", outer);
    }
}
