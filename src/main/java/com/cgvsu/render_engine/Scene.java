package com.cgvsu.render_engine;

import com.cgvsu.render_engine.camera_gizmo.CameraGizmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scene — единое хранилище объектов и камер.
 */
public class Scene {

    private final List<SceneObject> objects = new ArrayList<>();
    private final List<Camera> cameras = new ArrayList<>();
    private Camera activeCamera;

    // ---------- Objects ----------
    public List<SceneObject> getObjects() {
        return objects;
    }

    public void addObject(SceneObject obj) {
        objects.add(obj);
    }

    public void removeObject(SceneObject obj) {
        objects.remove(obj);
    }

    /**
     * Возвращает количество объектов сцены, не считая камеры (CameraGizmo).
     */
    public int getObjectCountWithoutCameras() {
        int count = 0;
        for (SceneObject obj : objects) {
            if (!(obj instanceof CameraGizmo)) {
                count++;
            }
        }
        return count;
    }
    /**
     * Удаляет выбранные объекты сцены.
     * Камеры удаляются, если они НЕ являются активными.
     */
    public void removeSelectedObjects(List<SceneObject> selectedObjects) {

        for (SceneObject obj : selectedObjects) {

            // Если это гизмо камеры
            if (obj instanceof CameraGizmo gizmo) {
                Camera cam = gizmo.getCamera();

                // НЕЛЬЗЯ удалять активную камеру
                if (cam == activeCamera) {
                    continue;
                }

                // Удаляем камеру из списка камер
                cameras.remove(cam);
            }

            // Удаляем объект из сцены (включая гизмо)
            objects.remove(obj);
        }
    }


    // ---------- Cameras ----------
    public List<Camera> getCameras() {
        return Collections.unmodifiableList(cameras);
    }

    public Camera getActiveCamera() {
        return activeCamera;
    }

    public boolean hasActiveCamera() {
        return activeCamera != null;
    }

    public void addCamera(Camera camera, boolean makeActive) {
        cameras.add(camera);
        if (activeCamera == null || makeActive) {
            activeCamera = camera;
        }
    }

    public void removeCamera(Camera camera) {
        cameras.remove(camera);
        if (camera == activeCamera) {
            activeCamera = cameras.isEmpty() ? null : cameras.get(0);
        }
    }

    public void nextCamera() {
        if (cameras.isEmpty()) return;
        int idx = cameras.indexOf(activeCamera);
        activeCamera = cameras.get((idx + 1) % cameras.size());
    }

    public void setActiveCamera(int index) {
        if (index < 0 || index >= cameras.size()) {
            return;
        }
        activeCamera = cameras.get(index);
    }
}
