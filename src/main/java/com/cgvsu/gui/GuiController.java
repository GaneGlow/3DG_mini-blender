package com.cgvsu.gui;

import com.cgvsu.math.Matrix4;
import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;
import com.cgvsu.model.PolygonSelection;
import com.cgvsu.objwriter.ObjWriter;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuiController {

    final private float TRANSLATION = 0.5F;
    private Color backgroundColor = Color.web("#313131");

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
    @FXML private CheckBox polygonCheckBox;

    private List<PolygonSelection> selectedPolygons = new ArrayList<>();
    private boolean polygonSelectionMode = false;

    private CameraManager cameraManager;
    private GuiMethods guiMethods;
    private GuiButtons guiButtons;
    private Timeline timeline;

    private final Scene scene = new Scene();
    private final RenderSettings renderSettings = new RenderSettings();
    private final List<SceneObject> selectedObjects = new ArrayList<>();
    private SceneObject hoveredObject = null;
    private Texture currentTexture = null;
    private boolean isLeftMousePressed = false;
    private boolean isRightMousePressed = false;
    private double lastMouseX;
    private double lastMouseY;

    private int mouseX = 0;
    private int mouseY = 0;
    private boolean updatingTransformUI = false;

    @FXML
    private void initialize() {
        guiMethods = new GuiMethods(this, scene, renderSettings, selectedObjects);
        guiButtons = new GuiButtons(this, guiMethods, scene, renderSettings, selectedObjects);

        cameraManager = new CameraManager(scene);
        cameraManager.initializeWithUI(viewMenu, addCameraMenuItem);

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

        guiMethods.initializeUI(anchorPane, canvas, modelsListView, modelInfoLabel, textureInfoLabel);

        setupMouseListeners();

        guiMethods.setupSpinners(
                Arrays.asList(translationXSpinner, translationYSpinner, translationZSpinner),
                Arrays.asList(rotationXSpinner, rotationYSpinner, rotationZSpinner),
                Arrays.asList(scaleXSpinner, scaleYSpinner, scaleZSpinner),
                selectedObjects
        );

        setupCheckBoxes();

        startAnimation();

        guiMethods.updateModelInfoLabel();

        setupPolygonCheckBox();
    }

    private void setupPolygonCheckBox() {
        polygonCheckBox.selectedProperty().addListener((o, a, b) -> {
            polygonSelectionMode = b;
            if (!b) {
                selectedPolygons.clear();
            }
        });
    }

    private void setupMouseListeners() {
        canvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (polygonSelectionMode && !selectedObjects.isEmpty()) {
                    handlePolygonSelection((int) event.getX(), (int) event.getY());
                    renderFrame();
                } else {
                    guiMethods.handleObjectSelection((int) event.getX(), (int) event.getY());
                    guiMethods.updateModelInfoLabel();
                    selectedPolygons.clear();
                }
            }
        });

        canvas.setOnMouseMoved(event -> {
            mouseX = (int) event.getX();
            mouseY = (int) event.getY();
            guiMethods.handleMouseMove(mouseX, mouseY);
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
            if (!selectedObjects.isEmpty()) {
                guiMethods.handleMouseDrag(event, lastMouseX, lastMouseY, selectedObjects);
                lastMouseX = event.getX();
                lastMouseY = event.getY();
            }
        });

        canvas.setOnScroll(event -> {
            guiMethods.handleMouseScroll(event, selectedObjects);
        });
    }

    private void handlePolygonSelection(int mouseX, int mouseY) {
        if (selectedObjects.isEmpty()) {
            return;
        }

        SceneObject selectedObject = selectedObjects.get(0);
        Model mesh = selectedObject.getModel();

        if (mesh == null) return;

        Polygon selectedPolygon = findPolygonUnderCursor(selectedObject, mouseX, mouseY);

        if (selectedPolygon != null) {
            PolygonSelection selection = new PolygonSelection(selectedObject, selectedPolygon);

            boolean alreadySelected = selectedPolygons.stream()
                    .anyMatch(ps -> ps.equals(selection));

            if (alreadySelected) {
                selectedPolygons.removeIf(ps -> ps.equals(selection));
            } else {
                selectedPolygons.add(selection);
            }
        }
    }

    private Polygon findPolygonUnderCursor(SceneObject object, int mouseX, int mouseY) {
        Model mesh = object.getModel();
        if (mesh == null || mesh.polygons.isEmpty()) return null;

        Camera camera = scene.getActiveCamera();
        Matrix4 modelMatrix = RenderEngine.getModelMatrix(object);
        Matrix4 viewMatrix = camera.getViewMatrix();
        Matrix4 projectionMatrix = camera.getProjectionMatrix();
        Matrix4 modelViewProjectionMatrix = projectionMatrix.multiply(viewMatrix).multiply(modelMatrix);

        for (Polygon polygon : mesh.polygons) {
            if (polygon.getVertexIndices().size() < 3) continue;

            List<Integer> vertexIndices = polygon.getVertexIndices();
            List<Vector3> screenVertices = new ArrayList<>();

            for (int vertexIndex : vertexIndices) {
                Vector3 vertex = mesh.vertices.get(vertexIndex);
                Vector3 screenPos = RenderEngine.transformVertex(vertex, modelViewProjectionMatrix,
                        (int) canvas.getWidth(), (int) canvas.getHeight());
                screenVertices.add(screenPos);
            }

            if (isPointInPolygon(mouseX, mouseY, screenVertices)) {
                return polygon;
            }
        }

        return null;
    }

    private boolean isPointInPolygon(int x, int y, List<Vector3> vertices) {
        int windingNumber = 0;
        int n = vertices.size();

        for (int i = 0; i < n; i++) {
            Vector3 v1 = vertices.get(i);
            Vector3 v2 = vertices.get((i + 1) % n);

            if (v1.y <= y) {
                if (v2.y > y) {
                    if (isLeft(v1, v2, x, y) > 0) {
                        windingNumber++;
                    }
                }
            } else {
                if (v2.y <= y) {
                    if (isLeft(v1, v2, x, y) < 0) {
                        windingNumber--;
                    }
                }
            }
        }

        return windingNumber != 0;
    }

    private double isLeft(Vector3 v1, Vector3 v2, int x, int y) {
        return (v2.x - v1.x) * (y - v1.y) - (x - v1.x) * (v2.y - v1.y);
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
                    (int) height,
                    selectedPolygons
            );
        }
    }

    public void clearSelectedPolygons() {
        selectedPolygons.clear();
    }

    @FXML
    private void onOpenModelMenuItemClick() {
        guiButtons.onOpenModelMenuItemClick(canvas);
        guiMethods.updateModelInfoLabel();
    }

    @FXML
    private void onOpenTextureMenuItemClick() {
        Texture loadedTexture = guiButtons.onOpenTextureMenuItemClick(canvas, textureInfoLabel);
        if (loadedTexture != null) {
            currentTexture = loadedTexture;
        }
    }

    @FXML
    private void onSaveModelMenuItemClick() {
        saveSelectedModel(false);
    }

    @FXML
    private void onSaveModelWithChangesMenuItemClick() {
        saveSelectedModel(true);
    }

    private void saveSelectedModel(final boolean withChanges) {
        final SceneObject selected = getSelectedObjectForIO();
        if (selected == null || selected.getModel() == null) {
            guiButtons.showAlert("Сохранение", "Сначала выберите модель для сохранения.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wavefront OBJ (*.obj)", "*.obj"));
        fc.setInitialFileName("model.obj");
        File file = fc.showSaveDialog(anchorPane.getScene().getWindow());
        if (file == null) return;

        try {
            if (withChanges) {
                // Сохраняем с примененными преобразованиями
                ObjWriter.write(selected.getModel(), file.getAbsolutePath(), selected.getTransform());
            } else {
                // Сохраняем без изменений (оригинальные координаты)
                ObjWriter.write(selected.getModel(), file.getAbsolutePath());
            }
            guiButtons.showAlert(
                    "Сохранение",
                    withChanges ? "Модель сохранена с применёнными преобразованиями." : "Модель сохранена без изменений."
            );
        } catch (IOException e) {
            guiButtons.showAlert("Ошибка сохранения", "Не удалось сохранить файл: " + e.getMessage());
        } catch (RuntimeException e) {
            guiButtons.showAlert("Ошибка сохранения", e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка.");
        }
    }

    private SceneObject getSelectedObjectForIO() {
        if (!selectedObjects.isEmpty()) {
            return selectedObjects.get(0);
        }
        if (modelsListView != null && modelsListView.getSelectionModel() != null) {
            return modelsListView.getSelectionModel().getSelectedItem();
        }
        return null;
    }

    @FXML
    private void onResetTranslationButtonClick() {
        guiButtons.onResetTranslationButtonClick(selectedObjects);
        guiMethods.updateTransformSpinnersFromSelection();
    }

    @FXML
    private void onResetRotationButtonClick() {
        guiButtons.onResetRotationButtonClick(selectedObjects);
        guiMethods.updateTransformSpinnersFromSelection();
    }

    @FXML
    private void onResetScaleButtonClick() {
        guiButtons.onResetScaleButtonClick(selectedObjects);
        guiMethods.updateTransformSpinnersFromSelection();
    }

    @FXML
    private void onDeleteSelectedButtonClick(ActionEvent event) {
        if (selectedObjects.isEmpty()) {
            return;
        }

        if (selectedPolygons.isEmpty()) {
            guiButtons.onDeleteSelectedButtonClick(event, selectedObjects, modelsListView, hoveredObject);

            if (hoveredObject != null && selectedObjects.contains(hoveredObject)) {
                hoveredObject = null;
            }
            guiMethods.updateModelInfoLabel();
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Удаление полигонов");
        confirmation.setHeaderText("Удалить выделенные полигоны?");
        confirmation.setContentText("Вы собираетесь удалить " + selectedPolygons.size() +
                " полигон(ов).");

        ButtonType deleteButton = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(deleteButton, cancelButton);

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == deleteButton) {
            Map<SceneObject, List<Integer>> polygonsToRemoveByObject = new HashMap<>();

            for (PolygonSelection selection : selectedPolygons) {
                SceneObject object = selection.getObject();
                Model model = object.getModel();

                if (model != null) {
                    int polygonIndex = model.polygons.indexOf(selection.getPolygon()) + 1;
                    if (polygonIndex > 0) {
                        polygonsToRemoveByObject
                                .computeIfAbsent(object, k -> new ArrayList<>())
                                .add(polygonIndex);
                    }
                }
            }

            for (Map.Entry<SceneObject, List<Integer>> entry : polygonsToRemoveByObject.entrySet()) {
                SceneObject object = entry.getKey();
                Model model = object.getModel();
                List<Integer> polygonIndices = entry.getValue();

                model.removePolygons(
                        new ArrayList<>(polygonIndices),
                        model.vertices,
                        model.textureVertices,
                        model.normals,
                        model.polygons,
                        true
                );
            }

            selectedPolygons.clear();

            renderFrame();
            guiButtons.showAlert("Удаление", "Полигоны успешно удалены!");
        }
    }

    @FXML
    private void onExitMenuItemClick() {
        System.exit(0);
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

    public Canvas getCanvas() {
        return canvas;
    }

    public Scene getScene() {
        return scene;
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

    public boolean isUpdatingTransformUI() {
        return updatingTransformUI;
    }

    public void setUpdatingTransformUI(boolean updatingTransformUI) {
        this.updatingTransformUI = updatingTransformUI;
    }
}