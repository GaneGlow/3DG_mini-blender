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