package com.jennyyn.recommender.view;

import com.jennyyn.recommender.controller.MainController;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WritingPanelTest {

    // -------------------------------
    // Helper methods
    // -------------------------------

    private <T> T getPrivateField(Object obj, String fieldName, Class<T> type) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return type.cast(f.get(obj));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton btn && text.equals(btn.getText())) {
                return btn;
            }
            if (c instanceof Container container) {
                JButton found = findButton(container, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    // -------------------------------
    // View-only tests (NO Mockito)
    // -------------------------------

    @Test
    void setAndGetOriginalText() {
        WritingPanel panel = new WritingPanel();
        panel.setOriginalText("Hello");
        assertEquals("Hello", panel.getOriginalText());
    }

    @Test
    void setAndGetRewrittenText() {
        WritingPanel panel = new WritingPanel();
        panel.setRewrittenText("Done");
        assertEquals("Done", panel.getRewrittenText());
    }

    @Test
    void loadingStateTogglesSpinnerAndCancel() {
        WritingPanel panel = new WritingPanel();

        JLabel spinner = getPrivateField(panel, "spinnerLabel", JLabel.class);
        JButton cancel = getPrivateField(panel, "cancelButton", JButton.class);

        panel.showLoadingState(true);
        assertTrue(spinner.isVisible());
        assertTrue(cancel.isVisible());

        panel.showLoadingState(false);
        assertFalse(spinner.isVisible());
        assertFalse(cancel.isVisible());
    }

    @Test
    void rewriteButtonEnableDisable() {
        WritingPanel panel = new WritingPanel();
        JButton rewrite = getPrivateField(panel, "rewriteButton", JButton.class);

        panel.setRewriteEnabled(false);
        assertFalse(rewrite.isEnabled());

        panel.setRewriteEnabled(true);
        assertTrue(rewrite.isEnabled());
    }

    // -------------------------------
    // Controller interaction tests (Mockito)
    // -------------------------------

    @Test
    void rewriteButtonCallsController() {
        WritingPanel panel = new WritingPanel();
        MainController controller = mock(MainController.class);
        panel.setController(controller);

        panel.setOriginalText("Test text");

        JButton rewrite = getPrivateField(panel, "rewriteButton", JButton.class);
        rewrite.doClick();

        verify(controller).handleRewriteRequest("Test text", "Creative");
    }

    @Test
    void cancelButtonCallsController() {
        WritingPanel panel = new WritingPanel();
        MainController controller = mock(MainController.class);
        panel.setController(controller);

        panel.showLoadingState(true);

        JButton cancel = getPrivateField(panel, "cancelButton", JButton.class);
        cancel.doClick();

        verify(controller).handleCancelRequest();
    }

    @Test
    void saveAndLoadButtonsCallController() {
        WritingPanel panel = new WritingPanel();
        MainController controller = mock(MainController.class);
        panel.setController(controller);

        JButton save = findButton(panel, "Save");
        JButton load = findButton(panel, "Load");

        assertNotNull(save);
        assertNotNull(load);

        save.doClick();
        load.doClick();

        verify(controller).handleSaveRequest();
        verify(controller).handleLoadRequest();
    }
}
