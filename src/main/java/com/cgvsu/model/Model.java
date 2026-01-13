package com.cgvsu.model;
import com.cgvsu.math.Vector2;
import com.cgvsu.math.Vector3;

import java.util.*;

public class Model {
    public ArrayList<Vector3> vertices = new ArrayList<>();
    public ArrayList<Vector2> textureVertices = new ArrayList<>();
    public ArrayList<Vector3> normals = new ArrayList<>();
    public ArrayList<Polygon> polygons = new ArrayList<>();

    public ArrayList<Vector3> getVertices() {
        return vertices;
    }

    public void setVertices(ArrayList<Vector3> vertices) {
        this.vertices = vertices;
    }

    public ArrayList<Vector2> getTextureVertices() {
        return textureVertices;
    }

    public void setTextureVertices(ArrayList<Vector2> textureVertices) {
        this.textureVertices = textureVertices;
    }

    public ArrayList<Vector3> getNormals() {
        return normals;
    }

    public void setNormals(ArrayList<Vector3> normals) {
        this.normals = normals;
    }

    public ArrayList<Polygon> getPolygons() {
        return polygons;
    }

    public void setPolygons(ArrayList<Polygon> polygons) {
        this.polygons = polygons;
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

    /// Методы для удаления полигонов, вершин, нормалей и текстурных вершин

    public void removePolygons(ArrayList<Integer> polygonIndices, ArrayList<Vector3> vertices,
                               ArrayList<Vector2> textureVertices, ArrayList<Vector3> normals,
                               ArrayList<Polygon> polygons, boolean removeVertices) {
        boolean[] checkIdxVertices = new boolean[vertices.size()];
        boolean[] checkIdxTexture = new boolean[textureVertices.size()];
        boolean[] checkIdxNormals = new boolean[normals.size()];
        Arrays.fill(checkIdxVertices, true);
        Arrays.fill(checkIdxTexture, true);
        Arrays.fill(checkIdxNormals, true);
        ArrayList<Polygon> polygonResult = new ArrayList<>();

        for (int i = 0; i < polygons.size(); i++) {
            Polygon original = polygons.get(i);
            if (!polygonIndices.contains(i + 1)) {
                for (int idx : original.getVertexIndices()) {
                    checkIdxVertices[idx] = false;
                }
                for (int idx : original.getTextureVertexIndices()) {
                    checkIdxTexture[idx] = false;
                }
                for (int idx : original.getNormalIndices()) {
                    checkIdxNormals[idx] = false;
                }
                polygonResult.add(original);
            }
        }


        if (removeVertices) {
            ArrayList<Integer> offsetVertices = createRemoveListVertices(checkIdxVertices);
            ArrayList<Integer> offsetTexture = createRemoveListTexture(checkIdxTexture);
            ArrayList<Integer> offsetNormals = createRemoveListNormals(checkIdxNormals);
            for (Polygon polygon : polygonResult) {
                ArrayList<Integer> newVertexIndexes = new ArrayList<>();
                ArrayList<Integer> newTextureIndexes = new ArrayList<>();
                ArrayList<Integer> newNormalsIndexes = new ArrayList<>();
                for (int vertexIdx : polygon.getVertexIndices()) {
                    int c = offsetVertices.get(vertexIdx);
                    int idx = vertexIdx + c;
                    newVertexIndexes.add(idx);
                }
                for (int vertexIdx : polygon.getTextureVertexIndices()) {
                    int c = offsetTexture.get(vertexIdx);
                    int idx = vertexIdx + c;
                    newTextureIndexes.add(idx);
                }
                for (int vertexIdx : polygon.getNormalIndices()) {
                    int c = offsetNormals.get(vertexIdx);
                    int idx = vertexIdx + c;
                    newNormalsIndexes.add(idx);
                }
                polygon.setVertexIndices(newVertexIndexes);
                polygon.setTextureVertexIndices(newTextureIndexes);
                polygon.setNormalIndices(newNormalsIndexes);
            }
        }
        setPolygons(polygonResult);
    }

    // удаление вершин
    private ArrayList<Integer> createRemoveListVertices(boolean[] checkIdx) {
        ArrayList<Integer> offset = new ArrayList<>();
        ArrayList<Vector3> newVertices = new ArrayList<>();
        int c = 0;
        for (int i = 0; i < checkIdx.length; i++) {
            boolean value = checkIdx[i];
            if (value) {
                c--;
            } else {
                newVertices.add(vertices.get(i));
            }
            offset.add(c);
        }
        setVertices(newVertices);
        return offset;
    }

    // удаление текстурных вершин
    private ArrayList<Integer> createRemoveListTexture(boolean[] checkIdx) {
        ArrayList<Integer> offset = new ArrayList<>();
        ArrayList<Vector2> newTexture = new ArrayList<>();
        int c = 0;
        for (int i = 0; i < checkIdx.length; i++) {
            boolean value = checkIdx[i];
            if (value) {
                c--;
            } else {
                newTexture.add(textureVertices.get(i));
            }
            offset.add(c);
        }
        setTextureVertices(newTexture);
        return offset;
    }

    // удаление нормалей
    private ArrayList<Integer> createRemoveListNormals(boolean[] checkIdx) {
        ArrayList<Integer> offset = new ArrayList<>();
        ArrayList<Vector3> newNormals = new ArrayList<>();
        int c = 0;
        for (int i = 0; i < checkIdx.length; i++) {
            boolean value = checkIdx[i];
            if (value) {
                c--;
            } else {
                newNormals.add(normals.get(i));
            }
            offset.add(c);
        }
        setNormals(newNormals);
        return offset;
    }
}