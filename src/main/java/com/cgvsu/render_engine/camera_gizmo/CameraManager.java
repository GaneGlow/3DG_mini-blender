package com.cgvsu.render_engine.camera_gizmo;

import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.render_engine.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.ArrayList;
import java.util.List;

public class CameraManager {

    private final Scene scene;

    private Menu viewMenu;
    private MenuItem addCameraMenuItem;
    private final ObservableList<MenuItem> cameraMenuItems =
            FXCollections.observableArrayList();

    private static final int MAX_CAMERAS = 8;

    public CameraManager(Scene scene) {
        this.scene = scene;
        initializeDefaultCamera();
    }

    public void initializeWithUI(Menu viewMenu, MenuItem addCameraMenuItem) {
        this.viewMenu = viewMenu;
        this.addCameraMenuItem = addCameraMenuItem;
        updateCameraMenu();
    }

    private void initializeDefaultCamera() {
        Camera camera = new Camera(
                new Vector3(0, 0, 100),
                new Vector3(0, 0, 0),
                1.0F,
                1,
                0.01F,
                100
        );

        // Добавляем и делаем активной
        scene.addCamera(camera, true);

        // Гизмо для камеры
        Model gizmoModel = CameraGizmoFactory.create(5f);
        CameraGizmo gizmo = new CameraGizmo(
                "Camera 1",
                gizmoModel,
                camera
        );

        scene.addObject(gizmo);
        updateCameraGizmoVisibility();
    }

    public boolean addNewCamera() {
        int cameraCount = scene.getCameras().size();
        if (cameraCount >= MAX_CAMERAS) {
            return false;
        }

        Camera newCamera = new Camera(
                new Vector3(cameraCount * 30, 0, 100),
                new Vector3(0, 0, 0),
                1.0F,
                1,
                0.01F,
                100
        );

        scene.addCamera(newCamera, false);

        Model gizmoModel = CameraGizmoFactory.create(5f);
        CameraGizmo gizmo = new CameraGizmo(
                "Camera " + (cameraCount + 1),
                gizmoModel,
                newCamera
        );

        scene.addObject(gizmo);

        updateCameraGizmoVisibility();
        updateCameraMenu();
        return true;
    }

    public boolean removeCamera(Camera camera) {
        if (camera == null) return false;
        if (camera == scene.getActiveCamera()) return false;
        if (scene.getCameras().size() <= 1) return false;

        CameraGizmo gizmoToRemove = null;
        for (SceneObject obj : scene.getObjects()) {
            if (obj instanceof CameraGizmo gizmo && gizmo.getCamera() == camera) {
                gizmoToRemove = gizmo;
                break;
            }
        }

        if (gizmoToRemove != null) {
            scene.removeObject(gizmoToRemove);
        }

        scene.removeCamera(camera);

        updateCameraGizmoVisibility();
        updateCameraMenu();
        return true;
    }

    public int removeSelectedCameras(List<SceneObject> selectedObjects) {
        List<Camera> camerasToRemove = new ArrayList<>();
        List<CameraGizmo> gizmosToRemove = new ArrayList<>();

        for (SceneObject obj : selectedObjects) {
            if (obj instanceof CameraGizmo gizmo) {
                Camera cam = gizmo.getCamera();
                if (cam != scene.getActiveCamera() &&
                        scene.getCameras().size() > 1) {
                    camerasToRemove.add(cam);
                    gizmosToRemove.add(gizmo);
                }
            }
        }

        for (CameraGizmo gizmo : gizmosToRemove) {
            scene.removeObject(gizmo);
        }

        for (Camera cam : camerasToRemove) {
            scene.removeCamera(cam);
        }

        if (!camerasToRemove.isEmpty()) {
            updateCameraGizmoVisibility();
            updateCameraMenu();
        }

        return camerasToRemove.size();
    }

    public boolean switchToCamera(int index) {
        if (index < 0 || index >= scene.getCameras().size()) {
            return false;
        }

        scene.setActiveCamera(index);
        updateCameraGizmoVisibility();
        updateCameraMenuSelection();
        return true;
    }

    public void switchToNextCamera() {
        if (scene.getCameras().size() <= 1) return;

        scene.nextCamera();
        updateCameraGizmoVisibility();
        updateCameraMenuSelection();
    }

    /**
     * Активная камера НЕ отображается как объект сцены.
     */
    private void updateCameraGizmoVisibility() {
        for (SceneObject obj : scene.getObjects()) {
            if (obj instanceof CameraGizmo gizmo) {
                boolean isActive =
                        gizmo.getCamera() == scene.getActiveCamera();
                gizmo.setVisible(!isActive);
            }
        }
    }

    public void updateCameraMenu() {
        if (viewMenu == null || addCameraMenuItem == null) return;

        viewMenu.getItems().clear();
        cameraMenuItems.clear();

        List<Camera> cameras = scene.getCameras();

        for (int i = 0; i < cameras.size(); i++) {
            final int index = i;

            MenuItem item = new MenuItem("Вид " + (i + 1));
            item.setOnAction(e -> switchToCamera(index));

            if (i < 8) {
                KeyCode code = getKeyCodeForIndex(i);
                if (code != KeyCode.UNDEFINED) {
                    item.setAccelerator(
                            new KeyCodeCombination(
                                    code,
                                    KeyCombination.ALT_DOWN
                            )
                    );

                }
            }

            cameraMenuItems.add(item);
            viewMenu.getItems().add(item);
        }

        if (!cameras.isEmpty()) {
            viewMenu.getItems().add(new SeparatorMenuItem());
        }

        addCameraMenuItem.setDisable(scene.getCameras().size() >= MAX_CAMERAS);
        viewMenu.getItems().add(addCameraMenuItem);

        updateCameraMenuSelection();
    }

    private void updateCameraMenuSelection() {
        int activeIndex = scene.getCameras()
                .indexOf(scene.getActiveCamera());

        for (int i = 0; i < cameraMenuItems.size(); i++) {
            MenuItem item = cameraMenuItems.get(i);
            if (i == activeIndex) {
                item.setStyle("-fx-font-weight: bold;");
            } else {
                item.setStyle("");
            }
        }
    }

    public int getActiveCameraIndex() {
        Camera active = scene.getActiveCamera();
        if (active == null) return -1;
        return scene.getCameras().indexOf(active);
    }

    public boolean handleKeyPressed(KeyCode code, boolean altDown) {
        if (!altDown) {
            return false;
        }

        int cameraIndex = -1;
        switch (code) {
            case DIGIT1 -> cameraIndex = 0;
            case DIGIT2 -> cameraIndex = 1;
            case DIGIT3 -> cameraIndex = 2;
            case DIGIT4 -> cameraIndex = 3;
            case DIGIT5 -> cameraIndex = 4;
            case DIGIT6 -> cameraIndex = 5;
            case DIGIT7 -> cameraIndex = 6;
            case DIGIT8 -> cameraIndex = 7;

            case Q -> { // Alt + Q → следующая камера
                switchToNextCamera();
                return true;
            }

            default -> {
                return false;
            }
        }

        if (cameraIndex >= 0 && cameraIndex < getCameraCount()) {
            return switchToCamera(cameraIndex);
        }

        return false;
    }


    private KeyCode getKeyCodeForIndex(int index) {
        return switch (index) {
            case 0 -> KeyCode.DIGIT1;
            case 1 -> KeyCode.DIGIT2;
            case 2 -> KeyCode.DIGIT3;
            case 3 -> KeyCode.DIGIT4;
            case 4 -> KeyCode.DIGIT5;
            case 5 -> KeyCode.DIGIT6;
            case 6 -> KeyCode.DIGIT7;
            case 7 -> KeyCode.DIGIT8;
            default -> KeyCode.UNDEFINED;
        };
    }

    public int getCameraCount() {
        return scene.getCameras().size();
    }
}
