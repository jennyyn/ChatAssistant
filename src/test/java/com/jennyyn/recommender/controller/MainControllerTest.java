package com.jennyyn.recommender.controller;

import com.jennyyn.recommender.model.RateLimitException;
import com.jennyyn.recommender.model.RewriteResult;
import com.jennyyn.recommender.service.APIService;
import com.jennyyn.recommender.service.FileService;
import com.jennyyn.recommender.view.MainFrame;
import com.jennyyn.recommender.view.WritingPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    APIService apiService;

    @Mock
    FileService fileService;

    @Mock
    MainFrame mainFrame;

    @Mock
    WritingPanel writingPanel;

    MainController controller;

    @BeforeEach
    void setup() {
        controller = new MainController(apiService, fileService, mainFrame, writingPanel);
    }

    // --------------------------------------------------
    // 1. Empty input should NOT call API
    // --------------------------------------------------
    @Test
    void handleRewriteRequest_emptyInput_doesNothing() {
        controller.handleRewriteRequest("", "Creative");

        verify(apiService, never()).rewriteTextAsync(any(), any(), any(), any(), any());
    }

    // --------------------------------------------------
    // 2. Successful rewrite updates UI
    // --------------------------------------------------
    @Test
    void handleRewriteRequest_success_callsDisplayResult() throws Exception {
        ArgumentCaptor<Consumer<RewriteResult>> successCaptor =
                ArgumentCaptor.forClass(Consumer.class);

        doNothing().when(apiService).rewriteTextAsync(
                anyString(),
                any(),
                successCaptor.capture(),
                any(),
                any()
        );

        controller.handleRewriteRequest("Hello", "Creative");

        // Simulate API success
        successCaptor.getValue().accept(new RewriteResult("Rewritten text"));

        // Let the EDT process the invokeLater()
        SwingUtilities.invokeAndWait(() -> { /* no-op, just flush EDT */ });

        verify(mainFrame).displayResult("Rewritten text");
    }

    // --------------------------------------------------
    // 3. Cancel request stops API and resets UI
    // --------------------------------------------------
    @Test
    void handleCancelRequest_callsCancelAndResetsUI() throws Exception {
        controller.handleCancelRequest();

        // Wait for Swing EDT to process invokeLater()
        SwingUtilities.invokeAndWait(() -> {});

        verify(apiService).cancel();
        verify(writingPanel).showLoadingState(false);
        verify(writingPanel).setRewriteEnabled(true);
    }

}

