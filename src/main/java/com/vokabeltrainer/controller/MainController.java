package com.vokabeltrainer.controller;

import com.vokabeltrainer.persistence.VocabStore;
import com.vokabeltrainer.service.ReminderService;
import com.vokabeltrainer.view.*;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Haupt-Controller: erstellt Sidebar-Navigation und wechselt
 * mit Fade-Animation zwischen den Views.
 */
public class MainController {

    public enum View { DASHBOARD, VOCAB_LIST, LEARN, STATS, SETTINGS }

    private final HBox root = new HBox();
    private final StackPane contentArea = new StackPane();
    private final VocabStore store = VocabStore.getInstance();

    // Views (lazy creation)
    private DashboardView dashboardView;
    private VocabListView vocabListView;
    private LearnView learnView;
    private StatsView statsView;
    private SettingsView settingsView;

    private final Map<View, Button> navButtons = new LinkedHashMap<>();
    private View currentView;

    public MainController() {
        root.getStyleClass().add("root-container");

        VBox sidebar = createSidebar();
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        contentArea.getStyleClass().add("content-area");

        root.getChildren().addAll(sidebar, contentArea);
        navigateTo(View.DASHBOARD);

        // Erinnerungs-Dienst starten
        ReminderService.getInstance().start();
    }

    public HBox getRoot() {
        return root;
    }

    // ── Navigation ───────────────────────────────────────

    public void navigateTo(View view) {
        if (view == currentView) return;
        currentView = view;

        navButtons.forEach((v, btn) -> {
            btn.getStyleClass().remove("nav-active");
            if (v == view) btn.getStyleClass().add("nav-active");
        });

        Region target = switch (view) {
            case DASHBOARD  -> getDashboard();
            case VOCAB_LIST -> getVocabList();
            case LEARN      -> getLearn();
            case STATS      -> getStats();
            case SETTINGS   -> getSettings();
        };

        FadeTransition fade = new FadeTransition(Duration.millis(180), target);
        fade.setFromValue(0);
        fade.setToValue(1);

        contentArea.getChildren().setAll(target);
        fade.play();
    }

    // ── Sidebar ──────────────────────────────────────────

    private VBox createSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(220);
        sidebar.setAlignment(Pos.TOP_LEFT);

        Label logo = new Label("📖");
        logo.setStyle("-fx-font-size: 28px; -fx-padding: 0;");

        Label title = new Label("VokabelTrainer");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Spaced Repetition");
        subtitle.getStyleClass().add("app-subtitle");

        VBox header = new VBox(2, logo, title, subtitle);
        header.setPadding(new Insets(28, 20, 24, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        Button dashBtn     = createNavButton("🏠   Dashboard",  View.DASHBOARD);
        Button vocabBtn    = createNavButton("📚   Vokabeln",   View.VOCAB_LIST);
        Button learnBtn    = createNavButton("🧠   Lernen",     View.LEARN);
        Button statsBtn    = createNavButton("📊   Statistik",  View.STATS);

        VBox nav = new VBox(2, dashBtn, vocabBtn, learnBtn, statsBtn);
        nav.setPadding(new Insets(0, 8, 0, 8));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Einstellungen unten
        Button settingsBtn = createNavButton("⚙️   Einstellungen", View.SETTINGS);
        settingsBtn.getStyleClass().add("nav-button-settings");
        VBox settingsNav = new VBox(2, settingsBtn);
        settingsNav.setPadding(new Insets(0, 8, 8, 8));

        Label footer = new Label("v2.0 • Leitner-System");
        footer.getStyleClass().add("sidebar-footer");
        footer.setPadding(new Insets(0, 0, 4, 20));

        sidebar.getChildren().addAll(header, nav, spacer, settingsNav, footer);
        return sidebar;
    }

    private Button createNavButton(String text, View view) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> navigateTo(view));
        navButtons.put(view, btn);
        return btn;
    }

    // ── View Getters (lazy + refresh) ────────────────────

    private DashboardView getDashboard() {
        if (dashboardView == null) dashboardView = new DashboardView(store, this);
        dashboardView.refresh();
        return dashboardView;
    }

    private VocabListView getVocabList() {
        if (vocabListView == null) vocabListView = new VocabListView(store);
        vocabListView.refresh();
        return vocabListView;
    }

    private LearnView getLearn() {
        if (learnView == null) learnView = new LearnView(store, this);
        learnView.refresh();
        return learnView;
    }

    private StatsView getStats() {
        if (statsView == null) statsView = new StatsView(store);
        statsView.refresh();
        return statsView;
    }

    private SettingsView getSettings() {
        if (settingsView == null) settingsView = new SettingsView(store);
        settingsView.refresh();
        return settingsView;
    }
}
