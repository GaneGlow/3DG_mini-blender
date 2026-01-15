package com.cgvsu.normals;

import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RecalculateNormalsTest {

    @Test
    void testTriangleNormalCalculation() {
        Model model = new Model();

        // Треугольник в плоскости XY
        model.vertices.add(new Vector3(0, 0, 0));
        model.vertices.add(new Vector3(1, 0, 0));
        model.vertices.add(new Vector3(0, 1, 0));

        Polygon triangle = new Polygon();
        triangle.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2)));
        model.polygons.add(triangle);

        model.recalculateNormals();

        assertEquals(3, model.normals.size());
        // Нормаль должна быть направлена по положительной оси Z
        assertEquals(new Vector3(0, 0, 1), model.normals.get(0));
    }

    @Test
    void testCubeNormalCalculation() {
        Model model = new Model();

        // Вершины куба
        model.vertices.add(new Vector3(0, 0, 0));
        model.vertices.add(new Vector3(1, 0, 0));
        model.vertices.add(new Vector3(1, 1, 0));
        model.vertices.add(new Vector3(0, 1, 0));
        model.vertices.add(new Vector3(0, 0, 1));
        model.vertices.add(new Vector3(1, 0, 1));
        model.vertices.add(new Vector3(1, 1, 1));
        model.vertices.add(new Vector3(0, 1, 1));

        // Нижняя грань
        Polygon bottom = new Polygon();
        bottom.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2, 3)));
        model.polygons.add(bottom);

        model.recalculateNormals();

        assertEquals(8, model.normals.size());
        // Проверяем, что нормали вычислены
        assertNotNull(model.normals.get(0));
        assertTrue(model.normals.get(0).length() > 0.9f); // Должна быть нормализована
    }

    @Test
    void testDegeneratePolygonHandling() {
        Model model = new Model();

        model.vertices.add(new Vector3(0, 0, 0));
        model.vertices.add(new Vector3(1, 0, 0));
        model.vertices.add(new Vector3(2, 0, 0)); // Добавляем третью вершину

        Polygon degenerate = new Polygon();
        degenerate.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2)));
        model.polygons.add(degenerate);

        // Не должно быть исключения
        assertDoesNotThrow(() -> model.recalculateNormals());

        // Проверяем, что нормали вычислены (хотя и могут быть нулевыми)
        assertEquals(3, model.normals.size());
    }

    @Test
    void testEmptyModel() {
        Model model = new Model();

        assertDoesNotThrow(() -> model.recalculateNormals());
        assertTrue(model.normals.isEmpty());
    }

    @Test
    void testNormalDirection() {
        Model model = new Model();

        // Треугольник с определенным порядком вершин
        model.vertices.add(new Vector3(0, 0, 0));
        model.vertices.add(new Vector3(1, 0, 0));
        model.vertices.add(new Vector3(0, 0, 1));

        Polygon triangle = new Polygon();
        triangle.setVertexIndices(new ArrayList<>(Arrays.asList(0, 1, 2)));
        model.polygons.add(triangle);

        model.recalculateNormals();

        // Нормаль должна быть направлена по отрицательной оси Y
        assertEquals(new Vector3(0, -1, 0), model.normals.get(0));
    }
}