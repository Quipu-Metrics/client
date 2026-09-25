package org.quipumetrics.mod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoaderInfoTest {

    /**
     * No loader is on the test classpath, which is the same situation as any JVM
     * that is not a modded server. Detection must degrade to the fallbacks rather
     * than throw, because a crash here would take down the host mod at startup.
     */
    @Test
    void fallsBackWhenNoLoaderIsPresent() {
        assertEquals("other", LoaderInfo.platform());
        assertEquals("unknown", LoaderInfo.minecraftVersion());
    }

    @Test
    void builderFillsEverythingExceptTheHostSuppliedValues() {
        assertDoesNotThrow(() -> ModMetrics.builder(1234, "1.0.0")
                .playerCount(() -> 7)
                .onlineMode(() -> 1)
                .build());
    }
}
