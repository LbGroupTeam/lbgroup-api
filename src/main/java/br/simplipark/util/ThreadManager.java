package br.simplipark.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadManager {
    private ThreadManager() {
    }

    public static void schedulePeriodicTask(Runnable task, long period, TimeUnit timeUnit) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        if (period <= 0) {
            throw new IllegalArgumentException("Period must be greater than 0");
        }

        var executor = Executors.newSingleThreadScheduledExecutor();

        task = wrapRunnableWithTryCatch(task, executor);

        executor.scheduleAtFixedRate(task, period, period, timeUnit);
    }

    private static Runnable wrapRunnableWithTryCatch(Runnable task, ScheduledExecutorService executor) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();

                executor.close();
            }
        };
    }
}
