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
        this.position.add(translation);
    }

    public void moveTarget(final Vector3 translation) {
        this.target.add(target);
    }

    public float getFov() { return fov; }

    public void setFov(float fov) { this.fov = fov; }

    Matrix4 getViewMatrix() {
        return GraphicConveyor.lookAt(position, target);
    }

    Matrix4 getProjectionMatrix() {
        return GraphicConveyor.perspective(fov, aspectRatio, nearPlane, farPlane);
    }

    private Vector3 position;
    private Vector3 target;
    private float fov;
    private float aspectRatio;
    private float nearPlane;
    private float farPlane;
}