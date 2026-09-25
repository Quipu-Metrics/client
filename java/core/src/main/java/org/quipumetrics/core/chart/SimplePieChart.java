package org.quipumetrics.core.chart;

import org.quipumetrics.core.JsonObject;

import java.util.concurrent.Callable;

public final class SimplePieChart extends CustomChart {

    private final Callable<String> value;

    public SimplePieChart(String chartId, Callable<String> value) {
        super(chartId);
        this.value = value;
    }

    @Override
    protected JsonObject data() throws Exception {
        String current = value.call();
        return validLabel(current) ? new JsonObject().add("value", current) : null;
    }
}
