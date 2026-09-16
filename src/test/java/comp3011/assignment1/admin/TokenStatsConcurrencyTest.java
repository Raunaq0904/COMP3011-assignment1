package comp3011.assignment1.admin;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenStatsConcurrencyTest {

    @Test
    void recordUsageIsThreadSafeUnderConcurrentAccess() throws InterruptedException {
        TokenStats tokenStats = new TokenStats();
        int threadCount = 500;

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    tokenStats.recordUsage(1, 2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(threadCount, tokenStats.getInputTokens(),
                "Every input token increment should be counted exactly once, with none lost to a race condition.");
        assertEquals(threadCount * 2, tokenStats.getOutputTokens(),
                "Every output token increment should be counted exactly once, with none lost to a race condition.");
    }
}