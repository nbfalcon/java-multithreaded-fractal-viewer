package org.nbfalcon.fractalViewer.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nbfalcon.fractalViewer.util.TimeUtils;

public class TimeUtilsTest {
    @Test
    public void timeFormat() {
        Assertions.assertEquals("1ms", TimeUtils.formatTimeMicros(1000));
        Assertions.assertEquals("10.09ms", TimeUtils.formatTimeMicros(10_090));
        Assertions.assertEquals("10.90ms", TimeUtils.formatTimeMicros(10_900));
        Assertions.assertEquals("100.90ms", TimeUtils.formatTimeMicros(100_900));
        Assertions.assertEquals("1s", TimeUtils.formatTimeMicros(1_000_900));
        Assertions.assertEquals("1.90s", TimeUtils.formatTimeMicros(1_900_000));
        Assertions.assertEquals("1.09s", TimeUtils.formatTimeMicros(1_090_000));

        Assertions.assertEquals("1m", TimeUtils.formatTimeMicros(1_000_000L * 60));
        Assertions.assertEquals("1h", TimeUtils.formatTimeMicros(1_000_000L * 60 * 60));
        Assertions.assertEquals("2h", TimeUtils.formatTimeMicros(1_000_000L * 60 * 60 * 2));
    }
}
