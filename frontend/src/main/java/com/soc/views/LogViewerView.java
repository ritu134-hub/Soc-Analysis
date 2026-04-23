package com.soc.views;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.soc.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.concurrent.Executors;

/**
 * Log Viewer View
 * - Upload log files (syslog / CSV / JSON)
 * - Search & filter by keyword, severity
 * - Run threat analysis
 * - View results in a paginated table
 */
public class LogViewerView {

    private Label statusLabel;
    private TableView<LogRow> table;
    private ObservableList<LogRow> tableData;
    private TextField searchField;
    private ComboBox<String> severityFilter;
    private int currentPage = 1;
    private long totalLogs  = 0;

    public Node build() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0d1117;");

        // Header
        root.getChildren().add(buildHeader());

        // Toolbar: Upload + Search + Filter + Analyse + Clear
        root.getChildren().add(buildToolbar());

        // Status bar
        statusLabel = new Label("Ready. Upload a log file to begin.");
        statusLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        root.getChildren().add(statusLabel);

        // Table
        root.getChildren().add(buildTable());

        // Pagination
        root.getChildren().add(buildPagination());

        // Initial load
        loadLogs();

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #0d1117; -fx-background-color: #0d1117;");
        return sp;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private Node buildHeader() {
        VBox h = new VBox(4);
        Label title = new Label("Log Viewer");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");
        Label sub = new Label("Upload, search, and analyse security logs");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #8b949e;");
        h.getChildren().addAll(title, sub);
        return h;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private Node buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Upload
        Button upload = styledBtn("📂  Upload Log File", "#58a6ff", "#0d1117");
        upload.setOnAction(e -> uploadFile());

        // Search
        searchField = new TextField();
        searchField.setPromptText("🔍  Search logs...");
        searchField.setPrefWidth(240);
        searchField.setStyle(
            "-fx-background-color: #161b22; -fx-text-fill: #e6edf3; " +
            "-fx-prompt-text-fill: #8b949e; -fx-border-color: #30363d; " +
            "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;"
        );
        searchField.setOnAction(e -> { currentPage = 1; loadLogs(); });

        // Severity filter
        severityFilter = new ComboBox<>(
            FXCollections.observableArrayList("ALL","CRITICAL","HIGH","MEDIUM","LOW","INFO")
        );
        severityFilter.setValue("ALL");
        severityFilter.setStyle(
            "-fx-background-color: #161b22; -fx-text-fill: #e6edf3; " +
            "-fx-border-color: #30363d; -fx-border-radius: 6;"
        );
        severityFilter.setOnAction(e -> { currentPage = 1; loadLogs(); });

        Button search = styledBtn("Search", "#30363d", "#e6edf3");
        search.setOnAction(e -> { currentPage = 1; loadLogs(); });

        // Analyse
        Button analyse = styledBtn("⚡  Run Analysis", "#ff8c00", "#0d1117");
        analyse.setOnAction(e -> runAnalysis());

        // Live Monitoring Toggle
        Button liveToggle = styledBtn("📡  Live Monitoring: OFF", "#30363d", "#8b949e");
        liveToggle.setOnAction(e -> {
            boolean isOff = liveToggle.getText().contains("OFF");
            try {
                if (isOff) {
                    ApiClient.startLiveMonitor();
                    liveToggle.setText("📡  Live Monitoring: ON");
                    liveToggle.setStyle("-fx-background-color: #4caf50; -fx-text-fill: #0d1117; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 7; -fx-cursor: hand;");
                    setStatus("📡 Live monitoring started. Scanning System & Network logs...");
                } else {
                    ApiClient.stopLiveMonitor();
                    liveToggle.setText("📡  Live Monitoring: OFF");
                    liveToggle.setStyle("-fx-background-color: #30363d; -fx-text-fill: #8b949e; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 7; -fx-cursor: hand;");
                    setStatus("📡 Live monitoring stopped.");
                }
            } catch (Exception ex) {
                setStatus("❌ Live mode error: " + ex.getMessage());
            }
        });

        // Clear
        Button clear = styledBtn("🗑️  Clear Logs", "#ff4d4d55", "#ff4d4d");
        clear.setOnAction(e -> clearLogs());

        bar.getChildren().addAll(upload, searchField, severityFilter, search, analyse, liveToggle, clear);

        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────────
    private Node buildTable() {
        tableData = FXCollections.observableArrayList();
        table = new TableView<>(tableData);
        table.setPrefHeight(480);
        table.setStyle(
            "-fx-background-color: #161b22; -fx-border-color: #30363d; " +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );
        table.setPlaceholder(new Label("No logs to display.") {{ setStyle("-fx-text-fill: #8b949e;"); }});

        table.getColumns().addAll(
            col("Timestamp",  "timestamp",  160),
            severityCol("Severity",   "severity",    100),
            col("Category",   "category",   110),
            col("Source IP",  "sourceIp",   130),
            col("Message",    "message",    500)
        );

        // Color rows slightly by severity for better scannability
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LogRow item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    // Subtle background tint for high-risk rows
                    if ("CRITICAL".equals(item.getSeverity())) setStyle("-fx-background-color: #ff4d4d0a;");
                    else if ("HIGH".equals(item.getSeverity())) setStyle("-fx-background-color: #ff8c0008;");
                    else setStyle("");
                }
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private TableColumn<LogRow, String> col(String title, String field, double width) {
        TableColumn<LogRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(field));
        col.setPrefWidth(width);
        col.setStyle("-fx-alignment: CENTER-LEFT;");
        return col;
    }

    private TableColumn<LogRow, String> severityCol(String title, String field, double width) {
        TableColumn<LogRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(field));
        col.setPrefWidth(width);
        col.setStyle("-fx-alignment: CENTER;");

        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    String color = switch (item) {
                        case "CRITICAL" -> "#ff4d4d";
                        case "HIGH"     -> "#ff8c00";
                        case "MEDIUM"   -> "#ffd700";
                        case "LOW"      -> "#4caf50";
                        default         -> "#2196f3";
                    };
                    badge.setStyle(
                        "-fx-background-color: " + color + "22; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-weight: bold; -fx-font-size: 10px; " +
                        "-fx-padding: 2 8; -fx-background-radius: 4; " +
                        "-fx-border-color: " + color + "44; -fx-border-radius: 4;"
                    );
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        return col;
    }


    // ── Pagination ────────────────────────────────────────────────────────────
    private Label pageLabel;

    private Node buildPagination() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER);

        Button prev = styledBtn("◀  Prev", "#30363d", "#e6edf3");
        prev.setOnAction(e -> { if (currentPage > 1) { currentPage--; loadLogs(); } });

        pageLabel = new Label("Page 1");
        pageLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px;");

        Button next = styledBtn("Next  ▶", "#30363d", "#e6edf3");
        next.setOnAction(e -> { currentPage++; loadLogs(); });

        bar.getChildren().addAll(prev, pageLabel, next);
        return bar;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void uploadFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Log File");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Log Files", "*.log", "*.txt", "*.csv", "*.json"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fc.showOpenDialog(new Stage());
        if (file == null) return;

        setStatus("⏳ Uploading " + file.getName() + "...");
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String resp = ApiClient.uploadLogFile(file);
                JsonObject obj = ApiClient.parseJson(resp);
                long inserted = obj.get("inserted").getAsLong();
                Platform.runLater(() -> {
                    setStatus("✅ Uploaded " + file.getName() + " — " + inserted + " log entries imported.");
                    currentPage = 1;
                    loadLogs();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("❌ Upload failed: " + ex.getMessage()));
            }
        });
    }

    private void loadLogs() {
        String q   = searchField == null ? "" : searchField.getText();
        String sev = severityFilter == null ? "ALL" : severityFilter.getValue();

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String resp = ApiClient.searchLogs(q, sev, currentPage);
                JsonObject obj  = ApiClient.parseJson(resp);
                totalLogs       = obj.get("total").getAsLong();
                JsonArray logs  = obj.getAsJsonArray("logs");

                ObservableList<LogRow> rows = FXCollections.observableArrayList();
                for (var el : logs) {
                    JsonObject lg = el.getAsJsonObject();
                    rows.add(new LogRow(
                        safeStr(lg, "timestamp"),
                        safeStr(lg, "severity"),
                        safeStr(lg, "category"),
                        safeStr(lg, "source_ip"),
                        safeStr(lg, "message")
                    ));
                }

                Platform.runLater(() -> {
                    tableData.setAll(rows);
                    if (pageLabel != null)
                        pageLabel.setText("Page " + currentPage + " — " + totalLogs + " total");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("❌ Load error: " + ex.getMessage()));
            }
        });
    }

    private void runAnalysis() {
        setStatus("⚡ Running threat analysis...");
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String resp = ApiClient.runAnalysis();
                JsonObject obj = ApiClient.parseJson(resp);
                long found = obj.get("alerts_found").getAsLong();
                Platform.runLater(() ->
                    setStatus("✅ Analysis complete — " + found + " threat(s) detected. Check Dashboard.")
                );
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("❌ Analysis failed: " + ex.getMessage()));
            }
        });
    }

    private void clearLogs() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Clear ALL logs and alerts from the database?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm Clear");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Executors.newSingleThreadExecutor().submit(() -> {
                    try {
                        ApiClient.clearLogs();
                        Platform.runLater(() -> {
                            tableData.clear();
                            setStatus("🗑️ All logs cleared.");
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> setStatus("❌ Clear failed: " + ex.getMessage()));
                    }
                });
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private String safeStr(JsonObject obj, String key) {
        try { return obj.get(key).getAsString(); } catch (Exception e) { return "—"; }
    }

    private Button styledBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
            "-fx-font-weight: bold; -fx-padding: 8 16; " +
            "-fx-background-radius: 7; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        return b;
    }

    // ── Row Model ─────────────────────────────────────────────────────────────
    public static class LogRow {
        private final String timestamp, severity, category, sourceIp, message;

        public LogRow(String timestamp, String severity, String category,
                      String sourceIp, String message) {
            this.timestamp = timestamp;
            this.severity  = severity;
            this.category  = category;
            this.sourceIp  = sourceIp;
            this.message   = message;
        }

        public String getTimestamp() { return timestamp; }
        public String getSeverity()  { return severity;  }
        public String getCategory()  { return category;  }
        public String getSourceIp()  { return sourceIp;  }
        public String getMessage()   { return message;   }
    }
}
