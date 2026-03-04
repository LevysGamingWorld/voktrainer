package com.vokabeltrainer.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Eine einzelne Vokabel mit Spaced-Repetition-Daten (Leitner-System).
 * <p>
 * Die 5 Leitner-Boxen bestimmen den Wiederholungs-Rhythmus:
 * <ul>
 *   <li>Box 1 „Neu"       → Wiederholung nach 1 Tag</li>
 *   <li>Box 2 „Bekannt"   → nach 3 Tagen</li>
 *   <li>Box 3 „Vertraut"  → nach 7 Tagen</li>
 *   <li>Box 4 „Sicher"    → nach 14 Tagen</li>
 *   <li>Box 5 „Gemeistert"→ nach 30 Tagen</li>
 * </ul>
 * Richtig → eine Box hoch. Falsch → zurück auf Box 1.
 */
public class Vocabulary {

    private static final long DAY_MS = 86_400_000L;

    public static final String[] BOX_NAMES = {
        "Neu", "Bekannt", "Vertraut", "Sicher", "Gemeistert"
    };
    public static final String[] BOX_INTERVALS = {
        "1 Tag", "3 Tage", "7 Tage", "14 Tage", "30 Tage"
    };

    private String id;
    private String word;
    private String translation;
    private String category;
    private String language;      // z.B. "Englisch", "Latein", "Französisch"
    private int box;              // Leitner-Box 1–5
    private int correctCount;
    private int wrongCount;
    private long lastPracticed;   // Epoch-Millis
    private long nextReview;      // Epoch-Millis
    private long createdAt;       // Epoch-Millis

    /** Gson braucht einen leeren Konstruktor. */
    public Vocabulary() {}

    public Vocabulary(String word, String translation, String category, String language) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.word = word;
        this.translation = translation;
        this.category = category;
        this.language = language != null ? language : "Englisch";
        this.box = 1;
        this.createdAt = Instant.now().toEpochMilli();
        this.nextReview = this.createdAt; // sofort lernbar
    }

    // ── Spaced Repetition ────────────────────────────────

    /** Richtig beantwortet → eine Box hoch, nächstes Review berechnen. */
    public void markCorrect() {
        correctCount++;
        lastPracticed = Instant.now().toEpochMilli();
        if (box < 5) box++;
        nextReview = calcNextReview();
    }

    /** Falsch beantwortet → zurück auf Box 1. */
    public void markWrong() {
        wrongCount++;
        lastPracticed = Instant.now().toEpochMilli();
        box = 1;
        nextReview = calcNextReview();
    }

    /** Box manuell setzen (für Box-Verwaltung). */
    public void setBox(int box) {
        this.box = Math.max(1, Math.min(5, box));
        this.nextReview = calcNextReview();
    }

    /** Box zurücksetzen (Lernfortschritt löschen). */
    public void resetProgress() {
        this.box = 1;
        this.correctCount = 0;
        this.wrongCount = 0;
        this.lastPracticed = 0;
        this.nextReview = Instant.now().toEpochMilli();
    }

    private long calcNextReview() {
        long now = Instant.now().toEpochMilli();
        return switch (box) {
            case 1 -> now + DAY_MS;
            case 2 -> now + 3 * DAY_MS;
            case 3 -> now + 7 * DAY_MS;
            case 4 -> now + 14 * DAY_MS;
            case 5 -> now + 30 * DAY_MS;
            default -> now + DAY_MS;
        };
    }

    /** Ist die Vokabel fällig zur Wiederholung? */
    public boolean isDue() {
        return Instant.now().toEpochMilli() >= nextReview;
    }

    /** Erfolgsquote 0.0–1.0 */
    public double getSuccessRate() {
        int total = correctCount + wrongCount;
        return total == 0 ? 0.0 : (double) correctCount / total;
    }

    /** Wurde mindestens einmal geübt? */
    public boolean hasBeenPracticed() {
        return lastPracticed > 0;
    }

    /** Ist gemeistert (Box 5)? */
    public boolean isMastered() {
        return box >= 5;
    }

    /** Name der aktuellen Box (z.B. „Vertraut"). */
    public String getBoxName() {
        return BOX_NAMES[Math.max(0, Math.min(4, box - 1))];
    }

    /** Wiederholungs-Intervall der aktuellen Box. */
    public String getBoxInterval() {
        return BOX_INTERVALS[Math.max(0, Math.min(4, box - 1))];
    }

    // ── Getters & Setters ────────────────────────────────

    public String getId()             { return id; }
    public String getWord()           { return word; }
    public String getTranslation()    { return translation; }
    public String getCategory()       { return category; }
    public String getLanguage()       { return language != null ? language : "Englisch"; }
    public int    getBox()            { return box; }
    public int    getCorrectCount()   { return correctCount; }
    public int    getWrongCount()     { return wrongCount; }
    public long   getLastPracticed()  { return lastPracticed; }
    public long   getNextReview()     { return nextReview; }
    public long   getCreatedAt()      { return createdAt; }

    public void setWord(String word)               { this.word = word; }
    public void setTranslation(String translation) { this.translation = translation; }
    public void setCategory(String category)       { this.category = category; }
    public void setLanguage(String language)        { this.language = language; }

    @Override
    public String toString() {
        return word + " → " + translation + " [" + getLanguage() + ", Box " + box + " " + getBoxName() + "]";
    }
}
