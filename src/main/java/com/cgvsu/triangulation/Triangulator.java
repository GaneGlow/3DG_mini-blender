package com.cgvsu.triangulation;

import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;
import com.cgvsu.model.TriangulatedModel;

public class Triangulator {

    public static TriangulatedModel triangulate(Model model) {
        return new TriangulatedModel(model);
    }

    public static boolean needsTriangulation(Model model) {
        for (Polygon polygon : model.polygons) {
            if (polygon.getVertexIndices().size() > 3) {
                return true;
            }
        }
        return false;
    }
}