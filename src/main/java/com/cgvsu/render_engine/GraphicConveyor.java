package com.cgvsu.render_engine;

import com.cgvsu.math.*;

public class GraphicConveyor {

    public static Matrix4 lookAt(final Vector3 eye, final Vector3 target) {
        return lookAt(eye, target, new Vector3(0F, 1.0F, 0F));
    }


    public static Matrix4 lookAt(final Vector3 eye, final Vector3 target, final Vector3 up) {
        Vector3 z = target.subtract(eye);
        z = z.normalized();

        Vector3 x = up.cross(z);
        x = x.normalized();

        Vector3 y = z.cross(x);
        y = y.normalized();

        float[][] values = new float[4][4];

        values[0][0] = x.x;
        values[0][1] = x.y;
        values[0][2] = x.z;
        values[0][3] = -x.dot(eye);

        values[1][0] = y.x;
        values[1][1] = y.y;
        values[1][2] = y.z;
        values[1][3] = -y.dot(eye);

        values[2][0] = z.x;
        values[2][1] = z.y;
        values[2][2] = z.z;
        values[2][3] = -z.dot(eye);

        // Четвертая строка
        values[3][0] = 0;
        values[3][1] = 0;
        values[3][2] = 0;
        values[3][3] = 1;

        return new Matrix4(values);
    }


    public static Matrix4 perspective(
            final float fov,
            final float aspectRatio,
            final float nearPlane,
            final float farPlane) {
        float[][] values = new float[4][4];

        final float f = (float) (1.0F / Math.tan(fov * 0.5F));

        // Инициализация нулями
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                values[i][j] = 0;
            }
        }

        // Проекция x и y
        values[0][0] = f / aspectRatio;
        values[1][1] = f;

        // Проекция z в диапазон [-1, 1] при w' = z
        values[2][2] = (farPlane + nearPlane) / (farPlane - nearPlane);
        values[2][3] = 2.0F * (nearPlane * farPlane) / (nearPlane - farPlane);

        // w' = z
        values[3][2] = 1.0F;
        values[3][3] = 0.0F;

        return new Matrix4(values);
    }

    public static Vector4 multiplyMatrix4ByVector4(final Matrix4 matrix, final Vector3 vertex) {
        Vector4 v4 = new Vector4(vertex.x, vertex.y, vertex.z, 1.0f);
        return matrix.multiply(v4);
    }

    public static Vector3 multiplyMatrix4ByVector3(final Matrix4 matrix, final Vector3 vertex) {
        Vector4 vertex4 = new Vector4(vertex.x, vertex.y, vertex.z, 1.0f);
        Vector4 result4 = matrix.multiply(vertex4);
        if (Math.abs(result4.w) < 1e-7f) {
            return new Vector3(Float.NaN, Float.NaN, Float.NaN);
        }
        return new Vector3(
                result4.x / result4.w,
                result4.y / result4.w,
                result4.z / result4.w
        );
    }

    public static Point2 vertexToPoint(final Vector3 vertex, final int width, final int height) {
        return new Point2(
                (vertex.x * width * 0.5F) + width * 0.5F,
                (-vertex.y * height * 0.5F) + height * 0.5F);
    }

    public static Matrix4 createTranslationMatrix(Vector3 translation) {
        float[][] values = Matrix4.identity().m;

        values[0][3] = translation.x;
        values[1][3] = translation.y;
        values[2][3] = translation.z;

        return new Matrix4(values);
    }

    public static Matrix4 createScaleMatrix(Vector3 scale) {
        float[][] values = new float[4][4];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                values[i][j] = 0;
            }
        }

        values[0][0] = scale.x;
        values[1][1] = scale.y;
        values[2][2] = scale.z;
        values[3][3] = 1.0f;

        return new Matrix4(values);
    }

    public static Matrix4 createRotationXMatrix(float angle) {
        final float cos = (float) Math.cos(angle);
        final float sin = (float) Math.sin(angle);

        float[][] values = Matrix4.identity().m;

        values[1][1] = cos;
        values[1][2] = -sin;
        values[2][1] = sin;
        values[2][2] = cos;

        return new Matrix4(values);
    }

    public static Matrix4 createRotationYMatrix(float angle) {
        final float cos = (float) Math.cos(angle);
        final float sin = (float) Math.sin(angle);

        float[][] values = Matrix4.identity().m;

        values[0][0] = cos;
        values[0][2] = sin;
        values[2][0] = -sin;
        values[2][2] = cos;

        return new Matrix4(values);
    }

    public static Matrix4 createRotationZMatrix(float angle) {
        final float cos = (float) Math.cos(angle);
        final float sin = (float) Math.sin(angle);

        float[][] values = Matrix4.identity().m;

        values[0][0] = cos;
        values[0][1] = -sin;
        values[1][0] = sin;
        values[1][1] = cos;

        return new Matrix4(values);
    }

    public static Matrix4 createModelMatrix(Vector3 translation, Vector3 rotation, Vector3 scale) {
        final Matrix4 scaleMatrix = createScaleMatrix(scale);
        final Matrix4 rotX = createRotationXMatrix(rotation.x);
        final Matrix4 rotY = createRotationYMatrix(rotation.y);
        final Matrix4 rotZ = createRotationZMatrix(rotation.z);
        Matrix4 rotationMatrix = rotZ.multiply(rotY).multiply(rotX);
        final Matrix4 translationMatrix = createTranslationMatrix(translation);
        return translationMatrix.multiply(rotationMatrix).multiply(scaleMatrix);
    }
}