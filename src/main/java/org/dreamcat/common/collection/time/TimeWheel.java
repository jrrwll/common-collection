package org.dreamcat.common.collection.time;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2022-05-30
 */
@SuppressWarnings("unchecked")
public class TimeWheel<E> {

    final Set<WheelEntry<E>>[] secRing; // 1s - 59s
    AtomicInteger secIndex = new AtomicInteger();
    final Set<WheelEntry<E>>[] minRing; // 1m - 60m
    AtomicInteger minIndex = new AtomicInteger();
    final Set<WheelEntry<E>>[] hourRing; // 1h - 24h
    AtomicInteger hourIndex = new AtomicInteger();
    final Set<WheelEntry<E>>[] dayRing; // 1d - 30d
    AtomicInteger dayIndex = new AtomicInteger();

    private static final long bound = 3600L * 24 * 30;

    final ScheduledExecutorService scheduledExecutorService;
    final ExecutorService executorService;
    final Consumer<E> callable;

    public TimeWheel(ScheduledExecutorService scheduledExecutorService,
            ExecutorService executorService, Consumer<E> callable) {
        this(scheduledExecutorService, executorService, callable, null);
    }

    public TimeWheel(ScheduledExecutorService scheduledExecutorService,
            ExecutorService executorService,
            Consumer<E> callable,
            Comparator<? super E> comparator) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.executorService = executorService;
        this.callable = callable;

        Comparator<WheelEntry<E>> c;
        if (comparator != null) {
            c = (p1, p2) -> comparator.compare(p1.element, p2.element);
        } else {
            c = (p1, p2) -> ((Comparable<E>) p1.element).compareTo(p2.element);
        }
        initRing(secRing = new Set[59], c);
        initRing(minRing = new Set[60], c);
        initRing(hourRing = new Set[60], c);
        initRing(dayRing = new Set[60], c);
    }

    private void initRing(Set<WheelEntry<E>>[] ring, Comparator<WheelEntry<E>> comparator) {
        int n = ring.length;
        for (int i = 0; i < n; i++) {
            ring[i] = new ConcurrentSkipListSet<>(comparator);
        }
    }

    /**
     * start to schedule
     */
    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(
                () -> watch(secRing, secIndex), 0, 1, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleAtFixedRate(
                () -> watch(minRing, minIndex), 0, 1, TimeUnit.MINUTES);
        scheduledExecutorService.scheduleAtFixedRate(
                () -> watch(hourRing, hourIndex), 0, 1, TimeUnit.HOURS);
        scheduledExecutorService.scheduleAtFixedRate(
                () -> watch(dayRing, dayIndex), 0, 1, TimeUnit.DAYS);
    }

    /**
     * set a timer for the element
     *
     * @param element which to callback
     * @param delay delay in second
     */
    public void setTimeout(E element, int delay) {
        Objects.requireNonNull(element);
        ObjectUtil.requireRange(delay, 1, bound, "delay");

        int s, m, h, d;
        if ((s = delay % 60) > 0) {
            int i = (secIndex.get() + s - 1) % secRing.length;
            secRing[i].add(WheelEntry.of(element, delay  - s));
        } else if ((m = delay % 3600) > 0) {
            int i = (minIndex.get() + m - 1) % minRing.length;
            minRing[i].add(WheelEntry.of(element, delay - m));
        } else if ((h = delay % 86400) > 0) {
            int i = (hourIndex.get() + h - 1) % hourRing.length;
            hourRing[i].add(WheelEntry.of(element, delay - h));
        } else {
            d = delay / 86400;
            int i = (dayIndex.get() + d - 1) % dayRing.length;
            dayRing[i].add(WheelEntry.of(element, 0));
        }
    }

    /**
     * clear the timer bounded to the element
     *
     * @param element which set a timer
     * @return ok or not
     */
    public boolean clearTimeout(E element) {
        WheelEntry<E> entry = WheelEntry.of(element);
        for (Set<WheelEntry<E>> pairs : secRing) {
            if (pairs.remove(entry)) return true;
        }
        for (Set<WheelEntry<E>> pairs : minRing) {
            if (pairs.remove(entry)) return true;
        }
        for (Set<WheelEntry<E>> pairs : hourRing) {
            if (pairs.remove(entry)) return true;
        }
        for (Set<WheelEntry<E>> pairs : dayRing) {
            if (pairs.remove(entry)) return true;
        }
        return false;
    }

    private void watch(final Set<WheelEntry<E>>[] ring, AtomicInteger index) {
        Map<Boolean, List<WheelEntry<E>>> map;
        synchronized (ring) {
            Set<WheelEntry<E>> todoSet = ring[index.getAndIncrement()];
            if (index.get() == secRing.length) {
                index.set(0);
            }
            map = todoSet.stream().collect(Collectors.partitioningBy(entry -> entry.delay == 0));
            todoSet.clear();
        }
        List<WheelEntry<E>> todoList = map.get(false);
        for (WheelEntry<E> entry : todoList) {
            setTimeout(entry.element, entry.delay);
        }
        List<WheelEntry<E>> callList = map.get(true);
        for (WheelEntry<E> entry : callList) {
            executorService.submit(() -> callable.accept(entry.element));
        }
    }

    @Getter
    private static class WheelEntry<E> {

        E element;
        int delay;

        static <E> WheelEntry<E> of(E element) {
            return of(element, 0);
        }

        static <E> WheelEntry<E> of(E element, int delay) {
            WheelEntry<E> entry = new WheelEntry<>();
            entry.element = element;
            entry.delay = delay;
            return entry;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WheelEntry<?> that = (WheelEntry<?>) o;
            return Objects.equals(element, that.element);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(element);
        }
    }
}
