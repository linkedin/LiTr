/*
 * Copyright 2020 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.io;

/**
 * Data class used to define a range (start, stop) of media. For example, it can be used to
 * define a "selection" in a MediaSource
 */
public class MediaRange {

    private final long start;
    private final long end;

    /**
     * Create an instance of MediaRange
     * @param start range start, in microseconds
     * @param end range end, in microseconds, greater than start
     */
    public MediaRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Get range start, in microseconds
     */
    public long getStart() {
        return start;
    }

    /**
     * Get range end, in microseconds
     */
    public long getEnd() {
        return end;
    }
}
