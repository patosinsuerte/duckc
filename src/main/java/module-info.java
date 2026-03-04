module org.pato.duckc {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires java.prefs;
    requires java.desktop;

    opens org.pato.duckc to javafx.fxml;
    opens org.pato.duckc.controllers to javafx.fxml;
    exports org.pato.duckc;
}