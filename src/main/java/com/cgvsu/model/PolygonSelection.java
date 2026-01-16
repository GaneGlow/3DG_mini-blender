package com.cgvsu.model;

import com.cgvsu.model.Polygon;
import com.cgvsu.render_engine.scene.SceneObject;

public class PolygonSelection {
    private final SceneObject sceneObject;
    private final Polygon polygon;

    public PolygonSelection(SceneObject sceneObject, Polygon polygon) {
        this.sceneObject = sceneObject;
        this.polygon = polygon;
    }

    public SceneObject getSceneObject() {
        return sceneObject;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PolygonSelection that = (PolygonSelection) obj;
        return sceneObject.equals(that.sceneObject) &&
                polygon.equals(that.polygon);
    }

    @Override
    public int hashCode() {
        return 31 * sceneObject.hashCode() + polygon.hashCode();
    }
}