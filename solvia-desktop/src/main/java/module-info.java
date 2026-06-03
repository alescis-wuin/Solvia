module fr.seynax.solvia.desktop {
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires javafx.controls;

    exports fr.seynax.solvia.desktop;
    exports fr.seynax.solvia.desktop.api;
    exports fr.seynax.solvia.desktop.ui;
}
