package com.vokabeltrainer.view;

import com.vokabeltrainer.controller.MainController;
import com.vokabeltrainer.model.Vocabulary;
import com.vokabeltrainer.persistence.VocabStore;
import com.vokabeltrainer.service.FunMessages;
import com.vokabeltrainer.service.NotificationPopup;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lern-View mit 3 Modi: Karteikarten, Multiple Choice, Eingabe.
 * Implementiert Spaced Repetition (Leitner-System) mit Sprach-Filter.
 */
public class LearnView extends VBox {

    private enum Mode { FLASHCARD, MULTIPLE_CHOICE, TYPE_ANSWER }
    private enum Phase { MODE_SELECT, QUIZ, RESULT }

    private final VocabStore store;
    private final MainController controller;

    private Phase phase = Phase.MODE_SELECT;
    private Mode mode = Mode.FLASHCARD;

    // Quiz-State
    private List<Vocabulary> sessionVocabs = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private int wrongCount = 0;
    private boolean cardFlipped = false;

    private StackPane contentPane;

    public LearnView(VocabStore store, MainController controller) {
        this.store = store;
        this.controller = controller;
        getStyleClass().add("view-container");
        setPadding(new Insets(32, 36, 32, 36));
    }

    public void refresh() {
        getChildren().clear();
        phase = Phase.MODE_SELECT;
        currentIndex = 0;
        correctCount = 0;
        wrongCount = 0;

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        getChildren().add(contentPane);

        showModeSelect();
    }

    // ══════════════════════════════════════════════════════
    //  Phase 1: Modus-Auswahl
    // ══════════════════════════════════════════════════════

    private void showModeSelect() {
        phase = Phase.MODE_SELECT;
        VBox layout = new VBox(28);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        Label title = new Label("🧠 Lernmodus wählen");
        title.getStyleClass().add("view-title");

        int dueTotal = store.getDueCount();
        Label sub = new Label(dueTotal > 0
            ? dueTotal + " Vokabeln fällig – ran an die Bücher! 📚"
            : "Alle aktuell! Trotzdem weiterüben? 🌟");
        sub.getStyleClass().add("view-subtitle");

        // Modus-Karten
        HBox modes = new HBox(16);
        modes.setAlignment(Pos.CENTER);
        modes.getChildren().addAll(
            modeCard("🃏", "Karteikarten", "Aufdecken & bewerten", Mode.FLASHCARD),
            modeCard("🔤", "Multiple Choice", "Richtige Antwort wählen", Mode.MULTIPLE_CHOICE),
            modeCard("⌨️", "Eingabe", "Übersetzung eintippen", Mode.TYPE_ANSWER)
        );

        // Sprach-Auswahl
        HBox langRow = new HBox(10);
        langRow.setAlignment(Pos.CENTER);

        Label langLabel = new Label("Sprache:");
        langLabel.getStyleClass().add("text-secondary");

        ComboBox<String> langBox = new ComboBox<>();
        langBox.getItems().add("Alle Sprachen");
        langBox.getItems().addAll(store.getLanguages());
        langBox.setValue("Alle Sprachen");
        langBox.getStyleClass().add("combo-box-custom");

        langRow.getChildren().addAll(langLabel, langBox);

        // Kategorie-Auswahl
        HBox catRow = new HBox(10);
        catRow.setAlignment(Pos.CENTER);

        Label catLabel = new Label("Kategorie:");
        catLabel.getStyleClass().add("text-secondary");

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getStyleClass().add("combo-box-custom");

        // Kategorien bei Sprach-Auswahl aktualisieren
        Runnable refreshCats = () -> {
            String lang = "Alle Sprachen".equals(langBox.getValue()) ? null : langBox.getValue();
            String prevCat = catBox.getValue();
            catBox.getItems().clear();
            catBox.getItems().add("Alle Kategorien");
            catBox.getItems().addAll(store.getCategories(lang));
            if (prevCat != null && catBox.getItems().contains(prevCat)) {
                catBox.setValue(prevCat);
            } else {
                catBox.setValue("Alle Kategorien");
            }
        };

        langBox.setOnAction(e -> refreshCats.run());
        refreshCats.run();

        catRow.getChildren().addAll(catLabel, catBox);

        // Anzahl Slider
        HBox countRow = new HBox(10);
        countRow.setAlignment(Pos.CENTER);

        Label countLabel = new Label("Anzahl:");
        countLabel.getStyleClass().add("text-secondary");

        int maxVocabs = Math.max(5, store.getTotalCount());
        Slider countSlider = new Slider(5, Math.min(50, maxVocabs), 10);
        countSlider.setBlockIncrement(5);
        countSlider.setMajorTickUnit(5);
        countSlider.setMinorTickCount(0);
        countSlider.setSnapToTicks(true);
        countSlider.setShowTickLabels(true);
        countSlider.setPrefWidth(220);
        countSlider.getStyleClass().add("custom-slider");

        Label countValLabel = new Label("10");
        countValLabel.getStyleClass().add("text-secondary");
        countSlider.valueProperty().addListener((obs, o, n) ->
            countValLabel.setText(String.valueOf(n.intValue())));

        countRow.getChildren().addAll(countLabel, countSlider, countValLabel);

        // Start-Button
        Button startBtn = new Button("▶  Los geht's!");
        startBtn.getStyleClass().add("accent-button-large");
        startBtn.setOnAction(e -> {
            String lang = "Alle Sprachen".equals(langBox.getValue()) ? null : langBox.getValue();
            String cat  = "Alle Kategorien".equals(catBox.getValue()) ? null : catBox.getValue();
            startSession((int) countSlider.getValue(), cat, lang);
        });

        layout.getChildren().addAll(title, sub, modes, langRow, catRow, countRow, startBtn);
        setContent(layout);
    }

    private VBox modeCard(String icon, String name, String desc, Mode m) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px;");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("mode-name");

        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("mode-desc");

        VBox card = new VBox(8, iconLabel, nameLabel, descLabel);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("mode-card");
        card.setPadding(new Insets(24, 32, 24, 32));
        card.setPrefWidth(200);

        if (m == mode) card.getStyleClass().add("mode-card-active");

        card.setOnMouseClicked(e -> {
            this.mode = m;
            showModeSelect();
        });

        return card;
    }

    // ══════════════════════════════════════════════════════
    //  Phase 2: Quiz
    // ══════════════════════════════════════════════════════

    private void startSession(int count, String category, String language) {
        sessionVocabs = store.getLearnSession(count, category, language);
        if (sessionVocabs.isEmpty()) {
            showEmpty();
            return;
        }
        currentIndex = 0;
        correctCount = 0;
        wrongCount = 0;
        phase = Phase.QUIZ;
        showCurrentQuestion();
    }

    private void showCurrentQuestion() {
        if (currentIndex >= sessionVocabs.size()) {
            showResult();
            return;
        }

        Vocabulary v = sessionVocabs.get(currentIndex);
        cardFlipped = false;

        switch (mode) {
            case FLASHCARD       -> showFlashcard(v);
            case MULTIPLE_CHOICE -> showMultipleChoice(v);
            case TYPE_ANSWER     -> showTypeAnswer(v);
        }
    }

    // ── Karteikarte ──────────────────────────────────────

    private void showFlashcard(Vocabulary v) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);

        HBox progress = createProgressBar();

        // Sprach-Hinweis
        Label langHint = new Label(v.getLanguage() + "  →  Deutsch");
        langHint.getStyleClass().add("text-muted");
        langHint.setStyle("-fx-font-size: 12px;");

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("flashcard");
        card.setPrefSize(440, 260);
        card.setMaxSize(440, 260);

        Label wordLabel = new Label(v.getWord());
        wordLabel.getStyleClass().add("flashcard-word");

        Label boxHint = new Label(Vocabulary.BOX_NAMES[v.getBox() - 1] + " · " + Vocabulary.BOX_INTERVALS[v.getBox() - 1]);
        boxHint.getStyleClass().add("flashcard-hint");
        boxHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        Label hintLabel = new Label("Klicken zum Aufdecken ↓");
        hintLabel.getStyleClass().add("flashcard-hint");

        Label transLabel = new Label(v.getTranslation());
        transLabel.getStyleClass().add("flashcard-translation");
        transLabel.setVisible(false);

        Label catLabel = new Label(v.getCategory() != null ? "#" + v.getCategory() : "");
        catLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        catLabel.setVisible(false);

        card.getChildren().addAll(wordLabel, boxHint, hintLabel, transLabel, catLabel);

        Button correctBtn = new Button("✓  Gewusst");
        correctBtn.getStyleClass().add("success-button");
        correctBtn.setVisible(false);

        Button wrongBtn = new Button("✗  Nicht gewusst");
        wrongBtn.getStyleClass().add("danger-button");
        wrongBtn.setVisible(false);

        HBox buttons = new HBox(16, wrongBtn, correctBtn);
        buttons.setAlignment(Pos.CENTER);

        card.setOnMouseClicked(e -> {
            if (cardFlipped) return;
            cardFlipped = true;

            ScaleTransition shrink = new ScaleTransition(Duration.millis(150), card);
            shrink.setFromX(1);
            shrink.setToX(0);
            shrink.setOnFinished(ev -> {
                hintLabel.setVisible(false);
                transLabel.setVisible(true);
                catLabel.setVisible(true);
                card.getStyleClass().add("flashcard-flipped");

                ScaleTransition grow = new ScaleTransition(Duration.millis(150), card);
                grow.setFromX(0);
                grow.setToX(1);
                grow.setOnFinished(ev2 -> {
                    correctBtn.setVisible(true);
                    wrongBtn.setVisible(true);
                });
                grow.play();
            });
            shrink.play();
        });

        correctBtn.setOnAction(e -> {
            boolean movedUp = v.getBox() < 5;
            int prevBox = v.getBox();
            v.markCorrect();
            store.save();
            correctCount++;
            if (movedUp && v.getBox() != prevBox) {
                NotificationPopup.showSuccess(FunMessages.getBoxUp());
            }
            advance();
        });
        wrongBtn.setOnAction(e -> {
            v.markWrong();
            store.save();
            wrongCount++;
            advance();
        });

        layout.getChildren().addAll(progress, langHint, card, buttons);
        setContent(layout);
    }

    // ── Multiple Choice ──────────────────────────────────

    private void showMultipleChoice(Vocabulary v) {
        VBox layout = new VBox(24);
        layout.setAlignment(Pos.CENTER);

        HBox progress = createProgressBar();

        Label langHint = new Label(v.getLanguage() + "  →  Deutsch");
        langHint.getStyleClass().add("text-muted");
        langHint.setStyle("-fx-font-size: 12px;");

        Label prompt = new Label("Was bedeutet:");
        prompt.getStyleClass().add("text-secondary");

        Label wordLabel = new Label(v.getWord());
        wordLabel.getStyleClass().add("quiz-word");

        List<String> options = generateOptions(v);
        VBox optionBox = new VBox(10);
        optionBox.setAlignment(Pos.CENTER);
        optionBox.setMaxWidth(420);

        for (String option : options) {
            Button btn = new Button(option);
            btn.getStyleClass().add("option-button");
            btn.setMaxWidth(Double.MAX_VALUE);

            btn.setOnAction(e -> {
                boolean correct = option.equals(v.getTranslation());
                if (correct) {
                    v.markCorrect();
                    store.save();
                    correctCount++;
                    btn.getStyleClass().add("option-correct");
                } else {
                    v.markWrong();
                    store.save();
                    wrongCount++;
                    btn.getStyleClass().add("option-wrong");
                    optionBox.getChildren().forEach(n -> {
                        if (n instanceof Button b && b.getText().equals(v.getTranslation())) {
                            b.getStyleClass().add("option-correct");
                        }
                    });
                }
                optionBox.getChildren().forEach(n -> { if (n instanceof Button b) b.setDisable(true); });

                PauseTransition pause = new PauseTransition(Duration.millis(900));
                pause.setOnFinished(ev -> advance());
                pause.play();
            });

            optionBox.getChildren().add(btn);
        }

        layout.getChildren().addAll(progress, langHint, prompt, wordLabel, optionBox);
        setContent(layout);
    }

    private List<String> generateOptions(Vocabulary correct) {
        List<String> allTranslations = store.getAll().stream()
            .map(Vocabulary::getTranslation)
            .filter(t -> !t.equals(correct.getTranslation()))
            .distinct()
            .collect(Collectors.toList());

        Collections.shuffle(allTranslations);
        List<String> options = new ArrayList<>();
        options.add(correct.getTranslation());
        options.addAll(allTranslations.stream().limit(3).toList());
        Collections.shuffle(options);
        return options;
    }

    // ── Eingabe-Modus ────────────────────────────────────

    private void showTypeAnswer(Vocabulary v) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);

        HBox progress = createProgressBar();

        Label langHint = new Label(v.getLanguage() + "  →  Deutsch");
        langHint.getStyleClass().add("text-muted");
        langHint.setStyle("-fx-font-size: 12px;");

        Label prompt = new Label("Übersetze:");
        prompt.getStyleClass().add("text-secondary");

        Label wordLabel = new Label(v.getWord());
        wordLabel.getStyleClass().add("quiz-word");

        TextField input = new TextField();
        input.setPromptText("Übersetzung eingeben...");
        input.getStyleClass().add("answer-field");
        input.setMaxWidth(380);

        Label feedback = new Label();
        feedback.getStyleClass().add("feedback-label");
        feedback.setVisible(false);
        feedback.setWrapText(true);

        Button checkBtn = new Button("Prüfen");
        checkBtn.getStyleClass().add("accent-button");

        Runnable check = () -> {
            String answer = input.getText().trim();
            if (answer.isEmpty()) return;

            checkBtn.setDisable(true);
            input.setDisable(true);

            boolean correct = answer.equalsIgnoreCase(v.getTranslation().trim());
            if (correct) {
                v.markCorrect();
                store.save();
                correctCount++;
                feedback.setText("✓ " + FunMessages.getCorrect());
                feedback.getStyleClass().removeAll("feedback-wrong");
                feedback.getStyleClass().add("feedback-correct");
            } else {
                v.markWrong();
                store.save();
                wrongCount++;
                feedback.setText("✗ " + FunMessages.getWrong() + " – Richtig: " + v.getTranslation());
                feedback.getStyleClass().removeAll("feedback-correct");
                feedback.getStyleClass().add("feedback-wrong");
            }
            feedback.setVisible(true);

            PauseTransition pause = new PauseTransition(Duration.millis(1400));
            pause.setOnFinished(e -> advance());
            pause.play();
        };

        checkBtn.setOnAction(e -> check.run());
        input.setOnAction(e -> check.run());

        layout.getChildren().addAll(progress, langHint, prompt, wordLabel, input, checkBtn, feedback);
        setContent(layout);

        input.requestFocus();
    }

    // ══════════════════════════════════════════════════════
    //  Phase 3: Ergebnis
    // ══════════════════════════════════════════════════════

    private void showResult() {
        phase = Phase.RESULT;
        int total = correctCount + wrongCount;
        double rate = total == 0 ? 0 : (double) correctCount / total;

        // Tagesziel prüfen
        int practiced = store.getPracticedToday();
        int goal = store.getSettings().dailyGoal;
        if (practiced >= goal) {
            NotificationPopup.showSuccess(FunMessages.getGoalReached());
        }

        int streak = store.getStreak();
        if (streak >= 3) {
            NotificationPopup.showInfo(FunMessages.getStreak(streak));
        }

        VBox layout = new VBox(24);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        Label trophy = new Label(rate >= 0.9 ? "🏆" : rate >= 0.7 ? "🌟" : rate >= 0.5 ? "👍" : "💪");
        trophy.setStyle("-fx-font-size: 52px;");

        Label title = new Label("Geschafft!");
        title.getStyleClass().add("result-title");

        Label scoreLabel = new Label(correctCount + "/" + total + " richtig (" + Math.round(rate * 100) + "%)");
        scoreLabel.getStyleClass().add("result-score");

        HBox stats = new HBox(20,
            resultCard("✓ " + correctCount, "Richtig", "result-correct"),
            resultCard("✗ " + wrongCount, "Falsch", "result-wrong")
        );
        stats.setAlignment(Pos.CENTER);

        // FunMessages basiert auf Ergebnis
        String msg;
        if (rate >= 0.9)      msg = FunMessages.getCorrect();
        else if (rate >= 0.5) msg = "Solide! " + FunMessages.getCorrect();
        else                  msg = FunMessages.getWrong() + " Nochmal versuchen!";
        Label msgLabel = new Label(msg);
        msgLabel.getStyleClass().add("result-message");

        Button retryBtn = new Button("🔄 Nochmal");
        retryBtn.getStyleClass().add("accent-button");
        retryBtn.setOnAction(e -> {
            currentIndex = 0;
            correctCount = 0;
            wrongCount = 0;
            Collections.shuffle(sessionVocabs);
            showCurrentQuestion();
        });

        Button newBtn = new Button("🧠 Neue Session");
        newBtn.getStyleClass().add("secondary-button");
        newBtn.setOnAction(e -> showModeSelect());

        Button backBtn = new Button("🏠 Dashboard");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> controller.navigateTo(MainController.View.DASHBOARD));

        HBox buttons = new HBox(12, retryBtn, newBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(trophy, title, scoreLabel, stats, msgLabel, buttons);
        setContent(layout);
    }

    private VBox resultCard(String value, String label, String styleClass) {
        Label val = new Label(value);
        val.getStyleClass().addAll("result-card-value", styleClass);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("result-card-label");

        VBox card = new VBox(4, val, lbl);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("result-card");
        card.setPadding(new Insets(20, 40, 20, 40));
        return card;
    }

    // ── Hilfs-Methoden ───────────────────────────────────

    private void advance() {
        currentIndex++;
        showCurrentQuestion();
    }

    private HBox createProgressBar() {
        int total = sessionVocabs.size();

        Label progressLabel = new Label((currentIndex + 1) + " / " + total);
        progressLabel.getStyleClass().add("progress-label");

        ProgressBar bar = new ProgressBar((double) currentIndex / total);
        bar.getStyleClass().add("quiz-progress");
        bar.setPrefWidth(300);
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label score = new Label("✓ " + correctCount + "  ✗ " + wrongCount);
        score.getStyleClass().add("score-label");

        // Abbrechen-Button
        Button abortBtn = new Button("✕");
        abortBtn.getStyleClass().add("icon-button");
        abortBtn.setOnAction(e -> showModeSelect());

        HBox box = new HBox(12, progressLabel, bar, score, abortBtn);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(560);
        return box;
    }

    private void showEmpty() {
        VBox layout = new VBox(16);
        layout.setAlignment(Pos.CENTER);

        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 48px;");

        Label msg = new Label("Keine Vokabeln gefunden.\nFüge zuerst Vokabeln hinzu oder wähle andere Filter!");
        msg.getStyleClass().add("text-muted");
        msg.setStyle("-fx-text-alignment: center;");

        Button addBtn = new Button("📚 Vokabeln hinzufügen");
        addBtn.getStyleClass().add("accent-button");
        addBtn.setOnAction(e -> controller.navigateTo(MainController.View.VOCAB_LIST));

        Button backBtn = new Button("← Zurück");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> showModeSelect());

        HBox btns = new HBox(12, addBtn, backBtn);
        btns.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(icon, msg, btns);
        setContent(layout);
    }

    private void setContent(Region content) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        contentPane.getChildren().setAll(content);
        fade.play();
    }
}
