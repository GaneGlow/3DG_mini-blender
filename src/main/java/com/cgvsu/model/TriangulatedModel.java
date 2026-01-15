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

}