package fr.seynax.solvia.desktop.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;

/**
 * Captures the currently focused node before a non-destructive refresh and restores it after
 * the JavaFX scene graph has been updated.
 */
public final class FocusSnapshot {

    private final Scene scene;
    private final Node focusedNode;
    private final int anchor;
    private final int caretPosition;

    private FocusSnapshot(Scene scene, Node focusedNode, int anchor, int caretPosition) {
        this.scene = scene;
        this.focusedNode = focusedNode;
        this.anchor = anchor;
        this.caretPosition = caretPosition;
    }

    public static FocusSnapshot capture(Node scope) {
        if (scope == null || scope.getScene() == null) {
            return empty();
        }
        Scene scene = scope.getScene();
        Node focused = scene.getFocusOwner();
        if (focused == null) {
            return empty();
        }
        if (focused instanceof TextInputControl input) {
            return new FocusSnapshot(scene, focused, input.getAnchor(), input.getCaretPosition());
        }
        return new FocusSnapshot(scene, focused, -1, -1);
    }

    public void restoreLater() {
        if (focusedNode == null) {
            return;
        }
        Platform.runLater(this::restoreNow);
    }

    private void restoreNow() {
        if (focusedNode.getScene() != scene || focusedNode.isDisabled()) {
            return;
        }
        focusedNode.requestFocus();
        if (focusedNode instanceof TextInputControl input && anchor >= 0 && caretPosition >= 0) {
            int length = input.getLength();
            int safeAnchor = clamp(anchor, length);
            int safeCaret = clamp(caretPosition, length);
            input.selectRange(safeAnchor, safeCaret);
        }
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    private static FocusSnapshot empty() {
        return new FocusSnapshot(null, null, -1, -1);
    }
}
