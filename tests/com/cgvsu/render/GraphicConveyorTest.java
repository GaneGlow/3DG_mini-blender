package com.cgvsu.objreader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import javax.vecmath.*;

import static com.cgvsu.render_engine.GraphicConveyor.*;

class GraphicConveyorTest {

    private static final float EPSILON = 0.0001f;

    @Test
    void testLookAtIdentity() {
        // Камера смотрит вдоль оси Z в положительном направлении
        Vector3f eye = new Vector3f(0, 0, 0);
        Vector3f target = new Vector3f(0, 0, 1);

        Matrix4f viewMatrix = lookAt(eye, target);

        // Ожидается единичная матрица
        Assertions.assertEquals(1.0f, viewMatrix.m00, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m01, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m02, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m03, EPSILON);

        Assertions.assertEquals(0.0f, viewMatrix.m10, EPSILON);
        Assertions.assertEquals(1.0f, viewMatrix.m11, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m12, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m13, EPSILON);

        Assertions.assertEquals(0.0f, viewMatrix.m20, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m21, EPSILON);
        Assertions.assertEquals(1.0f, viewMatrix.m22, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m23, EPSILON);
    }

    @Test
    void testLookAtWithTranslation() {
        Vector3f eye = new Vector3f(1, 2, 3);
        Vector3f target = new Vector3f(1, 2, 4); // Смотрим вдоль Z
        Vector3f up = new Vector3f(0, 1, 0);

        Matrix4f viewMatrix = lookAt(eye, target, up);

        // Проверяем компоненты матрицы вида
        Assertions.assertEquals(1.0f, viewMatrix.m00, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m01, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m02, EPSILON);
        Assertions.assertEquals(-1.0f, viewMatrix.m03, EPSILON); // -dot(x, eye)

        Assertions.assertEquals(0.0f, viewMatrix.m10, EPSILON);
        Assertions.assertEquals(1.0f, viewMatrix.m11, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m12, EPSILON);
        Assertions.assertEquals(-2.0f, viewMatrix.m13, EPSILON); // -dot(y, eye)

        Assertions.assertEquals(0.0f, viewMatrix.m20, EPSILON);
        Assertions.assertEquals(0.0f, viewMatrix.m21, EPSILON);
        Assertions.assertEquals(1.0f, viewMatrix.m22, EPSILON);
        Assertions.assertEquals(-3.0f, viewMatrix.m23, EPSILON); // -dot(z, eye)
    }

    @Test
    void testPerspectiveProjection() {
        float fov = (float) Math.toRadians(90.0f);
        float aspectRatio = 16.0f / 9.0f;
        float near = 0.1f;
        float far = 100.0f;

        Matrix4f projMatrix = perspective(fov, aspectRatio, near, far);

        // Проверяем основные компоненты матрицы проекции
        float expectedF = 1.0f / (float) Math.tan(fov * 0.5f);

        Assertions.assertEquals(expectedF / aspectRatio, projMatrix.m00, EPSILON);
        Assertions.assertEquals(expectedF, projMatrix.m11, EPSILON);
        Assertions.assertEquals((far + near) / (far - near), projMatrix.m22, EPSILON);
        Assertions.assertEquals(2.0f * (near * far) / (near - far), projMatrix.m23, EPSILON);
        Assertions.assertEquals(1.0f, projMatrix.m32, EPSILON); // w' = z
        Assertions.assertEquals(0.0f, projMatrix.m33, EPSILON);
    }

    @Test
    void testMultiplyMatrix4ByVector3() {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m03 = 1.0f; // Сдвиг по X
        matrix.m13 = 2.0f; // Сдвиг по Y

        Vector3f vertex = new Vector3f(1, 1, 1);
        Vector3f result = multiplyMatrix4ByVector3(matrix, vertex);

        Assertions.assertEquals(2.0f, result.x, EPSILON); // 1*1 + 0*1 + 0*1 + 1 = 2
        Assertions.assertEquals(3.0f, result.y, EPSILON); // 0*1 + 1*1 + 0*1 + 2 = 3
        Assertions.assertEquals(1.0f, result.z, EPSILON); // 0*1 + 0*1 + 1*1 + 0 = 1
    }

    @Test
    void testMultiplyMatrix4ByVector3WithDivision() {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m33 = 2.0f; // w = 2, должно делиться

        Vector3f vertex = new Vector3f(2, 4, 6);
        Vector3f result = multiplyMatrix4ByVector3(matrix, vertex);

        Assertions.assertEquals(1.0f, result.x, EPSILON); // 2 / 2 = 1
        Assertions.assertEquals(2.0f, result.y, EPSILON); // 4 / 2 = 2
        Assertions.assertEquals(3.0f, result.z, EPSILON); // 6 / 2 = 3
    }

    @Test
    void testVertexToPoint() {
        // NDC координаты [-1, 1] в центр экрана
        Vector3f vertex = new Vector3f(0, 0, 0);
        int width = 800;
        int height = 600;

        Point2f result = vertexToPoint(vertex, width, height);

        Assertions.assertEquals(400.0f, result.x, EPSILON); // (0 + 1) * 800/2
        Assertions.assertEquals(300.0f, result.y, EPSILON); // (1 - 0) * 600/2
    }

    @Test
    void testVertexToPointCorner() {
        // Верхний левый угол NDC [-1, 1] должен быть (0, 0) в экранных координатах
        Vector3f vertex = new Vector3f(-1, 1, 0);
        int width = 800;
        int height = 600;

        Point2f result = vertexToPoint(vertex, width, height);

        Assertions.assertEquals(0.0f, result.x, EPSILON);
        Assertions.assertEquals(0.0f, result.y, EPSILON);
    }

    @Test
    void testCreateTranslationMatrix() {
        Vector3f translation = new Vector3f(1, 2, 3);
        Matrix4f matrix = createTranslationMatrix(translation);

        Assertions.assertEquals(1.0f, matrix.m00, EPSILON);
        Assertions.assertEquals(1.0f, matrix.m11, EPSILON);
        Assertions.assertEquals(1.0f, matrix.m22, EPSILON);
        Assertions.assertEquals(1.0f, matrix.m03, EPSILON);
        Assertions.assertEquals(2.0f, matrix.m13, EPSILON);
        Assertions.assertEquals(3.0f, matrix.m23, EPSILON);
        Assertions.assertEquals(1.0f, matrix.m33, EPSILON);
    }

    @Test
    void testCreateScaleMatrix() {
        Vector3f scale = new Vector3f(2, 3, 4);
        Matrix4f matrix = createScaleMatrix(scale);

        Assertions.assertEquals(2.0f, matrix.m00, EPSILON);
        Assertions.assertEquals(3.0f, matrix.m11, EPSILON);
        Assertions.assertEquals(4.0f, matrix.m22, EPSILON);
        Assertions.assertEquals(1.0f, matrix.m33, EPSILON);
    }

    @Test
    void testCreateRotationXMatrix() {
        float angle = (float) Math.PI / 2; // 90 градусов
        Matrix4f matrix = createRotationXMatrix(angle);

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        Assertions.assertEquals(1.0f, matrix.m00, EPSILON);
        Assertions.assertEquals(cos, matrix.m11, EPSILON);
        Assertions.assertEquals(-sin, matrix.m12, EPSILON);
        Assertions.assertEquals(sin, matrix.m21, EPSILON);
        Assertions.assertEquals(cos, matrix.m22, EPSILON);
        Assertions.assertEquals(1.0f, matrix.m33, EPSILON);
    }

    @Test
    void testCreateModelMatrix() {
        Vector3f translation = new Vector3f(1, 2, 3);
        Vector3f rotation = new Vector3f(0, 0, 0); // Нет вращения
        Vector3f scale = new Vector3f(2, 2, 2);

        Matrix4f modelMatrix = createModelMatrix(translation, rotation, scale);

        // Проверяем комбинацию T * I * S = T * S
        Assertions.assertEquals(2.0f, modelMatrix.m00, EPSILON);
        Assertions.assertEquals(2.0f, modelMatrix.m11, EPSILON);
        Assertions.assertEquals(2.0f, modelMatrix.m22, EPSILON);
        Assertions.assertEquals(1.0f, modelMatrix.m03, EPSILON);
        Assertions.assertEquals(2.0f, modelMatrix.m13, EPSILON);
        Assertions.assertEquals(3.0f, modelMatrix.m23, EPSILON);
    }

    @Test
    void testCreateRotationYMatrix() {
        float angle = (float) (Math.PI / 2.0); // 90°
        Matrix4f m = createRotationYMatrix(angle);

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        Assertions.assertEquals(cos, m.m00, EPSILON);
        Assertions.assertEquals(sin, m.m02, EPSILON);
        Assertions.assertEquals(-sin, m.m20, EPSILON);
        Assertions.assertEquals(cos, m.m22, EPSILON);
        Assertions.assertEquals(1.0f, m.m11, EPSILON);
        Assertions.assertEquals(1.0f, m.m33, EPSILON);

        // Вектор (0,0,1) при +90° вокруг Y должен перейти в (1,0,0)
        Vector3f v = new Vector3f(0, 0, 1);
        Vector3f r = multiplyMatrix4ByVector3(m, v);
        Assertions.assertEquals(1.0f, r.x, EPSILON);
        Assertions.assertEquals(0.0f, r.y, EPSILON);
        Assertions.assertEquals(0.0f, r.z, EPSILON);
    }

    @Test
    void testCreateRotationZMatrix() {
        float angle = (float) (Math.PI / 2.0); // 90°
        Matrix4f m = createRotationZMatrix(angle);

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        Assertions.assertEquals(cos, m.m00, EPSILON);
        Assertions.assertEquals(-sin, m.m01, EPSILON);
        Assertions.assertEquals(sin, m.m10, EPSILON);
        Assertions.assertEquals(cos, m.m11, EPSILON);
        Assertions.assertEquals(1.0f, m.m22, EPSILON);
        Assertions.assertEquals(1.0f, m.m33, EPSILON);

        // (1,0,0) при +90° вокруг Z должен перейти в (0,1,0)
        Vector3f v = new Vector3f(1, 0, 0);
        Vector3f r = multiplyMatrix4ByVector3(m, v);
        Assertions.assertEquals(0.0f, r.x, EPSILON);
        Assertions.assertEquals(1.0f, r.y, EPSILON);
        Assertions.assertEquals(0.0f, r.z, EPSILON);
    }

    @Test
    void testLookAtMapsEyeToOrigin() {
        Vector3f eye = new Vector3f(1, 2, 3);
        Vector3f target = new Vector3f(1, 3, 4);
        Vector3f up = new Vector3f(0, 1, 0);

        Matrix4f view = lookAt(eye, target, up);

        Vector3f eyeInCamera = multiplyMatrix4ByVector3(view, eye);
        Assertions.assertEquals(0.0f, eyeInCamera.x, EPSILON);
        Assertions.assertEquals(0.0f, eyeInCamera.y, EPSILON);
        Assertions.assertEquals(0.0f, eyeInCamera.z, EPSILON);
    }

    @Test
    void testLookAtMapsTargetToPositiveZAxisWithDistance() {
        Vector3f eye = new Vector3f(1, 2, 3);
        Vector3f target = new Vector3f(1, 3, 4);
        Vector3f up = new Vector3f(0, 1, 0);

        Vector3f dir = new Vector3f();
        dir.sub(target, eye);
        float dist = dir.length();

        Matrix4f view = lookAt(eye, target, up);
        Vector3f targetInCamera = multiplyMatrix4ByVector3(view, target);

        Assertions.assertEquals(0.0f, targetInCamera.x, EPSILON);
        Assertions.assertEquals(0.0f, targetInCamera.y, EPSILON);
        Assertions.assertEquals(dist, targetInCamera.z, EPSILON);
    }

    @Test
    void testLookAtBasisIsOrthonormal() {
        Vector3f eye = new Vector3f(1, 2, 3);
        Vector3f target = new Vector3f(2, 4, 6); // не параллельно up
        Vector3f up = new Vector3f(0, 1, 0);

        Matrix4f view = lookAt(eye, target, up);

        Vector3f x = new Vector3f(view.m00, view.m01, view.m02);
        Vector3f y = new Vector3f(view.m10, view.m11, view.m12);
        Vector3f z = new Vector3f(view.m20, view.m21, view.m22);

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

        Matrix4f p = perspective(fov, aspect, near, far);

        Vector3f vNear = new Vector3f(0, 0, near);
        Vector3f vFar = new Vector3f(0, 0, far);

        Vector3f rNear = multiplyMatrix4ByVector3(p, vNear);
        Vector3f rFar = multiplyMatrix4ByVector3(p, vFar);

        Assertions.assertEquals(-1.0f, rNear.z, EPSILON);
        Assertions.assertEquals(1.0f, rFar.z, EPSILON);
    }

    @Test
    void testPerspectiveXDependsOnZBecauseOfDivisionByW() {
        float fov = (float) Math.toRadians(90.0f); // f = 1
        float aspect = 1.0f;
        float near = 0.1f;
        float far = 100.0f;

        Matrix4f p = perspective(fov, aspect, near, far);

        // x_ndc = (f/aspect * x) / z
        Vector3f v1 = new Vector3f(1, 0, 1);
        Vector3f v2 = new Vector3f(1, 0, 2);

        Vector3f r1 = multiplyMatrix4ByVector3(p, v1);
        Vector3f r2 = multiplyMatrix4ByVector3(p, v2);

        Assertions.assertEquals(1.0f, r1.x, EPSILON);
        Assertions.assertEquals(0.5f, r2.x, EPSILON);
    }

    @Test
    void testMultiplyMatrix4ByVector3ReturnsNaNWhenWIsZero() {
        float fov = (float) Math.toRadians(90.0f);
        float aspect = 1.0f;
        float near = 0.1f;
        float far = 100.0f;

        Matrix4f p = perspective(fov, aspect, near, far);

        // Для вашей перспективы w' = z, значит при z=0 получаем w=0 -> NaN
        Vector3f onCameraPlane = new Vector3f(0, 0, 0);
        Vector3f r = multiplyMatrix4ByVector3(p, onCameraPlane);

        Assertions.assertTrue(Float.isNaN(r.x));
        Assertions.assertTrue(Float.isNaN(r.y));
        Assertions.assertTrue(Float.isNaN(r.z));
    }

    @Test
    void testVertexToPointBottomRightCorner() {
        Vector3f vertex = new Vector3f(1, -1, 0); // правый нижний NDC
        int width = 800;
        int height = 600;

        Point2f p = vertexToPoint(vertex, width, height);

        Assertions.assertEquals(800.0f, p.x, EPSILON);
        Assertions.assertEquals(600.0f, p.y, EPSILON);
    }

    @Test
    void testCreateModelMatrixAppliesScaleThenRotationThenTranslation() {
        Vector3f translation = new Vector3f(1, 2, 3);
        Vector3f rotation = new Vector3f(0, 0, (float) (Math.PI / 2.0)); // Z +90°
        Vector3f scale = new Vector3f(2, 1, 1);

        Matrix4f model = createModelMatrix(translation, rotation, scale);

        // Вершина (1,0,0):
        // S: (2,0,0)
        // Rz(90): (0,2,0)
        // T(+1,+2,+3): (1,4,3)
        Vector3f v = new Vector3f(1, 0, 0);
        Vector3f r = multiplyMatrix4ByVector3(model, v);

        Assertions.assertEquals(1.0f, r.x, EPSILON);
        Assertions.assertEquals(4.0f, r.y, EPSILON);
        Assertions.assertEquals(3.0f, r.z, EPSILON);
    }

    @Test
    void testCreateModelMatrixRotationOrderIsRxThenRyThenRz() {
        // В вашей реализации rotationMatrix = Rz * Ry * Rx,
        // а значит на вектор сначала действует Rx, потом Ry, потом Rz.
        Vector3f translation = new Vector3f(0, 0, 0);
        Vector3f scale = new Vector3f(1, 1, 1);

        Vector3f rotation = new Vector3f(
                (float) (Math.PI / 2.0), // Rx 90°
                (float) (Math.PI / 2.0), // Ry 90°
                0.0f                      // Rz 0°
        );

        Matrix4f model = createModelMatrix(translation, rotation, scale);

        // Берем (0,0,1):
        // Rx(90): (0,-1,0)
        // Ry(90): (0,-1,0) (не меняется, т.к. x=z=0)
        Vector3f v = new Vector3f(0, 0, 1);
        Vector3f r = multiplyMatrix4ByVector3(model, v);

        Assertions.assertEquals(0.0f, r.x, EPSILON);
        Assertions.assertEquals(-1.0f, r.y, EPSILON);
        Assertions.assertEquals(0.0f, r.z, EPSILON);
    }
}