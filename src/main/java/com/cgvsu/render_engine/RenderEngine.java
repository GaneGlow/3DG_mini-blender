package com.cgvsu.render_engine;

import java.util.ArrayList;

import com.cgvsu.math.Matrix4;
import com.cgvsu.math.Point2;
import com.cgvsu.math.Vector3;
import com.cgvsu.rasterization.Rasterization;
import javafx.scene.canvas.GraphicsContext;
import com.cgvsu.model.Model;
import javafx.scene.paint.Color;

import static com.cgvsu.render_engine.GraphicConveyor.*;

public class RenderEngine {

    public static void render(
            final GraphicsContext graphicsContext,
            final Camera camera,
            final Model mesh,
            final int width,
            final int height)
    {
        Matrix4 modelMatrix = rotateScaleTranslate();
        Matrix4 viewMatrix = camera.getViewMatrix();
        Matrix4 projectionMatrix = camera.getProjectionMatrix();

        Matrix4 modelViewProjectionMatrix = modelMatrix.multiply(viewMatrix).multiply(projectionMatrix);

        final int nPolygons = mesh.polygons.size();

        // Шаг 1: Заливаем все треугольники серым цветом
        for (int polygonInd = 0; polygonInd < nPolygons; ++polygonInd) {
            final int nVerticesInPolygon = mesh.polygons.get(polygonInd).getVertexIndices().size();

            // Так как модель триангулирована, каждый полигон - треугольник
            if (nVerticesInPolygon == 3) {
                // Получаем вершины треугольника
                Vector3 vertex1 = mesh.vertices.get(mesh.polygons.get(polygonInd).getVertexIndices().get(0));
                Vector3 vertex2 = mesh.vertices.get(mesh.polygons.get(polygonInd).getVertexIndices().get(1));
                Vector3 vertex3 = mesh.vertices.get(mesh.polygons.get(polygonInd).getVertexIndices().get(2));

                // Преобразуем вершины в экранные координаты
                Point2 point1 = vertexToPoint(multiplyMatrix4ByVector3(modelViewProjectionMatrix, vertex1), width, height);
                Point2 point2 = vertexToPoint(multiplyMatrix4ByVector3(modelViewProjectionMatrix, vertex2), width, height);
                Point2 point3 = vertexToPoint(multiplyMatrix4ByVector3(modelViewProjectionMatrix, vertex3), width, height);

                // Заполняем треугольник серым цветом
                Rasterization.fillTriangle(
                        graphicsContext,
                        point1.x, point1.y,
                        point2.x, point2.y,
                        point3.x, point3.y,
                        Color.GRAY
                );
            }
        }

        /*Шаг 2: Рисуем полигональную сетку черным цветом поверх заливки
        graphicsContext.setStroke(Color.BLACK);
        graphicsContext.setLineWidth(1.0);

        for (int polygonInd = 0; polygonInd < nPolygons; ++polygonInd) {
            final int nVerticesInPolygon = mesh.polygons.get(polygonInd).getVertexIndices().size();

            ArrayList<Point2> resultPoints = new ArrayList<>();
            for (int vertexInPolygonInd = 0; vertexInPolygonInd < nVerticesInPolygon; ++vertexInPolygonInd) {
                Vector3 vertex = mesh.vertices.get(mesh.polygons.get(polygonInd).getVertexIndices().get(vertexInPolygonInd));

                Point2 resultPoint = vertexToPoint(multiplyMatrix4ByVector3(modelViewProjectionMatrix, vertex), width, height);
                resultPoints.add(resultPoint);
            }

            // Рисуем линии треугольника
            for (int vertexInPolygonInd = 1; vertexInPolygonInd < nVerticesInPolygon; ++vertexInPolygonInd) {
                graphicsContext.strokeLine(
                        resultPoints.get(vertexInPolygonInd - 1).x,
                        resultPoints.get(vertexInPolygonInd - 1).y,
                        resultPoints.get(vertexInPolygonInd).x,
                        resultPoints.get(vertexInPolygonInd).y);
            }

            // Замыкаем треугольник (последняя вершина -> первая)
            if (nVerticesInPolygon > 0) {
                graphicsContext.strokeLine(
                        resultPoints.get(nVerticesInPolygon - 1).x,
                        resultPoints.get(nVerticesInPolygon - 1).y,
                        resultPoints.get(0).x,
                        resultPoints.get(0).y);
            }
        }*/
    }
}