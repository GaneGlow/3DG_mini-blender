package com.cgvsu.render_engine;

import com.cgvsu.math.*;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;
import com.cgvsu.rasterization.Rasterization;
import com.cgvsu.rasterization.ZBuffer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

import static com.cgvsu.rasterization.Rasterization.drawLineWithZBuffer;

public class RenderEngine {

    public static void render(
            final GraphicsContext graphicsContext,
            final Camera camera,
            final Model mesh,
            final Texture texture,
            final RenderSettings settings,
            final int width,
            final int height) {

        // Очистка экрана
        graphicsContext.clearRect(0, 0, width, height);

        graphicsContext.setStroke(Color.WHITE);
        graphicsContext.setLineWidth(1.0);

        // Инициализация Z-буфера
        ZBuffer zBuffer = new ZBuffer(width, height);
        zBuffer.clear();

        // Матрицы преобразования
        Matrix4 modelMatrix = GraphicConveyor.rotateScaleTranslate();
        Matrix4 viewMatrix = camera.getViewMatrix();
        Matrix4 projectionMatrix = camera.getProjectionMatrix();

        Matrix4 modelViewProjectionMatrix = modelMatrix.multiply(viewMatrix).multiply(projectionMatrix);

        // Источник освещения (привязан к камере)
        Lighting.Light light = Lighting.createCameraLight(
                camera.getPosition(),
                camera.getTarget()
        );

        // Матрица вида-модели для нормалей
        Matrix4 modelViewMatrix = modelMatrix.multiply(viewMatrix);

        // Проходим по всем полигонам (треугольникам)
        for (Polygon polygon : mesh.polygons) {
            if (polygon.getVertexIndices().size() != 3) {
                continue; // Пропускаем не-треугольники
            }

            // Индексы вершин треугольника
            int vIdx1 = polygon.getVertexIndices().get(0);
            int vIdx2 = polygon.getVertexIndices().get(1);
            int vIdx3 = polygon.getVertexIndices().get(2);

            // Координаты вершин
            Vector3 v1 = mesh.vertices.get(vIdx1);
            Vector3 v2 = mesh.vertices.get(vIdx2);
            Vector3 v3 = mesh.vertices.get(vIdx3);

            // Преобразуем вершины в экранные координаты
            Vector3 projV1 = transformVertex(v1, modelViewProjectionMatrix, width, height);
            Vector3 projV2 = transformVertex(v2, modelViewProjectionMatrix, width, height);
            Vector3 projV3 = transformVertex(v3, modelViewProjectionMatrix, width, height);

            // Проверяем видимость треугольника (back-face culling)
            if (!isTriangleVisible(projV1, projV2, projV3)) {
                continue;
            }

            // --- ОТРИСОВКА ЗАПОЛНЕНИЯ ---
            if (settings.useTexture && texture != null &&
                    polygon.getTextureVertexIndices().size() == 3) {

                // ТЕКСТУРИРОВАННЫЙ ТРЕУГОЛЬНИК
                int tIdx1 = polygon.getTextureVertexIndices().get(0);
                int tIdx2 = polygon.getTextureVertexIndices().get(1);
                int tIdx3 = polygon.getTextureVertexIndices().get(2);

                Vector2 tex1 = mesh.textureVertices.get(tIdx1);
                Vector2 tex2 = mesh.textureVertices.get(tIdx2);
                Vector2 tex3 = mesh.textureVertices.get(tIdx3);

                if (settings.useLighting && polygon.getNormalIndices().size() == 3) {
                    // Текстура + освещение
                    int nIdx1 = polygon.getNormalIndices().get(0);
                    int nIdx2 = polygon.getNormalIndices().get(1);
                    int nIdx3 = polygon.getNormalIndices().get(2);

                    Vector3 n1 = mesh.normals.get(nIdx1);
                    Vector3 n2 = mesh.normals.get(nIdx2);
                    Vector3 n3 = mesh.normals.get(nIdx3);

                    // Преобразуем нормали
                    Matrix3 normalMatrix = calculateNormalMatrix(modelViewMatrix);
                    Vector3 transformedN1 = transformNormal(n1, normalMatrix);
                    Vector3 transformedN2 = transformNormal(n2, normalMatrix);
                    Vector3 transformedN3 = transformNormal(n3, normalMatrix);

                    drawTexturedTriangleWithLighting(
                            graphicsContext.getPixelWriter(),
                            zBuffer,
                            projV1, projV2, projV3,
                            tex1, tex2, tex3,
                            transformedN1, transformedN2, transformedN3,
                            texture, light
                    );
                } else {
                    // Только текстура
                    Rasterization.fillTriangleTextured(
                            graphicsContext.getPixelWriter(),
                            zBuffer,
                            projV1.x, projV1.y, projV1.z, tex1.x, tex1.y,
                            projV2.x, projV2.y, projV2.z, tex2.x, tex2.y,
                            projV3.x, projV3.y, projV3.z, tex3.x, tex3.y,
                            texture
                    );
                }
            }
            else if (settings.useLighting && polygon.getNormalIndices().size() == 3) {
                // ОСВЕЩЕННЫЙ ТРЕУГОЛЬНИК (без текстуры)
                int nIdx1 = polygon.getNormalIndices().get(0);
                int nIdx2 = polygon.getNormalIndices().get(1);
                int nIdx3 = polygon.getNormalIndices().get(2);

                Vector3 n1 = mesh.normals.get(nIdx1);
                Vector3 n2 = mesh.normals.get(nIdx2);
                Vector3 n3 = mesh.normals.get(nIdx3);

                // Преобразуем нормали
                Matrix3 normalMatrix = calculateNormalMatrix(modelViewMatrix);
                Vector3 transformedN1 = transformNormal(n1, normalMatrix);
                Vector3 transformedN2 = transformNormal(n2, normalMatrix);
                Vector3 transformedN3 = transformNormal(n3, normalMatrix);

                // Вычисляем цвета с учетом освещения
                Color c1 = Lighting.applySmoothLighting(settings.baseColor, transformedN1, light);
                Color c2 = Lighting.applySmoothLighting(settings.baseColor, transformedN2, light);
                Color c3 = Lighting.applySmoothLighting(settings.baseColor, transformedN3, light);

                Rasterization.fillTriangle(
                        graphicsContext,
                        zBuffer,
                        projV1.x, projV1.y, projV1.z,
                        projV2.x, projV2.y, projV2.z,
                        projV3.x, projV3.y, projV3.z,
                        c1, c2, c3
                );
            }
            else {
                // ПРОСТОЙ ТРЕУГОЛЬНИК (без текстуры и освещения)
                Rasterization.fillTriangle(
                        graphicsContext,
                        zBuffer,
                        projV1.x, projV1.y, projV1.z,
                        projV2.x, projV2.y, projV2.z,
                        projV3.x, projV3.y, projV3.z,
                        settings.baseColor
                );
            }

            // --- ОТРИСОВКА ПОЛИГОНАЛЬНОЙ СЕТКИ ---
            if (settings.drawWireframe) {
                drawWireframeTriangle(
                        graphicsContext.getPixelWriter(),
                        zBuffer,
                        projV1, projV2, projV3,
                        Color.BLACK
                );
            }
        }
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =============

    /**
     * Преобразует вершину из мировых в экранные координаты
     */
    private static Vector3 transformVertex(Vector3 vertex, Matrix4 modelViewProjectionMatrix,
                                           int width, int height) {
        // Применяем матрицу преобразования
        Vector3 transformed = GraphicConveyor.multiplyMatrix4ByVector3(
                modelViewProjectionMatrix, vertex
        );

        // Преобразуем в экранные координаты
        float screenX = (transformed.x + 1.0f) * width / 2.0f;
        float screenY = (1.0f - transformed.y) * height / 2.0f; // Инвертируем Y
        float screenZ = transformed.z;

        return new Vector3(screenX, screenY, screenZ);
    }

    /**
     * Вычисляет матрицу для преобразования нормалей
     */
    private static Matrix3 calculateNormalMatrix(Matrix4 modelViewMatrix) {
        // Берем верхнюю левую 3x3 подматрицу матрицы модель-вид
        float[][] m = new float[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                m[i][j] = modelViewMatrix.get(i, j);
            }
        }


        // Нормали преобразуются обратной транспонированной матрицей
        Matrix3 rotationScale = new Matrix3(m);
        return rotationScale.inverse().transpose();
    }

    /**
     * Преобразует нормаль с помощью нормальной матрицы
     */
    private static Vector3 transformNormal(Vector3 normal, Matrix3 normalMatrix) {
        return normalMatrix.multiply(normal).normalized();
    }

    /**
     * Проверяет, виден ли треугольник (back-face culling)
     */
    private static boolean isTriangleVisible(Vector3 v1, Vector3 v2, Vector3 v3) {
        // Векторы сторон треугольника
        Vector3 edge1 = v2.subtract(v1);
        Vector3 edge2 = v3.subtract(v1);

        // Нормаль треугольника (не нормализованная)
        Vector3 normal = edge1.cross(edge2);

        // Если Z-компонента нормали положительна, треугольник смотрит от нас
        return normal.z <= 0;
    }

    /**
     * Рисует текстурированный треугольник с освещением
     */
    private static void drawTexturedTriangleWithLighting(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            Vector3 v1, Vector3 v2, Vector3 v3,
            Vector2 tex1, Vector2 tex2, Vector2 tex3,
            Vector3 n1, Vector3 n2, Vector3 n3,
            Texture texture,
            Lighting.Light light) {

        // Определяем ограничивающий прямоугольник
        int minX = (int) Math.max(0, Math.floor(Math.min(v1.x, Math.min(v2.x, v3.x))));
        int maxX = (int) Math.min(zBuffer.getWidth() - 1,
                Math.ceil(Math.max(v1.x, Math.max(v2.x, v3.x))));
        int minY = (int) Math.max(0, Math.floor(Math.min(v1.y, Math.min(v2.y, v3.y))));
        int maxY = (int) Math.min(zBuffer.getHeight() - 1,
                Math.ceil(Math.max(v1.y, Math.max(v2.y, v3.y))));

        // Вычисляем константы для барицентрических координат
        double det = (v1.x - v3.x) * (v2.y - v3.y) - (v2.x - v3.x) * (v1.y - v3.y);

        if (Math.abs(det) < 1e-10) {
            return; // Вырожденный треугольник
        }

        double invDet = 1.0 / det;

        // Проходим по всем пикселям в ограничивающем прямоугольнике
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {

                // Вычисляем барицентрические координаты
                double lambda1 = ((x - v3.x) * (v2.y - v3.y) -
                        (v2.x - v3.x) * (y - v3.y)) * invDet;
                double lambda2 = ((v1.x - v3.x) * (y - v3.y) -
                        (x - v3.x) * (v1.y - v3.y)) * invDet;
                double lambda3 = 1 - lambda1 - lambda2;

                // Проверяем, находится ли точка внутри треугольника
                if (lambda1 >= 0 && lambda2 >= 0 && lambda3 >= 0) {

                    // Интерполируем глубину
                    double z = lambda1 * v1.z + lambda2 * v2.z + lambda3 * v3.z;

                    // Проверяем Z-буфер
                    if (z < zBuffer.get(x, y)) {

                        // Интерполируем текстурные координаты
                        double u = lambda1 * tex1.x + lambda2 * tex2.x + lambda3 * tex3.x;
                        double v = lambda1 * tex1.y + lambda2 * tex2.y + lambda3 * tex3.y;

                        // Получаем цвет текстуры
                        Color texColor = texture.getColor(u, v);

                        // Интерполируем нормаль
                        double nx = lambda1 * n1.x + lambda2 * n2.x + lambda3 * n3.x;
                        double ny = lambda1 * n1.y + lambda2 * n2.y + lambda3 * n3.y;
                        double nz = lambda1 * n1.z + lambda2 * n2.z + lambda3 * n3.z;

                        Vector3 normal = new Vector3((float)nx, (float)ny, (float)nz).normalized();

                        // Применяем освещение
                        Color finalColor = Lighting.applySimpleLighting(texColor, normal, light);

                        // Рисуем пиксель
                        pixelWriter.setColor(x, y, finalColor);
                        zBuffer.set(x, y, z);
                    }
                }
            }
        }
    }

    /**
     * Рисует полигональную сетку треугольника
     */
    private static void drawWireframeTriangle(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            Vector3 v1, Vector3 v2, Vector3 v3,
            Color color) {

        // Рисуем три ребра треугольника
        drawLineWithZBuffer(pixelWriter, zBuffer, v1, v2, color);
        drawLineWithZBuffer(pixelWriter, zBuffer, v2, v3, color);
        drawLineWithZBuffer(pixelWriter, zBuffer, v3, v1, color);
    }


}