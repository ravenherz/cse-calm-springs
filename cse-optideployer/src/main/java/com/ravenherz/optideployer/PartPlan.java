package com.ravenherz.optideployer;

public final class PartPlan {

    public static final int STREAMS = 8;

    private PartPlan() {
    }

    public static long[] lengths(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("size");
        }
        long[] lengths = new long[STREAMS];
        long base = size / STREAMS;
        long extra = size % STREAMS;
        for (int i = 0; i < STREAMS; i++) {
            lengths[i] = base + (i < extra ? 1 : 0);
        }
        return lengths;
    }
}
