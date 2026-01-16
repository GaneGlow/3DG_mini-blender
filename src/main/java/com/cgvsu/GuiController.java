package com.cgvsu;

import com.cgvsu.math.Matrix4;
import com.cgvsu.math.Vector3;
import com.cgvsu.model.ModelPreparationUtils;
import com.cgvsu.render_engine.*;
import com.cgvsu.render_engine.camera_gizmo.CameraManager;
import com.cgvsu.render_engine.scene.Scene;
import com.cgvsu.render_engine.scene.SceneObject;
import javafx.fxml.FXML;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.File;
import java.util.*;

import com.cgvsu.model.Model;
import com.cgvsu.objreader.ObjReader;
import javafx.scene.image.Image;
import javafx.util.converter.DoubleStringConverter;

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

    @FXML
    private Spinner<Double> translationXSpinner;

    @FXML
    private Spinner<Double> translationYSpinner;

    @FXML
    private Spinner<Double> translationZSpinner;

    @FXML
    private Spinner<Double> rotationXSpinner;

    @FXML
    private Spinner<Double> rotationYSpinner;

    @FXML
    private Spinner<Double> rotationZSpinner;

    @FXML
    private Spinner<Double> scaleXSpinner;

    @FXML
    private Spinner<Double> scaleYSpinner;

    @FXML
    private Spinner<Double> scaleZSpinner;

    @FXML
    private Menu viewMenu;

    @FXML
    private MenuItem addCameraMenuItem;

    // Менеджер камер
    private CameraManager cameraManager;

    // Хранилище для начальных трансформаций объектов
    private final Map<SceneObject, Transform> initialTransforms = new HashMap<>();


    private final RenderSettings renderSettings = new RenderSettings();

    private final Scene scene = new Scene();
    private Texture currentTexture = null;

    private Model mesh = null;

    private Timeline timeline;

    private Color backgroundColor = Color.web("#313131");

    private final List<SceneObject> selectedObjects = new ArrayList<>();
    private SceneObject hoveredObject = null; // Объект под курсором
    private int mouseX = 0;
    private int mouseY = 0;

    private double lastMouseX;
    private double lastMouseY;
    private boolean isLeftMousePressed = false;
    private boolean isRightMousePressed = false;

    /**
     * Флаг, чтобы программное обновление спиннеров (например, при выборе объекта)
     * не вызывало применение трансформаций через слушатели.
     */
    private boolean updatingTransformUI = false;

    private enum Axis { X, Y, Z }

    private enum TransformKind { TRANSLATION, ROTATION, SCALE }

    @FXML
    private void initialize() {
        // Инициализируем менеджер камер
        cameraManager = new CameraManager(scene);
        cameraManager.initializeWithUI(viewMenu, addCameraMenuItem);

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

        // Обработка клика для выделения
        canvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                handleObjectSelection((int) event.getX(), (int) event.getY());
                updateModelInfoLabel();
            }
        });

        // Обработка движения мыши для подсветки
        canvas.setOnMouseMoved(this::handleMouseMove);

        // Обработка ухода мыши с Canvas
        canvas.setOnMouseExited(event -> {
            if (hoveredObject != null) {
                hoveredObject = null;
                updateObjectColors();
                updateModelInfoLabel();
            }
        });

        // Обработка нажатия левой кнопки мыши
        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                isLeftMousePressed = true;
                lastMouseX = event.getX();
                lastMouseY = event.getY();
            }
        });

        canvas.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                isLeftMousePressed = false;
            }
            if (event.getButton() == MouseButton.SECONDARY) {
                isRightMousePressed = false;
            }
        });

        // Обработка движения мыши с зажатой кнопкой
        canvas.setOnMouseDragged(event -> {
            if (!selectedObjects.isEmpty()) {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;

                // При зажатой ПКМ - вращение
                if (event.getButton() == MouseButton.SECONDARY) {
                    float rotationSpeed = 0.01f;
                    float rotY = (float) (deltaX * rotationSpeed);
                    float rotX = (float) (deltaY * rotationSpeed);

                    for (SceneObject obj : selectedObjects) {
                        Transform t = ensureTransform(obj);
                        if (t != null) {
                            t.rotate(rotX, rotY, 0);
                        }
                    }
                }
                // При зажатой ЛКМ - перемещение
                else if (event.getButton() == MouseButton.PRIMARY) {
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

                lastMouseX = event.getX();
                lastMouseY = event.getY();

                // Обновляем UI
                updateTransformSpinnersFromSelection();
            }
        });

        // Обработка колесика мыши для масштабирования
        canvas.setOnScroll(event -> {
            if (!selectedObjects.isEmpty()) {
                double deltaY = event.getDeltaY();
                float scaleSpeed = 0.01f;
                float scaleFactor = 1.0f + (float) (-deltaY * scaleSpeed); // Инвертируем для интуитивного масштабирования

                // Ограничиваем масштаб, чтобы не было слишком маленьких/больших значений
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));

                for (SceneObject obj : selectedObjects) {
                    Transform t = ensureTransform(obj);
                    if (t != null) {
                        t.scale(scaleFactor);
                    }
                }

                // Обновляем UI
                updateTransformSpinnersFromSelection();
                event.consume();
            }
        });


        setupSpinner(translationXSpinner, 0.0, "X", -1000.0, 1000.0);

        setupSpinner(translationYSpinner, 0.0, "Y", -1000.0, 1000.0);

        setupSpinner(translationZSpinner, 0.0, "Z", -1000.0, 1000.0);

        setupSpinner(rotationXSpinner, 0.0, "X", -1000.0, 1000.0);

        setupSpinner(rotationYSpinner, 0.0, "Y", -1000.0, 1000.0);

        setupSpinner(rotationZSpinner, 0.0, "Z", -1000.0, 1000.0);

        setupSpinner(scaleXSpinner, 0.0, "X", -1000.0, 1000.0);

        setupSpinner(scaleYSpinner, 0.0, "Y", -1000.0, 1000.0);

        setupSpinner(scaleZSpinner, 0.0, "Z", -1000.0, 1000.0);

        // Подключаем аффинные преобразования (Translation/Rotation/Scale) к UI.
        bindTransformSpinners();

        timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame frame = new KeyFrame(Duration.millis(15), event -> {
            double width = canvas.getWidth();
            double height = canvas.getHeight();

            canvas.getGraphicsContext2D().clearRect(0, 0, width, height);
            scene.getActiveCamera().setAspectRatio((float) (width / height));


            renderFrame();
        });

        wireframeCheckBox.selectedProperty().addListener((o, a, b) -> {
            renderSettings.drawWireframe = b;
            applySettingsToSelected(); // Применяем только к выбранным
        });

        textureCheckBox.selectedProperty().addListener((o, a, b) -> {
            renderSettings.useTexture = b;
            applySettingsToSelected(); // Применяем только к выбранным
        });

        lightingCheckBox.selectedProperty().addListener((o, a, b) -> {
            renderSettings.useLighting = b;
            applySettingsToSelected(); // Применяем только к выбранным
        });


        timeline.getKeyFrames().add(frame);
        timeline.play();
    }

    ///КАМЕРЫ
    @FXML
    private void onView1() {
        cameraManager.switchToCamera(0);
    }

    @FXML
    private void onAddCamera(ActionEvent event) {
        if (cameraManager.addNewCamera()) {
            showAlert("Камера добавлена",
                    "Добавлена новая камера. Всего камер: " + cameraManager.getCameraCount());
        } else {
            showAlert("Максимальное количество камер",
                    "Достигнуто максимальное количество камер (8).");
        }
    }

    private void setupSpinner(Spinner<Double> spinner, double initialValue, String axis, double min, double max) {
        if (spinner == null) {
            return;
        }

        // Очищаем текущую фабрику значений
        spinner.setValueFactory(null);

        // Создаем новую фабрику значений с правильными параметрами
        SpinnerValueFactory<Double> valueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initialValue, 1.0);
        spinner.setValueFactory(valueFactory);

        // Настраиваем форматирование отображения (2 знака после запятой)
        spinner.getValueFactory().setConverter(new DoubleStringConverter() {
            @Override
            public String toString(Double value) {
                return String.format("%.2f", value);
            }

            @Override
            public Double fromString(String string) {
                try {
                    // Очищаем строку от лишних символов
                    string = string.replace(',', '.');
                    return Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    // Возвращаем текущее значение при ошибке
                    return spinner.getValue();
                }
            }
        });

        // Разрешаем редактирование вручную
        spinner.setEditable(true);

        // Обработка ручного ввода
        spinner.getEditor().setOnAction(event -> {
            try {
                String text = spinner.getEditor().getText();
                text = text.replace(',', '.');
                double value = Double.parseDouble(text);

                // Проверяем границы
                if (value < min) value = min;
                if (value > max) value = max;

                spinner.getValueFactory().setValue(value);
            } catch (NumberFormatException e) {
                // Восстанавливаем предыдущее значение при ошибке
                spinner.getEditor().setText(
                        String.format("%.2f", spinner.getValue())
                );
            }
        });

        // Также обновляем текст при изменении значения через стрелки
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            spinner.getEditor().setText(String.format("%.2f", newVal));
        });
    }

    /**
     * Подключает спиннеры Translation/Rotation/Scale к аффинным преобразованиям объектов сцены.
     *
     * Важно:
     * - Rotation в UI считаем в градусах, в Transform храним в радианах.
     * - При изменении спиннера применяем ДЕЛЬТУ (new-old), чтобы корректно работало
     *   для нескольких выбранных объектов и не "затирало" их индивидуальные значения.
     */
    private void bindTransformSpinners() {
        bindTransformSpinner(translationXSpinner, TransformKind.TRANSLATION, Axis.X);
        bindTransformSpinner(translationYSpinner, TransformKind.TRANSLATION, Axis.Y);
        bindTransformSpinner(translationZSpinner, TransformKind.TRANSLATION, Axis.Z);

        bindTransformSpinner(rotationXSpinner, TransformKind.ROTATION, Axis.X);
        bindTransformSpinner(rotationYSpinner, TransformKind.ROTATION, Axis.Y);
        bindTransformSpinner(rotationZSpinner, TransformKind.ROTATION, Axis.Z);

        bindTransformSpinner(scaleXSpinner, TransformKind.SCALE, Axis.X);
        bindTransformSpinner(scaleYSpinner, TransformKind.SCALE, Axis.Y);
        bindTransformSpinner(scaleZSpinner, TransformKind.SCALE, Axis.Z);
    }

    private void bindTransformSpinner(
            final Spinner<Double> spinner,
            final TransformKind kind,
            final Axis axis
    ) {
        if (spinner == null) {
            return;
        }

        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingTransformUI) {
                return;
            }
            if (oldValue == null || newValue == null) {
                return;
            }
            if (Math.abs(newValue - oldValue) < 1e-12) {
                return;
            }
            if (selectedObjects.isEmpty()) {
                return;
            }

            applyTransformDelta(kind, axis, oldValue, newValue);
        });
    }

    private void applyTransformDelta(
            final TransformKind kind,
            final Axis axis,
            final double oldValue,
            final double newValue
    ) {
        // Применяем изменения к выбранным объектам
        switch (kind) {
            case TRANSLATION: {
                final float d = (float) (newValue - oldValue);
                for (SceneObject obj : selectedObjects) {
                    Transform t = ensureTransform(obj);
                    if (t == null) {
                        continue;
                    }

                    switch (axis) {
                        case X:
                            t.translate(d, 0, 0);
                            break;
                        case Y:
                            t.translate(0, d, 0);
                            break;
                        case Z:
                            t.translate(0, 0, d);
                            break;
                    }
                }
                break;
            }
            case ROTATION: {
                // В UI градусы, в Transform радианы.
                final float dRad = (float) Math.toRadians(newValue - oldValue);
                for (SceneObject obj : selectedObjects) {
                    Transform t = ensureTransform(obj);
                    if (t == null) {
                        continue;
                    }

                    switch (axis) {
                        case X:
                            t.rotate(dRad, 0, 0);
                            break;
                        case Y:
                            t.rotate(0, dRad, 0);
                            break;
                        case Z:
                            t.rotate(0, 0, dRad);
                            break;
                    }
                }
                break;
            }
            case SCALE: {
                // Масштабирование уже работает, оставляем как есть
                final double EPS = 1e-9;
                final boolean oldIsZero = Math.abs(oldValue) < EPS;

                if (oldIsZero) {
                    final float absolute = (float) newValue;
                    for (SceneObject obj : selectedObjects) {
                        Transform t = ensureTransform(obj);
                        if (t == null) {
                            continue;
                        }
                        setScaleComponent(t, axis, absolute);
                    }
                } else {
                    final float factor = (float) (newValue / oldValue);
                    for (SceneObject obj : selectedObjects) {
                        Transform t = ensureTransform(obj);
                        if (t == null) {
                            continue;
                        }
                        multiplyScaleComponent(t, axis, factor);
                    }
                }
                break;
            }
        }
        // Разрешаем обновление UI на время
        updatingTransformUI = true;
        try {
            updateTransformSpinnersFromSelection();
        } finally {
            updatingTransformUI = false;
        }
    }

    private Transform ensureTransform(final SceneObject obj) {
        if (obj == null) {
            return null;
        }
        Transform t = obj.getTransform();
        if (t == null) {
            t = new Transform();
            obj.setTransform(t);
        }
        return t;
    }

    private void setScaleComponent(final Transform t, final Axis axis, final float value) {
        if (t == null) return;

        Vector3 s = (t.getScale() != null) ? t.getScale() : new Vector3(1, 1, 1);
        switch (axis) {
            case X:
                s.x = value;
                break;
            case Y:
                s.y = value;
                break;
            case Z:
                s.z = value;
                break;
        }
        t.setScale(s);
    }

    private void multiplyScaleComponent(final Transform t, final Axis axis, final float factor) {
        if (t == null) return;

        // В Transform есть scaleX/scaleY/scaleZ, которые как раз умножают компоненту.
        switch (axis) {
            case X:
                t.scaleX(factor);
                break;
            case Y:
                t.scaleY(factor);
                break;
            case Z:
                t.scaleZ(factor);
                break;
        }
    }

    private void updateTransformSpinnersFromSelection() {
        updatingTransformUI = true;
        try {
            if (selectedObjects.isEmpty()) {
                setSpinnerValue(translationXSpinner, 0.0);
                setSpinnerValue(translationYSpinner, 0.0);
                setSpinnerValue(translationZSpinner, 0.0);

                setSpinnerValue(rotationXSpinner, 0.0);
                setSpinnerValue(rotationYSpinner, 0.0);
                setSpinnerValue(rotationZSpinner, 0.0);

                setSpinnerValue(scaleXSpinner, 1.0);
                setSpinnerValue(scaleYSpinner, 1.0);
                setSpinnerValue(scaleZSpinner, 1.0);
                return;
            }

            // Показываем параметры последнего выбранного объекта.
            SceneObject active = selectedObjects.get(selectedObjects.size() - 1);
            Transform t = (active != null) ? active.getTransform() : null;

            Vector3 tr = (t != null && t.getTranslation() != null) ? t.getTranslation() : new Vector3(0, 0, 0);
            Vector3 rot = (t != null && t.getRotation() != null) ? t.getRotation() : new Vector3(0, 0, 0);
            Vector3 sc = (t != null && t.getScale() != null) ? t.getScale() : new Vector3(1, 1, 1);

            setSpinnerValue(translationXSpinner, tr.x);
            setSpinnerValue(translationYSpinner, tr.y);
            setSpinnerValue(translationZSpinner, tr.z);

            // Rotation в UI в градусах
            setSpinnerValue(rotationXSpinner, Math.toDegrees(rot.x));
            setSpinnerValue(rotationYSpinner, Math.toDegrees(rot.y));
            setSpinnerValue(rotationZSpinner, Math.toDegrees(rot.z));

            setSpinnerValue(scaleXSpinner, sc.x);
            setSpinnerValue(scaleYSpinner, sc.y);
            setSpinnerValue(scaleZSpinner, sc.z);
        } finally {
            updatingTransformUI = false;
        }
    }

    private void setSpinnerValue(final Spinner<Double> spinner, final double value) {
        if (spinner == null || spinner.getValueFactory() == null) {
            return;
        }
        spinner.getValueFactory().setValue(value);
    }

    // Метод для применения параметра к выбранным объектам
    private void applyParameterToSelectedObjects(double value) {
        if (!selectedObjects.isEmpty()) {
            System.out.println("Applying parameter " + value + " to " + selectedObjects.size() + " object(s)");
            for (SceneObject obj : selectedObjects) {
                // Здесь можно применить параметр к объекту
                // Например, масштабирование:
                // obj.setScale(value);

                System.out.println("  - Object: " + obj.getName() + ", value: " + value);
            }
        }
    }

    // Метод для обновления информации о моделях
    private void updateModelInfoLabel() {
        if (modelInfoLabel == null) {
            return;
        }

        String infoText;

        // Если курсор над моделью (приоритет выше, чем выделение)
        if (hoveredObject != null) {
            Model hoveredModel = hoveredObject.getModel();
            infoText = String.format("Наведено на: %s (Вершин: %d, Полигонов: %d)",
                    hoveredObject.getName(),
                    hoveredModel.vertices.size(),
                    hoveredModel.polygons.size());
        }
        // Если есть выделенные модели
        else if (!selectedObjects.isEmpty()) {
            if (selectedObjects.size() == 1) {
                // Одна выделенная модель
                SceneObject selected = selectedObjects.get(0);
                Model selectedModel = selected.getModel();
                infoText = String.format("Выделено: %s (Вершин: %d, Полигонов: %d)",
                        selected.getName(),
                        selectedModel.vertices.size(),
                        selectedModel.polygons.size());
            } else {
                // Несколько выделенных моделей - берем последнюю
                SceneObject lastSelected = selectedObjects.get(selectedObjects.size() - 1);
                Model lastModel = lastSelected.getModel();
                infoText = String.format("Выделено моделей: %d || Последняя: %s (Вершин: %d, Полигонов: %d)",
                        selectedObjects.size(),
                        lastSelected.getName(),
                        lastModel.vertices.size(),
                        lastModel.polygons.size());
            }
        }
        // Если нет выделенных моделей и курсор не над моделью
        else {
            // Только количество моделей на сцене
            infoText = String.format("Объектов на сцене: %d",
                    scene.getObjectCountWithoutCameras());
        }

        modelInfoLabel.setText(infoText);
    }


    // Метод для применения настроек к выбранным объектам
    private void applySettingsToSelected() {
        if (!selectedObjects.isEmpty()) {
            // Копируем текущие настройки из UI
            RenderSettings currentUISettings = new RenderSettings();
            currentUISettings.drawWireframe = wireframeCheckBox.isSelected();
            currentUISettings.useTexture = textureCheckBox.isSelected();
            currentUISettings.useLighting = lightingCheckBox.isSelected();
            currentUISettings.baseColor = renderSettings.baseColor;

            for (SceneObject obj : selectedObjects) {
                obj.applyRenderSettings(currentUISettings);
            }
        } else {
            // Если ничего не выбрано, сбрасываем настройки у всех объектов
            for (SceneObject obj : scene.getObjects()) {
                obj.resetRenderSettings();
            }
        }
    }

    private void handleMouseMove(MouseEvent event) {
        mouseX = (int) event.getX();
        mouseY = (int) event.getY();

        // Обновляем hoveredObject
        SceneObject oldHovered = hoveredObject;
        hoveredObject = findObjectUnderCursor(mouseX, mouseY);

        // Если hoveredObject изменился, обновляем цвета
        if (oldHovered != hoveredObject) {
            updateObjectColors();
            updateModelInfoLabel();
        }
    }

    private SceneObject findObjectUnderCursor(int x, int y) {
        for (SceneObject obj : scene.getObjects()) {
            if (isMouseOverObject(obj, x, y)) {
                return obj;
            }
        }
        return null;
    }

    private void updateUIFromSelectedObjects() {
        if (selectedObjects.isEmpty()) {
            // Если нет выделенных объектов, показываем глобальные настройки
            wireframeCheckBox.setSelected(renderSettings.drawWireframe);
            textureCheckBox.setSelected(renderSettings.useTexture);
            lightingCheckBox.setSelected(renderSettings.useLighting);
        } else {
            // Если есть выделенные объекты, показываем настройки первого из них
            SceneObject firstSelected = selectedObjects.get(0);
            RenderSettings settings = firstSelected.getRenderSettings();

            if (settings != null) {
                wireframeCheckBox.setSelected(settings.drawWireframe);
                textureCheckBox.setSelected(settings.useTexture);
                lightingCheckBox.setSelected(settings.useLighting);
            } else {
                // Если у объекта нет настроек, показываем глобальные
                wireframeCheckBox.setSelected(renderSettings.drawWireframe);
                textureCheckBox.setSelected(renderSettings.useTexture);
                lightingCheckBox.setSelected(renderSettings.useLighting);
            }
        }

        // Также обновляем значения трансформаций в UI
        // (показываем параметры последнего выбранного объекта).
        updateTransformSpinnersFromSelection();
    }

    private void handleObjectSelection(int mouseX, int mouseY) {
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
        updateUIFromSelectedObjects(); // Обновляем UI
    }

    private boolean isMouseOverObject(SceneObject obj, int mouseX, int mouseY) {
        if (obj == null || obj.getModel() == null) {
            return false;
        }

        // Вычисляем bounding box объекта в экранных координатах
        // Для этого нужно преобразовать вершины в экранные координаты

        Model mesh = obj.getModel();
        Camera camera = scene.getActiveCamera(); // Используем текущую камеру

        // Матрицы преобразования (как в RenderEngine): v_clip = P * V * M * v
        Matrix4 modelMatrix = RenderEngine.getModelMatrix(obj);
        Matrix4 viewMatrix = camera.getViewMatrix();
        Matrix4 projectionMatrix = camera.getProjectionMatrix();
        Matrix4 modelViewProjectionMatrix = projectionMatrix.multiply(viewMatrix).multiply(modelMatrix);

        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        // Проходим по всем вершинам и находим границы
        for (Vector3 vertex : mesh.vertices) {
            Vector3 screenPos = RenderEngine.transformVertex(vertex, modelViewProjectionMatrix,
                    (int) canvas.getWidth(), (int) canvas.getHeight());

            // multiplyMatrix4ByVector3 может вернуть NaN, если w ~ 0
            if (Float.isNaN(screenPos.x) || Float.isNaN(screenPos.y)) {
                continue;
            }

            minX = Math.min(minX, screenPos.x);
            maxX = Math.max(maxX, screenPos.x);
            minY = Math.min(minY, screenPos.y);
            maxY = Math.max(maxY, screenPos.y);
        }

        // Если все вершины оказались невалидными (например, объект полностью на плоскости камеры)
        if (!Float.isFinite(minX) || !Float.isFinite(maxX) || !Float.isFinite(minY) || !Float.isFinite(maxY)) {
            return false;
        }

        // Проверяем, попадает ли точка в bounding box
        return mouseX >= minX && mouseX <= maxX &&
                mouseY >= minY && mouseY <= maxY;
    }

    // Обновление цветов объектов в зависимости от выбора
    private void updateObjectColors() {
        for (SceneObject obj : scene.getObjects()) {
            if (selectedObjects.contains(obj)) {
                // Выделение (клик) - голубой цвет для сетки и серо-голубой для модели
                obj.setWireframeColor(Color.CYAN);
                obj.setModelColor(Color.rgb(173, 216, 230)); // Серо-голубой
            } else if (obj == hoveredObject) {
                obj.setWireframeColor(Color.CYAN);
                obj.setModelColor(Color.rgb(173, 216, 230));
                // Подсветка при наведении - желтый цвет для сетки и светло-желтый для модели
                /*obj.setWireframeColor(Color.YELLOW);
                obj.setModelColor(Color.rgb(255, 255, 200));*/ // Светло-желтый
            } else {
                // Обычный режим - стандартные цвета
                obj.setWireframeColor(Color.WHITE);
                obj.setModelColor(renderSettings.baseColor);
            }
        }
    }

    private void renderFrame() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if (width <= 0 || height <= 0) return;

        canvas.getGraphicsContext2D().setFill(backgroundColor);
        canvas.getGraphicsContext2D().fillRect(0, 0, width, height);

        scene.getActiveCamera().setAspectRatio((float) (width / height));


        if (!scene.getObjects().isEmpty()) {
            // Передаем глобальные настройки, но каждый объект будет использовать свои,
            // если они заданы
            RenderEngine.render(
                    canvas.getGraphicsContext2D(),
                    scene,
                    currentTexture,  // Это глобальная текстура, но можно убрать
                    renderSettings,  // Глобальные настройки (будут использованы, если у объекта нет своих)
                    (int) width,
                    (int) height
            );
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

            String objectName = file.getName().replace(".obj", "");
            SceneObject newObject = new SceneObject(objectName, mesh, currentTexture);

            // Сохраняем начальную трансформацию (значения по умолчанию)
            Transform initialTransform = new Transform();
            initialTransforms.put(newObject, initialTransform);

            scene.addObject(newObject);

            newObject.setWireframeColor(Color.WHITE);
            newObject.setModelColor(renderSettings.baseColor);

            if (!selectedObjects.isEmpty() && selectedObjects.contains(newObject)) {
                // Только если объект сразу выделен, применяем настройки
                applySettingsToSelected();
            }

            updateModelInfoLabel();

        } catch (IOException exception) {
            System.err.println("Ошибка загрузки модели: " + exception.getMessage());
            System.err.println("Error loading model: " + exception.getMessage());
        }
    }

    @FXML
    private void onResetTranslationButtonClick() {
        resetTransformComponent("перемещения", TransformComponent.TRANSLATION);
    }

    @FXML
    private void onResetRotationButtonClick() {
        resetTransformComponent("вращения", TransformComponent.ROTATION);
    }

    @FXML
    private void onResetScaleButtonClick() {
        resetTransformComponent("масштаба", TransformComponent.SCALE);
    }

    private void resetTransformComponent(String componentName, TransformComponent component) {
        if (selectedObjects.isEmpty()) {
            showAlert("Нет выделенных объектов", "Выберите объект для сброса " + componentName);
            return;
        }

        for (SceneObject obj : selectedObjects) {
            Transform initialTransform = initialTransforms.get(obj);
            if (initialTransform != null && obj.getTransform() != null) {
                Vector3 initialValue = getInitialVector(initialTransform, component);
                setTransformComponent(obj.getTransform(), component, initialValue);
            }
        }

        updateTransformSpinnersFromSelection();
    }

    private Vector3 getInitialVector(Transform transform, TransformComponent component) {
        if (component == TransformComponent.TRANSLATION) {
            return transform.getTranslation();
        } else if (component == TransformComponent.ROTATION) {
            return transform.getRotation();
        } else {
            return transform.getScale();
        }
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

    /*@FXML
    private void onResetAllButtonClick(ActionEvent event) {
        if (selectedObjects.isEmpty()) {
            showAlert("Нет выделенных объектов", "Выберите объект для полного сброса");
            return;
        }

        for (SceneObject obj : selectedObjects) {
            Transform initialTransform = initialTransforms.get(obj);
            if (initialTransform != null) {
                // Полный сброс к начальному состоянию
                obj.setTransform(new Transform());
            }
        }

        updateTransformSpinnersFromSelection();
    }*/

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
            currentTexture = new Texture(image);
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
    private void onDeleteSelectedButtonClick(ActionEvent event) {
        if (selectedObjects.isEmpty()) {
            // Показываем уведомление, если ничего не выбрано
            showAlert("Нет выделенных объектов", "Пожалуйста, выберите один или несколько объектов для удаления.");
            return;
        }

        // Спрашиваем подтверждение
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Удаление объектов");
        confirmation.setHeaderText("Удалить выбранные объекты?");
        confirmation.setContentText("Вы собираетесь удалить " + selectedObjects.size() + " объект(ов).\nЭто действие нельзя отменить.");

        // Настраиваем кнопки
        ButtonType deleteButton = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(deleteButton, cancelButton);

        // Показываем диалог и ждем ответа
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == deleteButton) {
            // Удаляем выбранные объекты из сцены
            scene.removeSelectedObjects(selectedObjects);

            // Очищаем список выбранных объектов
            selectedObjects.clear();

            // Сбрасываем hoveredObject
            hoveredObject = null;

            // Обновляем UI
            updateUIFromSelectedObjects();
            updateModelInfoLabel();

            // Показываем уведомление об успешном удалении
            showAlert("Объекты удалены", "Удалено " + selectedObjects.size() + " объект(ов).");
        }
    }

    // Вспомогательный метод для показа уведомлений
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onExitMenuItemClick() {
        System.exit(0);
    }

    @FXML
    public void handleCameraForward(ActionEvent actionEvent) {
        scene.getActiveCamera().movePosition(new Vector3(0, 0, -TRANSLATION));
    }

    @FXML
    public void handleCameraBackward(ActionEvent actionEvent) {
        scene.getActiveCamera().movePosition(new Vector3(0, 0, TRANSLATION));
    }

    @FXML
    public void handleCameraLeft(ActionEvent actionEvent) {
        scene.getActiveCamera().movePosition(new Vector3(TRANSLATION, 0, 0));
    }

    @FXML
    public void handleCameraRight(ActionEvent actionEvent) {
        scene.getActiveCamera().movePosition(new Vector3(-TRANSLATION, 0, 0));
    }

    @FXML
    public void handleCameraUp(ActionEvent actionEvent) {
        scene.getActiveCamera().movePosition(new Vector3(0, TRANSLATION, 0));
    }

    @FXML
    public void handleCameraDown(ActionEvent actionEvent) {
        scene.getActiveCamera().movePosition(new Vector3(0, -TRANSLATION, 0));
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        // Камеры — ТОЛЬКО через Alt
        boolean handled = cameraManager.handleKeyPressed(
                event.getCode(),
                event.isAltDown()
        );

        if (handled) {
            return;
        }

        // Остальные горячие клавиши
        switch (event.getCode()) {
            case DELETE:
            case BACK_SPACE:
                if (!selectedObjects.isEmpty()) {
                    onDeleteSelectedButtonClick(new ActionEvent());
                }
                break;
        }
    }

}