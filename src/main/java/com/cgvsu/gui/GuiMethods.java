package com.cgvsu.gui;

import com.cgvsu.math.Matrix4;
import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.render_engine.*;
import com.cgvsu.render_engine.scene.Scene;
import com.cgvsu.render_engine.scene.SceneObject;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

public class GuiMethods {

    private final GuiController controller;
    private final Scene scene;
    private final RenderSettings renderSettings;
    private final List<SceneObject> selectedObjects;
    private boolean updatingTransformUI = false;

    public GuiMethods(GuiController controller, Scene scene, RenderSettings renderSettings,
                      List<SceneObject> selectedObjects) {
        this.controller = controller;
        this.scene = scene;
        this.renderSettings = renderSettings;
        this.selectedObjects = selectedObjects;
    }

    public void initializeUI(AnchorPane anchorPane, Canvas canvas, ListView<SceneObject> modelsListView,
                             Label modelInfoLabel, Label textureInfoLabel) {
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

        initializeModelsListView(modelsListView);
    }

    private void initializeModelsListView(ListView<SceneObject> modelsListView) {
        if (modelsListView == null) return;

        modelsListView.setCellFactory(lv -> new ListCell<SceneObject>() {
            @Override
            protected void updateItem(SceneObject item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        modelsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null && !isCameraObject(newVal)) {
                        handleListViewSelection(newVal);
                    }
                }
        );
    }

    private void handleListViewSelection(SceneObject selectedObject) {
        if (isCameraObject(selectedObject)) return;

        selectedObjects.clear();
        selectedObjects.add(selectedObject);
        updateObjectColors();
        updateUIFromSelectedObjects();
        updateModelInfoLabel();
    }

    private boolean isCameraObject(SceneObject obj) {
        return obj != null &&
                (obj.getName() != null &&
                        (obj.getName().toLowerCase().contains("камера") ||
                                obj.getName().toLowerCase().contains("camera")));
    }

    public void handleObjectSelection(int mouseX, int mouseY) {
        SceneObject clickedObject = findObjectUnderCursor(mouseX, mouseY);

        if (clickedObject != null) {
            if (selectedObjects.contains(clickedObject)) {
                selectedObjects.remove(clickedObject);
            } else {
                selectedObjects.add(clickedObject);
            }
        } else {
            selectedObjects.clear();
        }

        updateObjectColors();
        updateUIFromSelectedObjects();
        updateModelsListView();
    }

    public void handleMouseMove(int mouseX, int mouseY) {
        SceneObject oldHovered = controller.getHoveredObject();
        SceneObject newHovered = findObjectUnderCursor(mouseX, mouseY);
        controller.setHoveredObject(newHovered);

        if (oldHovered != newHovered) {
            updateObjectColors();
            updateModelInfoLabel();
        }
    }

    public void handleMouseExit() {
        if (controller.getHoveredObject() != null) {
            controller.setHoveredObject(null);
            updateObjectColors();
            updateModelInfoLabel();
        }
    }

    public void handleMouseDrag(MouseEvent event, double lastMouseX, double lastMouseY,
                                List<SceneObject> selectedObjects) {
        if (selectedObjects.isEmpty()) return;

        double deltaX = event.getX() - lastMouseX;
        double deltaY = event.getY() - lastMouseY;

        if (event.getButton() == MouseButton.SECONDARY) {
            // Вращение
            float rotationSpeed = 0.01f;
            float rotY = (float) (deltaX * rotationSpeed);
            float rotX = (float) (deltaY * rotationSpeed);

            for (SceneObject obj : selectedObjects) {
                Transform t = ensureTransform(obj);
                if (t != null) {
                    t.rotate(rotX, rotY, 0);
                }
            }
        } else if (event.getButton() == MouseButton.PRIMARY) {
            // Перемещение
            float moveSpeed = 0.1f;
            float moveX = (float) (-deltaX * moveSpeed);
            float moveY = (float) (-deltaY * moveSpeed);

            for (SceneObject obj : selectedObjects) {
                Transform t = ensureTransform(obj);
                if (t != null) {
                    t.translate(moveX, moveY, 0);
                }
            }
        }

        updateTransformSpinnersFromSelection();
    }

    public void handleMouseScroll(ScrollEvent event, List<SceneObject> selectedObjects) {
        if (selectedObjects.isEmpty()) return;

        double deltaY = event.getDeltaY();
        float scaleSpeed = 0.01f;
        float scaleFactor = 1.0f + (float) (deltaY * scaleSpeed);
        scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));

        for (SceneObject obj : selectedObjects) {
            Transform t = ensureTransform(obj);
            if (t != null) {
                t.scale(scaleFactor);
            }
        }

        updateTransformSpinnersFromSelection();
        event.consume();
    }

    private Transform ensureTransform(SceneObject obj) {
        if (obj == null) return null;
        Transform t = obj.getTransform();
        if (t == null) {
            t = new Transform();
            obj.setTransform(t);
        }
        return t;
    }

    private SceneObject findObjectUnderCursor(int x, int y) {
        for (SceneObject obj : scene.getObjects()) {
            if (isMouseOverObject(obj, x, y)) {
                return obj;
            }
        }
        return null;
    }

    private boolean isMouseOverObject(SceneObject obj, int mouseX, int mouseY) {
        if (obj == null || obj.getModel() == null) return false;

        Model mesh = obj.getModel();
        Camera camera = scene.getActiveCamera();

        Matrix4 modelMatrix = RenderEngine.getModelMatrix(obj);
        Matrix4 viewMatrix = camera.getViewMatrix();
        Matrix4 projectionMatrix = camera.getProjectionMatrix();
        Matrix4 modelViewProjectionMatrix = projectionMatrix.multiply(viewMatrix).multiply(modelMatrix);

        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        Canvas canvas = controller.getCanvas();
        if (canvas == null) return false;

        for (Vector3 vertex : mesh.vertices) {
            Vector3 screenPos = RenderEngine.transformVertex(vertex, modelViewProjectionMatrix,
                    (int) canvas.getWidth(), (int) canvas.getHeight());

            if (Float.isNaN(screenPos.x) || Float.isNaN(screenPos.y)) {
                continue;
            }

            minX = Math.min(minX, screenPos.x);
            maxX = Math.max(maxX, screenPos.x);
            minY = Math.min(minY, screenPos.y);
            maxY = Math.max(maxY, screenPos.y);
        }

        if (!Float.isFinite(minX) || !Float.isFinite(maxX) ||
                !Float.isFinite(minY) || !Float.isFinite(maxY)) {
            return false;
        }

        return mouseX >= minX && mouseX <= maxX &&
                mouseY >= minY && mouseY <= maxY;
    }

    public void updateObjectColors() {
        for (SceneObject obj : scene.getObjects()) {
            if (selectedObjects.contains(obj)) {
                obj.setWireframeColor(Color.CYAN);
                obj.setModelColor(Color.rgb(173, 216, 230));
            } else if (obj == controller.getHoveredObject()) {
                obj.setWireframeColor(Color.CYAN);
                obj.setModelColor(Color.rgb(173, 216, 230));
            } else {
                obj.setWireframeColor(Color.WHITE);
                obj.setModelColor(renderSettings.baseColor);
            }
        }
    }

    public void updateUIFromSelectedObjects() {
        if (selectedObjects.isEmpty()) {
            controller.getWireframeCheckBox().setSelected(renderSettings.drawWireframe);
            controller.getTextureCheckBox().setSelected(renderSettings.useTexture);
            controller.getLightingCheckBox().setSelected(renderSettings.useLighting);
        } else {
            SceneObject firstSelected = selectedObjects.get(0);
            RenderSettings settings = firstSelected.getRenderSettings();

            if (settings != null) {
                controller.getWireframeCheckBox().setSelected(settings.drawWireframe);
                controller.getTextureCheckBox().setSelected(settings.useTexture);
                controller.getLightingCheckBox().setSelected(settings.useLighting);
            } else {
                controller.getWireframeCheckBox().setSelected(renderSettings.drawWireframe);
                controller.getTextureCheckBox().setSelected(renderSettings.useTexture);
                controller.getLightingCheckBox().setSelected(renderSettings.useLighting);
            }
        }

        updateTransformSpinnersFromSelection();
    }

    public void updateTransformSpinnersFromSelection() {
        updatingTransformUI = true;
        try {
            if (selectedObjects.isEmpty()) {
                resetSpinnersToDefault();
                return;
            }

            SceneObject active = selectedObjects.get(selectedObjects.size() - 1);
            Transform t = (active != null) ? active.getTransform() : null;

            Vector3 tr = (t != null && t.getTranslation() != null) ?
                    t.getTranslation() : new Vector3(0, 0, 0);
            Vector3 rot = (t != null && t.getRotation() != null) ?
                    t.getRotation() : new Vector3(0, 0, 0);
            Vector3 sc = (t != null && t.getScale() != null) ?
                    t.getScale() : new Vector3(1, 1, 1);

            setSpinnerValue(controller.getTranslationXSpinner(), tr.x);
            setSpinnerValue(controller.getTranslationYSpinner(), tr.y);
            setSpinnerValue(controller.getTranslationZSpinner(), tr.z);

            setSpinnerValue(controller.getRotationXSpinner(), Math.toDegrees(rot.x));
            setSpinnerValue(controller.getRotationYSpinner(), Math.toDegrees(rot.y));
            setSpinnerValue(controller.getRotationZSpinner(), Math.toDegrees(rot.z));

            setSpinnerValue(controller.getScaleXSpinner(), sc.x);
            setSpinnerValue(controller.getScaleYSpinner(), sc.y);
            setSpinnerValue(controller.getScaleZSpinner(), sc.z);
        } finally {
            updatingTransformUI = false;
        }
    }

    private void resetSpinnersToDefault() {
        setSpinnerValue(controller.getTranslationXSpinner(), 0.0);
        setSpinnerValue(controller.getTranslationYSpinner(), 0.0);
        setSpinnerValue(controller.getTranslationZSpinner(), 0.0);

        setSpinnerValue(controller.getRotationXSpinner(), 0.0);
        setSpinnerValue(controller.getRotationYSpinner(), 0.0);
        setSpinnerValue(controller.getRotationZSpinner(), 0.0);

        setSpinnerValue(controller.getScaleXSpinner(), 1.0);
        setSpinnerValue(controller.getScaleYSpinner(), 1.0);
        setSpinnerValue(controller.getScaleZSpinner(), 1.0);
    }

    private void setSpinnerValue(Spinner<Double> spinner, double value) {
        if (spinner == null || spinner.getValueFactory() == null) return;
        spinner.getValueFactory().setValue(value);
    }

    public void updateModelsListView() {
        ListView<SceneObject> modelsListView = controller.getModelsListView();
        if (modelsListView == null) return;

        List<SceneObject> sceneObjects = scene.getObjects().stream()
                .filter(obj -> !isCameraObject(obj))
                .collect(Collectors.toList());

        modelsListView.getItems().setAll(sceneObjects);

        if (!selectedObjects.isEmpty()) {
            modelsListView.getSelectionModel().clearSelection();
            for (SceneObject obj : selectedObjects) {
                if (!isCameraObject(obj)) {
                    modelsListView.getSelectionModel().select(obj);
                }
            }
        } else {
            modelsListView.getSelectionModel().clearSelection();
        }
    }

    public void updateModelInfoLabel() {
        Label modelInfoLabel = controller.getModelInfoLabel();
        if (modelInfoLabel == null) return;

        String infoText;
        SceneObject hoveredObject = controller.getHoveredObject();

        if (hoveredObject != null) {
            Model hoveredModel = hoveredObject.getModel();
            infoText = String.format("Наведено на: %s (Вершин: %d, Полигонов: %d)",
                    hoveredObject.getName(),
                    hoveredModel.vertices.size(),
                    hoveredModel.polygons.size());
        } else if (!selectedObjects.isEmpty()) {
            if (selectedObjects.size() == 1) {
                SceneObject selected = selectedObjects.get(0);
                Model selectedModel = selected.getModel();
                infoText = String.format("Выделено: %s (Вершин: %d, Полигонов: %d)",
                        selected.getName(),
                        selectedModel.vertices.size(),
                        selectedModel.polygons.size());
            } else {
                SceneObject lastSelected = selectedObjects.get(selectedObjects.size() - 1);
                Model lastModel = lastSelected.getModel();
                infoText = String.format("Выделено моделей: %d || Последняя: %s (Вершин: %d, Полигонов: %d)",
                        selectedObjects.size(),
                        lastSelected.getName(),
                        lastModel.vertices.size(),
                        lastModel.polygons.size());
            }
        } else {
            infoText = String.format("Объектов на сцене: %d",
                    scene.getObjectCountWithoutCameras());
        }

        modelInfoLabel.setText(infoText);
    }

    public void applySettingsToSelected(RenderSettings settings, List<SceneObject> selectedObjects) {
        if (!selectedObjects.isEmpty()) {
            RenderSettings currentUISettings = new RenderSettings();
            currentUISettings.drawWireframe = controller.getWireframeCheckBox().isSelected();
            currentUISettings.useTexture = controller.getTextureCheckBox().isSelected();
            currentUISettings.useLighting = controller.getLightingCheckBox().isSelected();
            currentUISettings.baseColor = settings.baseColor;

            for (SceneObject obj : selectedObjects) {
                obj.applyRenderSettings(currentUISettings);
            }
        } else {
            for (SceneObject obj : scene.getObjects()) {
                obj.resetRenderSettings();
            }
        }
    }

    public void setupTransformSpinners(
            List<Spinner<Double>> translationSpinners,
            List<Spinner<Double>> rotationSpinners,
            List<Spinner<Double>> scaleSpinners,
            List<SceneObject> selectedObjects) {

        // Настройка фабрик значений для спиннеров
        for (Spinner<Double> spinner : translationSpinners) {
            if (spinner != null) {
                setupDoubleSpinner(spinner, -1000.0, 1000.0, 1.0, 0.0);
                addSpinnerListener(spinner, TransformComponent.TRANSLATION, selectedObjects);
            }
        }

        for (Spinner<Double> spinner : rotationSpinners) {
            if (spinner != null) {
                setupDoubleSpinner(spinner, -360.0, 360.0, 0.1, 0.0);
                addSpinnerListener(spinner, TransformComponent.ROTATION, selectedObjects);
            }
        }

        for (Spinner<Double> spinner : scaleSpinners) {
            if (spinner != null) {
                setupDoubleSpinner(spinner, 0.01, 100.0, 1.0, 1.0);
                addSpinnerListener(spinner, TransformComponent.SCALE, selectedObjects);
            }
        }
    }

    private void setupDoubleSpinner(Spinner<Double> spinner, double min, double max, double step, double initial) {
        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, step);
        spinner.setValueFactory(valueFactory);

        // Настройка редактора для ввода значений
        spinner.setEditable(true);

        // Добавляем валидатор для текстового ввода
        TextFormatter<Double> formatter = new TextFormatter<>(valueFactory.getConverter(), valueFactory.getValue());
        spinner.getEditor().setTextFormatter(formatter);
        valueFactory.valueProperty().bindBidirectional(formatter.valueProperty());
    }

    private void addSpinnerListener(Spinner<Double> spinner, TransformComponent component,
                                    List<SceneObject> selectedObjects) {
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingTransformUI || newVal == null) {
                return;
            }

            if (selectedObjects.isEmpty()) {
                return;
            }

            for (SceneObject obj : selectedObjects) {
                Transform t = ensureTransform(obj);
                if (t != null) {
                    Vector3 current = getTransformComponent(t, component);
                    Vector3 newVector = updateVectorComponent(current, component, spinner, newVal);
                    setTransformComponent(t, component, newVector);
                }
            }
        });
    }

    private Vector3 getTransformComponent(Transform transform, TransformComponent component) {
        if (component == TransformComponent.TRANSLATION) {
            return transform.getTranslation() != null ? transform.getTranslation() : new Vector3(0, 0, 0);
        } else if (component == TransformComponent.ROTATION) {
            return transform.getRotation() != null ? transform.getRotation() : new Vector3(0, 0, 0);
        } else {
            return transform.getScale() != null ? transform.getScale() : new Vector3(1, 1, 1);
        }
    }

    private Vector3 updateVectorComponent(Vector3 current, TransformComponent component,
                                          Spinner<Double> spinner, Double newValue) {
        Vector3 result = new Vector3(current.x, current.y, current.z);

        // Определяем, какая ось изменилась
        if (spinner == controller.getTranslationXSpinner() ||
                spinner == controller.getRotationXSpinner() ||
                spinner == controller.getScaleXSpinner()) {
            if (component == TransformComponent.ROTATION) {
                result.x = (float) Math.toRadians(newValue);  // Явное приведение к float
            } else {
                result.x = newValue.floatValue();  // Преобразование Double к float
            }
        } else if (spinner == controller.getTranslationYSpinner() ||
                spinner == controller.getRotationYSpinner() ||
                spinner == controller.getScaleYSpinner()) {
            if (component == TransformComponent.ROTATION) {
                result.y = (float) Math.toRadians(newValue);  // Явное приведение к float
            } else {
                result.y = newValue.floatValue();  // Преобразование Double к float
            }
        } else if (spinner == controller.getTranslationZSpinner() ||
                spinner == controller.getRotationZSpinner() ||
                spinner == controller.getScaleZSpinner()) {
            if (component == TransformComponent.ROTATION) {
                result.z = (float) Math.toRadians(newValue);
            } else {
                result.z = newValue.floatValue();
            }
        }

        return result;
    }

    private void setTransformComponent(Transform transform, TransformComponent component, Vector3 value) {
        if (component == TransformComponent.TRANSLATION) {
            transform.setTranslation(value);
        } else if (component == TransformComponent.ROTATION) {
            transform.setRotation(value);
        } else {
            transform.setScale(value);
        }
    }
}