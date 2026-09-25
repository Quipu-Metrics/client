package org.quipumetrics.core.chart;

import java.util.Map;
import java.util.concurrent.Callable;

public final class MultiLineChart extends FlatValuesChart {

    public MultiLineChart(String chartId, Callable<Map<String, Integer>> values) {
        super(chartId, values);
    }
}
