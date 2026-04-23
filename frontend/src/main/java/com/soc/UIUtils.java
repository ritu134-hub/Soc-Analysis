package com.soc;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Utility class for UI enhancements like animations.
 */
public class UIUtils {

    /**
     * Applies a smooth scale-up animation on hover and scale-down on click.
     * Includes a subtle glow effect on hover.
     * @param btn The button to animate
     */
    public static void applyHoverAnimation(Button btn) {
        // Glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#58a6ff", 0.4));
        glow.setRadius(0);
        glow.setSpread(0);
        btn.setEffect(glow);

        // Animation for Hover Enter
        ScaleTransition hoverIn = new ScaleTransition(Duration.millis(150), btn);
        hoverIn.setToX(1.05);
        hoverIn.setToY(1.05);
        
        Timeline glowIn = new Timeline(
            new KeyFrame(Duration.millis(150), new KeyValue(glow.radiusProperty(), 12))
        );

        // Animation for Hover Exit
        ScaleTransition hoverOut = new ScaleTransition(Duration.millis(150), btn);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);
        
        Timeline glowOut = new Timeline(
            new KeyFrame(Duration.millis(150), new KeyValue(glow.radiusProperty(), 0))
        );

        btn.setOnMouseEntered(e -> {
            hoverOut.stop();
            glowOut.stop();
            hoverIn.playFromStart();
            glowIn.playFromStart();
        });

        btn.setOnMouseExited(e -> {
            hoverIn.stop();
            glowIn.stop();
            hoverOut.playFromStart();
            glowOut.playFromStart();
        });

        // Instant scale for click feedback
        btn.setOnMousePressed(e -> {
            btn.setScaleX(0.96);
            btn.setScaleY(0.96);
        });

        btn.setOnMouseReleased(e -> {
            if (btn.isHover()) {
                btn.setScaleX(1.05);
                btn.setScaleY(1.05);
            } else {
                btn.setScaleX(1.0);
                btn.setScaleY(1.0);
            }
        });
    }

    /**
     * Creates a continuous pulse animation on a node.
     * @param node The node to animate
     * @return The animation object so it can be controlled (stopped)
     */
    public static Animation createPulseAnimation(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(1000), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.03);
        st.setToY(1.03);
        st.setCycleCount(Animation.INDEFINITE);
        st.setAutoReverse(true);
        return st;
    }

    /**
     * Shows a smooth entry animation (fade + slide up).
     * @param node The node to animate
     * @param delayMs Delay before starting the animation
     */
    public static void showEntryAnimation(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(20);

        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), node);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /**
     * Shows a premium toast notification at the bottom center of the window.
     * @param owner The node used to find the window
     * @param message The message to display
     * @param bgColor Hex color for background
     */
    public static void showToast(Node owner, String message, String bgColor) {
        if (owner == null || owner.getScene() == null) return;

        javafx.stage.Window window = owner.getScene().getWindow();
        javafx.stage.Popup popup = new javafx.stage.Popup();

        Label label = new Label(message);
        label.setStyle(
            "-fx-background-color: " + bgColor + "ee; " +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
            "-fx-padding: 12 24; -fx-background-radius: 30; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);"
        );
        label.setMinWidth(200);
        label.setAlignment(Pos.CENTER);

        popup.getContent().add(label);

        // Position at bottom center
        popup.setOnShown(e -> {
            popup.setX(window.getX() + window.getWidth() / 2 - label.getWidth() / 2);
            popup.setY(window.getY() + window.getHeight() - 80);
        });

        // Entry animation
        label.setOpacity(0);
        label.setTranslateY(20);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), label);
        fadeIn.setToValue(1.0);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), label);
        slideUp.setToY(0);

        // Exit animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), label);
        fadeOut.setDelay(Duration.millis(2500));
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());

        popup.show(window);
        new ParallelTransition(fadeIn, slideUp, fadeOut).play();
    }
}
