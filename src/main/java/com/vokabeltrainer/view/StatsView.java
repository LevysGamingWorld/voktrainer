package com.vokabeltrainer.view;

import com.vokabeltrainer.persistence.VocabStore;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Statistik-View mit Erfolgsquote, Box-Verteilung und Pro-Sprache Statistiken.
 */
public class StatsView extends VBox {

    private final VocabStore store;
    private String activeLanguage = null;

    public StatsView(VocabStore store) {
        this.store = store;
        getStyleClass().add("view-container");
        setSpacing(24);
        setPadding(new Insets(32, 36, 32, 36));
    }

    public void refresh() {
        getChildren().clear();

        Label title = new Label("📊 Statistik");
        title.getStyleClass().add("view-title");

        // ── Sprach-Tabs ──────────────────────────────────
        HBox langTabs = buildLanguageTabs();

        // ── Gesamt-Statistiken ───────────────────────────
        double rate   = activeLanguage == null ? store.getOverallSuccessRate() : store.getSuccessRate(activeLanguage);
        int total     = store.getTotalCount(activeLanguage);
        int mastered  = (int) store.getVocabsInBox(5, activeLanguage).size();
        int due       = store.getDueVocabs(activeLanguage).size();
        int streak    = store.getStreak();
        int practiced = store.getPracticedToday();

        // Großer Kreis-Indikator
        StackPane circlePane = createCircleIndicator(rate);

        // Info-Karten
        HBox infoCards = new HBox(12,
            infoCard("📚", String.valueOf(total), "Vokabeln"),
            infoCard("⭐", String.valueOf(mastered), "Gemeistert"),
            infoCard("📋", String.valueOf(due), "Fällig"),
            infoCard("🔥", streak + " T.", "Streak"),
            infoCard("✏️", String.valueOf(practiced), "Heute")
        );
        infoCards.setAlignment(Pos.CENTER);

        // ── Leitner-Box-Verteilung ───────────────────────
        VBox boxSection = new VBox(12);
        Label boxTitle = new Label("📦 Leitner-Boxen");
        boxTitle.getStyleClass().add("section-title");

        int[] boxes = store.getBoxDistribution(activeLanguage);
        HBox boxBars = new HBox(20);
        boxBars.setAlignment(Pos.BOTTOM_CENTER);
        boxBars.setPadding(new Insets(16, 0, 0, 0));

        String[] boxLabels = { "Neu\n1 Tag", "Bekannt\n3 Tage", "Vertraut\n7 Tage", "Sicher\n14 Tage", "Gemeistert\n30 Tage" };
        String[] boxColors = { "#ef4444", "#f97316", "#eab308", "#22c55e", "#3b82f6" };

        int maxBoxCount = 1;
        for (int b : boxes) maxBoxCount = Math.max(maxBoxCount, b);

        for (int i = 0; i < 5; i++) {
            boxBars.getChildren().add(createBarColumn(boxes[i], maxBoxCount, boxLabels[i], i + 1, boxColors[i]));
            HBox.setHgrow(boxBars.getChildren().getLast(), Priority.ALWAYS);
        }

        // Legende
        Label boxLegend = new Label("Box 1 = neue Vokabeln (täglich) → Box 5 = gemeistert (monatlich)");
        boxLegend.getStyleClass().add("text-muted");
        boxLegend.setStyle("-fx-font-size: 11px;");

        boxSection.getChildren().addAll(boxTitle, boxLegend, boxBars);

        // ── Pro-Sprache Übersicht ────────────────────────
        VBox langSection = buildLanguageOverview();

        // ── Tipps ────────────────────────────────────────
        VBox tipsSection = buildTips(total, due, rate, mastered, streak, practiced);

        // ── Zusammenbau ──────────────────────────────────
        VBox content = new VBox(24, title, langTabs, circlePane, infoCards, boxSection, langSection, tipsSection);
        content.setPadding(new Insets(0));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    // ── Sections ─────────────────────────────────────────

    private HBox buildLanguageTabs() {
        HBox tabs = new HBox(6);
        tabs.setAlignment(Pos.CENTER_LEFT);

        Button allBtn = new Button("🌍 Gesamt");
        allBtn.getStyleClass().add("lang-tab");
        if (activeLanguage == null) allBtn.getStyleClass().add("lang-tab-active");
        allBtn.setOnAction(e -> { activeLanguage = null; refresh(); });
        tabs.getChildren().add(allBtn);

        for (String lang : store.getLanguages()) {
            Button btn = new Button(lang);
            btn.getStyleClass().add("lang-tab");
            if (lang.equals(activeLanguage)) btn.getStyleClass().add("lang-tab-active");
            btn.setOnAction(e -> { activeLanguage = lang; refresh(); });
            tabs.getChildren().add(btn);
        }

        return tabs;
    }

    private VBox buildLanguageOverview() {
        List<String> languages = store.getLanguages();
        if (languages.size() <= 1) return new VBox(); // Keine Übersicht nötig

        Label title = new Label("🌍 Sprachen im Vergleich");
        title.getStyleClass().add("section-title");

        VBox rows = new VBox(8);
        for (String lang : languages) {
            int totalL   = store.getTotalCount(lang);
            int masteredL = store.getVocabsInBox(5, lang).size();
            int dueL     = store.getDueVocabs(lang).size();
            double rateL = store.getSuccessRate(lang);
            double progress = totalL == 0 ? 0 : (double) masteredL / totalL;

            Label langLabel = new Label(lang);
            langLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e5e5e5; -fx-font-size: 13px; -fx-min-width: 110px;");

            ProgressBar bar = new ProgressBar(progress);
            bar.getStyleClass().add("lang-progress-bar");
            bar.setPrefWidth(150);
            bar.setPrefHeight(8);
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label statsLabel = new Label(totalL + " • " + masteredL + " ⭐ • " + dueL + " fällig • " +
                Math.round(rateL * 100) + "% Erfolg");
            statsLabel.getStyleClass().add("text-muted");
            statsLabel.setStyle("-fx-font-size: 11px;");

            HBox row = new HBox(12, langLabel, bar, statsLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("lang-stat-row");
            row.setPadding(new Insets(10, 14, 10, 14));
            rows.getChildren().add(row);
        }

        return new VBox(10, title, rows);
    }

    private VBox buildTips(int total, int due, double rate, int mastered, int streak, int practiced) {
        Label tipsTitle = new Label("💡 Tipps");
        tipsTitle.getStyleClass().add("section-title");

        String tip;
        if (total == 0) {
            tip = "Füge deine ersten Vokabeln hinzu um loszulegen!";
        } else if (due > 15) {
            tip = "Du hast " + due + " fällige Vokabeln – eine Session hilft enorm!";
        } else if (rate < 0.4 && total > 5) {
            tip = "Tipp: Kürzere Sessions mit 5-10 Wörtern helfen beim Einprägen.";
        } else if (mastered > total / 2) {
            tip = "Über die Hälfte gemeistert! Füge neue Herausforderungen hinzu. 🌟";
        } else if (streak >= 7) {
            tip = "🔥 " + streak + " Tage Streak! Konsistenz ist der Schlüssel!";
        } else if (practiced == 0) {
            tip = "Heute noch nicht gelernt – 10 Minuten reichen für echten Fortschritt!";
        } else {
            tip = "Regelmäßiges Lernen (täglich 10 Min.) ist besser als einmalige Marathon-Sessions!";
        }

        Label tipLabel = new Label("→ " + tip);
        tipLabel.getStyleClass().add("tip-text");
        tipLabel.setWrapText(true);

        return new VBox(8, tipsTitle, tipLabel);
    }

    // ── Hilfs-Widgets ─────────────────────────────────────

    private StackPane createCircleIndicator(double rate) {
        int size = 180;
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = size / 2.0, cy = size / 2.0, r = 70;
        double lineWidth = 10;

        gc.setStroke(Color.web("#252525"));
        gc.setLineWidth(lineWidth);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);

        gc.setStroke(Color.web("#14b8a6"));
        gc.setLineWidth(lineWidth);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        double angle = rate * 360;
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -angle, javafx.scene.shape.ArcType.OPEN);

        gc.setFill(Color.web("#e5e5e5"));
        gc.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 28));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(Math.round(rate * 100) + "%", cx, cy + 4);

        gc.setFill(Color.web("#888888"));
        gc.setFont(Font.font("System", 12));
        gc.fillText("Erfolgsquote", cx, cy + 22);

        StackPane pane = new StackPane(canvas);
        pane.setPadding(new Insets(8));
        return pane;
    }

    private VBox createBarColumn(int count, int max, String label, int boxNum, String color) {
        double maxBarH = 100;
        double ratio = max == 0 ? 0 : (double) count / max;

        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().add("bar-count");

        Region bar = new Region();
        bar.setPrefHeight(Math.max(4, ratio * maxBarH));
        bar.setMinHeight(4);
        bar.setPrefWidth(50);
        bar.setMaxWidth(50);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4 4 0 0;");

        Label desc = new Label(label);
        desc.getStyleClass().add("bar-label");
        desc.setStyle("-fx-text-alignment: center; -fx-alignment: center;");

        VBox col = new VBox(4, countLabel, bar, desc);
        col.setAlignment(Pos.BOTTOM_CENTER);
        return col;
    }

    private VBox infoCard(String icon, String value, String label) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("info-value");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("info-label");

        VBox card = new VBox(4, iconLabel, valLabel, nameLabel);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("info-card");
        card.setPadding(new Insets(12, 10, 12, 10));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }
}
