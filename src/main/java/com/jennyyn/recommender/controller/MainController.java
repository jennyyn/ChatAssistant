package com.jennyyn.recommender.controller;


import com.jennyyn.recommender.model.*;
import com.jennyyn.recommender.service.APIService;
import com.jennyyn.recommender.service.FileService;
import com.jennyyn.recommender.view.MainFrame;
import com.jennyyn.recommender.view.WritingPanel;
import javax.swing.JOptionPane;
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

    public void handleRewriteRequest(String text, String mode) {
        // Check for empty input first
        if (text == null || text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Please enter some text before rewriting.",
                    "Input Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return; // stop further execution
        }

        WritingStrategy strategy;

        switch (mode) {
            case "Creative": strategy = new CreativeStrategy(); break;
            case "Academic": strategy = new AcademicStrategy(); break;
            case "Professional": strategy = new ProfessionalStrategy(); break;
            default: strategy = new CreativeStrategy(); // fallback
        }

        try {
            RewriteResult result = apiService.rewriteText(text, strategy);
            mainFrame.displayResult(result.getRewrittenText());
        } catch (Exception e) {
            // Show an error dialog if API call fails
            JOptionPane.showMessageDialog(
                    null,
                    "Error: Unable to contact API. Please check your internet connection or API key and try again.",
                    "API Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
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

        // Ask the View which session to load
        int choice = writingPanel.showSessionSelectionDialog(options);
        if (choice >= 0) {
            String[] data = allSessions.get(choice);
            writingPanel.setOriginalText(data[0]);
            writingPanel.setRewrittenText(data[1]);
        }
    }

}


