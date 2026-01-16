package com.cgvsu.render_engine;

import com.cgvsu.math.Vector3;

public class Transform {
    private Vector3 translation = new Vector3(0, 0, 0);
    private Vector3 rotation = new Vector3(0, 0, 0); // углы в радианах
    private Vector3 scale = new Vector3(1, 1, 1);

    public Transform() {}

    public Vector3 getTranslation() { return translation; }
    public Vector3 getRotation() { return rotation; }
    public Vector3 getScale() { return scale; }

    public void setTranslation(Vector3 translation) { this.translation = translation; }
    public void setRotation(Vector3 rotation) { this.rotation = rotation; }
    public void setScale(Vector3 scale) { this.scale = scale; }

    public void translate(float dx, float dy, float dz) {
        translation.x += dx;
        translation.y += dy;
        translation.z += dz;
    }

    public void rotate(float dx, float dy, float dz) {
        rotation.x += dx;
        rotation.y += dy;
        rotation.z += dz;
    }

    // Масштабирование по всем осям для увеличения/уменьшения
    public void scale(float factor) {
        scale.x *= factor;
        scale.y *= factor;
        scale.z *= factor;
    }

    public void scaleX(float factor) {
        scale.x *= factor;
    }

    public void scaleY(float factor) {
        scale.y *= factor;
    }

    public void scaleZ(float factor) {
        scale.z *= factor;
    }

    public void reset() {
        translation = new Vector3(0, 0, 0);
        rotation = new Vector3(0, 0, 0);
        scale = new Vector3(1, 1, 1);
    }
}