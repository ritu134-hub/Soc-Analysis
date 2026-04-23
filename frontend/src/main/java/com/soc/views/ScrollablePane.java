package com.soc.views;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Helper wrapper that puts a VBox into a styled ScrollPane.
 */
public class ScrollablePane {

    private final VBox content = new VBox();

    public VBox getContent() {
        return content;
    }

    public Node build() {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background: #0d1117; -fx-background-color: #0d1117;");
        return sp;
    }
}
