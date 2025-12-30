package src.main.java.com.cgvsu.triangulation;

import src.main.java.com.cgvsu.model.Model;
import src.main.java.com.cgvsu.model.Polygon;
import src.main.java.com.cgvsu.model.TriangulatedModel;

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