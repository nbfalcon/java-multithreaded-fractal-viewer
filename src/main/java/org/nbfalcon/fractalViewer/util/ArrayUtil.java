package org.nbfalcon.fractalViewer.util;

public class ArrayUtil {
    public static int max(int[] arr) {
        int max = Integer.MIN_VALUE;
        for (int n : arr) {
            max = Math.max(max, n);
        }
        return max;
    }
}
