package com.vokabeltrainer.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vokabeltrainer.model.Vocabulary;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Speichert und lädt Vokabeln als JSON-Datei im Home-Verzeichnis.
 * Singleton – eine Instanz für die gesamte App.
 */
public class VocabStore {

    private static final Path STORE_DIR    = Path.of(System.getProperty("user.home"), ".vokabeltrainer");
    private static final Path STORE_FILE   = STORE_DIR.resolve("vocabs.json");
    private static final Path SETTINGS_FILE = STORE_DIR.resolve("settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Vocabulary>>() {}.getType();

    private static VocabStore instance;
    private final List<Vocabulary> vocabs = new ArrayList<>();
    private Settings settings;

    /** Einstellungen (JSON-serialisierbar). */
    public static class Settings {
        public int dailyGoal = 20;
        public int reminderIntervalMinutes = 60;
        public boolean remindersEnabled = true;
        public String defaultLanguage = "Englisch";
    }

    private VocabStore() {
        loadSettings();
        load();
        if (vocabs.isEmpty()) {
            seedDefaults();
            save();
        }
    }

    public static VocabStore getInstance() {
        if (instance == null) instance = new VocabStore();
        return instance;
    }

    // ── Settings ─────────────────────────────────────────

    public Settings getSettings() { return settings; }

    public void saveSettings() {
        try {
            Files.createDirectories(STORE_DIR);
            try (Writer w = Files.newBufferedWriter(SETTINGS_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(settings, w);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Speichern der Settings: " + e.getMessage());
        }
    }

    private void loadSettings() {
        if (Files.exists(SETTINGS_FILE)) {
            try (Reader r = Files.newBufferedReader(SETTINGS_FILE, StandardCharsets.UTF_8)) {
                settings = GSON.fromJson(r, Settings.class);
            } catch (Exception e) {
                settings = new Settings();
            }
        } else {
            settings = new Settings();
        }
    }

    // ── CRUD ─────────────────────────────────────────────

    public List<Vocabulary> getAll() {
        return Collections.unmodifiableList(vocabs);
    }

    public void add(Vocabulary v) {
        vocabs.add(v);
        save();
    }

    public void addAll(List<Vocabulary> list) {
        vocabs.addAll(list);
        save();
    }

    public void update(Vocabulary v) {
        save();
    }

    public void delete(Vocabulary v) {
        vocabs.remove(v);
        save();
    }

    /** Alle Vokabeln einer bestimmten Box verschieben. */
    public void moveAllInBox(int fromBox, int toBox) {
        vocabs.stream()
            .filter(v -> v.getBox() == fromBox)
            .forEach(v -> v.setBox(toBox));
        save();
    }

    /** Fortschritt komplett zurücksetzen. */
    public void resetAllProgress() {
        vocabs.forEach(Vocabulary::resetProgress);
        save();
    }

    /** Fortschritt für eine Sprache zurücksetzen. */
    public void resetProgressForLanguage(String language) {
        vocabs.stream()
            .filter(v -> language.equals(v.getLanguage()))
            .forEach(Vocabulary::resetProgress);
        save();
    }

    public Optional<Vocabulary> findById(String id) {
        return vocabs.stream().filter(v -> v.getId().equals(id)).findFirst();
    }

    // ── Sprach-Abfragen ──────────────────────────────────

    /** Alle einzigartigen Sprachen. */
    public List<String> getLanguages() {
        return vocabs.stream()
            .map(Vocabulary::getLanguage)
            .filter(l -> l != null && !l.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /** Anzahl Vokabeln pro Sprache. */
    public Map<String, Long> getLanguageCounts() {
        return vocabs.stream()
            .collect(Collectors.groupingBy(Vocabulary::getLanguage, Collectors.counting()));
    }

    // ── Abfragen ─────────────────────────────────────────

    /** Alle fälligen Vokabeln. */
    public List<Vocabulary> getDueVocabs() {
        return vocabs.stream().filter(Vocabulary::isDue).collect(Collectors.toList());
    }

    /** Fällige Vokabeln für eine Sprache. */
    public List<Vocabulary> getDueVocabs(String language) {
        return vocabs.stream()
            .filter(Vocabulary::isDue)
            .filter(v -> language == null || language.isEmpty() || language.equals(v.getLanguage()))
            .collect(Collectors.toList());
    }

    /** Alle einzigartigen Kategorien. */
    public List<String> getCategories() {
        return vocabs.stream()
            .map(Vocabulary::getCategory)
            .filter(c -> c != null && !c.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /** Kategorien für eine Sprache. */
    public List<String> getCategories(String language) {
        return vocabs.stream()
            .filter(v -> language == null || language.isEmpty() || language.equals(v.getLanguage()))
            .map(Vocabulary::getCategory)
            .filter(c -> c != null && !c.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /** Vokabeln filtern nach Sprache, Kategorie und Suchbegriff. */
    public List<Vocabulary> search(String query, String category, String language) {
        return vocabs.stream()
            .filter(v -> language == null || language.isEmpty() || language.equals(v.getLanguage()))
            .filter(v -> category == null || category.isEmpty() || category.equals(v.getCategory()))
            .filter(v -> query == null || query.isEmpty()
                || v.getWord().toLowerCase().contains(query.toLowerCase())
                || v.getTranslation().toLowerCase().contains(query.toLowerCase()))
            .collect(Collectors.toList());
    }

    /** Vokabeln für eine Lernsession. */
    public List<Vocabulary> getLearnSession(int maxCount, String category, String language) {
        List<Vocabulary> pool = vocabs.stream()
            .filter(v -> language == null || language.isEmpty() || language.equals(v.getLanguage()))
            .filter(v -> category == null || category.isEmpty() || category.equals(v.getCategory()))
            .sorted(Comparator
                .comparing(Vocabulary::isDue).reversed()
                .thenComparing(Vocabulary::getBox)
                .thenComparing(Vocabulary::getLastPracticed))
            .limit(maxCount)
            .collect(Collectors.toList());
        Collections.shuffle(pool);
        return pool;
    }

    /** Vokabeln einer Box. */
    public List<Vocabulary> getVocabsInBox(int box) {
        return vocabs.stream().filter(v -> v.getBox() == box).collect(Collectors.toList());
    }

    /** Vokabeln einer Box für eine Sprache. */
    public List<Vocabulary> getVocabsInBox(int box, String language) {
        return vocabs.stream()
            .filter(v -> v.getBox() == box)
            .filter(v -> language == null || language.isEmpty() || language.equals(v.getLanguage()))
            .collect(Collectors.toList());
    }

    // ── Statistiken ──────────────────────────────────────

    public int getTotalCount() { return vocabs.size(); }

    public int getTotalCount(String language) {
        return (int) vocabs.stream()
            .filter(v -> language == null || language.equals(v.getLanguage()))
            .count();
    }

    public int getMasteredCount() {
        return (int) vocabs.stream().filter(Vocabulary::isMastered).count();
    }

    public int getDueCount() {
        return (int) vocabs.stream().filter(Vocabulary::isDue).count();
    }

    public double getOverallSuccessRate() {
        int correct = vocabs.stream().mapToInt(Vocabulary::getCorrectCount).sum();
        int wrong   = vocabs.stream().mapToInt(Vocabulary::getWrongCount).sum();
        int total   = correct + wrong;
        return total == 0 ? 0.0 : (double) correct / total;
    }

    public double getSuccessRate(String language) {
        int correct = vocabs.stream().filter(v -> language.equals(v.getLanguage())).mapToInt(Vocabulary::getCorrectCount).sum();
        int wrong   = vocabs.stream().filter(v -> language.equals(v.getLanguage())).mapToInt(Vocabulary::getWrongCount).sum();
        int total   = correct + wrong;
        return total == 0 ? 0.0 : (double) correct / total;
    }

    public int[] getBoxDistribution() {
        int[] dist = new int[5];
        for (Vocabulary v : vocabs) {
            int idx = Math.max(0, Math.min(4, v.getBox() - 1));
            dist[idx]++;
        }
        return dist;
    }

    public int[] getBoxDistribution(String language) {
        int[] dist = new int[5];
        vocabs.stream()
            .filter(v -> language == null || language.equals(v.getLanguage()))
            .forEach(v -> dist[Math.max(0, Math.min(4, v.getBox() - 1))]++);
        return dist;
    }

    public int getPracticedToday() {
        long startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return (int) vocabs.stream().filter(v -> v.getLastPracticed() >= startOfDay).count();
    }

    public int getStreak() {
        Set<LocalDate> practiceDays = vocabs.stream()
            .filter(Vocabulary::hasBeenPracticed)
            .map(v -> Instant.ofEpochMilli(v.getLastPracticed()).atZone(ZoneId.systemDefault()).toLocalDate())
            .collect(Collectors.toSet());

        int streak = 0;
        LocalDate day = LocalDate.now();
        while (practiceDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    /** Tagesziel-Fortschritt (0.0–1.0+). */
    public double getDailyProgress() {
        int practiced = getPracticedToday();
        int goal = settings.dailyGoal;
        return goal <= 0 ? 1.0 : (double) practiced / goal;
    }

    // ── Laden / Speichern ────────────────────────────────

    private void load() {
        if (!Files.exists(STORE_FILE)) return;
        try (Reader reader = Files.newBufferedReader(STORE_FILE, StandardCharsets.UTF_8)) {
            List<Vocabulary> loaded = GSON.fromJson(reader, LIST_TYPE);
            if (loaded != null) {
                vocabs.addAll(loaded);
                // Migration: alte Vokabeln ohne Sprache → "Englisch"
                vocabs.forEach(v -> {
                    if (v.getLanguage() == null || v.getLanguage().isBlank()) {
                        v.setLanguage("Englisch");
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Laden: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(STORE_DIR);
            try (Writer writer = Files.newBufferedWriter(STORE_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(vocabs, LIST_TYPE, writer);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Speichern: " + e.getMessage());
        }
    }

    // ── Standard-Vokabeln ────────────────────────────────

    private void seedDefaults() {
        // ── Englisch ─────────────────────────────────────
        vocabs.add(new Vocabulary("cat", "Katze", "Tiere", "Englisch"));
        vocabs.add(new Vocabulary("dog", "Hund", "Tiere", "Englisch"));
        vocabs.add(new Vocabulary("bird", "Vogel", "Tiere", "Englisch"));
        vocabs.add(new Vocabulary("fish", "Fisch", "Tiere", "Englisch"));
        vocabs.add(new Vocabulary("horse", "Pferd", "Tiere", "Englisch"));

        vocabs.add(new Vocabulary("apple", "Apfel", "Essen", "Englisch"));
        vocabs.add(new Vocabulary("bread", "Brot", "Essen", "Englisch"));
        vocabs.add(new Vocabulary("cheese", "Käse", "Essen", "Englisch"));
        vocabs.add(new Vocabulary("milk", "Milch", "Essen", "Englisch"));
        vocabs.add(new Vocabulary("water", "Wasser", "Essen", "Englisch"));

        vocabs.add(new Vocabulary("house", "Haus", "Alltag", "Englisch"));
        vocabs.add(new Vocabulary("door", "Tür", "Alltag", "Englisch"));
        vocabs.add(new Vocabulary("window", "Fenster", "Alltag", "Englisch"));
        vocabs.add(new Vocabulary("table", "Tisch", "Alltag", "Englisch"));
        vocabs.add(new Vocabulary("chair", "Stuhl", "Alltag", "Englisch"));

        // ── Latein ───────────────────────────────────────
        vocabs.add(new Vocabulary("aqua", "Wasser", "Natur", "Latein"));
        vocabs.add(new Vocabulary("terra", "Erde", "Natur", "Latein"));
        vocabs.add(new Vocabulary("ignis", "Feuer", "Natur", "Latein"));
        vocabs.add(new Vocabulary("ventus", "Wind", "Natur", "Latein"));
        vocabs.add(new Vocabulary("caelum", "Himmel", "Natur", "Latein"));

        vocabs.add(new Vocabulary("amicus", "Freund", "Menschen", "Latein"));
        vocabs.add(new Vocabulary("puella", "Mädchen", "Menschen", "Latein"));
        vocabs.add(new Vocabulary("magister", "Lehrer", "Menschen", "Latein"));
        vocabs.add(new Vocabulary("rex", "König", "Menschen", "Latein"));
        vocabs.add(new Vocabulary("deus", "Gott", "Menschen", "Latein"));

        // ── Französisch ──────────────────────────────────
        vocabs.add(new Vocabulary("bonjour", "Guten Tag", "Begrüßung", "Französisch"));
        vocabs.add(new Vocabulary("merci", "Danke", "Begrüßung", "Französisch"));
        vocabs.add(new Vocabulary("maison", "Haus", "Alltag", "Französisch"));
        vocabs.add(new Vocabulary("chat", "Katze", "Tiere", "Französisch"));
        vocabs.add(new Vocabulary("fleur", "Blume", "Natur", "Französisch"));
    }
}
