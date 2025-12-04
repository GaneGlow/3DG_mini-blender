package com.cgvsu.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Matrix3Test {
    private static final double EPS = 1e-9;

    @Test
    void identity_zero_and_basic() {
        Matrix3 I = Matrix3.identity();
        Matrix3 Z = Matrix3.zero();

        Vector3 v = new Vector3(1,2,3);
        assertTrue(I.approxEquals(I.transpose().transpose(), EPS));
        assertTrue(Z.approxEquals(Z.transpose(), EPS));

        Vector3 Iv = I.multiply(v);
        assertEquals(v.x, Iv.x, EPS);
        assertEquals(v.y, Iv.y, EPS);
        assertEquals(v.z, Iv.z, EPS);

        Vector3 Zv = Z.multiply(v);
        assertEquals(0, Zv.x, EPS);
        assertEquals(0, Zv.y, EPS);
        assertEquals(0, Zv.z, EPS);
    }

    @Test
    void add_sub_mul() {
        Matrix3 A = new Matrix3(new double[][]{
                {1,2,3},
                {4,5,6},
                {7,8,9}
        });
        Matrix3 B = new Matrix3(new double[][]{
                {9,8,7},
                {6,5,4},
                {3,2,1}
        });

        Matrix3 S = A.add(B);
        assertEquals(10, S.get(0,0), EPS);
        assertEquals(10, S.get(1,1), EPS);
        assertEquals(10, S.get(2,2), EPS);

        Matrix3 D = A.subtract(B);
        assertEquals(-8, D.get(0,0), EPS);
        assertEquals(0, D.get(1,1), EPS);
        assertEquals(8, D.get(2,2), EPS);

        Matrix3 M = A.multiply(B);
        assertEquals(30, M.get(0,0), EPS);
        assertEquals(24, M.get(0,1), EPS);
        assertEquals(18, M.get(0,2), EPS);
    }

    @Test
    void determinant_inverse_solve() {
        Matrix3 A = new Matrix3(new double[][]{
                {2, 1, 3},
                {0, -1, 2},
                {1, 4, 0}
        });
        double det = A.determinant();
        assertEquals(-11, det, EPS);

        Matrix3 Inv = A.inverse();
        Matrix3 I = Inv.multiply(A);
        assertTrue(I.approxEquals(Matrix3.identity(), 1e-9));

        Vector3 b = new Vector3(7, 3, 5);
        Vector3 x = A.solve(b);
        Vector3 check = A.multiply(x);
        assertEquals(b.x, check.x, 1e-8);
        assertEquals(b.y, check.y, 1e-8);
        assertEquals(b.z, check.z, 1e-8);

        Matrix3 singular = new Matrix3(new double[][]{
                {1,2,3},
                {2,4,6},
                {0,0,0}
        });
        assertEquals(0.0, singular.determinant(), 1e-12);
        assertThrows(ArithmeticException.class, singular::inverse);
        assertThrows(ArithmeticException.class, () -> singular.solve(new Vector3(1,2,3)));
    }
}