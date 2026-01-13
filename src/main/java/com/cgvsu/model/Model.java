package com.cgvsu.model;
import com.cgvsu.math.Vector2;
import com.cgvsu.math.Vector3;

import java.util.*;

public class Model {
    public ArrayList<Vector3> vertices = new ArrayList<>();
    public ArrayList<Vector2> textureVertices = new ArrayList<>();
    public ArrayList<Vector3> normals = new ArrayList<>();
    public ArrayList<Polygon> polygons = new ArrayList<>();


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

            Vector3 polygonNormal = calculatePolygonNormal(polygon);

            for (Integer vertexIndex : vertexIndices) {
                Vector3 currentNormal = vertexNormals.get(vertexIndex);
                vertexNormals.set(vertexIndex, currentNormal.add(polygonNormal));
            }
        }

        // Нормализуем все нормали вершин
        for (Vector3 normal : vertexNormals) {
            normals.add(normal.normalized());
        }

        for (Polygon polygon : polygons) {
            ArrayList<Integer> normalIndices = new ArrayList<>();
            for (Integer vertexIndex : polygon.getVertexIndices()) {
                normalIndices.add(vertexIndex);
            }
            polygon.setNormalIndices(normalIndices);
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