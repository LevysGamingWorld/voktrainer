package com.vokabeltrainer.view;

import com.vokabeltrainer.persistence.VocabStore;
import com.vokabeltrainer.service.NotificationPopup;
import com.vokabeltrainer.service.ReminderService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Einstellungen – Erinnerungen, Tagesziel, Sprachen und Verwaltung.
 */
public class SettingsView extends VBox {

    private final VocabStore store;

    public SettingsView(VocabStore store) {
        this.store = store;
        getStyleClass().add("view-container");
        setSpacing(28);
        setPadding(new Insets(32, 36, 32, 36));
    }

    public void refresh() {
        getChildren().clear();

        Label title = new Label("⚙️ Einstellungen");
        title.getStyleClass().add("view-title");

        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, scroll);
    }

    private VBox buildContent() {
        VocabStore.Settings s = store.getSettings();

        // ── Erinnerungen ─────────────────────────────────
        Label reminderTitle = new Label("🔔 Erinnerungen");
        reminderTitle.getStyleClass().add("section-title");

        Label reminderDesc = new Label(
            "Der VokabelTrainer erinnert dich regelmäßig mit motivierenden Nachrichten ans Lernen."
        );
        reminderDesc.getStyleClass().add("text-muted");
        reminderDesc.setWrapText(true);

        CheckBox remindersEnabled = new CheckBox("Erinnerungen aktivieren");
        remindersEnabled.getStyleClass().add("custom-checkbox");
        remindersEnabled.setSelected(s.remindersEnabled);

        // Intervall-Slider
        Label intervalLabel = new Label("Erinnerung alle " + s.reminderIntervalMinutes + " Minuten");
        intervalLabel.getStyleClass().add("text-secondary");

        Slider intervalSlider = new Slider(5, 240, s.reminderIntervalMinutes);
        intervalSlider.setBlockIncrement(5);
        intervalSlider.setMajorTickUnit(60);
        intervalSlider.setMinorTickCount(2);
        intervalSlider.setSnapToTicks(false);
        intervalSlider.setShowTickLabels(true);
        intervalSlider.getStyleClass().add("custom-slider");
        intervalSlider.setMaxWidth(400);
        intervalSlider.valueProperty().addListener((obs, o, n) ->
            intervalLabel.setText("Erinnerung alle " + n.intValue() + " Minuten"));
        intervalSlider.setDisable(!s.remindersEnabled);
        remindersEnabled.selectedProperty().addListener((obs, o, n) -> intervalSlider.setDisable(!n));

        Button testBtn = new Button("🔔 Test-Erinnerung anzeigen");
        testBtn.getStyleClass().add("secondary-button");
        testBtn.setOnAction(e -> NotificationPopup.showReminder());

        VBox reminderSection = new VBox(10,
            reminderTitle, reminderDesc, remindersEnabled, intervalLabel, intervalSlider, testBtn
        );
        reminderSection.getStyleClass().add("settings-section");
        reminderSection.setPadding(new Insets(18));

        // ── Tagesziel ────────────────────────────────────
        Label goalTitle = new Label("🎯 Tagesziel");
        goalTitle.getStyleClass().add("section-title");

        Label goalDesc = new Label("Wie viele Vokabeln möchtest du täglich lernen?");
        goalDesc.getStyleClass().add("text-muted");

        Label goalValueLabel = new Label(s.dailyGoal + " Vokabeln pro Tag");
        goalValueLabel.getStyleClass().add("text-secondary");

        Slider goalSlider = new Slider(5, 100, s.dailyGoal);
        goalSlider.setBlockIncrement(5);
        goalSlider.setMajorTickUnit(20);
        goalSlider.setMinorTickCount(3);
        goalSlider.setSnapToTicks(false);
        goalSlider.setShowTickLabels(true);
        goalSlider.getStyleClass().add("custom-slider");
        goalSlider.setMaxWidth(400);
        goalSlider.valueProperty().addListener((obs, o, n) ->
            goalValueLabel.setText(n.intValue() + " Vokabeln pro Tag"));

        VBox goalSection = new VBox(10, goalTitle, goalDesc, goalValueLabel, goalSlider);
        goalSection.getStyleClass().add("settings-section");
        goalSection.setPadding(new Insets(18));

        // ── Standardsprache ──────────────────────────────
        Label langTitle = new Label("🌍 Standard-Sprache");
        langTitle.getStyleClass().add("section-title");

        Label langDesc = new Label("Vorausgewählte Sprache beim Hinzufügen neuer Vokabeln.");
        langDesc.getStyleClass().add("text-muted");

        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll("Englisch", "Latein", "Französisch", "Spanisch", "Italienisch", "Japanisch", "Chinesisch");
        // Auch vorhandene Sprachen hinzufügen
        for (String l : store.getLanguages()) {
            if (!langCombo.getItems().contains(l)) langCombo.getItems().add(l);
        }
        langCombo.setValue(s.defaultLanguage);
        langCombo.getStyleClass().add("combo-box-custom");

        VBox langSection = new VBox(10, langTitle, langDesc, langCombo);
        langSection.getStyleClass().add("settings-section");
        langSection.setPadding(new Insets(18));

        // ── Box-Verwaltung ───────────────────────────────
        Label boxTitle = new Label("📦 Box-Verwaltung");
        boxTitle.getStyleClass().add("section-title");

        Label boxDesc = new Label(
            "Hier kannst du den Lernfortschritt zurücksetzen. Das setzt alle Vokabeln auf Box 1 zurück.\n" +
            "Nützlich um eine Sprache von Grund auf neu zu lernen."
        );
        boxDesc.getStyleClass().add("text-muted");
        boxDesc.setWrapText(true);

        // Zurücksetzen pro Sprache
        VBox resetButtons = new VBox(8);
        List<String> languages = store.getLanguages();
        if (languages.isEmpty()) {
            Label noLang = new Label("Noch keine Vokabeln vorhanden.");
            noLang.getStyleClass().add("text-muted");
            resetButtons.getChildren().add(noLang);
        } else {
            for (String lang : languages) {
                Button resetLangBtn = new Button("⟳ " + lang + " zurücksetzen");
                resetLangBtn.getStyleClass().add("secondary-button");
                resetLangBtn.setOnAction(e -> {
                    Alert alert = confirmDialog(
                        "Fortschritt zurücksetzen",
                        "Alle " + lang + " Vokabeln werden auf Box 1 zurückgesetzt.\nFortfahren?"
                    );
                    alert.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            store.resetProgressForLanguage(lang);
                            NotificationPopup.showInfo(lang + " wurde zurückgesetzt! Frischer Start! 🌟");
                        }
                    });
                });
                resetButtons.getChildren().add(resetLangBtn);
            }
        }

        Button resetAllBtn = new Button("⚠️  Alles zurücksetzen");
        resetAllBtn.getStyleClass().add("icon-button-danger");
        resetAllBtn.setOnAction(e -> {
            Alert alert = confirmDialog(
                "Alles zurücksetzen",
                "Alle Vokabeln werden auf Box 1 zurückgesetzt!\nDieser Vorgang kann nicht rückgängig gemacht werden."
            );
            alert.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    store.resetAllProgress();
                    NotificationPopup.showInfo("Alles zurückgesetzt – frischer Start! 💪");
                }
            });
        });
        resetButtons.getChildren().add(resetAllBtn);

        VBox boxSection = new VBox(12, boxTitle, boxDesc, resetButtons);
        boxSection.getStyleClass().add("settings-section");
        boxSection.setPadding(new Insets(18));

        // ── Speichern-Button ─────────────────────────────
        Button saveBtn = new Button("💾  Einstellungen speichern");
        saveBtn.getStyleClass().add("accent-button");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            s.remindersEnabled         = remindersEnabled.isSelected();
            s.reminderIntervalMinutes  = (int) intervalSlider.getValue();
            s.dailyGoal                = (int) goalSlider.getValue();
            s.defaultLanguage          = langCombo.getValue();

            store.saveSettings();
            ReminderService.getInstance().restart();

            NotificationPopup.showSuccess("Einstellungen gespeichert! ✅");
        });

        return new VBox(20, reminderSection, goalSection, langSection, boxSection, saveBtn);
    }

    private Alert confirmDialog(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Bestätigung");
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert;
    }
}
