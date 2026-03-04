package com.vokabeltrainer.view;

import com.vokabeltrainer.controller.MainController;
import com.vokabeltrainer.model.Vocabulary;
import com.vokabeltrainer.persistence.VocabStore;
import com.vokabeltrainer.service.FunMessages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard – Übersicht mit Statistik-Karten, Begrüßung, Sprach-Übersicht,
 * Leitner-Box-Erklärung, Tagesziel und fälligen Vokabeln.
 */
public class DashboardView extends VBox {

    private final VocabStore store;
    private final MainController controller;

    public DashboardView(VocabStore store, MainController controller) {
        this.store = store;
        this.controller = controller;
        getStyleClass().add("view-container");
        setSpacing(24);
        setPadding(new Insets(32, 36, 32, 36));
    }

    public void refresh() {
        getChildren().clear();

        // ── Begrüßung ────────────────────────────────────
        Label greeting = new Label(FunMessages.getDashboardGreeting());
        greeting.getStyleClass().add("view-title");

        int dueCount = store.getDueCount();
        String subText = dueCount > 0
            ? dueCount + " Vokabeln warten auf Wiederhholung – viel Spaß"
            : "Alles aufgearbeitet – klasse!";
        Label sub = new Label(subText);
        sub.getStyleClass().add("view-subtitle");

        VBox header = new VBox(4, greeting, sub);
        header.setPadding(new Insets(0, 0, 8, 0));

        // ── Stat-Karten ──────────────────────────────────
        int streak = store.getStreak();
        HBox stats = new HBox(16,
            statCard(String.valueOf(store.getTotalCount()), "Vokabeln", "📚"),
            statCard(String.valueOf(store.getPracticedToday()), "Heute geübt", "✏️"),
            statCard(formatPercent(store.getOverallSuccessRate()), "Erfolgsquote", "🎯"),
            statCard(streak + (streak == 1 ? " Tag" : " Tage"), "Streak", "📅")
        );
        stats.setAlignment(Pos.CENTER_LEFT);

        // ── Tagesziel-Fortschritt ─────────────────────────
        VocabStore.Settings settings = store.getSettings();
        VBox goalSection = buildGoalSection(settings);

        // ── Jetzt lernen ─────────────────────────────────
        Button learnBtn = new Button("🧠Jetzt lernen" +
            (dueCount > 0 ? "  (" + dueCount + " fällig)" : ""));
        learnBtn.getStyleClass().add("accent-button");
        learnBtn.setMaxWidth(Double.MAX_VALUE);
        learnBtn.setOnAction(e -> controller.navigateTo(MainController.View.LEARN));

        // ── Sprachen ─────────────────────────────────────
        VBox langSection = buildLanguageSection();

        // ── Leitner-Boxen ─────────────────────────────────
        VBox boxSection = buildBoxSection();

        // ── Fällige Vokabeln ─────────────────────────────
        VBox dueSection = buildDueSection();

        // ── ScrollPane ───────────────────────────────────
        VBox content = new VBox(24, header, stats, goalSection, learnBtn, langSection, boxSection, dueSection);
        content.setPadding(new Insets(0));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    // ── Sections ──────────────────────────────────────────

    private VBox buildGoalSection(VocabStore.Settings settings) {
        int practiced  = store.getPracticedToday();
        int goal       = settings.dailyGoal;
        double progress = goal <= 0 ? 1.0 : Math.min(1.0, (double) practiced / goal);

        Label title = new Label("Tagesziel: " + practiced + " / " + goal + " Vokabeln");
        title.getStyleClass().add("section-title");

        ProgressBar bar = new ProgressBar(progress);
        bar.getStyleClass().add("goal-progress-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(12);

        String goalMsg = progress >= 1.0
            ? FunMessages.getGoalReached()
            : "Noch " + Math.max(0, goal - practiced) + " Vokabeln bis zum Tagesziel!";
        Label goalLabel = new Label(goalMsg);
        goalLabel.getStyleClass().add("text-muted");
        goalLabel.setStyle("-fx-font-size: 12px;");

        VBox section = new VBox(6, title, bar, goalLabel);
        section.getStyleClass().add("goal-section");
        section.setPadding(new Insets(14));
        return section;
    }

    private VBox buildLanguageSection() {
        Label title = new Label("Meine Sprachen");
        title.getStyleClass().add("section-title");

        Map<String, Long> counts = store.getLanguageCounts();
        if (counts.isEmpty()) {
            Label empty = new Label("Noch keine Sprachen vorhanden.");
            empty.getStyleClass().add("text-muted");
            return new VBox(8, title, empty);
        }

        HBox langCards = new HBox(12);
        langCards.setAlignment(Pos.CENTER_LEFT);
        counts.forEach((lang, count) -> {
            int due = store.getDueVocabs(lang).size();
            VBox card = languageCard(lang, count, due);
            langCards.getChildren().add(card);
        });

        return new VBox(10, title, langCards);
    }

    private VBox buildBoxSection() {
        Label title = new Label("Das Leitner-System – Wie es funktioniert");
        title.getStyleClass().add("section-title");

        Label explanation = new Label(
            "Das Leitner-System verteilt Vokabeln auf 5 Boxen. " +
            "Richtige Antworten → nächste Box (seltener wiederholt). " +
            "Falsche Antworten → zurück zu Box 1."
        );
        explanation.getStyleClass().add("text-muted");
        explanation.setWrapText(true);
        explanation.setStyle("-fx-font-size: 12px;");

        // Box-Balken mit Beschreibung
        int[] boxes = store.getBoxDistribution();
        HBox boxRow = new HBox(12);
        boxRow.setAlignment(Pos.CENTER_LEFT);

        String[] boxColors = {"#ef4444", "#f97316", "#eab308", "#22c55e", "#3b82f6"};
        for (int i = 0; i < 5; i++) {
            boxRow.getChildren().add(boxCard(i + 1, boxes[i], boxColors[i]));
        }

        return new VBox(10, title, explanation, boxRow);
    }

    private VBox buildDueSection() {
        List<Vocabulary> dueVocabs = store.getDueVocabs();

        Label dueTitle = new Label("Fällig zur Wiederholung (" + dueVocabs.size() + ")");
        dueTitle.getStyleClass().add("section-title");

        VBox dueList = new VBox(6);
        if (dueVocabs.isEmpty()) {
            Label empty = new Label("Keine Vokabeln fällig – alles top!");
            empty.getStyleClass().add("text-muted");
            dueList.getChildren().add(empty);
        } else {
            dueVocabs.stream().limit(10).forEach(v -> dueList.getChildren().add(createDueCard(v)));
            if (dueVocabs.size() > 10) {
                Label more = new Label("… und " + (dueVocabs.size() - 10) + " weitere");
                more.getStyleClass().add("text-muted");
                dueList.getChildren().add(more);
            }
        }

        return new VBox(8, dueTitle, dueList);
    }

    // ── Hilfs-Widgets ────────────────────────────────────

    private VBox statCard(String value, String label, String icon) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("stat-value");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("stat-label");

        VBox card = new VBox(6, iconLabel, valLabel, nameLabel);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(160);
        card.setMinWidth(120);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox languageCard(String lang, long total, int due) {
        Label langLabel = new Label(lang);
        langLabel.getStyleClass().add("lang-card-title");

        Label totalLabel = new Label(total + " Vokabeln");
        totalLabel.getStyleClass().add("text-muted");
        totalLabel.setStyle("-fx-font-size: 11px;");

        Label dueLabel = new Label(due > 0 ? + due + " fällig" : "Alles aktuell");
        dueLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (due > 0 ? "#14b8a6" : "#22c55e") + ";");

        VBox card = new VBox(4, langLabel, totalLabel, dueLabel);
        card.getStyleClass().add("lang-card");
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setMinWidth(120);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> controller.navigateTo(MainController.View.LEARN));
        return card;
    }

    private VBox boxCard(int boxNum, int count, String color) {
        String[] names      = Vocabulary.BOX_NAMES;
        String[] intervals  = Vocabulary.BOX_INTERVALS;

        Label numLabel = new Label("Box " + boxNum);
        numLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-size: 13px;");

        Label nameLabel = new Label(names[boxNum - 1]);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e5e5e5;");

        Label intervalLabel = new Label(intervals[boxNum - 1]);
        intervalLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        Label countLabel = new Label(count + " Vokabeln");
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");

        Region bar = new Region();
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");
        bar.setPrefHeight(3);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setOpacity(0.4);

        VBox card = new VBox(3, numLabel, nameLabel, intervalLabel, bar, countLabel);
        card.getStyleClass().add("box-info-card");
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setMinWidth(110);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox createDueCard(Vocabulary v) {
        Label word = new Label(v.getWord());
        word.getStyleClass().add("vocab-word");
        word.setMinWidth(120);

        Label arrow = new Label("→");
        arrow.getStyleClass().add("text-muted");

        Label trans = new Label(v.getTranslation());
        trans.getStyleClass().add("vocab-translation");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label langTag = new Label(v.getLanguage());
        langTag.getStyleClass().add("cat-tag");

        Label box = new Label(Vocabulary.BOX_NAMES[v.getBox() - 1]);
        box.getStyleClass().addAll("box-badge", "box-" + v.getBox());

        HBox card = new HBox(10, word, arrow, trans, spacer, langTag, box);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("due-card");
        card.setPadding(new Insets(10, 14, 10, 14));
        return card;
    }

    private String formatPercent(double rate) {
        return Math.round(rate * 100) + "%";
    }
}

