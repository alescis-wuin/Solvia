package fr.seynax.solvia.desktop.backend;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class BackendLauncher {

    private volatile Process process;

    public CompletableFuture<LaunchResult> start() {
        return CompletableFuture.supplyAsync(this::startBlocking);
    }

    public boolean running() {
        return process != null && process.isAlive();
    }

    public void stop() {
        Process current = process;
        if (current != null && current.isAlive()) {
            current.destroy();
        }
    }

    private synchronized LaunchResult startBlocking() {
        if (running()) {
            return LaunchResult.success("Backend deja lance par Solvia.");
        }
        File root = projectRoot();
        if (root == null) {
            return LaunchResult.failure("Racine du projet introuvable. Demarrer le backend manuellement depuis le dossier du projet.");
        }
        try {
            ProcessBuilder builder = new ProcessBuilder("mvn", "-pl", "solvia-backend", "-am", "spring-boot:run");
            builder.directory(root);
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            process = builder.start();
            return LaunchResult.success("Demarrage du backend lance depuis " + root.getAbsolutePath());
        } catch (IOException exception) {
            return LaunchResult.failure("Impossible de demarrer le backend: " + exception.getMessage());
        }
    }

    private File projectRoot() {
        File cursor = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        while (cursor != null) {
            if (new File(cursor, "pom.xml").isFile() && new File(cursor, "solvia-backend").isDirectory()) {
                return cursor;
            }
            cursor = cursor.getParentFile();
        }
        return null;
    }

    public record LaunchResult(boolean success, String message) {
        static LaunchResult success(String message) {
            return new LaunchResult(true, message);
        }

        static LaunchResult failure(String message) {
            return new LaunchResult(false, message);
        }
    }
}
