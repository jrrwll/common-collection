package org.dreamcat.common.collection.time;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.SneakyThrows;
import org.dreamcat.common.util.DateUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-05-30
 */
class TimeWheelTest {

    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());

    @Test
    @Disabled
    @SneakyThrows
    void test() {
        final TimeWheel<Integer> timeWheel = new TimeWheel<>(executorService, executorService, this::call);
        for (int i = 1; i <= 9; i++) {
            timeWheel.setTimeout(i, i);
            timeWheel.setTimeout(i * 10 + i, i * 3);
        }
        timeWheel.start();
        Thread.sleep(30_000);
    }

    void call(int i) {
        DateUtil.format(new Date());
        System.out.printf("%s:\t %04d%n", DateUtil.formatNow(), i);
    }
}
