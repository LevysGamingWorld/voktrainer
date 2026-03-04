package com.vokabeltrainer.service;

import java.util.Random;

/**
 * Motivierend & süße Nachrichten – perfekt für deine Freundin 💕
 */
public class FunMessages {

    private static final Random RNG = new Random();

    /** Erinnerungen zum Lernen. */
    private static final String[] REMINDER_MESSAGES = {
        "Hey Prinzessin! Zeit für ein paar Vokabeln!",
        "Du bist soooo schlau, lern doch noch was Voks :)!",
        "Kleine Pause? Perfekt für 5 Minuten Vokabeln!",
        "Deine Vokabeln vermissen dich!",
        "Psst... deine Vokabeln warten auf dich!",
        "Lernzeit! Du schaffst das, ich glaub an dich!",
        "Kurz Vokabeln lernen? Danach hast du's verdient zu chillen!",
        "Hey! Dein Gehirn will gefüttert werden!",
        "Nur 5 Minuten – du wirst staunen wie viel du kannst (alles)!",
        "Deine Vokabeln brauchen Liebe, genau wie du!",
        "Schnell ein paar Wörter lernen? Los geht's!",
        "Hey Prinzessin! Lass ma zssm Voks lernen!",
    };

    /** Motivationssprüche nach korrekter Antwort. */
    private static final String[] CORRECT_MESSAGES = {
        "Richtig! Du bist ein Genie! ",
        "Perfekt! Weiter so, Schatz!",
        "Boom! Voll ins Schwarze!",
        "100 Punkte! So schlau!",
        "Mega! Das sitzt!",
        "Richtig! Dein Gehirn ist on fire!",
        "Yes! Du bist soooo smart!",
        "Genau! Wie aus dem Lehrbuch!",
        "Stark! Weiter so!",
    };

    /** Aufmunterung bei falscher Antwort. */
    private static final String[] WRONG_MESSAGES = {
        "Nicht schlimm! Beim nächsten Mal klappt's!",
        "Fast! Du bist so nah dran!",
        "Übung macht die Meisterin!",
        "Kopf hoch! Das lernst du!",
        "Nächster Versuch wird perfekt!",
        "Fehler sind zum Lernen da!",
        "Halb so wild – weiter geht's!",
        "Dran bleiben – du schaffst das!",
    };

    /** Streak-Nachrichten. */
    private static final String[] STREAK_MESSAGES = {
        "%d Tage Streak! Du bist unglaublich!",
        "%d Tage am Stück! Nicht aufhören!",
        "%d-Tage-Streak! Du bist eine Maschine!",
        "Wow, %d Tage! Disziplin-Queen!",
        "%d Tage Streak! Respekt!",
    };

    /** Tagesziel erreicht. */
    private static final String[] GOAL_REACHED_MESSAGES = {
        "Tagesziel erreicht! Du bist der Wahnsinn!",
        "Geschafft! Du hast dein Ziel erreicht!",
        "Alle Vokabeln für heute durch – Respekt!",
        "Tagesziel erledigt! Du hast Pause verdient!",
        "Ziel erreicht! Ab auf die Couch, Queen!",
    };

    /** Leitner-Box-Aufstieg. */
    private static final String[] BOX_UP_MESSAGES = {
        "Aufgestiegen! Diese Vokabel sitzt immer besser!",
        "Level up! Weiter zur nächsten Box!",
        "Perfekt! Ab in die nächste Box!",
        "Super gelernt – Box-Aufstieg!",
    };

    // ── Public API ───────────────────────────────────────

    public static String getReminder()     { return pick(REMINDER_MESSAGES); }
    public static String getCorrect()      { return pick(CORRECT_MESSAGES); }
    public static String getWrong()        { return pick(WRONG_MESSAGES); }
    public static String getGoalReached()  { return pick(GOAL_REACHED_MESSAGES); }
    public static String getBoxUp()        { return pick(BOX_UP_MESSAGES); }

    public static String getStreak(int days) {
        return String.format(pick(STREAK_MESSAGES), days);
    }

    /** Zusammenfassung für Dashboard-Begrüßung. */
    public static String getDashboardGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 6)  return "Hey Nachteule! Noch wach um diese Uhrzeit? Perfekt für Vokabeln!";
        if (hour < 12) return "Moini Schatz! Bereit für neue Vokabeln?";
        if (hour < 18) return "Hi, meine Prinzessin! Noch ne RUnde Voks?";
        return "Nah? Immernoch wach?? Waaaas um die Uhrzeit?? Na dann geht doch auchnochma kurz Voks lernen! :)";
    }

    private static String pick(String[] arr) {
        return arr[RNG.nextInt(arr.length)];
    }
}
