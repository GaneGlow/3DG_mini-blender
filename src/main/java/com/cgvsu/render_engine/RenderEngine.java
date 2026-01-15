package com.cgvsu.render_engine;

import com.cgvsu.math.*;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;
import com.cgvsu.rasterization.Rasterization;
import com.cgvsu.rasterization.ZBuffer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;

import static com.cgvsu.rasterization.Rasterization.drawLineWithZBuffer;

public class RenderEngine {

    // Z-буфер для треугольников (сохраняем между проходами)
    private static ZBuffer triangleZBuffer;

    public static void render(
            final GraphicsContext graphicsContext,
            final Camera camera,
            final List<SceneObject> sceneObjects,
            final Texture texture,
            final RenderSettings globalSettings,
            final int width,
            final int height) {

        // Очистка экрана
        graphicsContext.clearRect(0, 0, width, height);

        graphicsContext.setStroke(Color.WHITE);
        graphicsContext.setLineWidth(1.0);

        // ПЕРВЫЙ ПРОХОД: Отрисовка треугольников с Z-буфером
        renderTriangles(graphicsContext, camera, mesh, texture, settings, width, height);

        // ВТОРОЙ ПРОХОД: Отрисовка полигональной сетки (если нужно)
        if (settings.drawWireframe) {
            renderWireframe(graphicsContext, camera, mesh, width, height);
        }
    }

    /**
     * Рендерит только треугольники (заполнение)
     */
    private static void renderTriangles(
            final GraphicsContext graphicsContext,
            final Camera camera,
            final Model mesh,
            final Texture texture,
            final RenderSettings settings,
            final int width,
            final int height) {

        // Инициализация Z-буфера для треугольников
        triangleZBuffer = new ZBuffer(width, height);
        triangleZBuffer.clear();

        for (SceneObject sceneObject : sceneObjects) {
            if (!sceneObject.isVisible()) {
                continue; // Пропускаем невидимые объекты
            }

            Model mesh = sceneObject.getModel();
            //Texture texture = sceneObject.getTexture();

            Color wireframeColor = sceneObject.getWireframeColor();
            Color baseColor = sceneObject.getModelColor();

            // Получаем настройки объекта
            RenderSettings objectSettings = sceneObject.getRenderSettings();

            // Если у объекта нет своих настроек, используем глобальные
            if (objectSettings == null) {
                objectSettings = globalSettings;
            }

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
                if (objectSettings.useTexture && texture != null &&
                        polygon.getTextureVertexIndices().size() == 3) {

                    // ТЕКСТУРИРОВАННЫЙ ТРЕУГОЛЬНИК
                    int tIdx1 = polygon.getTextureVertexIndices().get(0);
                    int tIdx2 = polygon.getTextureVertexIndices().get(1);
                    int tIdx3 = polygon.getTextureVertexIndices().get(2);

                    Vector2 tex1 = mesh.textureVertices.get(tIdx1);
                    Vector2 tex2 = mesh.textureVertices.get(tIdx2);
                    Vector2 tex3 = mesh.textureVertices.get(tIdx3);

                    if (objectSettings.useLighting && polygon.getNormalIndices().size() == 3) {
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
                            triangleZBuffer,
                            projV1, projV2, projV3,
                            tex1, tex2, tex3,
                            transformedN1, transformedN2, transformedN3,
                            texture, light
                    );
                } else {
                    // Только текстура
                    Rasterization.fillTriangleTextured(
                            graphicsContext.getPixelWriter(),
                            triangleZBuffer,
                            projV1.x, projV1.y, projV1.z, tex1.x, tex1.y,
                            projV2.x, projV2.y, projV2.z, tex2.x, tex2.y,
                            projV3.x, projV3.y, projV3.z, tex3.x, tex3.y,
                            texture
                    );
                }
            }
            else if (settings.useLighting && polygon.getNormalIndices().size() == 3) {
                // ОСВЕЩЕННЫЙ ТРЕУГОЛЬНИК (без текстуры) с интерполяцией нормалей
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

                // Рисуем треугольник с интерполяцией нормалей
                drawLitTriangleWithNormalInterpolation(
                        graphicsContext,
                        triangleZBuffer,
                        projV1, projV2, projV3,
                        transformedN1, transformedN2, transformedN3,
                        settings.baseColor,
                        light
                );
            }
            else {
                // ПРОСТОЙ ТРЕУГОЛЬНИК (без текстуры и освещения)
                Rasterization.fillTriangle(
                        graphicsContext,
                        triangleZBuffer,
                        projV1.x, projV1.y, projV1.z,
                        projV2.x, projV2.y, projV2.z,
                        projV3.x, projV3.y, projV3.z,
                        settings.baseColor
                );
            }
        }
    }

    /**
     * Рендерит только полигональную сетку
     */
    private static void renderWireframe(
            final GraphicsContext graphicsContext,
            final Camera camera,
            final Model mesh,
            final int width,
            final int height) {

        // Используем отдельный Z-буфер для линий
        ZBuffer lineZBuffer = new ZBuffer(width, height);
        lineZBuffer.clear();

        // Матрицы преобразования
        Matrix4 modelMatrix = GraphicConveyor.rotateScaleTranslate();
        Matrix4 viewMatrix = camera.getViewMatrix();
        Matrix4 projectionMatrix = camera.getProjectionMatrix();
        Matrix4 modelViewProjectionMatrix = modelMatrix.multiply(viewMatrix).multiply(projectionMatrix);

        // Множество для хранения уникальных ВИДИМЫХ ребер
        Set<Edge> visibleEdges = new HashSet<>();

        // Собираем уникальные ребра ВИДИМЫХ треугольников
        for (Polygon polygon : mesh.polygons) {
            if (polygon.getVertexIndices().size() != 3) continue;

            int vIdx1 = polygon.getVertexIndices().get(0);
            int vIdx2 = polygon.getVertexIndices().get(1);
            int vIdx3 = polygon.getVertexIndices().get(2);

            Vector3 v1 = mesh.vertices.get(vIdx1);
            Vector3 v2 = mesh.vertices.get(vIdx2);
            Vector3 v3 = mesh.vertices.get(vIdx3);

            Vector3 projV1 = transformVertex(v1, modelViewProjectionMatrix, width, height);
            Vector3 projV2 = transformVertex(v2, modelViewProjectionMatrix, width, height);
            Vector3 projV3 = transformVertex(v3, modelViewProjectionMatrix, width, height);

            // Проверяем видимость треугольника (back-face culling)
            if (!isTriangleVisible(projV1, projV2, projV3)) {
                continue; // Пропускаем невидимые треугольники
            }

            // Добавляем ребра ВИДИМОГО треугольника
            visibleEdges.add(new Edge(projV1, projV2));
            visibleEdges.add(new Edge(projV2, projV3));
            visibleEdges.add(new Edge(projV3, projV1));
        }

        // Рисуем только видимые ребра
        PixelWriter pixelWriter = graphicsContext.getPixelWriter();
        for (Edge edge : visibleEdges) {
            Vector3 v1 = edge.v1;
            Vector3 v2 = edge.v2;

            // Небольшое смещение по Z, чтобы линии были поверх треугольников
            // Отрицательное значение, так как в Z-буфере меньшие значения ближе
            float zOffset = -0.0001f;
            Vector3 v1Offset = new Vector3(v1.x, v1.y, v1.z + zOffset);
            Vector3 v2Offset = new Vector3(v2.x, v2.y, v2.z + zOffset);

            drawLineWithZBuffer(pixelWriter, lineZBuffer, v1Offset, v2Offset, Color.BLACK);
        }
    }

    public static void render(
            final GraphicsContext graphicsContext,
            final Camera camera,
            final Model mesh,
            final Texture texture,
            final RenderSettings settings,
            final int width,
            final int height) {

        // Создаем временный список с одним объектом
        List<SceneObject> singleObjectList = new ArrayList<>();
        SceneObject singleObject = new SceneObject("SingleObject", mesh, texture);
        singleObject.setWireframeColor(Color.WHITE);
        singleObject.setModelColor(settings.baseColor);
        singleObjectList.add(singleObject);

        // Вызываем новую версию метода
        render(graphicsContext, camera, singleObjectList, texture, settings, width, height);
        //render(graphicsContext, camera, singleObjectList, settings, width, height);
    }

        // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =============

        /**
         * Преобразует вершину из мировых в экранные координаты
         */
        private static Vector3 transformVertex (Vector3 vertex, Matrix4 modelViewProjectionMatrix,
        int width, int height){
            // Применяем матрицу преобразования
            Vector3 transformed = GraphicConveyor.multiplyMatrix4ByVector3(
                    modelViewProjectionMatrix, vertex
            );

        // Преобразуем в экранные координаты
        float screenX = (transformed.x + 1.0f) * width / 2.0f;
        float screenY = (1.0f - transformed.y) * height / 2.0f;
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
                        double vCoord = lambda1 * tex1.y + lambda2 * tex2.y + lambda3 * tex3.y;

                        // Получаем цвет текстуры
                        Color texColor = texture.getColor(u, vCoord);

                        // Интерполируем нормаль
                        Vector3 normal = Lighting.interpolateNormal(
                                n1, n2, n3,
                                (float)lambda1, (float)lambda2, (float)lambda3
                        );

                        // Применяем освещение
                        Color finalColor = Lighting.applySmoothLighting(texColor, normal, light);

                        // Рисуем пиксель
                        pixelWriter.setColor(x, y, finalColor);
                        zBuffer.set(x, y, z);
                    }
                }
            }
        }
    }

    /**
     * Рисует треугольник с интерполяцией нормалей для сглаженного освещения
     */
    private static void drawLitTriangleWithNormalInterpolation(
            GraphicsContext graphicsContext,
            ZBuffer zBuffer,
            Vector3 v1, Vector3 v2, Vector3 v3,
            Vector3 n1, Vector3 n2, Vector3 n3,
            Color baseColor,
            Lighting.Light light) {

        PixelWriter pixelWriter = graphicsContext.getPixelWriter();

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

                        // Интерполируем нормаль
                        Vector3 normal = Lighting.interpolateNormal(
                                n1, n2, n3,
                                (float)lambda1, (float)lambda2, (float)lambda3
                        );

                        // Применяем освещение
                        Color finalColor = Lighting.applySmoothLighting(baseColor, normal, light);

                        // Рисуем пиксель
                        pixelWriter.setColor(x, y, finalColor);
                        zBuffer.set(x, y, z);
                    }
                }
            }
        }
    }

    /**
     * Класс для хранения уникальных ребер
     */
    private static class Edge {
        public final Vector3 v1;
        public final Vector3 v2;

        public Edge(Vector3 v1, Vector3 v2) {
            // Упорядочиваем вершины по хэш-коду для обеспечения уникальности
            // (A,B) и (B,A) должны считаться одинаковыми
            if (v1.hashCode() < v2.hashCode()) {
                this.v1 = v1;
                this.v2 = v2;
            } else {
                this.v1 = v2;
                this.v2 = v1;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            Edge other = (Edge) obj;
            return v1.equals(other.v1) && v2.equals(other.v2);
        }

        @Override
        public int hashCode() {
            // Симметричный хэш-код для (A,B) и (B,A)
            return v1.hashCode() ^ v2.hashCode();
        }
    }
}