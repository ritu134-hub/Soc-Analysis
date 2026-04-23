package com.soc;

import com.soc.views.DashboardView;
import com.soc.views.LogViewerView;
import com.soc.views.ReportsView;
import com.soc.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Main application window controller.
 * Provides the sidebar navigation and content area.
 */
public class MainController {

    private BorderPane root;
    private StackPane contentArea;

    private DashboardView dashboardView;
    private LogViewerView logViewerView;
    private ReportsView reportsView;

    private Node dashboardNode;
    private Node logViewerNode;
    private Node reportsNode;

    public void show(Stage stage) {
        root = new BorderPane();
        root.setLeft(buildSidebar());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #0d1117;");
        root.setCenter(contentArea);

        // Start on Dashboard
        navigateTo(0);

        Scene scene = new Scene(root, 1280, 800);
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/styles.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("[Java] Warning: Could not load styles.css. Proceeding with default styles.");
        }


        stage.setTitle("🛡️ SOC Analysis — Security Operations Center");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(220);
        sidebar.setStyle(
                "-fx-background-color: #0d1117;" +
                "-fx-border-color: #30363d;" +
                "-fx-border-width: 0 1 0 0;"
        );
        sidebar.setPadding(new Insets(0, 0, 20, 0));

        // Logo
        VBox logo = new VBox(4);
        logo.setAlignment(Pos.CENTER);
        logo.setPadding(new Insets(24, 16, 20, 16));
        logo.setStyle("-fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        Label icon = new Label("🛡️");
        icon.setStyle("-fx-font-size: 36px;");
        Label appName = new Label("SOC Analysis");
        appName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
        Label tagline = new Label("Security Operations");
        tagline.setStyle("-fx-font-size: 10px; -fx-text-fill: #8b949e; -fx-letter-spacing: 1;");
        logo.getChildren().addAll(icon, appName, tagline);

        // Nav buttons
        Button[] buttons = {
                navButton("📊  Dashboard",  0),
                navButton("📋  Log Viewer", 1),
                navButton("📄  Reports",    2),
        };

        // Status label at bottom
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label("● Backend Connected");
        statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px; -fx-padding: 0 0 0 16;");

        sidebar.getChildren().add(logo);
        for (Button b : buttons) sidebar.getChildren().add(b);
        sidebar.getChildren().addAll(spacer, statusLabel);

        return sidebar;
    }

    private Button navButton(String text, int index) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        
        // Base Style
        btn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #8b949e; " +
            "-fx-font-size: 13px; -fx-padding: 12 20; -fx-alignment: CENTER-LEFT; " +
            "-fx-cursor: hand;"
        );

        // Transition colors on hover manually since we are using inline styles
        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: #161b22; -fx-text-fill: #e6edf3; " +
                "-fx-font-size: 13px; -fx-padding: 12 20; -fx-alignment: CENTER-LEFT; " +
                "-fx-cursor: hand;"
            );
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8b949e; " +
                "-fx-font-size: 13px; -fx-padding: 12 20; -fx-alignment: CENTER-LEFT; " +
                "-fx-cursor: hand;"
            );
        });

        // Apply smooth scale animation
        UIUtils.applyHoverAnimation(btn);
        
        btn.setOnAction(e -> navigateTo(index));
        return btn;
    }

    private void navigateTo(int index) {
        switch (index) {
            case 0 -> {
                if (dashboardView == null) dashboardView = new DashboardView();
                if (dashboardNode == null) dashboardNode = dashboardView.build();
                showView(dashboardNode);
            }
            case 1 -> {
                if (logViewerView == null) logViewerView = new LogViewerView();
                if (logViewerNode == null) logViewerNode = logViewerView.build();
                showView(logViewerNode);
            }
            case 2 -> {
                if (reportsView == null) reportsView = new ReportsView();
                if (reportsNode == null) reportsNode = reportsView.build();
                showView(reportsNode);
            }
        }
    }

    private void showView(Node view) {
        contentArea.getChildren().setAll(view);
    }
}
