package fr.seynax.solvia.desktop.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DashboardDateTimeSelector extends VBox {

    private static final Locale DISPLAY_LOCALE = Locale.FRANCE;
    public static final DateTimeFormatter SUMMARY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", DISPLAY_LOCALE);

    private final Label title;
    private final DatePicker date = new DatePicker();
    private final Spinner<Integer> hour = spinner(0, 23, 0);
    private final Spinner<Integer> minute = spinner(0, 59, 0);
    private final Spinner<Integer> second = spinner(0, 59, 0);
    private final Label summary = Ui.help("Non défini : axe automatique.");
    private final Button startOfDay = smallButton("00:00:00");
    private final Button noon = smallButton("12:00");
    private final Button endOfDay = smallButton("23:59:59");
    private final Button now = smallButton("Maintenant");
    private final Button clear = smallButton("Auto");

    public DashboardDateTimeSelector(String titleText) {
        getStyleClass().add("date-time-selector");
        setSpacing(8);
        title = Ui.label(titleText, "date-time-title");
        date.getStyleClass().add("date-time-date");
        date.setPromptText("Date");
        date.valueProperty().addListener((observable, oldValue, newValue) -> refreshSummary());
        hour.valueProperty().addListener((observable, oldValue, newValue) -> refreshSummary());
        minute.valueProperty().addListener((observable, oldValue, newValue) -> refreshSummary());
        second.valueProperty().addListener((observable, oldValue, newValue) -> refreshSummary());

        startOfDay.setOnAction(event -> setTime(LocalTime.MIN));
        noon.setOnAction(event -> setTime(LocalTime.NOON));
        endOfDay.setOnAction(event -> setTime(LocalTime.of(23, 59, 59)));
        now.setOnAction(event -> setValue(LocalDateTime.now()));
        clear.setOnAction(event -> clear());

        HBox timeRow = new HBox(6, chip("h"), hour, chip("min"), minute, chip("s"), second);
        timeRow.getStyleClass().add("date-time-time-row");
        timeRow.setAlignment(Pos.CENTER_LEFT);

        HBox quickRow = new HBox(6, startOfDay, noon, endOfDay, now, clear);
        quickRow.getStyleClass().add("date-time-quick-row");
        quickRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, date, timeRow, quickRow, summary);
        refreshSummary();
    }

    public LocalDateTime value() {
        LocalDate selectedDate = date.getValue();
        if (selectedDate == null) {
            return null;
        }
        return LocalDateTime.of(selectedDate, LocalTime.of(hour.getValue(), minute.getValue(), second.getValue()));
    }

    public void setValue(LocalDateTime value) {
        if (value == null) {
            clear();
            return;
        }
        date.setValue(value.toLocalDate());
        hour.getValueFactory().setValue(value.getHour());
        minute.getValueFactory().setValue(value.getMinute());
        second.getValueFactory().setValue(value.getSecond());
        refreshSummary();
    }

    public void clear() {
        date.setValue(null);
        hour.getValueFactory().setValue(0);
        minute.getValueFactory().setValue(0);
        second.getValueFactory().setValue(0);
        refreshSummary();
    }

    public void setPromptValue(LocalDateTime value) {
        date.setPromptText(value == null ? "Date" : DateTimeFormatter.ISO_LOCAL_DATE.format(value.toLocalDate()));
    }

    @Override
    public void setDisable(boolean disabled) {
        super.setDisable(disabled);
        date.setDisable(disabled);
        hour.setDisable(disabled);
        minute.setDisable(disabled);
        second.setDisable(disabled);
        startOfDay.setDisable(disabled);
        noon.setDisable(disabled);
        endOfDay.setDisable(disabled);
        now.setDisable(disabled);
        clear.setDisable(disabled);
    }

    private void setTime(LocalTime time) {
        if (date.getValue() == null) {
            date.setValue(LocalDate.now());
        }
        hour.getValueFactory().setValue(time.getHour());
        minute.getValueFactory().setValue(time.getMinute());
        second.getValueFactory().setValue(time.getSecond());
        refreshSummary();
    }

    private void refreshSummary() {
        LocalDateTime value = value();
        summary.setText(value == null ? "Non défini : axe automatique." : SUMMARY_FORMATTER.format(value));
    }

    private static Spinner<Integer> spinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setEditable(true);
        spinner.getStyleClass().add("date-time-spinner");
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
        return spinner;
    }

    private static Label chip(String text) {
        Label label = Ui.label(text, "date-time-chip");
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private static Button smallButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("date-time-quick-button");
        return button;
    }
}
