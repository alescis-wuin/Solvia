module fr.seynax.solvia.desktop {
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires atlantafx.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;

    exports fr.seynax.solvia.desktop;
    exports fr.seynax.solvia.desktop.api;
    exports fr.seynax.solvia.desktop.ui;

    opens fr.seynax.solvia.desktop.api to com.fasterxml.jackson.databind;
    opens fr.seynax.solvia.desktop.ui to javafx.base;
}
