package com.shopsense;

import javafx.application.Application;

/**
 * Launcher class for the ShopSense Application.
 * This class is required as a separate entry point to bypass modular classpath
 * checking in newer versions of JavaFX.
 */
public class Main {
    public static void main(String[] args) {
        // Launch the JavaFX Application
        Application.launch(ShopSenseApp.class, args);
    }
}
