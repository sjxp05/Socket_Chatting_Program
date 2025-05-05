package tests;

import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.event.*;

public class FxPractice extends Application {
    @FXML
    private Label myLabel;

    @FXML
    private Button myButton;

    @FXML // 이거였누....
    private void buttonClick(ActionEvent event) {
        myLabel.setFont(new Font("Segoe UI Emoji", 20));
        myLabel.setText("this text has changed 🌟");
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(this.getClass().getResource("fxWindow.fxml"));
        stage.setTitle("Test Program");
        stage.setScene(new Scene(root));
        // or stage.setScene(new Scene(root, 300, 200));

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
