package org.corfudb.common.stats;

import lombok.Getter;

import java.util.concurrent.atomic.LongAdder;

/**
 *
 * Created by Maithem on 7/7/20.
 */
public class Counter implements Metric {

    private final LongAdder count = new LongAdder();

    @Getter
    private final String name;

    public Counter(String name) {
        this.name = name;
    }

    public void inc() {
        count.increment();
    }

    public void dec() {
        count.decrement();
    }

    public void inc(long value) {
        count.add(value);
    }

    public long sumThenReset() {
        return count.sumThenReset();
    }
}
