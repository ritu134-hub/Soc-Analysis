package com.soc.views;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.soc.ApiClient;
import com.soc.UIUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.util.concurrent.Executors;

/**
 * Reports View
 * - Generate a new HTML security report
 * - View list of all past reports
 * - Open any report in the default browser
 */
public class ReportsView {

    private VBox reportsList;
    private Label statusLabel;

    public Node build() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0d1117;");

        // Header
        root.getChildren().add(buildHeader());

        // Generate button + status
        HBox genBar = new HBox(14);
        genBar.setAlignment(Pos.CENTER_LEFT);

        Button generateBtn = new Button("📄  Generate New Report");
        generateBtn.setStyle(
            "-fx-background-color: #58a6ff; -fx-text-fill: #0d1117; " +
            "-fx-font-weight: bold; -fx-font-size: 14px; " +
            "-fx-padding: 12 24; -fx-background-radius: 9; -fx-cursor: hand;"
        );
        UIUtils.applyHoverAnimation(generateBtn);
        generateBtn.setOnAction(e -> generateReport());

        Button refreshBtn = new Button("🔄  Refresh List");
        refreshBtn.setStyle(
            "-fx-background-color: #30363d; -fx-text-fill: #e6edf3; " +
            "-fx-font-size: 13px; -fx-padding: 10 18; " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        UIUtils.applyHoverAnimation(refreshBtn);
        refreshBtn.setOnAction(e -> loadReports());

        genBar.getChildren().addAll(generateBtn, refreshBtn);
        root.getChildren().add(genBar);

        statusLabel = new Label("Click 'Generate New Report' to create a security analysis report.");
        statusLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        root.getChildren().add(statusLabel);

        // Info box
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(16));
        infoBox.setStyle("-fx-background-color: #58a6ff18; -fx-background-radius: 10; " +
                         "-fx-border-color: #58a6ff55; -fx-border-radius: 10; -fx-border-width: 1;");
        Label infoTitle = new Label("ℹ️  What's in a report?");
        infoTitle.setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label infoText  = new Label(
            "• Summary stats: total logs, threats, critical events\n" +
            "• Severity distribution chart\n" +
            "• Top source IPs bar chart\n" +
            "• Full alerts/threats table\n" +
            "• Last 50 log entries\n" +
            "\nReports are saved as HTML files and open in your default browser."
        );
        infoText.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        infoText.setWrapText(true);
        infoBox.getChildren().addAll(infoTitle, infoText);
        root.getChildren().add(infoBox);

        // Reports list section
        Label listTitle = new Label("Generated Reports");
        listTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");
        root.getChildren().add(listTitle);

        reportsList = new VBox(10);
        root.getChildren().add(reportsList);

        // Load existing reports
        loadReports();

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #0d1117; -fx-background-color: #0d1117;");
        return sp;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private Node buildHeader() {
        VBox h = new VBox(4);
        Label title = new Label("Reports");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");
        Label sub = new Label("Generate and view HTML security analysis reports");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #8b949e;");
        h.getChildren().addAll(title, sub);
        return h;
    }

    // ── Load Reports ──────────────────────────────────────────────────────────
    private void loadReports() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String resp = ApiClient.listReports();
                JsonObject obj = ApiClient.parseJson(resp);
                JsonArray reports = obj.getAsJsonArray("reports");

                Platform.runLater(() -> {
                    reportsList.getChildren().clear();
                    if (reports == null || reports.isEmpty()) {
                        Label none = new Label("No reports yet. Generate your first report above.");
                        none.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
                        reportsList.getChildren().add(none);
                    } else {
                        int index = 0;
                        for (var el : reports) {
                            JsonObject r = el.getAsJsonObject();
                            Node card = reportCard(r);
                            reportsList.getChildren().add(card);
                            
                            // Staggered entry animation for cards
                            UIUtils.showEntryAnimation(card, 100 + (index * 80));
                            index++;
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("❌ Error loading reports: " + e.getMessage()));
            }
        });
    }

    // ── Generate Report ───────────────────────────────────────────────────────
    private void generateReport() {
        setStatus("⏳ Generating report...");
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String resp = ApiClient.generateReport();
                JsonObject obj = ApiClient.parseJson(resp);
                String path = obj.get("path").getAsString();
                long logs   = obj.get("log_count").getAsLong();
                long alerts = obj.get("alert_count").getAsLong();

                Platform.runLater(() -> {
                    setStatus("✅ Report generated — " + logs + " logs, " + alerts + " alerts.");
                    loadReports();
                    // Auto-open in browser
                    openInBrowser(path);
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("❌ Report generation failed: " + e.getMessage()));
            }
        });
    }

    // ── Report Card ───────────────────────────────────────────────────────────
    private Node reportCard(JsonObject r) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
            "-fx-background-color: #161b22; -fx-background-radius: 10; " +
            "-fx-border-color: #30363d; -fx-border-radius: 10; -fx-border-width: 1;"
        );

        // Icon
        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 28px;");

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        String filename = safeStr(r, "filename");
        Label name = new Label(filename);
        name.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 13px; -fx-font-weight: bold;");

        long logCount   = safeLong(r, "log_count");
        long alertCount = safeLong(r, "alert_count");
        String genAt    = safeStr(r, "generated_at");

        Label meta = new Label(
            "Generated: " + genAt + "   |   " +
            logCount + " logs   |   " + alertCount + " alerts"
        );
        meta.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        info.getChildren().addAll(name, meta);

        // Badge
        Label badge = alertCount > 0
            ? badge("⚠ " + alertCount + " alerts", "#ff8c00")
            : badge("✅ Clean", "#4caf50");

        // Open button
        int reportId = safeLong(r, "id") > 0 ? (int) safeLong(r, "id") : -1;
        Button openBtn = new Button("🌐  Open");
        openBtn.setStyle(
            "-fx-background-color: #58a6ff22; -fx-text-fill: #58a6ff; " +
            "-fx-font-weight: bold; -fx-padding: 7 16; " +
            "-fx-background-radius: 7; -fx-cursor: hand;"
        );
        UIUtils.applyHoverAnimation(openBtn);
        openBtn.setOnAction(e -> openReport(reportId));

        card.getChildren().addAll(icon, info, badge, openBtn);
        return card;
    }

    private void openReport(int reportId) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String resp = ApiClient.getReportPath(reportId);
                JsonObject obj = ApiClient.parseJson(resp);
                String path = obj.get("path").getAsString();
                Platform.runLater(() -> openInBrowser(path));
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("❌ Cannot open report: " + e.getMessage()));
            }
        });
    }

    private void openInBrowser(String path) {
        try {
            Desktop.getDesktop().browse(new File(path).toURI());
        } catch (Exception ex) {
            setStatus("❌ Cannot open browser: " + ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle(
            "-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; " +
            "-fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-padding: 4 10; -fx-background-radius: 20;"
        );
        return l;
    }

    private String safeStr(JsonObject obj, String key) {
        try { return obj.get(key).getAsString(); } catch (Exception e) { return "—"; }
    }

    private long safeLong(JsonObject obj, String key) {
        try { return obj.get(key).getAsLong(); } catch (Exception e) { return 0L; }
    }
}
