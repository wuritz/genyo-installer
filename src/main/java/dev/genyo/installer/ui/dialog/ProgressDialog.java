package dev.genyo.installer.ui.dialog;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class ProgressDialog {

    private final Stage stage;
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label bytesLabel = new Label("0 B / 0 B");

    public ProgressDialog(Window owner) {
        stage = new Stage(StageStyle.UTILITY);
        stage.setTitle("Installing Genyo...");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);

        progressBar.setPrefWidth(320);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        Label title = new Label("Downloading the latest Genyo Addon release...");
        root.getChildren().addAll(title, progressBar, bytesLabel);

        stage.setScene(new Scene(root, 360, 130));
    }

    public void show() {
        stage.show();
    }

    public void close() {
        if (stage.isShowing()) {
            stage.close();
        }
    }

    /** Binds the progress bar + byte label to a running download {@link Task}. */
    public void bindTo(Task<?> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        bytesLabel.textProperty().bind(task.messageProperty());
    }

}
