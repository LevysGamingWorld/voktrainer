package com.vokabeltrainer.view;

import com.vokabeltrainer.model.Vocabulary;
import com.vokabeltrainer.persistence.VocabStore;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Vokabel-Verwaltung: Suchen, Filtern nach Sprache & Kategorie,
 * Hinzufügen, Bearbeiten, Löschen, Box-Verwaltung.
 */
public class VocabListView extends VBox {

    private final VocabStore store;
    private TextField searchField;
    private String activeCategory = null;
    private String activeLanguage = null;   // null = alle Sprachen
    private VBox listContainer;
    private HBox categoryBar;
    private HBox languageBar;

    // Inline-Formular
    private VBox addForm;
    private TextField wordField, transField, catField, langField;
    private ComboBox<Integer> boxCombo;
    private Vocabulary editingVocab = null;

    public VocabListView(VocabStore store) {
        this.store = store;
        getStyleClass().add("view-container");
        setSpacing(16);
        setPadding(new Insets(32, 36, 32, 36));
    }

    public void refresh() {
        getChildren().clear();

        // ── Header ───────────────────────────────────────
        Label title = new Label("📚 Meine Vokabeln");
        title.getStyleClass().add("view-title");

        Button addBtn = new Button("+ Neue Vokabel");
        addBtn.getStyleClass().add("accent-button");
        addBtn.setOnAction(e -> toggleAddForm(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, title, spacer, addBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Suche ────────────────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("🔍 Suchen...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, o, n) -> rebuildList());

        // ── Sprach-Tabs ──────────────────────────────────
        languageBar = new HBox(6);
        languageBar.setAlignment(Pos.CENTER_LEFT);
        rebuildLanguageBar();

        // ── Kategorie-Filter ─────────────────────────────
        categoryBar = new HBox(6);
        categoryBar.setAlignment(Pos.CENTER_LEFT);
        rebuildCategoryBar();

        // ── Inline Add/Edit Form ─────────────────────────
        addForm = createAddForm();
        addForm.setVisible(false);
        addForm.setManaged(false);

        // ── Liste ────────────────────────────────────────
        listContainer = new VBox(6);
        rebuildList();

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, searchField, languageBar, categoryBar, addForm, scroll);
    }

    // ── Sprach-Tab-Leiste ────────────────────────────────

    private void rebuildLanguageBar() {
        languageBar.getChildren().clear();

        Button allBtn = new Button("🌍 Alle");
        allBtn.getStyleClass().add("lang-tab");
        if (activeLanguage == null) allBtn.getStyleClass().add("lang-tab-active");
        allBtn.setOnAction(e -> { activeLanguage = null; activeCategory = null; rebuildLanguageBar(); rebuildCategoryBar(); rebuildList(); });
        languageBar.getChildren().add(allBtn);

        for (String lang : store.getLanguages()) {
            long count = store.getLanguageCounts().getOrDefault(lang, 0L);
            Button btn = new Button(lang + "  " + count);
            btn.getStyleClass().add("lang-tab");
            if (lang.equals(activeLanguage)) btn.getStyleClass().add("lang-tab-active");
            btn.setOnAction(e -> {
                activeLanguage = lang;
                activeCategory = null;
                rebuildLanguageBar();
                rebuildCategoryBar();
                rebuildList();
            });
            languageBar.getChildren().add(btn);
        }
    }

    // ── Kategorie-Leiste ─────────────────────────────────

    private void rebuildCategoryBar() {
        categoryBar.getChildren().clear();

        Button allBtn = new Button("Alle");
        allBtn.getStyleClass().add("cat-button");
        if (activeCategory == null) allBtn.getStyleClass().add("cat-active");
        allBtn.setOnAction(e -> { activeCategory = null; rebuildCategoryBar(); rebuildList(); });
        categoryBar.getChildren().add(allBtn);

        for (String cat : store.getCategories(activeLanguage)) {
            Button btn = new Button(cat);
            btn.getStyleClass().add("cat-button");
            if (cat.equals(activeCategory)) btn.getStyleClass().add("cat-active");
            btn.setOnAction(e -> { activeCategory = cat; rebuildCategoryBar(); rebuildList(); });
            categoryBar.getChildren().add(btn);
        }
    }

    // ── Liste neu aufbauen ───────────────────────────────

    private void rebuildList() {
        listContainer.getChildren().clear();

        String query = searchField != null ? searchField.getText() : "";
        List<Vocabulary> results = store.search(query, activeCategory, activeLanguage);

        if (results.isEmpty()) {
            Label empty = new Label("Keine Vokabeln gefunden.");
            empty.getStyleClass().add("text-muted");
            empty.setPadding(new Insets(20));
            listContainer.getChildren().add(empty);
            return;
        }

        Label count = new Label(results.size() + " Vokabel" + (results.size() != 1 ? "n" : ""));
        count.getStyleClass().add("text-muted");
        count.setPadding(new Insets(0, 0, 4, 4));
        listContainer.getChildren().add(count);

        for (Vocabulary v : results) {
            listContainer.getChildren().add(createVocabRow(v));
        }
    }

    // ── Vokabel-Zeile ────────────────────────────────────

    private HBox createVocabRow(Vocabulary v) {
        Label word = new Label(v.getWord());
        word.getStyleClass().add("vocab-word");
        word.setMinWidth(120);

        Label arrow = new Label("→");
        arrow.getStyleClass().add("text-muted");

        Label trans = new Label(v.getTranslation());
        trans.getStyleClass().add("vocab-translation");
        trans.setMinWidth(120);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label langTag = new Label(v.getLanguage());
        langTag.getStyleClass().add("lang-tag");

        Label catTag = new Label(v.getCategory() != null ? v.getCategory() : "");
        catTag.getStyleClass().add("cat-tag");

        // Box-Badge mit Namen
        String boxName = Vocabulary.BOX_NAMES[v.getBox() - 1];
        Label boxBadge = new Label(boxName);
        boxBadge.getStyleClass().addAll("box-badge", "box-" + v.getBox());
        boxBadge.setTooltip(new Tooltip("Box " + v.getBox() + ": " + Vocabulary.BOX_INTERVALS[v.getBox() - 1]));

        // Fortschritt
        int tot = v.getCorrectCount() + v.getWrongCount();
        Label perfLabel = new Label(tot > 0 ? (int)(100.0 * v.getCorrectCount() / tot) + "%" : "—");
        perfLabel.getStyleClass().add("text-muted");
        perfLabel.setStyle("-fx-font-size: 11px;");

        // Box manuell ändern
        Button boxUpBtn = new Button("⬆");
        boxUpBtn.getStyleClass().add("icon-button");
        boxUpBtn.setTooltip(new Tooltip("Eine Box höher"));
        boxUpBtn.setVisible(v.getBox() < 5);
        boxUpBtn.setManaged(v.getBox() < 5);
        boxUpBtn.setOnAction(e -> {
            v.setBox(v.getBox() + 1);
            store.update(v);
            rebuildList();
        });

        Button boxDownBtn = new Button("⬇");
        boxDownBtn.getStyleClass().add("icon-button");
        boxDownBtn.setTooltip(new Tooltip("Eine Box niedriger"));
        boxDownBtn.setVisible(v.getBox() > 1);
        boxDownBtn.setManaged(v.getBox() > 1);
        boxDownBtn.setOnAction(e -> {
            v.setBox(v.getBox() - 1);
            store.update(v);
            rebuildList();
        });

        Button editBtn = new Button("✏");
        editBtn.getStyleClass().add("icon-button");
        editBtn.setOnAction(e -> toggleAddForm(v));

        Button delBtn = new Button("🗑");
        delBtn.getStyleClass().add("icon-button-danger");
        delBtn.setOnAction(e -> {
            store.delete(v);
            rebuildLanguageBar();
            rebuildCategoryBar();
            rebuildList();
        });

        HBox row = new HBox(8, word, arrow, trans, spacer, langTag, catTag, boxBadge, perfLabel, boxDownBtn, boxUpBtn, editBtn, delBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("vocab-row");
        row.setPadding(new Insets(10, 14, 10, 14));
        return row;
    }

    // ── Inline Add/Edit Formular ─────────────────────────

    private VBox createAddForm() {
        wordField = new TextField();
        wordField.setPromptText("Wort (z.B. apple)");
        wordField.getStyleClass().add("form-field");

        transField = new TextField();
        transField.setPromptText("Übersetzung (z.B. Apfel)");
        transField.getStyleClass().add("form-field");

        catField = new TextField();
        catField.setPromptText("Kategorie (z.B. Essen)");
        catField.getStyleClass().add("form-field");

        langField = new TextField();
        langField.setPromptText("Sprache (z.B. Englisch)");
        langField.getStyleClass().add("form-field");
        langField.setText(store.getSettings().defaultLanguage);

        Label boxLabel = new Label("Box:");
        boxLabel.getStyleClass().add("text-muted");
        boxCombo = new ComboBox<>();
        for (int i = 1; i <= 5; i++) {
            boxCombo.getItems().add(i);
        }
        boxCombo.setValue(1);
        boxCombo.getStyleClass().add("combo-box-custom");
        boxCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) setText("Box " + item + " – " + Vocabulary.BOX_NAMES[item - 1]);
            }
        });
        boxCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) setText("Box " + item + " – " + Vocabulary.BOX_NAMES[item - 1] + " (" + Vocabulary.BOX_INTERVALS[item - 1] + ")");
            }
        });

        HBox boxRow = new HBox(8, boxLabel, boxCombo);
        boxRow.setAlignment(Pos.CENTER_LEFT);

        HBox row1 = new HBox(10, wordField, transField);
        HBox.setHgrow(wordField, Priority.ALWAYS);
        HBox.setHgrow(transField, Priority.ALWAYS);

        HBox row2 = new HBox(10, catField, langField);
        HBox.setHgrow(catField, Priority.ALWAYS);
        HBox.setHgrow(langField, Priority.ALWAYS);

        Button saveBtn = new Button("💾 Speichern");
        saveBtn.getStyleClass().add("accent-button");
        saveBtn.setOnAction(e -> saveVocab());

        Button cancelBtn = new Button("Abbrechen");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setOnAction(e -> hideAddForm());

        HBox actions = new HBox(10, saveBtn, cancelBtn, boxRow);
        actions.setAlignment(Pos.CENTER_LEFT);

        wordField.setOnAction(e -> transField.requestFocus());
        transField.setOnAction(e -> catField.requestFocus());
        catField.setOnAction(e -> langField.requestFocus());
        langField.setOnAction(e -> saveVocab());

        VBox form = new VBox(10, row1, row2, actions);
        form.getStyleClass().add("add-form");
        form.setPadding(new Insets(16));
        return form;
    }

    private void toggleAddForm(Vocabulary v) {
        editingVocab = v;
        if (v != null) {
            wordField.setText(v.getWord());
            transField.setText(v.getTranslation());
            catField.setText(v.getCategory() != null ? v.getCategory() : "");
            langField.setText(v.getLanguage());
            boxCombo.setValue(v.getBox());
        } else {
            wordField.clear();
            transField.clear();
            catField.clear();
            langField.setText(activeLanguage != null ? activeLanguage : store.getSettings().defaultLanguage);
            boxCombo.setValue(1);
        }
        addForm.setVisible(true);
        addForm.setManaged(true);
        wordField.requestFocus();
    }

    private void hideAddForm() {
        addForm.setVisible(false);
        addForm.setManaged(false);
        editingVocab = null;
    }

    private void saveVocab() {
        String w = wordField.getText().trim();
        String t = transField.getText().trim();
        String c = catField.getText().trim();
        String l = langField.getText().trim();
        int box  = boxCombo.getValue() != null ? boxCombo.getValue() : 1;

        if (w.isEmpty() || t.isEmpty()) return;

        if (editingVocab != null) {
            editingVocab.setWord(w);
            editingVocab.setTranslation(t);
            editingVocab.setCategory(c);
            editingVocab.setLanguage(l.isEmpty() ? "Englisch" : l);
            editingVocab.setBox(box);
            store.update(editingVocab);
        } else {
            Vocabulary nv = new Vocabulary(w, t, c, l.isEmpty() ? "Englisch" : l);
            nv.setBox(box);
            store.add(nv);
        }

        hideAddForm();
        rebuildLanguageBar();
        rebuildCategoryBar();
        rebuildList();
    }
}

