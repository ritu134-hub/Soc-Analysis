package com.soc;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SOC Analysis — JavaFX Entry Point
 *
 * On startup:
 *   1. Show a splash/loading screen
 *   2. Launch the Python FastAPI backend as a subprocess
 *   3. Poll /health until the backend is ready
 *   4. Open the main application window
 *
 * On close:
 *   - Kill the Python process cleanly
 */
public class App extends Application {

    private static Process pythonProcess;
    private static int BACKEND_PORT = 8000;

    private static final String HEALTH_URL;

    static {
        // Try to read port from .env
        try {
            Path envPath = Paths.get(System.getProperty("user.dir")).getParent().resolve(".env");
            if (java.nio.file.Files.exists(envPath)) {
                for (String line : java.nio.file.Files.readAllLines(envPath)) {
                    if (line.startsWith("BACKEND_PORT=")) {
                        BACKEND_PORT = Integer.parseInt(line.split("=")[1].trim());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
        HEALTH_URL = "http://localhost:" + BACKEND_PORT + "/health";
    }


    // ── Splash Stage ─────────────────────────────────────────────────────────
    private Stage splashStage;

    @Override
    public void start(Stage primaryStage) {
        showSplash(primaryStage);
        Executors.newSingleThreadExecutor().submit(() -> {
            startPythonBackend();
            waitForBackend();
            Platform.runLater(() -> {
                try {
                    if (splashStage != null) splashStage.close();
                    new MainController().show(new Stage());
                } catch (Exception e) {
                    System.err.println("[Java] FATAL ERROR during UI startup:");
                    e.printStackTrace();
                }
            });
        });
    }

    // ── Splash Screen ─────────────────────────────────────────────────────────
    private void showSplash(Stage stage) {
        splashStage = stage;

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(60, 60);
        spinner.setStyle("-fx-progress-color: #58a6ff;");

        Label title = new Label("SOC Analysis");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label subtitle = new Label("Starting backend engine...");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #8b949e;");

        VBox root = new VBox(18, title, spinner, subtitle);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-width: 1;");

        Scene scene = new Scene(root, 420, 280);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setTitle("SOC Analysis — Loading");
        stage.show();
    }

    // ── Python Process ────────────────────────────────────────────────────────
    private void startPythonBackend() {
        try {
            // Locate backend directory relative to frontend
            Path backendDir = Paths.get(System.getProperty("user.dir"))
                    .getParent()
                    .resolve("backend");

            // Try 'python' first, fall back to 'python3'
            String pythonCmd = "python";

            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd, "-m", "uvicorn", "main:app",
                    "--host", "0.0.0.0",
                    "--port", String.valueOf(BACKEND_PORT),
                    "--no-access-log"
            );
            pb.directory(backendDir.toFile());
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONUNBUFFERED", "1");

            try {
                pythonProcess = pb.start();
            } catch (IOException e) {
                System.out.println("[Java] 'python' command failed, trying 'python3'...");
                pb.command("python3", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", String.valueOf(BACKEND_PORT), "--no-access-log");
                pythonProcess = pb.start();
            }


            // Stream Python stdout to Java console (for debugging)
            Thread reader = new Thread(() -> {
                try (var br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("[Python] " + line);
                    }
                } catch (IOException ignored) {}
            });
            reader.setDaemon(true);
            reader.start();

            System.out.println("[Java] Python backend process started (PID: " + pythonProcess.pid() + ")");
        } catch (IOException e) {
            System.err.println("[Java] Failed to start Python backend: " + e.getMessage());
        }
    }

    private void waitForBackend() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        System.out.println("[Java] Waiting for backend to be ready...");
        for (int i = 0; i < 30; i++) {   // up to 30 seconds
            try {
                Thread.sleep(1000);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(HEALTH_URL))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    System.out.println("[Java] Backend is ready!");
                    return;
                }
            } catch (Exception ignored) {}
        }
        System.err.println("[Java] Backend did not start in time.");
    }

    // ── Shutdown Hook ─────────────────────────────────────────────────────────
    @Override
    public void stop() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.out.println("[Java] Shutting down Python backend...");
            pythonProcess.descendants().forEach(ProcessHandle::destroy);
            pythonProcess.destroy();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
