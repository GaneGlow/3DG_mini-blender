package com.cgvsu.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import com.cgvsu.math.*;
import com.cgvsu.render_engine.GraphicConveyor;

import static com.cgvsu.render_engine.GraphicConveyor.*;

class GraphicConveyorTest {

    private static final float EPSILON = 0.0001f;
/*
    @Test
    void testLookAtIdentity() {
        // Камера смотрит вдоль оси Z в положительном направлении
        Vector3 eye = new Vector3(0, 0, 0);
        Vector3 target = new Vector3(0, 0, 1);

        Matrix4 viewMatrix = lookAt(eye, target);

        // Ожидается единичная матрица
        Assertions.assertEquals(1.0f, viewMatrix.get(0, 0), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(0, 1), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(0, 2), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(0, 3), EPSILON);

        Assertions.assertEquals(0.0f, viewMatrix.get(1, 0), EPSILON);
        Assertions.assertEquals(1.0f, viewMatrix.get(1, 1), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(1, 2), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(1, 3), EPSILON);

        Assertions.assertEquals(0.0f, viewMatrix.get(2, 0), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(2, 1), EPSILON);
        Assertions.assertEquals(1.0f, viewMatrix.get(2, 2), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(2, 3), EPSILON);
    }

    @Test
    void testLookAtWithTranslation() {
        Vector3 eye = new Vector3(1, 2, 3);
        Vector3 target = new Vector3(1, 2, 4); // Смотрим вдоль Z
        Vector3 up = new Vector3(0, 1, 0);

        Matrix4 viewMatrix = lookAt(eye, target, up);

        // Проверяем компоненты матрицы вида
        Assertions.assertEquals(1.0f, viewMatrix.get(0, 0), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(0, 1), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(0, 2), EPSILON);
        Assertions.assertEquals(-1.0f, viewMatrix.get(0, 3), EPSILON); // -dot(x, eye)

        Assertions.assertEquals(0.0f, viewMatrix.get(1, 0), EPSILON);
        Assertions.assertEquals(1.0f, viewMatrix.get(1, 1), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(1, 2), EPSILON);
        Assertions.assertEquals(-2.0f, viewMatrix.get(1, 3), EPSILON); // -dot(y, eye)

        Assertions.assertEquals(0.0f, viewMatrix.get(2, 0), EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.get(2, 1), EPSILON);
        Assertions.assertEquals(1.0f, viewMatrix.get(2, 2), EPSILON);
        Assertions.assertEquals(-3.0f, viewMatrix.get(2, 3), EPSILON); // -dot(z, eye)
    }

    @Test
    void testPerspectiveProjection() {
        float fov = (float) Math.toRadians(90.0f);
        float aspectRatio = 16.0f / 9.0f;
        float near = 0.1f;
        float far = 100.0f;

        Matrix4 projMatrix = perspective(fov, aspectRatio, near, far);

        // Проверяем основные компоненты матрицы проекции
        float expectedF = 1.0f / (float) Math.tan(fov * 0.5f);

        Assertions.assertEquals(expectedF / aspectRatio, projMatrix.get(0, 0), EPSILON);
        Assertions.assertEquals(expectedF, projMatrix.get(1, 1), EPSILON);
        Assertions.assertEquals((far + near) / (far - near), projMatrix.get(2, 2), EPSILON);
        Assertions.assertEquals(2.0f * (near * far) / (near - far), projMatrix.get(2, 3), EPSILON);
        Assertions.assertEquals(1.0f, projMatrix.get(3, 2), EPSILON); // w' = z
        Assertions.assertEquals(0.0f, projMatrix.get(3, 3), EPSILON);
    }

    @Test
    void testMultiplyMatrix4ByVector3() {
        Matrix4 matrix = Matrix4.identity();
        float[][] values = matrix.m;
        values[0][3] = 1.0f; // Сдвиг по X
        values[1][3] = 2.0f; // Сдвиг по Y

        Vector3 vertex = new Vector3(1, 1, 1);
        Vector3 result = multiplyMatrix4ByVector3(matrix, vertex);

        Assertions.assertEquals(2.0f, result.x, EPSILON); // 1*1 + 0*1 + 0*1 + 1 = 2
        Assertions.assertEquals(3.0f, result.y, EPSILON); // 0*1 + 1*1 + 0*1 + 2 = 3
        Assertions.assertEquals(1.0f, result.z, EPSILON); // 0*1 + 0*1 + 1*1 + 0 = 1
    }

    @Test
    void testMultiplyMatrix4ByVector3WithDivision() {
        Matrix4 matrix = Matrix4.identity();
        float[][] values = matrix.m;
        values[3][3] = 2.0f; // w = 2, должно делиться

        Vector3 vertex = new Vector3(2, 4, 6);
        Vector3 result = multiplyMatrix4ByVector3(matrix, vertex);

        Assertions.assertEquals(1.0f, result.x, EPSILON); // 2 / 2 = 1
        Assertions.assertEquals(2.0f, result.y, EPSILON); // 4 / 2 = 2
        Assertions.assertEquals(3.0f, result.z, EPSILON); // 6 / 2 = 3
    }

    @Test
    void testVertexToPoint() {
        // NDC координаты [-1, 1] в центр экрана
        Vector3 vertex = new Vector3(0, 0, 0);
        int width = 800;
        int height = 600;

        Point2 result = vertexToPoint(vertex, width, height);

        Assertions.assertEquals(400.0f, result.x, EPSILON); // (0 + 1) * 800/2
        Assertions.assertEquals(300.0f, result.y, EPSILON); // (1 - 0) * 600/2
    }

    @Test
    void testVertexToPointCorner() {
        // Верхний левый угол NDC [-1, 1] должен быть (0, 0) в экранных координатах
        Vector3 vertex = new Vector3(-1, 1, 0);
        int width = 800;
        int height = 600;

        Point2 result = vertexToPoint(vertex, width, height);

        Assertions.assertEquals(0.0f, result.x, EPSILON);
        Assertions.assertEquals(0.0f, result.y, EPSILON);
    }

    @Test
    void testCreateTranslationMatrix() {
        Vector3 translation = new Vector3(1, 2, 3);
        Matrix4 matrix = createTranslationMatrix(translation);

        Assertions.assertEquals(1.0f, matrix.get(0, 0), EPSILON);
        Assertions.assertEquals(1.0f, matrix.get(1, 1), EPSILON);
        Assertions.assertEquals(1.0f, matrix.get(2, 2), EPSILON);
        Assertions.assertEquals(1.0f, matrix.get(0, 3), EPSILON);
        Assertions.assertEquals(2.0f, matrix.get(1, 3), EPSILON);
        Assertions.assertEquals(3.0f, matrix.get(2, 3), EPSILON);
        Assertions.assertEquals(1.0f, matrix.get(3, 3), EPSILON);
    }

    @Test
    void testCreateScaleMatrix() {
        Vector3 scale = new Vector3(2, 3, 4);
        Matrix4 matrix = createScaleMatrix(scale);

        Assertions.assertEquals(2.0f, matrix.get(0, 0), EPSILON);
        Assertions.assertEquals(3.0f, matrix.get(1, 1), EPSILON);
        Assertions.assertEquals(4.0f, matrix.get(2, 2), EPSILON);
        Assertions.assertEquals(1.0f, matrix.get(3, 3), EPSILON);
    }

    @Test
    void testCreateRotationXMatrix() {
        float angle = (float) Math.PI / 2; // 90 градусов
        Matrix4 matrix = createRotationXMatrix(angle);

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        Assertions.assertEquals(1.0f, matrix.get(0, 0), EPSILON);
        Assertions.assertEquals(cos, matrix.get(1, 1), EPSILON);
        Assertions.assertEquals(-sin, matrix.get(1, 2), EPSILON);
        Assertions.assertEquals(sin, matrix.get(2, 1), EPSILON);
        Assertions.assertEquals(cos, matrix.get(2, 2), EPSILON);
        Assertions.assertEquals(1.0f, matrix.get(3, 3), EPSILON);
    }

    @Test
    void testCreateModelMatrix() {
        Vector3 translation = new Vector3(1, 2, 3);
        Vector3 rotation = new Vector3(0, 0, 0); // Нет вращения
        Vector3 scale = new Vector3(2, 2, 2);

        Matrix4 modelMatrix = createModelMatrix(translation, rotation, scale);

        // Проверяем комбинацию T * I * S = T * S
        Assertions.assertEquals(2.0f, modelMatrix.get(0, 0), EPSILON);
        Assertions.assertEquals(2.0f, modelMatrix.get(1, 1), EPSILON);
        Assertions.assertEquals(2.0f, modelMatrix.get(2, 2), EPSILON);
        Assertions.assertEquals(1.0f, modelMatrix.get(0, 3), EPSILON);
        Assertions.assertEquals(2.0f, modelMatrix.get(1, 3), EPSILON);
        Assertions.assertEquals(3.0f, modelMatrix.get(2, 3), EPSILON);
    }

    @Test
    void testCreateRotationYMatrix() {
        float angle = (float) (Math.PI / 2.0); // 90°
        Matrix4 m = createRotationYMatrix(angle);

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        Assertions.assertEquals(cos, m.get(0, 0), EPSILON);
        Assertions.assertEquals(sin, m.get(0, 2), EPSILON);
        Assertions.assertEquals(-sin, m.get(2, 0), EPSILON);
        Assertions.assertEquals(cos, m.get(2, 2), EPSILON);
        Assertions.assertEquals(1.0f, m.get(1, 1), EPSILON);
        Assertions.assertEquals(1.0f, m.get(3, 3), EPSILON);

        // Вектор (0,0,1) при +90° вокруг Y должен перейти в (1,0,0)
        Vector3 v = new Vector3(0, 0, 1);
        Vector3 r = multiplyMatrix4ByVector3(m, v);
        Assertions.assertEquals(1.0f, r.x, EPSILON);
        Assertions.assertEquals(0.0f, r.y, EPSILON);
        Assertions.assertEquals(0.0f, r.z, EPSILON);
    }

    @Test
    void testCreateRotationZMatrix() {
        float angle = (float) (Math.PI / 2.0); // 90°
        Matrix4 m = createRotationZMatrix(angle);

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        Assertions.assertEquals(cos, m.get(0, 0), EPSILON);
        Assertions.assertEquals(-sin, m.get(0, 1), EPSILON);
        Assertions.assertEquals(sin, m.get(1, 0), EPSILON);
        Assertions.assertEquals(cos, m.get(1, 1), EPSILON);
        Assertions.assertEquals(1.0f, m.get(2, 2), EPSILON);
        Assertions.assertEquals(1.0f, m.get(3, 3), EPSILON);

        // (1,0,0) при +90° вокруг Z должен перейти в (0,1,0)
        Vector3 v = new Vector3(1, 0, 0);
        Vector3 r = multiplyMatrix4ByVector3(m, v);
        Assertions.assertEquals(0.0f, r.x, EPSILON);
        Assertions.assertEquals(1.0f, r.y, EPSILON);
        Assertions.assertEquals(0.0f, r.z, EPSILON);
    }

    @Test
    void testLookAtMapsEyeToOrigin() {
        Vector3 eye = new Vector3(1, 2, 3);
        Vector3 target = new Vector3(1, 3, 4);
        Vector3 up = new Vector3(0, 1, 0);

        Matrix4 view = lookAt(eye, target, up);

        Vector3 eyeInCamera = multiplyMatrix4ByVector3(view, eye);
        Assertions.assertEquals(0.0f, eyeInCamera.x, EPSILON);
        Assertions.assertEquals(0.0f, eyeInCamera.y, EPSILON);
        Assertions.assertEquals(0.0f, eyeInCamera.z, EPSILON);
    }

    @Test
    void testLookAtMapsTargetToPositiveZAxisWithDistance() {
        Vector3 eye = new Vector3(1, 2, 3);
        Vector3 target = new Vector3(1, 3, 4);
        Vector3 up = new Vector3(0, 1, 0);

        Vector3 dir = target.subtract(eye);
        float dist = dir.length();

        Matrix4 view = lookAt(eye, target, up);
        Vector3 targetInCamera = multiplyMatrix4ByVector3(view, target);

        Assertions.assertEquals(0.0f, targetInCamera.x, EPSILON);
        Assertions.assertEquals(0.0f, targetInCamera.y, EPSILON);
        Assertions.assertEquals(dist, targetInCamera.z, EPSILON);
    }

    @Test
    void testLookAtBasisIsOrthonormal() {
        Vector3 eye = new Vector3(1, 2, 3);
        Vector3 target = new Vector3(2, 4, 6); // не параллельно up
        Vector3 up = new Vector3(0, 1, 0);

        Matrix4 view = lookAt(eye, target, up);

        Vector3 x = new Vector3(view.get(0, 0), view.get(0, 1), view.get(0, 2));
        Vector3 y = new Vector3(view.get(1, 0), view.get(1, 1), view.get(1, 2));
        Vector3 z = new Vector3(view.get(2, 0), view.get(2, 1), view.get(2, 2));

        Assertions.assertEquals(1.0f, x.length(), EPSILON);
        Assertions.assertEquals(1.0f, y.length(), EPSILON);
        Assertions.assertEquals(1.0f, z.length(), EPSILON);

        Assertions.assertEquals(0.0f, x.dot(y), EPSILON);
        Assertions.assertEquals(0.0f, x.dot(z), EPSILON);
        Assertions.assertEquals(0.0f, y.dot(z), EPSILON);
    }

    @Test
    void testPerspectiveMapsNearToMinusOneAndFarToPlusOne() {
        float fov = (float) Math.toRadians(90.0f);
        float aspect = 1.0f;
        float near = 0.1f;
        float far = 100.0f;

        Matrix4 p = perspective(fov, aspect, near, far);

        Vector3 vNear = new Vector3(0, 0, near);
        Vector3 vFar = new Vector3(0, 0, far);

        Vector3 rNear = multiplyMatrix4ByVector3(p, vNear);
        Vector3 rFar = multiplyMatrix4ByVector3(p, vFar);

        Assertions.assertEquals(-1.0f, rNear.z, EPSILON);
        Assertions.assertEquals(1.0f, rFar.z, EPSILON);
    }

    @Test
    void testPerspectiveXDependsOnZBecauseOfDivisionByW() {
        float fov = (float) Math.toRadians(90.0f); // f = 1
        float aspect = 1.0f;
        float near = 0.1f;
        float far = 100.0f;

        Matrix4 p = perspective(fov, aspect, near, far);

        // x_ndc = (f/aspect * x) / z
        Vector3 v1 = new Vector3(1, 0, 1);
        Vector3 v2 = new Vector3(1, 0, 2);

        Vector3 r1 = multiplyMatrix4ByVector3(p, v1);
        Vector3 r2 = multiplyMatrix4ByVector3(p, v2);

        Assertions.assertEquals(1.0f, r1.x, EPSILON);
        Assertions.assertEquals(0.5f, r2.x, EPSILON);
    }

    @Test
    void testMultiplyMatrix4ByVector3ReturnsNaNWhenWIsZero() {
        float fov = (float) Math.toRadians(90.0f);
        float aspect = 1.0f;
        float near = 0.1f;
        float far = 100.0f;

        Matrix4 p = perspective(fov, aspect, near, far);

        // Для вашей перспективы w' = z, значит при z=0 получаем w=0 -> NaN
        Vector3 onCameraPlane = new Vector3(0, 0, 0);
        Vector3 r = multiplyMatrix4ByVector3(p, onCameraPlane);

        Assertions.assertTrue(Float.isNaN(r.x));
        Assertions.assertTrue(Float.isNaN(r.y));
        Assertions.assertTrue(Float.isNaN(r.z));
    }

    @Test
    void testVertexToPointBottomRightCorner() {
        Vector3 vertex = new Vector3(1, -1, 0); // правый нижний NDC
        int width = 800;
        int height = 600;

        Point2 p = vertexToPoint(vertex, width, height);

        Assertions.assertEquals(800.0f, p.x, EPSILON);
        Assertions.assertEquals(600.0f, p.y, EPSILON);
    }

    @Test
    void testCreateModelMatrixAppliesScaleThenRotationThenTranslation() {
        Vector3 translation = new Vector3(1, 2, 3);
        Vector3 rotation = new Vector3(0, 0, (float) (Math.PI / 2.0)); // Z +90°
        Vector3 scale = new Vector3(2, 1, 1);

        Matrix4 model = createModelMatrix(translation, rotation, scale);

        // Вершина (1,0,0):
        // S: (2,0,0)
        // Rz(90): (0,2,0)
        // T(+1,+2,+3): (1,4,3)
        Vector3 v = new Vector3(1, 0, 0);
        Vector3 r = multiplyMatrix4ByVector3(model, v);

        Assertions.assertEquals(1.0f, r.x, EPSILON);
        Assertions.assertEquals(4.0f, r.y, EPSILON);
        Assertions.assertEquals(3.0f, r.z, EPSILON);
    }

    @Test
    void testCreateModelMatrixRotationOrderIsRxThenRyThenRz() {
        // В вашей реализации rotationMatrix = Rz * Ry * Rx,
        // а значит на вектор сначала действует Rx, потом Ry, потом Rz.
        Vector3 translation = new Vector3(0, 0, 0);
        Vector3 scale = new Vector3(1, 1, 1);

        Vector3 rotation = new Vector3(
                (float) (Math.PI / 2.0), // Rx 90°
                (float) (Math.PI / 2.0), // Ry 90°
                0.0f                      // Rz 0°
        );

        Matrix4 model = createModelMatrix(translation, rotation, scale);

        // Берем (0,0,1):
        // Rx(90): (0,-1,0)
        // Ry(90): (0,-1,0) (не меняется, т.к. x=z=0)
        Vector3 v = new Vector3(0, 0, 1);
        Vector3 r = multiplyMatrix4ByVector3(model, v);

        Assertions.assertEquals(0.0f, r.x, EPSILON);
        Assertions.assertEquals(-1.0f, r.y, EPSILON);
        Assertions.assertEquals(0.0f, r.z, EPSILON);
    }*/
}