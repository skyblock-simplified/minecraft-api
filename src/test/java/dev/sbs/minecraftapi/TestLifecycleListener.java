package dev.sbs.minecraftapi;

import dev.sbs.api.scheduler.Scheduler;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Manages the {@link MinecraftApi} lifecycle around the test plan execution.
 * <p>
 * On startup, eagerly triggers the {@link MinecraftApi} static initializer
 * (H2 session, Gson adapters, Feign clients). On finish, shuts down the session
 * manager and scheduler so non-daemon threads do not prevent the test JVM from
 * exiting.
 */
public class TestLifecycleListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        System.out.println("[TestLifecycleListener] Initializing MinecraftApi");
        MinecraftApi.getSessionManager();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        System.out.println("[TestLifecycleListener] testPlanExecutionFinished called");

        if (MinecraftApi.getSessionManager().isActive()) {
            MinecraftApi.getSessionManager().shutdown();
            System.out.println("[TestLifecycleListener] SessionManager shut down");
        }

        if (!MinecraftApi.getScheduler().isShutdown()) {
            MinecraftApi.getScheduler().shutdown();
            System.out.println("[TestLifecycleListener] Scheduler shut down");
        }

        Scheduler.leakedThreads().forEach(t -> System.out.printf(
            "[TestLifecycleListener] Remaining non-daemon: %s (state=%s, group=%s)%n",
            t.getName(), t.getState(), t.getThreadGroup()
        ));
    }

}
