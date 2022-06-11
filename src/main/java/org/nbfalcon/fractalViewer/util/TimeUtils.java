package org.nbfalcon.fractalViewer.util;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static String formatTimeNanos(long nanos) {
        return formatTimeMicros(nanos / 1000);
    }

    private static String formatTime1(long fst, long snd, long sndMod, String unit) {
        snd = snd % sndMod;
        if (snd != 0) {
            String second = new DecimalFormat("#.00").format((float) snd / sndMod);
            return String.format("%d%s%s", fst, second, unit);
        } else {
            return fst + unit;
        }
    }

    public static String formatTimeMicros(long micros) {
        final TimeUnit timeUnit = TimeUnit.MICROSECONDS;

        if (timeUnit.toSeconds(micros) == 0)
            return formatTime1(timeUnit.toMillis(micros), micros, 1000, "ms");
        else if (timeUnit.toMinutes(micros) == 0)
            return formatTime1(timeUnit.toSeconds(micros), timeUnit.toMillis(micros), 1000, "s");
        else if (timeUnit.toHours(micros) == 0)
            return formatTime1(timeUnit.toMinutes(micros), timeUnit.toSeconds(micros), 60, "m");
        else
            return formatTime1(timeUnit.toHours(micros), timeUnit.toMinutes(micros), 60, "h");
    }
}
