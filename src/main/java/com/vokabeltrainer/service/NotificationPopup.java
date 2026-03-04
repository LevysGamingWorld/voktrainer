package com.vokabeltrainer.service;

import com.vokabeltrainer.persistence.VocabStore;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.*;
import javafx.util.Duration;

/**
 * Zeigt eine Benachrichtigungs-Popup mit Motivationssprüchen.
 * Erscheint oben rechts auf dem Bildschirm – immer im Vordergrund.
 */
public class NotificationPopup {

    private static final double POPUP_WIDTH  = 380;
    private static final double POPUP_HEIGHT = 130;
    private static final int    SHOW_SECONDS = 8;

    /**
     * Zeigt ein Reminder-Popup an.
     */
    public static void showReminder() {
        show(FunMessages.getReminder(), "📚 Lernzeit!", "#14b8a6");
    }

    /**
     * Zeigt eine benutzerdefinierte Nachricht an.
     */
    public static void show(String message, String title, String accentColor) {
        Platform.runLater(() -> createAndShow(message, title, accentColor));
    }

    /**
     * Zeigt eine Erfolgs-Nachricht (grün).
     */
    public static void showSuccess(String message) {
        show(message, "🎉 Super!", "#22c55e");
    }

    /**
     * Zeigt eine Info-Nachricht (blau).
     */
    public static void showInfo(String message) {
        show(message, "💡 Tipp", "#3b82f6");
    }

    private static void createAndShow(String message, String title, String accentColor) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.setAlwaysOnTop(true);

        // ── Layout ───────────────────────────────────────

        // Accent bar links
        Region accentBar = new Region();
        accentBar.setPrefWidth(5);
        accentBar.setMinWidth(5);
        accentBar.setMaxWidth(5);
        accentBar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 12 0 0 12;");

        // Icon
        Label icon = new Label("🔔");
        icon.setFont(Font.font(28));
        icon.setPadding(new Insets(0, 8, 0, 4));

        // Titel
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.WHITE);

        // Nachricht
        Label msgLabel = new Label(message);
        msgLabel.setFont(Font.font(13));
        msgLabel.setTextFill(Color.web("#a1a1aa"));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(POPUP_WIDTH - 80);

        // Fällige Vokabeln Zähler
        int dueCount = VocabStore.getInstance().getDueCount();
        Label dueLabel = new Label(dueCount + " Vokabeln fällig");
        dueLabel.setFont(Font.font(11));
        dueLabel.setTextFill(Color.web(accentColor));

        VBox textBox = new VBox(3, titleLabel, msgLabel, dueLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Schließen-Button
        Label closeBtn = new Label("✕");
        closeBtn.setFont(Font.font(16));
        closeBtn.setTextFill(Color.web("#71717a"));
        closeBtn.setCursor(javafx.scene.Cursor.HAND);
        closeBtn.setOnMouseClicked(e -> fadeOut(popup));
        closeBtn.setOnMouseEntered(e -> closeBtn.setTextFill(Color.WHITE));
        closeBtn.setOnMouseExited(e -> closeBtn.setTextFill(Color.web("#71717a")));
        closeBtn.setPadding(new Insets(4));

        HBox content = new HBox(10, icon, textBox, closeBtn);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(16, 16, 16, 12));

        HBox card = new HBox(0, accentBar, content);
        card.setStyle(
            "-fx-background-color: #1a1a1a;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #2a2a2a;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;"
        );
        card.setPrefWidth(POPUP_WIDTH);
        card.setPrefHeight(POPUP_HEIGHT);
        card.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.6)));

        // Click to open → zukünftig App in Vordergrund bringen
        card.setOnMouseClicked(e -> {
            if (!e.getTarget().equals(closeBtn)) {
                fadeOut(popup);
            }
        });

        // ── Scene + Position ─────────────────────────────

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(8));

        Scene scene = new Scene(root, POPUP_WIDTH + 16, POPUP_HEIGHT + 16);
        scene.setFill(Color.TRANSPARENT);

        popup.setScene(scene);

        // Oben rechts positionieren
        var screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        popup.setX(screen.getMaxX() - POPUP_WIDTH - 30);
        popup.setY(screen.getMinY() + 30);

        // ── Animation ────────────────────────────────────

        root.setOpacity(0);
        root.setTranslateY(-20);

        popup.show();

        // Slide-in
        Timeline slideIn = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(root.opacityProperty(), 0),
                new KeyValue(root.translateYProperty(), -20)
            ),
            new KeyFrame(Duration.millis(300),
                new KeyValue(root.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(root.translateYProperty(), 0, Interpolator.EASE_OUT)
            )
        );
        slideIn.play();

        // Auto-close nach SHOW_SECONDS
        PauseTransition pause = new PauseTransition(Duration.seconds(SHOW_SECONDS));
        pause.setOnFinished(e -> fadeOut(popup));
        pause.play();
    }

    private static void fadeOut(Stage popup) {
        if (!popup.isShowing()) return;
        var root = popup.getScene().getRoot();
        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(root.opacityProperty(), root.getOpacity()),
                new KeyValue(root.translateYProperty(), 0)
            ),
            new KeyFrame(Duration.millis(250),
                new KeyValue(root.opacityProperty(), 0, Interpolator.EASE_IN),
                new KeyValue(root.translateYProperty(), -15, Interpolator.EASE_IN)
            )
        );
        fadeOut.setOnFinished(e -> popup.close());
        fadeOut.play();
    }
}
