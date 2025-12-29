package com.cgvsu.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorTest {
    private static final float EPS = 1e-6f;

    @Test
    void vector2_basicOps() {
        Vector2 a = new Vector2(1, 2);
        Vector2 b = new Vector2(-3, 5);

        Vector2 sum = a.add(b);
        assertEquals(-2, sum.x, EPS);
        assertEquals(7, sum.y, EPS);

        Vector2 diff = a.subtract(b);
        assertEquals(4, diff.x, EPS);
        assertEquals(-3, diff.y, EPS);

        Vector2 scaled = a.scale(2.5f);
        assertEquals(2.5, scaled.x, EPS);
        assertEquals(5.0, scaled.y, EPS);

        Vector2 div = b.divide(2);
        assertEquals(-1.5, div.x, EPS);
        assertEquals(2.5, div.y, EPS);

        assertThrows(ArithmeticException.class, () -> a.divide(0)); // деление на 0

        assertEquals(Math.sqrt(5), new Vector2(1,2).length(), EPS);
        Vector2 n = new Vector2(3,4).normalized();
        assertEquals(1.0, n.length(), EPS);
        assertThrows(ArithmeticException.class, () -> new Vector2(0,0).normalized()); // попытка нормализации нулевого вектора

        assertEquals(-3 + 2 * 5, a.dot(b), EPS);
    }

    @Test
    void vector3_ops() {
        Vector3 a = new Vector3(1,2,3);
        Vector3 b = new Vector3(-4,5,6);

        Vector3 sum = a.add(b);
        assertEquals(-3, sum.x, EPS);
        assertEquals(7, sum.y, EPS);
        assertEquals(9, sum.z, EPS);

        assertEquals(-4 + 2 * 5 + 3 * 6, a.dot(b), EPS);

        Vector3 c = new Vector3(1,0,0);
        Vector3 d = new Vector3(0,1,0);
        Vector3 cross = c.cross(d);
        assertEquals(0, cross.x, EPS);
        assertEquals(0, cross.y, EPS);
        assertEquals(1, cross.z, EPS);

        Vector3 e = new Vector3(2,0,0);
        Vector3 cross2 = c.cross(e);
        assertEquals(0, cross2.length(), EPS);
    }

    @Test
    void vector4_ops() {
        Vector4 a = new Vector4(1,2,3,4);
        Vector4 b = new Vector4(-1,0.5f, 2, -3);

        Vector4 sum = a.add(b);
        assertEquals(0, sum.x, EPS);
        assertEquals(2.5, sum.y, EPS);
        assertEquals(5, sum.z, EPS);
        assertEquals(1, sum.w, EPS);

        assertEquals(-1 + 2 * 0.5f + 3 * 2 + 4 * (-3), a.dot(b), EPS);

        Vector4 n = a.normalized();
        assertEquals(1.0, n.length(), EPS);
        assertThrows(ArithmeticException.class, () -> new Vector4(0,0,0,0).normalized());
    }
}