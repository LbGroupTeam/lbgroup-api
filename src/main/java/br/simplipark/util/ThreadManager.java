package br.simplipark.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadManager {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private ThreadManager() {
    }

    public static ScheduledFuture<?> schedulePeriodicTask(Runnable task, long period, TimeUnit timeUnit) {
        Objects.requireNonNull(task, "Task cannot be null");

        if (period <= 0) {
            throw new IllegalArgumentException("Period must be greater than 0");
        }

        task = Util.wrapRunnableWithTryCatch(task);

        return executor.scheduleAtFixedRate(task, period, period, timeUnit);
    }
}
