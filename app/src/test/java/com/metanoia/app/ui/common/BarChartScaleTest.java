package com.metanoia.app.ui.common;

import static org.junit.Assert.assertEquals;

import com.metanoia.app.ui.common.BarChartView.AxisScale;
import com.metanoia.app.ui.common.BarChartView.Entry;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class BarChartScaleTest {

    @Test
    public void computeAxisScale_handlesPositiveOnlyAndOverride() {
        AxisScale positive = BarChartView.computeAxisScale(
                Arrays.asList(
                        new Entry("A", 100L, "100", false),
                        new Entry("B", 500L, "500", false)
                ),
                null
        );
        assertEquals(0L, positive.minValue);
        assertEquals(500L, positive.maxValue);
        assertEquals(1f, positive.baselineFraction, 0.001f);

        AxisScale fixedCeiling = BarChartView.computeAxisScale(
                Collections.singletonList(new Entry("09h", 2500L, "25%", false)),
                10_000L
        );
        assertEquals(0L, fixedCeiling.minValue);
        assertEquals(10_000L, fixedCeiling.maxValue);
        assertEquals(1f, fixedCeiling.baselineFraction, 0.001f);
    }

    @Test
    public void computeAxisScale_handlesMixedNegativePositiveAndAllZero() {
        AxisScale mixed = BarChartView.computeAxisScale(
                Arrays.asList(
                        new Entry("Lun", 300L, "$300", false),
                        new Entry("Mar", -100L, "-$100", true)
                ),
                null
        );
        assertEquals(-100L, mixed.minValue);
        assertEquals(300L, mixed.maxValue);
        assertEquals(0.75f, mixed.baselineFraction, 0.001f);

        AxisScale zeros = BarChartView.computeAxisScale(
                Collections.singletonList(new Entry("09h", 0L, "0%", true)),
                null
        );
        assertEquals(0L, zeros.minValue);
        assertEquals(1L, zeros.maxValue);
        assertEquals(1f, zeros.baselineFraction, 0.001f);
    }

    @Test
    public void computeLabelStep_preventsOverlapOnDenseSeries() {
        assertEquals(1, BarChartView.computeLabelStep(6, 360f, 48f));
        assertEquals(4, BarChartView.computeLabelStep(31, 360f, 40f));
    }
}
