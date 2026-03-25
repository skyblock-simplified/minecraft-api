package dev.sbs.minecraftapi;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftApiShutdownTest {

    @Test
    void fullShutdown_completesCleanly() throws Exception {
        // MinecraftApi static init connects the H2 session with 33 models
        assertTrue(MinecraftApi.getSessionManager().isActive());

        // Disconnect sessions (cancels repository refresh tasks, closes SessionFactory)
        MinecraftApi.getSessionManager().disconnect();
        assertFalse(MinecraftApi.getSessionManager().isActive(),
            "SessionManager should be inactive after disconnect()");

        // Shutdown the scheduler (kills executor threads)
        MinecraftApi.getScheduler().shutdown();
        assertTrue(MinecraftApi.getScheduler().isShutdown(),
            "Scheduler should report isShutdown() immediately");

        // Wait for termination
        long start = System.currentTimeMillis();
        while (!MinecraftApi.getScheduler().isTerminated() && System.currentTimeMillis() - start < 10_000)
            Thread.sleep(100);

        assertTrue(MinecraftApi.getScheduler().isTerminated(),
            "Scheduler did not terminate within 10 seconds after disconnect + shutdown");

        // Give threads time to wind down
        Thread.sleep(2_000);

        // Verify no application non-daemon threads remain
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        long leakedCount = threads.stream()
            .filter(t -> !t.isDaemon() && t.isAlive())
            .filter(t -> !t.getName().equals("main"))
            .filter(t -> !t.getName().startsWith("Reference Handler"))
            .filter(t -> !t.getName().startsWith("Signal Dispatcher"))
            .filter(t -> !t.getName().startsWith("Notification"))
            .filter(t -> !t.getName().startsWith("Finalizer"))
            .filter(t -> !t.getName().contains("workers"))
            .filter(t -> !t.getName().startsWith("Test worker"))
            .peek(t -> System.out.printf("[LEAKED non-daemon] %s (state=%s)%n", t.getName(), t.getState()))
            .count();

        assertEquals(0, leakedCount,
            "Found " + leakedCount + " leaked non-daemon thread(s) after full shutdown");
    }

}
