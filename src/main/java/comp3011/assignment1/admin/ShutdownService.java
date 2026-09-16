package comp3011.assignment1.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ShutdownService {

    private final ApplicationContext applicationContext;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

    public ShutdownService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public boolean requestShutdown() {
        boolean alreadyShuttingDown = shutdownInProgress.getAndSet(true);
        if (alreadyShuttingDown) {
            return false;
        }

        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            performExit();
        });
        shutdownThread.setDaemon(false);
        shutdownThread.start();

        return true;
    }

    // Extracted so tests can override this one method to avoid actually
    // terminating the JVM that's running the test suite itself.
    protected void performExit() {
        System.exit(SpringApplication.exit(applicationContext, () -> 0));
    }
}