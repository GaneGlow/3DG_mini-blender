package com.cgvsu.triangulator;

import com.cgvsu.math.Vector2;
import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;
import com.cgvsu.model.TriangulatedModel;
import com.cgvsu.triangulation.Triangulator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TriangulatorTest {
    @Test
    void testTriangulateQuad() {
        Model model = new Model();

        model.vertices.add(new Vector3(0, 0, 0));
        model.vertices.add(new Vector3(1, 0, 0));
        model.vertices.add(new Vector3(1, 1, 0));
        model.vertices.add(new Vector3(0, 1, 0));

        Polygon quad = new Polygon();
        quad.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2, 3)));
        model.polygons.add(quad);
        TriangulatedModel triangulated = Triangulator.triangulate(model);

        assertEquals(2, triangulated.polygons.size());

        for (Polygon polygon : triangulated.polygons) {
            assertTrue(polygon.isTriangle());
        }

        Polygon triangle1 = triangulated.polygons.get(0);
        assertEquals(Arrays.asList(0, 1, 2), triangle1.getVertexIndices());

        Polygon triangle2 = triangulated.polygons.get(1);
        assertEquals(Arrays.asList(0, 2, 3), triangle2.getVertexIndices());
    }

    @Test
    void testTriangulatePentagon() {
        Model model = new Model();

        model.vertices.add(new Vector3(0, 1, 0));
        model.vertices.add(new Vector3(1, 0.5f, 0));
        model.vertices.add(new Vector3(1, -0.5f, 0));
        model.vertices.add(new Vector3(0, -1, 0));
        model.vertices.add(new Vector3(-1, 0, 0));

        Polygon pentagon = new Polygon();
        pentagon.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4)));
        model.polygons.add(pentagon);

        TriangulatedModel triangulated = Triangulator.triangulate(model);
        assertEquals(3, triangulated.polygons.size());

        for (Polygon polygon : triangulated.polygons) {
            assertTrue(polygon.isTriangle());
        }

        Polygon triangle1 = triangulated.polygons.get(0);
        assertEquals(Arrays.asList(0, 1, 2), triangle1.getVertexIndices());

        Polygon triangle2 = triangulated.polygons.get(1);
        assertEquals(Arrays.asList(0, 2, 3), triangle2.getVertexIndices());

        Polygon triangle3 = triangulated.polygons.get(2);
        assertEquals(Arrays.asList(0, 3, 4), triangle3.getVertexIndices());

    }

    @Test
    void testTriangulateHexagon() {
        Model model = new Model();

        for (int i = 0; i < 6; i++) {
            float angle = (float) (2 * Math.PI * i / 6);
            model.vertices.add(new Vector3((float)Math.cos(angle), (float)Math.sin(angle), 0));
        }

        Polygon hexagon = new Polygon();
        hexagon.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5)));
        model.polygons.add(hexagon);

        TriangulatedModel triangulated = Triangulator.triangulate(model);

        assertEquals(4, triangulated.polygons.size());

        for (Polygon polygon : triangulated.polygons) {
            assertTrue(polygon.isTriangle());
        }
    }

    @Test
    void testNeedsTriangulation() {
        Model modelWithTriangles = new Model();
        Model modelWithQuads = new Model();

        Polygon triangle = new Polygon();
        triangle.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2)));
        modelWithTriangles.polygons.add(triangle);

        Polygon quad = new Polygon();
        quad.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2, 3)));
        modelWithQuads.polygons.add(quad);

        assertFalse(Triangulator.needsTriangulation(modelWithTriangles));
        assertTrue(Triangulator.needsTriangulation(modelWithQuads));
    }

    @Test
    void testTriangulateTriangle() {
        Model model = new Model();
        model.vertices.add(new Vector3(0, 0, 0));
        model.vertices.add(new Vector3(1, 0, 0));
        model.vertices.add(new Vector3(0, 1, 0));

        Polygon triangle = new Polygon();
        triangle.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2)));
        model.polygons.add(triangle);

        TriangulatedModel result = Triangulator.triangulate(model);

        assertEquals(1, result.polygons.size());
        assertEquals(triangle.getVertexIndices(), result.polygons.get(0).getVertexIndices());
    }

    @Test
    void testTriangulateEmptyModel() {
        Model model = new Model();
        TriangulatedModel result = Triangulator.triangulate(model);

        assertNotNull(result);
        assertTrue(result.vertices.isEmpty());
        assertTrue(result.polygons.isEmpty());
    }

    @Test
    void testNeedsTriangulationEmptyModel() {
        // Проверяет пустую модель (не требует триангуляции)
        Model model = new Model();
        assertFalse(Triangulator.needsTriangulation(model));
    }

    @Test
    void testNeedsTriangulationMixedPolygons() {
        Model model = new Model();

        Polygon triangle = new Polygon();
        triangle.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2)));

        Polygon quad = new Polygon();
        quad.setVertexIndices(new ArrayList<>(Arrays.asList(2, 3, 4, 5)));

        model.polygons.add(triangle);
        model.polygons.add(quad);

        assertTrue(Triangulator.needsTriangulation(model));
    }

    @Test
    void testTriangulatedModelDefaultConstructor() {
        TriangulatedModel model = new TriangulatedModel();
        assertNotNull(model);
        assertNotNull(model.vertices);
        assertNotNull(model.polygons);
        assertNotNull(model.textureVertices);
        assertNotNull(model.normals);
    }

    @Test
    void testTriangulateWithTextureAndNormals() {
        Model model = new Model();

        model.vertices.add(new Vector3(0, 0, 0));
        model.vertices.add(new Vector3(1, 0, 0));
        model.vertices.add(new Vector3(1, 1, 0));
        model.vertices.add(new Vector3(0, 1, 0));

        model.textureVertices.add(new Vector2(0, 0));
        model.textureVertices.add(new Vector2(1, 0));
        model.textureVertices.add(new Vector2(1, 1));
        model.textureVertices.add(new Vector2(0, 1));

        /*model.normals.add(new Vector3(0, 0, 1));
        model.normals.add(new Vector3(0, 0, 1));
        model.normals.add(new Vector3(0, 0, 1));
        model.normals.add(new Vector3(0, 0, 1));*/

        Polygon quad = new Polygon();
        quad.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2, 3)));
        quad.setTextureVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2, 3)));
        quad.setNormalIndices(new ArrayList<>(Arrays.asList(0, 1, 2, 3)));
        model.polygons.add(quad);

        TriangulatedModel triangulated = Triangulator.triangulate(model);

        assertEquals(2, triangulated.polygons.size());


        Polygon triangle1 = triangulated.polygons.get(0);
        assertEquals(Arrays.asList(0, 1, 2), triangle1.getVertexIndices());
        assertEquals(Arrays.asList(0, 1, 2), triangle1.getTextureVertexIndices());
        //assertEquals(Arrays.asList(0, 1, 2), triangle1.getNormalIndices());

        Polygon triangle2 = triangulated.polygons.get(1);
        assertEquals(Arrays.asList(0, 2, 3), triangle2.getVertexIndices());
        assertEquals(Arrays.asList(0, 2, 3), triangle2.getTextureVertexIndices());
        //assertEquals(Arrays.asList(0, 2, 3), triangle2.getNormalIndices());
    }
}
