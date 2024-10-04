package br.simplipark.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ThreadManagerTest {

    private int taskRunCount = 0;

    @Test
    void testSchedulePeriodicTask_NullTask() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ThreadManager.schedulePeriodicTask(null, 1, TimeUnit.SECONDS));
        assertEquals("Task cannot be null", exception.getMessage());
    }

    @Test
    void testSchedulePeriodicTask_NonPositivePeriod() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ThreadManager.schedulePeriodicTask(() -> {}, 0, TimeUnit.SECONDS));
        assertEquals("Period must be greater than 0", exception.getMessage());
    }

    @Test
    void testSchedulePeriodicTask_SuccessfulExecution() throws InterruptedException {
        Runnable task = () -> taskRunCount++;

        // Schedule the task to run every 100 milliseconds
        ScheduledFuture<?> future = ThreadManager.schedulePeriodicTask(task, 100, TimeUnit.MILLISECONDS);

        // Let the task run a few times
        Thread.sleep(250); // Allow it to run a few times (2-3 executions)

        // Verify that the task was executed
        assertTrue(taskRunCount > 0); // Ensure the task has run at least once

        // Cleanup if necessary (shutdown the executor)
        future.cancel(false); // Cancel the scheduled future
    }

    // TODO: Make a unit test for when the task throws an exception it should still run the task again after the period.
}
