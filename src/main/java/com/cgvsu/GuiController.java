package com.cgvsu;

import com.cgvsu.math.Matrix4;
import com.cgvsu.math.Vector3;
import com.cgvsu.model.ModelPreparationUtils;
import com.cgvsu.render_engine.*;
import javafx.fxml.FXML;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private final RenderSettings renderSettings = new RenderSettings();
    private final List<SceneObject> sceneObjects = new ArrayList<>(); // Добавляем список объектов
    private Texture currentTexture = null;

    private Model mesh = null;

    private Camera camera = new Camera(
            new Vector3(0, 0, 100),
            new Vector3(0, 0, 0),
            1.0F, 1, 0.01F, 100);

    private Timeline timeline;

    private Color backgroundColor = Color.web("#313131");

    private final List<SceneObject> selectedObjects = new ArrayList<>();
    private SceneObject hoveredObject = null; // Объект под курсором
    private int mouseX = 0;
    private int mouseY = 0;

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

        // Обработка клика для выделения
        canvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
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

        setupSpinner(translationXSpinner, 0.0, "X", -1000.0, 1000.0);

        setupSpinner(translationYSpinner, 0.0, "Y", -1000.0, 1000.0);

        setupSpinner(translationZSpinner, 0.0, "Z", -1000.0, 1000.0);

        setupSpinner(rotationXSpinner, 0.0, "X", -1000.0, 1000.0);

        setupSpinner(rotationYSpinner, 0.0, "Y", -1000.0, 1000.0);

        setupSpinner(rotationZSpinner, 0.0, "Z", -1000.0, 1000.0);

        setupSpinner(scaleXSpinner, 0.0, "X", -1000.0, 1000.0);

        setupSpinner(scaleYSpinner, 0.0, "Y", -1000.0, 1000.0);

        setupSpinner(scaleZSpinner, 0.0, "Z", -1000.0, 1000.0);

        timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame frame = new KeyFrame(Duration.millis(15), event -> {
            double width = canvas.getWidth();
            double height = canvas.getHeight();

            canvas.getGraphicsContext2D().clearRect(0, 0, width, height);
            camera.setAspectRatio((float) (width / height));

            /*if (mesh != null) {
                RenderEngine.render(
                        canvas.getGraphicsContext2D(),
                        camera,
                        mesh,
                        currentTexture,
                        renderSettings,
                        (int) width,
                        (int) height
                );
            }*/
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

    private void setupSpinner(Spinner<Double> spinner, double initialValue, String axis, double min, double max) {
        if (spinner == null) {
            return;
        }

        // Очищаем текущую фабрику значений
        spinner.setValueFactory(null);

        // Создаем новую фабрику значений с правильными параметрами
        SpinnerValueFactory<Double> valueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initialValue, 0.1);
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

        // Добавляем слушатель изменения значения
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            // метод для перемещения и тп, я пока не знаю как разделитель слушателя на три коробки
            // поэтому всё пока что здесь в одной куче
            // Катя разберись с этим потом!
        });

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
                    sceneObjects.size());
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
            for (SceneObject obj : sceneObjects) {
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
        for (SceneObject obj : sceneObjects) {
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
        Camera camera = this.camera; // Используем текущую камеру

        // Матрицы преобразования
        Matrix4 modelMatrix = GraphicConveyor.rotateScaleTranslate();
        Matrix4 viewMatrix = camera.getViewMatrix();
        Matrix4 projectionMatrix = camera.getProjectionMatrix();
        Matrix4 modelViewProjectionMatrix = modelMatrix.multiply(viewMatrix).multiply(projectionMatrix);

        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;

        // Проходим по всем вершинам и находим границы
        for (Vector3 vertex : mesh.vertices) {
            Vector3 screenPos = transformVertex(vertex, modelViewProjectionMatrix,
                    (int)canvas.getWidth(), (int)canvas.getHeight());

            minX = Math.min(minX, screenPos.x);
            maxX = Math.max(maxX, screenPos.x);
            minY = Math.min(minY, screenPos.y);
            maxY = Math.max(maxY, screenPos.y);
        }

        // Проверяем, попадает ли точка в bounding box
        return mouseX >= minX && mouseX <= maxX &&
                mouseY >= minY && mouseY <= maxY;
    }

    // Добавьте этот метод в GuiController:
    private Vector3 transformVertex(Vector3 vertex, Matrix4 modelViewProjectionMatrix, int width, int height) {
        // Аналогично методу в RenderEngine
        Vector3 transformed = GraphicConveyor.multiplyMatrix4ByVector3(modelViewProjectionMatrix, vertex);

        float screenX = (transformed.x + 1.0f) * width / 2.0f;
        float screenY = (1.0f - transformed.y) * height / 2.0f;

        return new Vector3(screenX, screenY, transformed.z);
    }

    // Обновление цветов объектов в зависимости от выбора
    private void updateObjectColors() {
        for (SceneObject obj : sceneObjects) {
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

        camera.setAspectRatio((float) (width / height));

        if (!sceneObjects.isEmpty()) {
            // Передаем глобальные настройки, но каждый объект будет использовать свои,
            // если они заданы
            RenderEngine.render(
                    canvas.getGraphicsContext2D(),
                    camera,
                    sceneObjects,
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
            sceneObjects.add(newObject);

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
            sceneObjects.removeAll(selectedObjects);

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