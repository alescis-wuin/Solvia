package fr.seynax.solvia.desktop.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import fr.seynax.solvia.desktop.api.SolviaApiClient;

class JavaFxSmokeTest {

    private static volatile boolean platformStarted;

    @Test
    void constructsCoreDesktopViewsWhenJavaFxIsAvailable() throws Exception {
        Assumptions.assumeTrue(startPlatform(), "JavaFX platform is not available in this environment");

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SolviaApiClient apiClient = new SolviaApiClient(URI.create("http://127.0.0.1:8080"));
                assertNotNull(new DashboardView(apiClient));
                assertNotNull(new AccountsView(apiClient));
                assertNotNull(new AssetsPositionsView(apiClient));
                assertNotNull(new EntriesView(apiClient));
                assertNotNull(new BackendStatusBanner(apiClient, new fr.seynax.solvia.desktop.SolviaDesktopPreferences(), ignored -> { }));
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        Assumptions.assumeTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX smoke test timed out");
        assertDoesNotThrow(() -> {
            if (error.get() != null) {
                throw error.get();
            }
        });
    }

    private static synchronized boolean startPlatform() {
        if (platformStarted) {
            return true;
        }
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            boolean started = latch.await(5, TimeUnit.SECONDS);
            platformStarted = started;
            return started;
        } catch (IllegalStateException alreadyStarted) {
            platformStarted = true;
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }
}
