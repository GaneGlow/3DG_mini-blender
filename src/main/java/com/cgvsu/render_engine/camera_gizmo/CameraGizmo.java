package com.cgvsu.render_engine.camera_gizmo;

import com.cgvsu.model.Model;
import com.cgvsu.render_engine.Camera;
import com.cgvsu.render_engine.RenderSettings;
import com.cgvsu.render_engine.SceneObject;
import javafx.scene.paint.Color;

public class CameraGizmo extends SceneObject {

    private final Camera camera;

    public CameraGizmo(String name, Model model, Camera camera) {
        super(name, model, null);
        this.camera = camera;

        RenderSettings settings = new RenderSettings();
        settings.drawWireframe = true;
        settings.useTexture = false;
        settings.useLighting = false;
        applyRenderSettings(settings);

        setWireframeColor(Color.LIGHTGREEN);
        setModelColor(Color.color(0.6, 0.9, 0.6));
    }

    public Camera getCamera() {
        return camera;
    }


}
