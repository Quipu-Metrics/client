package org.quipumetrics.core.chart;

import org.quipumetrics.core.JsonObject;

import java.util.concurrent.Callable;

public final class SingleLineChart extends CustomChart {

    private final Callable<Integer> value;

    public SingleLineChart(String chartId, Callable<Integer> value) {
        super(chartId);
        this.value = value;
    }

    @Override
    protected JsonObject data() throws Exception {
        Integer current = value.call();
        return current == null ? null : new JsonObject().add("value", current);
    }
}
