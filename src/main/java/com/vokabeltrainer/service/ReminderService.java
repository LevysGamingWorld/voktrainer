package com.vokabeltrainer.service;

import com.vokabeltrainer.persistence.VocabStore;
import javafx.application.Platform;

import java.util.concurrent.*;

/**
 * Hintergrund-Erinnerungsdienst – erinnert in regelmäßigen Abständen
 * an das Vokabellernen mit süßen Motivationssprüchen.
 */
public class ReminderService {

    private static ReminderService instance;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> currentTask;

    private ReminderService() {}

    public static ReminderService getInstance() {
        if (instance == null) instance = new ReminderService();
        return instance;
    }

    /**
     * Startet oder neustartet den Reminder-Service.
     */
    public void start() {
        stop();

        VocabStore.Settings settings = VocabStore.getInstance().getSettings();
        if (!settings.remindersEnabled) return;

        int intervalMinutes = Math.max(1, settings.reminderIntervalMinutes);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VokabelTrainer-Reminder");
            t.setDaemon(true);
            return t;
        });

        currentTask = scheduler.scheduleAtFixedRate(
            this::sendReminder,
            intervalMinutes,       // erster Reminder nach interval
            intervalMinutes,
            TimeUnit.MINUTES
        );

        System.out.println("ReminderService gestartet: alle " + intervalMinutes + " Minuten");
    }

    /**
     * Stoppt den Reminder-Service.
     */
    public void stop() {
        if (currentTask != null) {
            currentTask.cancel(false);
            currentTask = null;
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /**
     * Neustart mit aktualisierten Einstellungen.
     */
    public void restart() {
        start();
    }

    public boolean isRunning() {
        return currentTask != null && !currentTask.isCancelled();
    }

    private void sendReminder() {
        try {
            // Nur erinnern wenn es fällige Vokabeln gibt oder regelmäßig motivieren
            Platform.runLater(NotificationPopup::showReminder);
        } catch (Exception e) {
            System.err.println("Reminder-Fehler: " + e.getMessage());
        }
    }
}
