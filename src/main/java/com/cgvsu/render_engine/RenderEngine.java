package com.cgvsu.render_engine;

import com.cgvsu.math.*;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;
import com.cgvsu.model.PolygonSelection;
import com.cgvsu.rasterization.Rasterization;
import com.cgvsu.rasterization.ZBuffer;
import com.cgvsu.render_engine.scene.Scene;
import com.cgvsu.render_engine.scene.SceneObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.cgvsu.rasterization.Rasterization.drawLineWithDepthTestOnly;


public class RenderEngine {

    public static void render(
            final GraphicsContext graphicsContext,
            final Scene scene,
            final Texture texture,
            final RenderSettings globalSettings,
            final int width,
            final int height,
            final List<PolygonSelection> selectedPolygons) {

        // Очистка экрана
        graphicsContext.clearRect(0, 0, width, height);

        // Инициализация Z-буфера
        ZBuffer zBuffer = new ZBuffer(width, height);
        zBuffer.clear();

        for (SceneObject sceneObject : scene.getObjects()) {
            if (!sceneObject.isVisible()) {
                continue; // Пропускаем невидимые объекты
            }

            Model mesh = sceneObject.getModel();
            Color wireframeColor = sceneObject.getWireframeColor();
            Color baseColor = sceneObject.getModelColor();

            // Получаем настройки объекта
            RenderSettings objectSettings = sceneObject.getRenderSettings();

            // Если у объекта нет своих настроек, используем глобальные
            if (objectSettings == null) {
                objectSettings = globalSettings;
            }

            // Текстура объекта имеет приоритет над "глобальной" (если задана)
            final Texture objectTexture = (sceneObject.getTexture() != null) ? sceneObject.getTexture() : texture;

            // Модельная матрица из Transform объекта
            final Matrix4 modelMatrix = getModelMatrix(sceneObject);

            // ПЕРВЫЙ ПРОХОД: Отрисовка треугольников с Z-буфером
            renderTriangles(graphicsContext, scene.getActiveCamera(), mesh, objectTexture, objectSettings, baseColor,
                    modelMatrix, zBuffer, width, height);

            // ВТОРОЙ ПРОХОД: Отрисовка полигональной сетки (если нужно)
            if (objectSettings.drawWireframe) {
                renderWireframe(graphicsContext, scene.getActiveCamera(), mesh, wireframeColor, modelMatrix, zBuffer, width, height);
            }

            // ТРЕТИЙ ПРОХОД: Отрисовка выделенных полигонов
            if (selectedPolygons != null && !selectedPolygons.isEmpty()) {
                renderSelectedPolygons(graphicsContext, scene, selectedPolygons, width, height);
            }
        }
    }

    private static void renderSelectedPolygons(
            final GraphicsContext graphicsContext,
            final Scene scene,
            final List<PolygonSelection> selectedPolygons,
            final int width,
            final int height) {

        for (PolygonSelection selection : selectedPolygons) {
            SceneObject object = selection.getSceneObject();
            Polygon polygon = selection.getPolygon();

            if (!object.isVisible() || object.getModel() == null) {
                continue;
            }

            Model mesh = object.getModel();

            // Получаем вершины выделенного полигона
            List<Integer> vertexIndices = polygon.getVertexIndices();
            if (vertexIndices.size() < 3) continue;

            // Модельная матрица
            final Matrix4 modelMatrix = getModelMatrix(object);
            final Camera camera = scene.getActiveCamera();
            final Matrix4 viewMatrix = camera.getViewMatrix();
            final Matrix4 projectionMatrix = camera.getProjectionMatrix();
            final Matrix4 modelViewProjectionMatrix = projectionMatrix.multiply(viewMatrix).multiply(modelMatrix);

            // Собираем экранные координаты вершин
            List<Vector3> screenVertices = new ArrayList<>();
            for (int vertexIndex : vertexIndices) {
                Vector3 vertex = mesh.vertices.get(vertexIndex);
                Vector3 screenPos = transformVertex(vertex, modelViewProjectionMatrix, width, height);
                screenVertices.add(screenPos);
            }

            // Рисуем заполнение полигона красным цветом с прозрачностью
            fillPolygon(graphicsContext, screenVertices, Color.rgb(255, 0, 0, 0.3));

            // Рисуем контур полигона красным цветом
            drawPolygonOutline(graphicsContext, screenVertices, Color.RED);
        }
    }

    private static void fillPolygon(GraphicsContext gc, List<Vector3> vertices, Color color) {
        if (vertices.size() < 3) return;

        gc.setFill(color);
        gc.beginPath();

        Vector3 first = vertices.get(0);
        gc.moveTo(first.x, first.y);

        for (int i = 1; i < vertices.size(); i++) {
            Vector3 v = vertices.get(i);
            gc.lineTo(v.x, v.y);
        }

        gc.closePath();
        gc.fill();
    }

    private static void drawPolygonOutline(GraphicsContext gc, List<Vector3> vertices, Color color) {
        if (vertices.size() < 2) return;

        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.beginPath();

        Vector3 first = vertices.get(0);
        gc.moveTo(first.x, first.y);

        for (int i = 1; i < vertices.size(); i++) {
            Vector3 v = vertices.get(i);
            gc.lineTo(v.x, v.y);
        }

        // Замыкаем полигон
        gc.lineTo(first.x, first.y);
        gc.stroke();
    }


    public static Matrix4 getModelMatrix(final SceneObject sceneObject) {
        if (sceneObject == null || sceneObject.getTransform() == null) {
            return GraphicConveyor.createModelMatrix(
                    new Vector3(0, 0, 0),
                    new Vector3(0, 0, 0),
                    new Vector3(1, 1, 1)
            );
        }

        final Transform t = sceneObject.getTransform();

        final Vector3 translation = (t.getTranslation() != null) ? t.getTranslation() : new Vector3(0, 0, 0);
        final Vector3 rotation = (t.getRotation() != null) ? t.getRotation() : new Vector3(0, 0, 0);
        final Vector3 scale = (t.getScale() != null) ? t.getScale() : new Vector3(1, 1, 1);

        return GraphicConveyor.createModelMatrix(translation, rotation, scale);
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
            final Color baseColor,
            final Matrix4 modelMatrix,
            final ZBuffer zBuffer,
            final int width,
            final int height) {

        // Матрицы преобразования
        final Matrix4 viewMatrix = camera.getViewMatrix();
        final Matrix4 projectionMatrix = camera.getProjectionMatrix();

        // Для векторов-столбцов: v_clip = P * V * M * v
        final Matrix4 modelViewProjectionMatrix = projectionMatrix.multiply(viewMatrix).multiply(modelMatrix);

        // Источник освещения (привязан к камере)
        Lighting.Light light = Lighting.createCameraLight(
                camera.getPosition(),
                camera.getTarget()
        );

        // Матрица вида-модели для нормалей
        final Matrix4 modelViewMatrix = viewMatrix.multiply(modelMatrix);

        final PixelWriter pixelWriter = graphicsContext.getPixelWriter();

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

            // Достаём UV (если есть)
            Vector2 tex1 = null, tex2 = null, tex3 = null;
            if (settings.useTexture && texture != null && polygon.getTextureVertexIndices().size() == 3) {
                int tIdx1 = polygon.getTextureVertexIndices().get(0);
                int tIdx2 = polygon.getTextureVertexIndices().get(1);
                int tIdx3 = polygon.getTextureVertexIndices().get(2);
                tex1 = mesh.textureVertices.get(tIdx1);
                tex2 = mesh.textureVertices.get(tIdx2);
                tex3 = mesh.textureVertices.get(tIdx3);
            }

            // Достаём и преобразуем нормали (если есть)
            Vector3 transformedN1 = null, transformedN2 = null, transformedN3 = null;
            if (settings.useLighting && polygon.getNormalIndices().size() == 3) {
                int nIdx1 = polygon.getNormalIndices().get(0);
                int nIdx2 = polygon.getNormalIndices().get(1);
                int nIdx3 = polygon.getNormalIndices().get(2);

                Vector3 n1 = mesh.normals.get(nIdx1);
                Vector3 n2 = mesh.normals.get(nIdx2);
                Vector3 n3 = mesh.normals.get(nIdx3);

                Matrix3 normalMatrix = calculateNormalMatrix(modelViewMatrix);
                transformedN1 = transformNormal(n1, normalMatrix);
                transformedN2 = transformNormal(n2, normalMatrix);
                transformedN3 = transformNormal(n3, normalMatrix);
            }

            // Проекция (screen + invW + zOverW + attrs)
            ProjectedVertex pv1 = projectVertex(v1, tex1, transformedN1, modelViewProjectionMatrix, width, height);
            ProjectedVertex pv2 = projectVertex(v2, tex2, transformedN2, modelViewProjectionMatrix, width, height);
            ProjectedVertex pv3 = projectVertex(v3, tex3, transformedN3, modelViewProjectionMatrix, width, height);

            // Отбрасываем треугольники с вершинами "на/за" камерой (без клиппинга)
            if (pv1 == null || pv2 == null || pv3 == null) {
                continue;
            }

            // Проверяем видимость треугольника (back-face culling) по экранным координатам
            if (!isTriangleVisible(pv1, pv2, pv3)) {
                continue;
            }

            // --- ОТРИСОВКА ЗАПОЛНЕНИЯ ---
            if (settings.useTexture && texture != null && tex1 != null && tex2 != null && tex3 != null) {
                if (settings.useLighting && transformedN1 != null) {
                    // Текстура + освещение (perspective correct)
                    drawTexturedTriangleWithLightingPerspectiveCorrect(
                            pixelWriter,
                            zBuffer,
                            pv1, pv2, pv3,
                            texture,
                            light
                    );
                } else {
                    // Только текстура (perspective correct)
                    Rasterization.fillTriangleTexturedPerspectiveCorrect(
                            pixelWriter,
                            zBuffer,
                            pv1.x, pv1.y, pv1.invW, pv1.zOverW, pv1.uOverW, pv1.vOverW,
                            pv2.x, pv2.y, pv2.invW, pv2.zOverW, pv2.uOverW, pv2.vOverW,
                            pv3.x, pv3.y, pv3.invW, pv3.zOverW, pv3.uOverW, pv3.vOverW,
                            texture
                    );
                }
            } else if (settings.useLighting && transformedN1 != null) {
                // Освещение без текстуры (perspective correct depth + normals)
                drawLitTriangleWithNormalInterpolationPerspectiveCorrect(
                        pixelWriter,
                        zBuffer,
                        pv1, pv2, pv3,
                        baseColor,
                        light
                );
            } else {
                // Простой треугольник (perspective correct depth)
                Rasterization.fillTrianglePerspectiveCorrect(
                        pixelWriter,
                        zBuffer,
                        pv1.x, pv1.y, pv1.invW, pv1.zOverW,
                        pv2.x, pv2.y, pv2.invW, pv2.zOverW,
                        pv3.x, pv3.y, pv3.invW, pv3.zOverW,
                        baseColor
                );
            }
        }
    }

    private static void renderWireframe(
            final GraphicsContext graphicsContext,
            final Camera camera,
            final Model mesh,
            final Color wireframeColor,
            final Matrix4 modelMatrix,
            final ZBuffer zBuffer,
            final int width,
            final int height) {

        // Матрицы преобразования
        final Matrix4 viewMatrix = camera.getViewMatrix();
        final Matrix4 projectionMatrix = camera.getProjectionMatrix();

        // Для векторов-столбцов: v_clip = P * V * M * v
        final Matrix4 modelViewProjectionMatrix = projectionMatrix.multiply(viewMatrix).multiply(modelMatrix);

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


            float zOffset = -0.0001f;
            Vector3 v1Offset = new Vector3(v1.x, v1.y, v1.z + zOffset);
            Vector3 v2Offset = new Vector3(v2.x, v2.y, v2.z + zOffset);

            drawLineWithDepthTestOnly(pixelWriter, zBuffer, v1Offset, v2Offset, wireframeColor);

        }
    }


    public static ProjectedVertex projectVertex(
            Vector3 vertex,
            Vector2 texCoord,        // может быть null
            Vector3 normal,          // может быть null
            Matrix4 mvp,
            int width, int height
    ) {
        Vector4 clip = GraphicConveyor.multiplyMatrix4ByVector4(mvp, vertex);

        // ВАЖНО: отбрасываем точки "на/за камерой" (w <= 0 в вашей матрице перспективы: w' = z)
        if (!Float.isFinite(clip.w) || clip.w <= 1e-7f) {
            return null;
        }

        double invW = 1.0 / clip.w;

        double ndcX = clip.x * invW;
        double ndcY = clip.y * invW;
        double zOverW = clip.z * invW; // ndcZ

        float screenX = (float)((ndcX + 1.0) * width * 0.5);
        float screenY = (float)((1.0 - ndcY) * height * 0.5);

        double uOverW = 0.0, vOverW = 0.0;
        if (texCoord != null) {
            uOverW = texCoord.x * invW;
            vOverW = texCoord.y * invW;
        }

        double nxOverW = 0.0, nyOverW = 0.0, nzOverW = 0.0;
        if (normal != null) {
            nxOverW = normal.x * invW;
            nyOverW = normal.y * invW;
            nzOverW = normal.z * invW;
        }

        return new ProjectedVertex(screenX, screenY, invW, zOverW, uOverW, vOverW, nxOverW, nyOverW, nzOverW);
    }


    public static Vector3 transformVertex(final Vector3 vertex, final Matrix4 mvp, final int width, final int height) {
        Vector3 ndc = GraphicConveyor.multiplyMatrix4ByVector3(mvp, vertex);
        if (!Float.isFinite(ndc.x) || !Float.isFinite(ndc.y) || !Float.isFinite(ndc.z)) {
            return new Vector3(Float.NaN, Float.NaN, Float.NaN);
        }
        Point2 p = GraphicConveyor.vertexToPoint(ndc, width, height);
        return new Vector3(p.x, p.y, ndc.z);
    }



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
    private static boolean isTriangleVisible(ProjectedVertex v1, ProjectedVertex v2, ProjectedVertex v3) {
        // Векторы сторон треугольника в экранных координатах
        float e1x = v2.x - v1.x;
        float e1y = v2.y - v1.y;
        float e2x = v3.x - v1.x;
        float e2y = v3.y - v1.y;

        // Знак псевдоскалярного произведения (2D cross) определяет ориентацию
        float cross = e1x * e2y - e1y * e2x;

        // Для вашей системы координат (y вниз), условие может быть инвертировано.
        // Здесь оставляем тот же смысл, что и раньше: "невидимые" отсекаем.
        return cross <= 0;
    }

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
    // RenderEngine.java (или Rasterization — как удобнее, но у вас сейчас в RenderEngine)
    private static void drawTexturedTriangleWithLightingPerspectiveCorrect(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            ProjectedVertex v1, ProjectedVertex v2, ProjectedVertex v3,
            Texture texture,
            Lighting.Light light
    ) {
        if (texture == null || !texture.isValid()) return;

        int minX = (int) Math.max(0, Math.floor(Math.min(v1.x, Math.min(v2.x, v3.x))));
        int maxX = (int) Math.min(zBuffer.getWidth() - 1, Math.ceil(Math.max(v1.x, Math.max(v2.x, v3.x))));
        int minY = (int) Math.max(0, Math.floor(Math.min(v1.y, Math.min(v2.y, v3.y))));
        int maxY = (int) Math.min(zBuffer.getHeight() - 1, Math.ceil(Math.max(v1.y, Math.max(v2.y, v3.y))));

        double det = (v1.x - v3.x) * (v2.y - v3.y) - (v2.x - v3.x) * (v1.y - v3.y);
        if (Math.abs(det) < 1e-10) return;
        double invDet = 1.0 / det;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {

                double l1 = ((x - v3.x) * (v2.y - v3.y) - (v2.x - v3.x) * (y - v3.y)) * invDet;
                double l2 = ((v1.x - v3.x) * (y - v3.y) - (x - v3.x) * (v1.y - v3.y)) * invDet;
                double l3 = 1 - l1 - l2;

                if (l1 >= 0 && l2 >= 0 && l3 >= 0) {

                    double invW = l1 * v1.invW + l2 * v2.invW + l3 * v3.invW;
                    if (invW <= 1e-12 || !Double.isFinite(invW)) continue;

                    double z = (l1 * v1.zOverW + l2 * v2.zOverW + l3 * v3.zOverW) / invW;

                    if (z < zBuffer.get(x, y)) {
                        double u = (l1 * v1.uOverW + l2 * v2.uOverW + l3 * v3.uOverW) / invW;
                        double v = (l1 * v1.vOverW + l2 * v2.vOverW + l3 * v3.vOverW) / invW;

                        Color texColor = texture.getColor(u, v);

                        // Нормаль перспективно-корректно:
                        double nx = (l1 * v1.nxOverW + l2 * v2.nxOverW + l3 * v3.nxOverW) / invW;
                        double ny = (l1 * v1.nyOverW + l2 * v2.nyOverW + l3 * v3.nyOverW) / invW;
                        double nz = (l1 * v1.nzOverW + l2 * v2.nzOverW + l3 * v3.nzOverW) / invW;

                        Vector3 normal = new Vector3((float)nx, (float)ny, (float)nz).normalized();

                        Color finalColor = Lighting.applySmoothLighting(texColor, normal, light);

                        pixelWriter.setColor(x, y, finalColor);
                        zBuffer.set(x, y, z);
                    }
                }
            }
        }
    }

    /**
     * Освещённый треугольник без текстуры:
     *  - персп.-корректная глубина (z)
     *  - персп.-корректная интерполяция нормали (через nOverW и invW)
     */
    private static void drawLitTriangleWithNormalInterpolationPerspectiveCorrect(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            ProjectedVertex v1, ProjectedVertex v2, ProjectedVertex v3,
            Color baseColor,
            Lighting.Light light
    ) {
        int minX = (int) Math.max(0, Math.floor(Math.min(v1.x, Math.min(v2.x, v3.x))));
        int maxX = (int) Math.min(zBuffer.getWidth() - 1, Math.ceil(Math.max(v1.x, Math.max(v2.x, v3.x))));
        int minY = (int) Math.max(0, Math.floor(Math.min(v1.y, Math.min(v2.y, v3.y))));
        int maxY = (int) Math.min(zBuffer.getHeight() - 1, Math.ceil(Math.max(v1.y, Math.max(v2.y, v3.y))));

        double det = (v1.x - v3.x) * (v2.y - v3.y) - (v2.x - v3.x) * (v1.y - v3.y);
        if (Math.abs(det) < 1e-10) return;
        double invDet = 1.0 / det;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {

                double l1 = ((x - v3.x) * (v2.y - v3.y) - (v2.x - v3.x) * (y - v3.y)) * invDet;
                double l2 = ((v1.x - v3.x) * (y - v3.y) - (x - v3.x) * (v1.y - v3.y)) * invDet;
                double l3 = 1 - l1 - l2;

                if (l1 >= 0 && l2 >= 0 && l3 >= 0) {

                    double invW = l1 * v1.invW + l2 * v2.invW + l3 * v3.invW;
                    if (invW <= 1e-12 || !Double.isFinite(invW)) continue;

                    double z = (l1 * v1.zOverW + l2 * v2.zOverW + l3 * v3.zOverW) / invW;
                    if (!Double.isFinite(z)) continue;

                    if (z < zBuffer.get(x, y)) {
                        double nx = (l1 * v1.nxOverW + l2 * v2.nxOverW + l3 * v3.nxOverW) / invW;
                        double ny = (l1 * v1.nyOverW + l2 * v2.nyOverW + l3 * v3.nyOverW) / invW;
                        double nz = (l1 * v1.nzOverW + l2 * v2.nzOverW + l3 * v3.nzOverW) / invW;

                        Vector3 normal = new Vector3((float) nx, (float) ny, (float) nz).normalized();
                        Color finalColor = Lighting.applySmoothLighting(baseColor, normal, light);

                        pixelWriter.setColor(x, y, finalColor);
                        zBuffer.set(x, y, z);
                    }
                }
            }
        }
    }


    public static class ProjectedVertex {
        public final float x;      // screen
        public final float y;      // screen
        public final double invW;  // 1 / clip.w
        public final double zOverW; // clip.z / clip.w
        // Для текстур:
        public final double uOverW;
        public final double vOverW;
        // Для нормалей (опционально, но рекомендую для освещения):
        public final double nxOverW, nyOverW, nzOverW;

        public ProjectedVertex(
                float x, float y,
                double invW, double zOverW,
                double uOverW, double vOverW,
                double nxOverW, double nyOverW, double nzOverW
        ) {
            this.x = x; this.y = y;
            this.invW = invW;
            this.zOverW = zOverW;
            this.uOverW = uOverW; this.vOverW = vOverW;
            this.nxOverW = nxOverW; this.nyOverW = nyOverW; this.nzOverW = nzOverW;
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