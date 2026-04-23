module com.soc {
    requires javafx.controls;
    requires javafx.fxml;
    requires okhttp3;
    requires com.google.gson;
    requires java.desktop;
    requires java.net.http;

    opens com.soc to javafx.fxml;
    opens com.soc.views to javafx.fxml;

    exports com.soc;
    exports com.soc.views;
}
