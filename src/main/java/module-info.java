module com.dinidu.demochatapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.dinidu.demochatapp to javafx.fxml;
    opens com.dinidu.demochatapp.controller to javafx.fxml;
    exports com.dinidu.demochatapp;
    exports com.dinidu.demochatapp.controller to javafx.fxml;
}