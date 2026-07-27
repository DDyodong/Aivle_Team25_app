package com.example.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void workerRoleIsRecognized() {
        ApiClient.Session worker = new ApiClient.Session(
                "token", 1L, "worker", "작업자", java.util.List.of("WORKER")
        );
        ApiClient.Session admin = new ApiClient.Session(
                "token", 2L, "admin", "관리자", java.util.List.of("ADMIN")
        );

        assertTrue(worker.isWorker());
        assertFalse(admin.isWorker());
    }

    @Test
    public void missingWorkerRoleIsRejected() {
        ApiClient.Session session = new ApiClient.Session(
                "token", 3L, "viewer", "조회자", java.util.List.of()
        );

        assertFalse(session.isWorker());
    }
}
