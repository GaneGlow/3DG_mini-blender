package com.cgvsu.gui;

import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.model.ModelPreparationUtils;
import com.cgvsu.render_engine.*;
import com.cgvsu.render_engine.camera_gizmo.CameraManager;
import com.cgvsu.render_engine.scene.Scene;
import com.cgvsu.render_engine.scene.SceneObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GuiController {

    // Константы
    final private float TRANSLATION = 0.5F;
    private Color backgroundColor = Color.web("#313131");

    // FXML элементы
    @FXML private AnchorPane anchorPane;
    @FXML private Canvas canvas;
    @FXML private CheckBox wireframeCheckBox;
    @FXML private CheckBox textureCheckBox;
    @FXML private CheckBox lightingCheckBox;
    @FXML private VBox rightPanel;
    @FXML private Label panelTitle;
    @FXML private Label modelInfoLabel;
    @FXML private Label textureInfoLabel;
    @FXML private Spinner<Double> translationXSpinner;
    @FXML private Spinner<Double> translationYSpinner;
    @FXML private Spinner<Double> translationZSpinner;
    @FXML private Spinner<Double> rotationXSpinner;
    @FXML private Spinner<Double> rotationYSpinner;
    @FXML private Spinner<Double> rotationZSpinner;
    @FXML private Spinner<Double> scaleXSpinner;
    @FXML private Spinner<Double> scaleYSpinner;
    @FXML private Spinner<Double> scaleZSpinner;
    @FXML private Menu viewMenu;
    @FXML private MenuItem addCameraMenuItem;
    @FXML private ListView<SceneObject> modelsListView;

    // Менеджеры
    private CameraManager cameraManager;
    private GuiMethods guiMethods;
    private GuiButtons guiButtons;
    private Timeline timeline;

    // Состояние
    private final Scene scene = new Scene();
    private final RenderSettings renderSettings = new RenderSettings();
    private final List<SceneObject> selectedObjects = new ArrayList<>();
    private SceneObject hoveredObject = null;
    private Model mesh = null;
    private Texture currentTexture = null;
    private boolean isLeftMousePressed = false;
    private boolean isRightMousePressed = false;
    private double lastMouseX;
    private double lastMouseY;

    @FXML
    private void initialize() {
        // Инициализируем менеджеры
        guiMethods = new GuiMethods(this, scene, renderSettings, selectedObjects);
        guiButtons = new GuiButtons(this, guiMethods, scene, renderSettings, selectedObjects);

        // Инициализируем менеджер камер
        cameraManager = new CameraManager(scene);
        cameraManager.initializeWithUI(viewMenu, addCameraMenuItem);

        // Настройка UI
        guiMethods.initializeUI(anchorPane, canvas, modelsListView, modelInfoLabel, textureInfoLabel);

        // Настройка слушателей мыши
        setupMouseListeners();

        // Настройка спиннеров
        setupSpinners();

        // Настройка чекбоксов
        setupCheckBoxes();

        // Запуск анимации
        startAnimation();
    }

    private void setupMouseListeners() {
        canvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                guiMethods.handleObjectSelection((int) event.getX(), (int) event.getY());
            }
        });

        canvas.setOnMouseMoved(event -> {
            guiMethods.handleMouseMove((int) event.getX(), (int) event.getY());
        });

        canvas.setOnMouseExited(event -> {
            guiMethods.handleMouseExit();
        });

        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                isLeftMousePressed = true;
                lastMouseX = event.getX();
                lastMouseY = event.getY();
            }
        });

        canvas.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) isLeftMousePressed = false;
            if (event.getButton() == MouseButton.SECONDARY) isRightMousePressed = false;
        });

        canvas.setOnMouseDragged(event -> {
            guiMethods.handleMouseDrag(event, lastMouseX, lastMouseY, selectedObjects);
            lastMouseX = event.getX();
            lastMouseY = event.getY();
        });

        canvas.setOnScroll(event -> {
            guiMethods.handleMouseScroll(event, selectedObjects);
        });
    }

    private void setupSpinners() {
        List<Spinner<Double>> translationSpinners = Arrays.asList(
                translationXSpinner, translationYSpinner, translationZSpinner
        );
        List<Spinner<Double>> rotationSpinners = Arrays.asList(
                rotationXSpinner, rotationYSpinner, rotationZSpinner
        );
        List<Spinner<Double>> scaleSpinners = Arrays.asList(
                scaleXSpinner, scaleYSpinner, scaleZSpinner
        );

        guiMethods.setupTransformSpinners(
                translationSpinners, rotationSpinners, scaleSpinners,
                selectedObjects
        );
    }

    private void setupCheckBoxes() {
        wireframeCheckBox.selectedProperty().addListener((o, a, b) -> {
            renderSettings.drawWireframe = b;
            guiMethods.applySettingsToSelected(renderSettings, selectedObjects);
        });

        textureCheckBox.selectedProperty().addListener((o, a, b) -> {
            renderSettings.useTexture = b;
            guiMethods.applySettingsToSelected(renderSettings, selectedObjects);
        });

        lightingCheckBox.selectedProperty().addListener((o, a, b) -> {
            renderSettings.useLighting = b;
            guiMethods.applySettingsToSelected(renderSettings, selectedObjects);
        });
    }

    private void startAnimation() {
        timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);

        KeyFrame frame = new KeyFrame(Duration.millis(15), event -> {
            renderFrame();
        });

        timeline.getKeyFrames().add(frame);
        timeline.play();
    }

    private void renderFrame() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if (width <= 0 || height <= 0) return;

        canvas.getGraphicsContext2D().setFill(backgroundColor);
        canvas.getGraphicsContext2D().fillRect(0, 0, width, height);

        scene.getActiveCamera().setAspectRatio((float) (width / height));

        if (!scene.getObjects().isEmpty()) {
            RenderEngine.render(
                    canvas.getGraphicsContext2D(),
                    scene,
                    currentTexture,
                    renderSettings,
                    (int) width,
                    (int) height
            );
        }
    }

    // === Методы для кнопок (делегируются GuiButtons) ===

    @FXML
    private void onOpenModelMenuItemClick() {
        guiButtons.onOpenModelMenuItemClick(canvas, modelInfoLabel, modelsListView);
    }

    @FXML
    private void onOpenTextureMenuItemClick() {
        Texture loadedTexture = guiButtons.onOpenTextureMenuItemClick(canvas, textureInfoLabel);
        if (loadedTexture != null) {
            currentTexture = loadedTexture;
        }
    }

    @FXML
    private void onResetTranslationButtonClick() {
        guiButtons.onResetTranslationButtonClick(selectedObjects);
    }

    @FXML
    private void onResetRotationButtonClick() {
        guiButtons.onResetRotationButtonClick(selectedObjects);
    }

    @FXML
    private void onResetScaleButtonClick() {
        guiButtons.onResetScaleButtonClick(selectedObjects);
    }

    @FXML
    private void onDeleteSelectedButtonClick(ActionEvent event) {
        guiButtons.onDeleteSelectedButtonClick(event, selectedObjects, modelsListView, hoveredObject);
        if (hoveredObject != null && selectedObjects.contains(hoveredObject)) {
            hoveredObject = null;
        }
    }

    @FXML
    private void onExitMenuItemClick() {
        System.exit(0);
    }

    // === Методы для камеры ===

    @FXML
    private void onView1() {
        cameraManager.switchToCamera(0);
    }

    @FXML
    private void onAddCamera(ActionEvent event) {
        guiButtons.onAddCamera(event, cameraManager);
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
        guiButtons.handleKeyPressed(event, cameraManager, selectedObjects);
    }

    // === Геттеры для доступа из вспомогательных классов ===

    public Canvas getCanvas() {
        return canvas;
    }

    public Scene getScene() {
        return scene;
    }

    public RenderSettings getRenderSettings() {
        return renderSettings;
    }

    public List<SceneObject> getSelectedObjects() {
        return selectedObjects;
    }

    public SceneObject getHoveredObject() {
        return hoveredObject;
    }

    public void setHoveredObject(SceneObject hoveredObject) {
        this.hoveredObject = hoveredObject;
    }

    public CheckBox getWireframeCheckBox() {
        return wireframeCheckBox;
    }

    public CheckBox getTextureCheckBox() {
        return textureCheckBox;
    }

    public CheckBox getLightingCheckBox() {
        return lightingCheckBox;
    }

    public Spinner<Double> getTranslationXSpinner() {
        return translationXSpinner;
    }

    public Spinner<Double> getTranslationYSpinner() {
        return translationYSpinner;
    }

    public Spinner<Double> getTranslationZSpinner() {
        return translationZSpinner;
    }

    public Spinner<Double> getRotationXSpinner() {
        return rotationXSpinner;
    }

    public Spinner<Double> getRotationYSpinner() {
        return rotationYSpinner;
    }

    public Spinner<Double> getRotationZSpinner() {
        return rotationZSpinner;
    }

    public Spinner<Double> getScaleXSpinner() {
        return scaleXSpinner;
    }

    public Spinner<Double> getScaleYSpinner() {
        return scaleYSpinner;
    }

    public Spinner<Double> getScaleZSpinner() {
        return scaleZSpinner;
    }

    public Label getModelInfoLabel() {
        return modelInfoLabel;
    }

    public ListView<SceneObject> getModelsListView() {
        return modelsListView;
    }


    public Texture getCurrentTexture() {
        return currentTexture;
    }

    public void setCurrentTexture(Texture texture) {
        this.currentTexture = texture;
    }
}