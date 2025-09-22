package it.glucotrack.component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CustomPopupController {
    @FXML private StackPane popupRoot;
    @FXML private javafx.scene.control.Button closeButton;
    @FXML
    private void handleClose() {
        if (stage != null) {
            stage.close();
        } else if (closeButton != null) {
            // fallback: chiudi la finestra del bottone
            closeButton.getScene().getWindow().hide();
        }
    }
    @FXML private VBox contentBox;
    @FXML private VBox popupContent;
    @FXML private Label popupTitle;
    @FXML private Label popupSubtitle;

    private Stage stage;
    private boolean dragEnabled = true;

    public void setStage(Stage stage) {
        setStage(stage, true);
    }

    public void setStage(Stage stage, boolean dragEnabled) {
        this.stage = stage;
        this.dragEnabled = dragEnabled;
    }

    public void setTitle(String title) {
        popupTitle.setText(title);
    }
    public void setSubtitle(String subtitle) {
        popupSubtitle.setText(subtitle);
    }
    public VBox getPopupContent() {
        return popupContent;
    }

    public void setContent(VBox content) {
        contentBox.getChildren().setAll(content);
    }
}
