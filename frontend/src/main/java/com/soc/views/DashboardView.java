package com.soc.views;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.soc.ApiClient;
import com.soc.UIUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;

import java.util.concurrent.Executors;

/**
 * Dashboard View — shows summary stats, severity distribution, top alerts
 */
public class DashboardView {

    public Node build() {
        ScrollablePane pane = new ScrollablePane();
        VBox root = pane.getContent();
        root.setSpacing(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0d1117;");

        // Header
        HBox header = buildHeader();

        // Stat cards row
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        Label[] statValues = new Label[4];
        VBox card1 = statCard("📋 Total Logs",    "—", "#58a6ff",  statValues, 0);
        VBox card2 = statCard("🚨 Threats Found", "—", "#ff4d4d",  statValues, 1);
        VBox card3 = statCard("⚠️ High Severity", "—", "#ff8c00",  statValues, 2);
        VBox card4 = statCard("📄 Reports",       "—", "#4caf50",  statValues, 3);
        
        statsRow.getChildren().addAll(card1, card2, card3, card4);
        
        // Entry Animations for Stats
        UIUtils.showEntryAnimation(card1, 100);
        UIUtils.showEntryAnimation(card2, 200);
        UIUtils.showEntryAnimation(card3, 300);
        UIUtils.showEntryAnimation(card4, 400);

        // Severity breakdown grid
        Label severityTitle = sectionLabel("Severity Breakdown");
        HBox severityGrid = new HBox(12);
        severityGrid.setAlignment(Pos.CENTER_LEFT);
        Label[] sevValues = new Label[5];
        VBox b1 = severityBadge("CRITICAL", "#ff4d4d", sevValues, 0);
        VBox b2 = severityBadge("HIGH",     "#ff8c00", sevValues, 1);
        VBox b3 = severityBadge("MEDIUM",   "#ffd700", sevValues, 2);
        VBox b4 = severityBadge("LOW",      "#4caf50", sevValues, 3);
        VBox b5 = severityBadge("INFO",     "#2196f3", sevValues, 4);

        severityGrid.getChildren().addAll(b1, b2, b3, b4, b5);
        
        // Entry Animations for Severity Badges
        UIUtils.showEntryAnimation(b1, 500);
        UIUtils.showEntryAnimation(b2, 550);
        UIUtils.showEntryAnimation(b3, 600);
        UIUtils.showEntryAnimation(b4, 650);
        UIUtils.showEntryAnimation(b5, 700);

        // Top IPs section
        Label ipsTitle = sectionLabel("Top Source IPs");
        VBox ipsBox = new VBox(6);
        ipsBox.setStyle("-fx-background-color: #161b22; -fx-background-radius: 10; -fx-padding: 16;");
        ipsBox.getChildren().add(new Label("Loading...") {{ setStyle("-fx-text-fill: #8b949e;"); }});

        // Recent alerts section
        Label alertsTitle = sectionLabel("Recent Alerts");
        VBox alertsBox = new VBox(6);
        alertsBox.setStyle("-fx-background-color: #161b22; -fx-background-radius: 10; -fx-padding: 16;");
        alertsBox.getChildren().add(new Label("Run analysis first to see alerts.") {{ setStyle("-fx-text-fill: #8b949e;"); }});

        // Refresh button
        Button refresh = actionButton("🔄  Refresh Stats", "#58a6ff");
        refresh.setOnAction(e -> loadData(statValues, sevValues, ipsBox, alertsBox));

        root.getChildren().addAll(
            header, statsRow,
            severityTitle, severityGrid,
            ipsTitle, ipsBox,
            alertsTitle, alertsBox,
            refresh
        );

        // Animate the rest of the sections
        UIUtils.showEntryAnimation(ipsTitle, 750);
        UIUtils.showEntryAnimation(ipsBox, 800);
        UIUtils.showEntryAnimation(alertsTitle, 850);
        UIUtils.showEntryAnimation(alertsBox, 900);
        UIUtils.showEntryAnimation(refresh, 950);

        // Auto-load on open
        loadData(statValues, sevValues, ipsBox, alertsBox);

        return pane.build();
    }

    private void loadData(Label[] statValues, Label[] sevValues, VBox ipsBox, VBox alertsBox) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String statsJson  = ApiClient.getStats();
                String alertsJson = ApiClient.getAlerts();

                JsonObject stats  = ApiClient.parseJson(statsJson);
                JsonObject alerts = ApiClient.parseJson(alertsJson);

                long totalLogs    = stats.get("total_logs").getAsLong();
                long totalAlerts  = stats.get("total_alerts").getAsLong();
                long totalReports = stats.get("total_reports").getAsLong();

                JsonObject bySev  = stats.getAsJsonObject("by_severity");
                long critical = safeGet(bySev, "CRITICAL");
                long high     = safeGet(bySev, "HIGH");
                long medium   = safeGet(bySev, "MEDIUM");
                long low      = safeGet(bySev, "LOW");
                long info     = safeGet(bySev, "INFO");

                JsonArray topIps     = stats.getAsJsonArray("top_ips");
                JsonArray alertsList = alerts.getAsJsonArray("alerts");

                Platform.runLater(() -> {
                    statValues[0].setText(String.valueOf(totalLogs));
                    statValues[1].setText(String.valueOf(totalAlerts));
                    statValues[2].setText(String.valueOf(critical + high));
                    statValues[3].setText(String.valueOf(totalReports));

                    sevValues[0].setText(String.valueOf(critical));
                    sevValues[1].setText(String.valueOf(high));
                    sevValues[2].setText(String.valueOf(medium));
                    sevValues[3].setText(String.valueOf(low));
                    sevValues[4].setText(String.valueOf(info));

                    // Top IPs
                    ipsBox.getChildren().clear();
                    if (topIps == null || topIps.isEmpty()) {
                        ipsBox.getChildren().add(noData("No log data yet. Upload a log file first."));
                    } else {
                        for (var el : topIps) {
                            JsonObject ip = el.getAsJsonObject();
                            ipsBox.getChildren().add(ipRow(
                                ip.get("source_ip").getAsString(),
                                ip.get("c").getAsLong(),
                                totalLogs
                            ));
                        }
                    }

                    // Alerts
                    alertsBox.getChildren().clear();
                    if (alertsList == null || alertsList.isEmpty()) {
                        alertsBox.getChildren().add(noData("No alerts. Go to Log Viewer → Run Analysis."));
                    } else {
                        int limit = Math.min(alertsList.size(), 5);
                        for (int i = 0; i < limit; i++) {
                            JsonObject a = alertsList.get(i).getAsJsonObject();
                            alertsBox.getChildren().add(alertRow(
                                a.get("rule_name").getAsString(),
                                a.get("severity").getAsString(),
                                a.get("description").getAsString()
                            ));
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> System.err.println("[Dashboard] Load error: " + e.getMessage()));
            }
        });
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        VBox text = new VBox(4);
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");
        Label sub = new Label("Real-time security overview");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #8b949e;");
        text.getChildren().addAll(title, sub);
        h.getChildren().add(text);
        return h;
    }

    private VBox statCard(String label, String value, String color, Label[] arr, int idx) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #161b22; -fx-background-radius: 12; " +
                      "-fx-border-color: #30363d; -fx-border-radius: 12; -fx-border-width: 1;");

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        arr[idx] = val;

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #8b949e;");

        card.getChildren().addAll(val, lbl);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox severityBadge(String name, String color, Label[] arr, int idx) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16));
        box.setPrefWidth(150);
        box.setStyle("-fx-background-color: #161b22; -fx-background-radius: 10; " +
                     "-fx-border-color: " + color + "55; -fx-border-radius: 10; -fx-border-width: 1;");

        Label cnt = new Label("—");
        cnt.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        arr[idx] = cnt;

        Label lbl = new Label(name);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

        box.getChildren().addAll(cnt, lbl);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private HBox ipRow(String ip, long count, long total) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label ipLabel = new Label(ip);
        ipLabel.setMinWidth(140);
        ipLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 12px;");

        double pct = total > 0 ? (count * 100.0 / total) : 0;
        StackPane barBg = new StackPane();
        barBg.setPrefHeight(8);
        HBox.setHgrow(barBg, Priority.ALWAYS);
        barBg.setStyle("-fx-background-color: #30363d; -fx-background-radius: 4;");
        HBox barFill = new HBox();
        barFill.setPrefWidth(pct * 2);
        barFill.setMaxWidth(pct * 2);
        barFill.setPrefHeight(8);
        barFill.setStyle("-fx-background-color: #58a6ff; -fx-background-radius: 4;");
        barBg.getChildren().add(barFill);
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);

        Label cntLabel = new Label(count + " logs");
        cntLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");
        cntLabel.setMinWidth(70);

        row.getChildren().addAll(ipLabel, barBg, cntLabel);
        return row;
    }

    private HBox alertRow(String rule, String severity, String desc) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 8;");

        String color = severityColor(severity);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px;");

        VBox text = new VBox(2);
        Label name = new Label(rule);
        name.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label d = new Label(desc);
        d.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");
        d.setWrapText(true);
        text.getChildren().addAll(name, d);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label sev = new Label(severity);
        sev.setStyle("-fx-background-color: " + color + "33; -fx-text-fill: " + color +
                     "; -fx-font-size: 10px; -fx-font-weight: bold; " +
                     "-fx-padding: 3 8 3 8; -fx-background-radius: 20;");

        row.getChildren().addAll(dot, text, sev);
        return row;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #8b949e; " +
                   "-fx-padding: 8 0 4 0;");
        return l;
    }

    private Label noData(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-padding: 8 0 8 0;");
        return l;
    }

    private Button actionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #0d1117; " +
                     "-fx-font-weight: bold; -fx-padding: 10 22 10 22; " +
                     "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px;");
        UIUtils.applyHoverAnimation(btn);
        return btn;
    }

    private String severityColor(String sev) {
        return switch (sev) {
            case "CRITICAL" -> "#ff4d4d";
            case "HIGH"     -> "#ff8c00";
            case "MEDIUM"   -> "#ffd700";
            case "LOW"      -> "#4caf50";
            default         -> "#2196f3";
        };
    }

    private long safeGet(JsonObject obj, String key) {
        try { return obj.get(key).getAsLong(); } catch (Exception e) { return 0L; }
    }
}
