package br.lbgroup.nescharge.util;

import br.lbgroup.commons.util.ThreadManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ThreadManagerTest {

    private int taskRunCount = 0;

    @Test
    void testSchedulePeriodicTask_NullTask() {
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
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

        ScheduledFuture<?> future = ThreadManager.schedulePeriodicTask(task, 100, TimeUnit.MILLISECONDS);

        // Let the task run a few times
        Thread.sleep(250);

        assertTrue(taskRunCount > 0);

        // Cleanup
        future.cancel(false);
    }

    // TODO: Make a unit test for when the task throws an exception it should still run the task again after the period.
}
