package com.cgvsu.model;

import java.util.ArrayList;
import java.util.List;

import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;
public class TriangulatedModel extends Model {

    public TriangulatedModel() {
        super();
    }

    public TriangulatedModel(Model originalModel) {
        super();
        this.vertices = new ArrayList<>(originalModel.vertices);
        this.textureVertices = new ArrayList<>(originalModel.textureVertices);
        this.normals = new ArrayList<>(originalModel.normals);

        this.polygons = triangulatePolygons(originalModel.polygons);
    }

    private ArrayList<Polygon> triangulatePolygons(ArrayList<Polygon> originalPolygons) {
        ArrayList<Polygon> triangulatedPolygons = new ArrayList<>();

        for (Polygon polygon : originalPolygons) {
            ArrayList<Polygon> triangles = triangulatePolygon(polygon);
            triangulatedPolygons.addAll(triangles);
        }

        return triangulatedPolygons;
    }

    private ArrayList<Polygon> triangulatePolygon(Polygon polygon) {
        ArrayList<Polygon> triangles = new ArrayList<>();
        ArrayList<Integer> vertexIndices = polygon.getVertexIndices();
        ArrayList<Integer> textureIndices = polygon.getTextureVertexIndices();
        ArrayList<Integer> normalIndices = polygon.getNormalIndices();

        int vertexCount = vertexIndices.size();

        if (vertexCount == 3) {
            triangles.add(polygon);
            return triangles;
        }
        //сделать класс работающий со списком, чтобы убрать повторение кода и выше по уровню вызывать этот метод для текстур и нормалей
        for (int i = 1; i < vertexCount - 1; i++) {
            Polygon triangle = new Polygon();
            ArrayList<Integer> triangleVertexIndices = new ArrayList<>();
            triangleVertexIndices.add(vertexIndices.get(0));
            triangleVertexIndices.add(vertexIndices.get(i));
            triangleVertexIndices.add(vertexIndices.get(i + 1));
            triangle.setVertexIndices(triangleVertexIndices);

            if (!textureIndices.isEmpty() && textureIndices.size() == vertexCount) {
                ArrayList<Integer> triangleTextureIndices = new ArrayList<>();
                triangleTextureIndices.add(textureIndices.get(0));
                triangleTextureIndices.add(textureIndices.get(i));
                triangleTextureIndices.add(textureIndices.get(i + 1));
                triangle.setTextureVertexIndices(triangleTextureIndices);
            }

            if (!normalIndices.isEmpty() && normalIndices.size() == vertexCount) {
                ArrayList<Integer> triangleNormalIndices = new ArrayList<>();
                triangleNormalIndices.add(normalIndices.get(0));
                triangleNormalIndices.add(normalIndices.get(i));
                triangleNormalIndices.add(normalIndices.get(i + 1));
                triangle.setNormalIndices(triangleNormalIndices);
            }

            triangles.add(triangle);
        }

        return triangles;
    }

    // Метод для вычисления нормалей на основе геометрии
    public void recalculateNormals() {
        normals.clear();

        // Инициализируем список нормалей для вершин
        ArrayList<Vector3> vertexNormals = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            vertexNormals.add(new Vector3(0, 0, 0));
        }

        // Вычисляем нормали для каждого полигона и добавляем к вершинам
        for (Polygon polygon : polygons) {
            List<Integer> vertexIndices = polygon.getVertexIndices();

            if (vertexIndices.size() < 3) {
                continue; // Пропускаем полигоны с менее чем 3 вершинами
            }

            // Вычисляем нормаль полигона через векторное произведение
            Vector3 polygonNormal = calculatePolygonNormal(polygon);

            // Добавляем нормаль полигона ко всем его вершинам
            for (Integer vertexIndex : vertexIndices) {
                Vector3 currentNormal = vertexNormals.get(vertexIndex);
                vertexNormals.set(vertexIndex, currentNormal.add(polygonNormal));
            }
        }

        // Нормализуем все нормали вершин
        for (Vector3 normal : vertexNormals) {
            normals.add(normal.normalized());
        }

        // Обновляем индексы нормалей в полигонах
        for (Polygon polygon : polygons) {
            ArrayList<Integer> normalIndices = new ArrayList<>();
            for (Integer vertexIndex : polygon.getVertexIndices()) {
                normalIndices.add(vertexIndex); // Теперь нормаль вершины = индекс вершины
            }
            polygon.setNormalIndices(normalIndices); // обновляем ссылки в полигонах
        }
    }

    // Вычисление нормали полигона через векторное произведение
    private Vector3 calculatePolygonNormal(Polygon polygon) {
        List<Integer> vertexIndices = polygon.getVertexIndices();

        if (vertexIndices.size() < 3) {
            return new Vector3(0, 0, 0);
        }

        // Берем первые три вершины для вычисления нормали
        Vector3 v0 = vertices.get(vertexIndices.get(0));
        Vector3 v1 = vertices.get(vertexIndices.get(1));
        Vector3 v2 = vertices.get(vertexIndices.get(2));

        // Вычисляем векторы сторон
        Vector3 edge1 = v1.subtract(v0);
        Vector3 edge2 = v2.subtract(v0);

        // Векторное произведение дает нормаль
        return edge1.cross(edge2).normalized();
    }
}