package com.jennyyn.recommender.controller;


import com.jennyyn.recommender.model.*;
import com.jennyyn.recommender.service.APIService;
import com.jennyyn.recommender.service.FileService;
import com.jennyyn.recommender.view.MainFrame;
import com.jennyyn.recommender.view.WritingPanel;
import javax.swing.*;
import java.util.List;


public class MainController {

    private final APIService apiService;
    private final MainFrame mainFrame;
    private final WritingPanel writingPanel;
    private final FileService fileService;


    public MainController(MainFrame mainFrame, WritingPanel writingPanel) {
        this.apiService = new APIService();
        this.fileService = new FileService();
        this.mainFrame = mainFrame;
        this.writingPanel = writingPanel;
    }

    // TESTING CONSTRUCTOR (dependency injection)
    public MainController(
            APIService apiService,
            FileService fileService,
            MainFrame mainFrame,
            WritingPanel writingPanel
    ) {
        this.apiService = apiService;
        this.fileService = fileService;
        this.mainFrame = mainFrame;
        this.writingPanel = writingPanel;
    }


    public void handleRewriteRequest(String text, String mode) {
        // 1. Check for empty input first
        if (text == null || text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Please enter some text before rewriting.",
                    "Input Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return; // stop further execution
        }

        //2. strategy selection
        WritingStrategy strategy;
        switch (mode) {
            case "Creative": strategy = new CreativeStrategy();break;
            case "Academic": strategy = new AcademicStrategy();break;
            case "Professional": strategy = new ProfessionalStrategy();break;
            default:
                strategy = new CreativeStrategy(); // fallback
        }

        // 3. Update UI and show processing state
        SwingUtilities.invokeLater(() -> {
            writingPanel.showLoadingState(true);
            writingPanel.setRewriteEnabled(false);
        });

        // 4. Make async API call
        apiService.rewriteTextAsync(
                text,
                strategy,

                // SUCCESS
                result -> SwingUtilities.invokeLater(() -> {
                    mainFrame.displayResult(result.getRewrittenText());
                }),

                // ERROR
                // ERROR callback
                error -> SwingUtilities.invokeLater(() -> {
                    if (error instanceof RateLimitException) {
                        JOptionPane.showMessageDialog(
                                null,
                                "You're sending requests too quickly. Please wait a few seconds.",
                                "Rate Limit",
                                JOptionPane.WARNING_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                                null,
                                "Error: Unable to contact API. Please check your internet connection or API key.",
                                "API Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }),

                // FINALLY callback (always runs)
                () -> SwingUtilities.invokeLater(() -> {
                    writingPanel.showLoadingState(false);
                    writingPanel.setRewriteEnabled(true);
                })
        );
    }

    public void handleSaveRequest() {
        fileService.saveSession(
                writingPanel.getOriginalText(),
                writingPanel.getRewrittenText()
        );
    }

    public void handleLoadRequest() {
        List<String[]> allSessions = fileService.loadSession();
        if (allSessions.isEmpty()) return;

        // Create snippet list
        String[] options = new String[allSessions.size()];
        for (int i = 0; i < allSessions.size(); i++) {
            String snippet = allSessions.get(i)[0];
            snippet = snippet.length() > 30 ? snippet.substring(0, 30) + "..." : snippet;
            options[i] = "Session " + (i + 1) + ": " + snippet;
        }

        writingPanel.showSessionListWindow(
                options,

                // --- onLoad callback ---
                () -> {
                    int selected = (int) writingPanel.getClientProperty("selectedIndex");
                    String[] data = allSessions.get(selected);
                    writingPanel.setOriginalText(data[0]);
                    writingPanel.setRewrittenText(data[1]);
                },

                // --- onDelete callback ---
                () -> {
                    int selected = (int) writingPanel.getClientProperty("selectedIndex");
                    fileService.deleteSession(selected);
                }
        );

    }

    public void handleCancelRequest() {
        apiService.cancel();
        SwingUtilities.invokeLater(() -> {
            writingPanel.showLoadingState(false);
            writingPanel.setRewriteEnabled(true);
        });

    }

}


