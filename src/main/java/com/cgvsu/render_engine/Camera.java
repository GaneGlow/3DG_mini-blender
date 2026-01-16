package com.cgvsu.render_engine;


import com.cgvsu.math.Matrix4;
import com.cgvsu.math.Vector3;

public class Camera {

    public Camera(
            final Vector3 position,
            final Vector3 target,
            final float fov,
            final float aspectRatio,
            final float nearPlane,
            final float farPlane) {
        this.position = position;
        this.target = target;
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
    }

    public void setPosition(final Vector3 position) {
        this.position = position;
    }

    public void setTarget(final Vector3 target) {
        this.target = target;
    }

    public void setAspectRatio(final float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getTarget() {
        return target;
    }

    public void movePosition(final Vector3 translation) {
        if (translation == null) {
            return;
        }

        // Не полагаемся на то, изменяет ли Vector3 себя «на месте».
        // Явно пересчитываем координаты.
        this.position = new Vector3(
                this.position.x + translation.x,
                this.position.y + translation.y,
                this.position.z + translation.z
        );
    }

    public void moveTarget(final Vector3 translation) {
        if (translation == null) {
            return;
        }

        this.target = new Vector3(
                this.target.x + translation.x,
                this.target.y + translation.y,
                this.target.z + translation.z
        );
    }

    public Matrix4 getViewMatrix() {
        return GraphicConveyor.lookAt(position, target);
    }

    public Matrix4 getProjectionMatrix() {
        return GraphicConveyor.perspective(fov, aspectRatio, nearPlane, farPlane);
    }

    private Vector3 position;
    private Vector3 target;
    private float fov;
    private float aspectRatio;
    private float nearPlane;
    private float farPlane;
}