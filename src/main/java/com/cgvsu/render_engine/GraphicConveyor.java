package src.main.java.com.cgvsu.render_engine;

import src.main.java.com.cgvsu.math.Matrix4;
import src.main.java.com.cgvsu.math.Point2;
import src.main.java.com.cgvsu.math.Vector3;

public class GraphicConveyor {
    public static Matrix4 rotateScaleTranslate() {
        float[][] matrix = new float[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
        return new Matrix4(matrix);
    }

    public static Matrix4 lookAt(Vector3 eye, Vector3 target) {
        return lookAt(eye, target, new Vector3(0.0f, 1.0f, 0.0f));
    }

    public static Matrix4 lookAt(Vector3 eye, Vector3 target, Vector3 up) {
        Vector3 zAxis = target.subtract(eye).normalized();
        Vector3 xAxis = up.cross(zAxis).normalized();
        Vector3 yAxis = zAxis.cross(xAxis);

        float[][] matrix = new float[][]{
                {xAxis.x, yAxis.x, zAxis.x, 0},
                {xAxis.y, yAxis.y, zAxis.y, 0},
                {xAxis.z, yAxis.z, zAxis.z, 0},
                {-xAxis.dot(eye), -yAxis.dot(eye), -zAxis.dot(eye), 1}
        };

        return new Matrix4(matrix);
    }

    public static Matrix4 perspective(
            final float fov,
            final float aspectRatio,
            final float nearPlane,
            final float farPlane) {
        float tanHalfFov = (float)(1.0 / Math.tan(fov * 0.5));
        float range = farPlane - nearPlane;

        float[][] matrix = new float[][]{
                {tanHalfFov / aspectRatio, 0, 0, 0},
                {0, tanHalfFov, 0, 0},
                {0, 0, -(farPlane + nearPlane) / range, -1},
                {0, 0, -2 * farPlane * nearPlane / range, 0}
        };
        return new Matrix4(matrix);
    }

    public static Vector3 multiplyMatrix4ByVector3(final Matrix4 matrix, final Vector3 vertex) {
        float x = vertex.x * matrix.get(0, 0) + vertex.y * matrix.get(1, 0) + vertex.z * matrix.get(2, 0) + matrix.get(3, 0);
        float y = vertex.x * matrix.get(0, 1) + vertex.y * matrix.get(1, 1) + vertex.z * matrix.get(2, 1) + matrix.get(3, 1);
        float z = vertex.x * matrix.get(0, 2) + vertex.y * matrix.get(1, 2) + vertex.z * matrix.get(2, 2) + matrix.get(3, 2);
        float w = vertex.x * matrix.get(0, 3) + vertex.y * matrix.get(1, 3) + vertex.z * matrix.get(2, 3) + matrix.get(3, 3);

        if (Math.abs(w) > 1e-7f) {
            return new Vector3(x / w, y / w, z / w);
        }
        return new Vector3(x, y, z);
    }

    public static Point2 vertexToPoint(final Vector3 vertex, final int width, final int height) {
        return new Point2(
                vertex.x * width / 2.0f + width / 2.0f,
                -vertex.y * height / 2.0f + height / 2.0f
        );
    }
}