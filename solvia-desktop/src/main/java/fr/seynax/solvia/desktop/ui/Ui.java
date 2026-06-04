package fr.seynax.solvia.desktop.ui;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

public final class Ui {

    private Ui() {
    }

    public static Label label(String text, String styleClass) {
        Label label = new Label(text);
        if (styleClass != null && !styleClass.isBlank()) {
            label.getStyleClass().add(styleClass);
        }
        return label;
    }

    public static Label body(String text) {
        return label(text, "body-text");
    }

    public static Label help(String text) {
        Label label = label(text, "help-text");
        label.setWrapText(true);
        return label;
    }

    public static Label sectionTitle(String text) {
        return label(text, "section-title");
    }

    public static Label fieldLabel(String text, Control control) {
        Label label = label(text, "field-label");
        if (control != null) {
            label.setLabelFor(control);
            control.setAccessibleText(text);
        }
        return label;
    }

    public static <T extends Control> T tooltip(T control, String text) {
        if (control != null && text != null && !text.isBlank()) {
            control.setTooltip(new Tooltip(text));
            control.setAccessibleHelp(text);
        }
        return control;
    }

    public static <T extends Node> T style(T node, String... styleClasses) {
        if (node == null || styleClasses == null) {
            return node;
        }
        for (String styleClass : styleClasses) {
            if (styleClass != null && !styleClass.isBlank() && !node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        }
        return node;
    }
}
