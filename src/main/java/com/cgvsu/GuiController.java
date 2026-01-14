package com.cgvsu;

import com.cgvsu.math.Vector3;
import com.cgvsu.model.ModelPreparationUtils;
import com.cgvsu.render_engine.Texture;
import com.cgvsu.render_engine.RenderEngine;
import com.cgvsu.render_engine.RenderSettings;
import javafx.fxml.FXML;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.File;

import com.cgvsu.model.Model;
import com.cgvsu.objreader.ObjReader;
import com.cgvsu.render_engine.Camera;
import javafx.scene.image.Image;

public class GuiController {

    final private float TRANSLATION = 0.5F;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private Canvas canvas;

    @FXML
    private CheckBox wireframeCheckBox;

    @FXML
    private CheckBox textureCheckBox;

    @FXML
    private CheckBox lightingCheckBox;

    @FXML
    private VBox rightPanel;

    @FXML
    private Label panelTitle;

    @FXML
    private Label modelInfoLabel;

    @FXML
    private Label textureInfoLabel;

    private final RenderSettings renderSettings = new RenderSettings();
    private Texture texture = null;

    private Model mesh = null;

    private Camera camera = new Camera(
            new Vector3(0, 0, 100),
            new Vector3(0, 0, 0),
            1.0F, 1, 0.01F, 100);

    private Timeline timeline;

    private Color backgroundColor = Color.web("#313131");

    @FXML
    private void initialize() {
        // Настройка адаптивных размеров
        anchorPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue() - 250);
        });

        anchorPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue() - 30);
        });

        if (modelInfoLabel != null) {
            modelInfoLabel.setText("Модель не загружена");
        }

        if (textureInfoLabel != null) {
            textureInfoLabel.setText("Текстура не загружена");
        }

        timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame frame = new KeyFrame(Duration.millis(15), event -> {
            double width = canvas.getWidth();
            double height = canvas.getHeight();

            canvas.getGraphicsContext2D().clearRect(0, 0, width, height);
            camera.setAspectRatio((float) (width / height));

            if (mesh != null) {
                RenderEngine.render(
                        canvas.getGraphicsContext2D(),
                        camera,
                        mesh,
                        texture,
                        renderSettings,
                        (int) width,
                        (int) height
                );
            }
            renderFrame();
        });

        wireframeCheckBox.selectedProperty().addListener((o, a, b) ->
                renderSettings.drawWireframe = b);

        textureCheckBox.selectedProperty().addListener((o, a, b) ->
                renderSettings.useTexture = b);

        lightingCheckBox.selectedProperty().addListener((o, a, b) ->
                renderSettings.useLighting = b);


        timeline.getKeyFrames().add(frame);
        timeline.play();
    }

    private void renderFrame() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if (width <= 0 || height <= 0) return;

        // Очищаем Canvas с цветом фона
        canvas.getGraphicsContext2D().setFill(backgroundColor);
        canvas.getGraphicsContext2D().fillRect(0, 0, width, height);

        camera.setAspectRatio((float) (width / height));

        if (mesh != null) {
            RenderEngine.render(canvas.getGraphicsContext2D(), camera, mesh, texture,
                    renderSettings, (int) width, (int) height);
        }
    }

    @FXML
    private void onOpenModelMenuItemClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Model (*.obj)", "*.obj"));
        fileChooser.setTitle("Load Model");

        File file = fileChooser.showOpenDialog((Stage) canvas.getScene().getWindow());
        if (file == null) {
            return;
        }

        Path fileName = Path.of(file.getAbsolutePath());

        try {
            String fileContent = Files.readString(fileName);

            mesh = ObjReader.read(fileContent);
            mesh = ModelPreparationUtils.prepare(mesh);

            // Обновляем информацию о модели на нижней панели
            String infoText = String.format("Файл: %s || Вершин: %d || Полигонов: %d",
                    file.getName(),
                    mesh.vertices.size(),
                    mesh.polygons.size());

            if (modelInfoLabel != null) {
                modelInfoLabel.setText(infoText);
            }

        } catch (IOException exception) {
            System.err.println("Ошибка загрузки модели: " + exception.getMessage());
            System.err.println("Error loading model: " + exception.getMessage());
        }
    }

    @FXML
    private void onOpenTextureMenuItemClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        //fileChooser.setTitle("Load Texture");

        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            Image image = new Image(file.toURI().toString());
            texture = new Texture(image);
            renderSettings.useTexture = true;
            textureCheckBox.setSelected(true);

            String textureInfo = String.format("Текстура: %s (%dx%d)",
                    file.getName(),
                    (int) image.getWidth(),
                    (int) image.getHeight());

            if (textureInfoLabel != null) {
                textureInfoLabel.setText(textureInfo);
            }

        } catch (Exception exception) {
            System.err.println("Ошибка загрузки текстуры: " + exception.getMessage());
        }
    }

    @FXML
    public void handleCameraForward(ActionEvent actionEvent) {
        camera.movePosition(new Vector3(0, 0, -TRANSLATION));
    }

    @FXML
    public void handleCameraBackward(ActionEvent actionEvent) {
        camera.movePosition(new Vector3(0, 0, TRANSLATION));
    }

    @FXML
    public void handleCameraLeft(ActionEvent actionEvent) {
        camera.movePosition(new Vector3(TRANSLATION, 0, 0));
    }

    @FXML
    public void handleCameraRight(ActionEvent actionEvent) {
        camera.movePosition(new Vector3(-TRANSLATION, 0, 0));
    }

    @FXML
    public void handleCameraUp(ActionEvent actionEvent) {
        camera.movePosition(new Vector3(0, TRANSLATION, 0));
    }

    @FXML
    public void handleCameraDown(ActionEvent actionEvent) {
        camera.movePosition(new Vector3(0, -TRANSLATION, 0));
    }
}