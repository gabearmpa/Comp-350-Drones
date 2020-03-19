/**
 * Cyan Team
 * Author: Shaun Jorstad
 * <p>
 * controller for the Results FXML document
 */

package gui.controllers;

import gui.Navigation;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Results implements Initializable {

    public Button exportButton;
    public Button results;
    public Button home;
    public Button settings;
    public VBox vBox;
    public Label resultsTitle;
    public HBox navBar;
    public Button back;
    public ImageView backImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        results.setStyle("-fx-underline:true");

        File backFile = new File("assets/backArrow.png");
        Image backArrowImage = new Image(backFile.toURI().toString());
        backImage.setImage(backArrowImage);
    }

    public void handleNavigateHome(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.<Parent>load(getClass().getResource("/gui/layouts/Splash.fxml"));
        Navigation.inflateScene(root, "Splash", (Stage) home.getScene().getWindow());
        Navigation.pushScene("Results");
    }

    public void handleExport(ActionEvent actionEvent) {
        System.out.println();
    }

    public void HandleNavigateSettings(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.<Parent>load(getClass().getResource("/gui/layouts/FoodItems.fxml"));
        Navigation.inflateScene(root, "FoodItems", (Stage) home.getScene().getWindow());
        Navigation.pushScene("Results");
    }

    public void handleNavigateResults(ActionEvent actionEvent) {
        System.out.println("already in settings");
    }


    public void handleNavigateBack(ActionEvent actionEvent) throws IOException {
        String lastScene = Navigation.popScene();
        if (lastScene == null)
            return;
        String path = "/gui/layouts/" + lastScene + ".fxml";
        Parent root = FXMLLoader.<Parent>load(getClass().getResource(path));
        Navigation.inflateScene(root, lastScene, (Stage) home.getScene().getWindow());
    }
}