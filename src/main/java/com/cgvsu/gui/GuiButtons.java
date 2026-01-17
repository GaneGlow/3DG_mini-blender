package com.cgvsu.gui;

import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.model.ModelPreparationUtils;
import com.cgvsu.objreader.ObjReader;
import com.cgvsu.render_engine.RenderSettings;
import com.cgvsu.render_engine.Texture;
import com.cgvsu.render_engine.Transform;
import com.cgvsu.render_engine.TransformComponent;
import com.cgvsu.render_engine.camera_gizmo.CameraManager;
import com.cgvsu.render_engine.scene.Scene;
import com.cgvsu.render_engine.scene.SceneObject;
import javafx.event.ActionEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GuiButtons {

    private final GuiController controller;
    private final GuiMethods guiMethods;
    private final Scene scene;
    private final RenderSettings renderSettings;
    private final List<SceneObject> selectedObjects;
    private final Map<SceneObject, Transform> initialTransforms = new HashMap<>();

    public GuiButtons(GuiController controller, GuiMethods guiMethods, Scene scene,
                      RenderSettings renderSettings, List<SceneObject> selectedObjects) {
        this.controller = controller;
        this.guiMethods = guiMethods;
        this.scene = scene;
        this.renderSettings = renderSettings;
        this.selectedObjects = selectedObjects;
    }

    public void onOpenModelMenuItemClick(Canvas canvas) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Model (*.obj)", "*.obj"));
        fileChooser.setTitle("Load Model");

        File file = fileChooser.showOpenDialog((Stage) canvas.getScene().getWindow());
        if (file == null) return;

        Path fileName = Path.of(file.getAbsolutePath());

        try {
            String fileContent = Files.readString(fileName);
            Model mesh = ObjReader.read(fileContent);
            mesh = ModelPreparationUtils.prepare(mesh);

            String objectName = file.getName().replace(".obj", "");
            SceneObject newObject = new SceneObject(objectName, mesh, controller.getCurrentTexture());

            Transform initialTransform = new Transform();
            initialTransforms.put(newObject, initialTransform);

            scene.addObject(newObject);
            newObject.setWireframeColor(javafx.scene.paint.Color.WHITE);
            newObject.setModelColor(renderSettings.baseColor);

            if (!selectedObjects.isEmpty() && selectedObjects.contains(newObject)) {
                guiMethods.applySettingsToSelected(renderSettings, selectedObjects);
            }

            guiMethods.updateModelInfoLabel();
            guiMethods.updateModelsListView();

        } catch (IOException exception) {
            showAlert("Ошибка", "Ошибка загрузки модели: " + exception.getMessage());
        }
    }

    public Texture onOpenTextureMenuItemClick(Canvas canvas, Label textureInfoLabel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));

        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file == null) return null;

        try {
            Image image = new Image(file.toURI().toString());
            Texture texture = new Texture(image);

            String textureInfo = String.format("Текстура: %s (%dx%d)",
                    file.getName(),
                    (int) image.getWidth(),
                    (int) image.getHeight());

            if (textureInfoLabel != null) {
                textureInfoLabel.setText(textureInfo);
            }

            return texture;

        } catch (Exception exception) {
            showAlert("Ошибка", "Ошибка загрузки текстуры: " + exception.getMessage());
            return null;
        }
    }

    public void onResetTranslationButtonClick(List<SceneObject> selectedObjects) {
        resetTransformComponent("перемещения", TransformComponent.TRANSLATION, selectedObjects);
    }

    public void onResetRotationButtonClick(List<SceneObject> selectedObjects) {
        resetTransformComponent("вращения", TransformComponent.ROTATION, selectedObjects);
    }

    public void onResetScaleButtonClick(List<SceneObject> selectedObjects) {
        resetTransformComponent("масштаба", TransformComponent.SCALE, selectedObjects);
    }

    private void resetTransformComponent(String componentName, TransformComponent component,
                                         List<SceneObject> selectedObjects) {
        if (selectedObjects.isEmpty()) {
            showAlert("Нет выделенных объектов", "Выберите объект для сброса " + componentName);
            return;
        }

        for (SceneObject obj : selectedObjects) {
            Transform currentTransform = obj.getTransform();
            if (currentTransform == null) {
                currentTransform = new Transform();
                obj.setTransform(currentTransform);
            }

            if (component == TransformComponent.TRANSLATION) {
                currentTransform.setTranslation(new Vector3(0, 0, 0));
            } else if (component == TransformComponent.ROTATION) {
                currentTransform.setRotation(new Vector3(0, 0, 0));
            } else if (component == TransformComponent.SCALE) {
                currentTransform.setScale(new Vector3(1, 1, 1));
            }
        }

        guiMethods.updateTransformSpinnersFromSelection();
    }

    public void onDeleteSelectedButtonClick(ActionEvent event, List<SceneObject> selectedObjects,
                                            ListView<SceneObject> modelsListView, SceneObject hoveredObject) {
        if (selectedObjects.isEmpty()) {
            showAlert("Нет выделенных объектов",
                    "Пожалуйста, выберите один или несколько объектов для удаления.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Удаление объектов");
        confirmation.setHeaderText("Удалить выбранные объекты?");
        confirmation.setContentText("Вы собираетесь удалить " + selectedObjects.size() +
                " объект(ов).");

        ButtonType deleteButton = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(deleteButton, cancelButton);

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == deleteButton) {
            scene.removeSelectedObjects(selectedObjects);
            modelsListView.getItems().removeAll(selectedObjects);
            selectedObjects.clear();

            guiMethods.updateUIFromSelectedObjects();
            guiMethods.updateModelInfoLabel();

            showAlert("Удаление", "Модели успешно удалены!");
        }
    }

    public void onAddCamera(ActionEvent event, CameraManager cameraManager) {
        if (cameraManager.addNewCamera()) {
            showAlert("Камера добавлена",
                    "Добавлена новая камера. Всего камер: " + cameraManager.getCameraCount());
        } else {
            showAlert("Максимальное количество камер",
                    "Достигнуто максимальное количество камер (8).");
        }
    }

    void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}