package com.vokabeltrainer;

import javafx.application.Application;

/**
 * Launcher-Klasse (ohne JavaFX-Vererbung) –
 * nötig damit jpackage und fat-JARs korrekt funktionieren.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(VokabelTrainerApp.class, args);
    }
}
