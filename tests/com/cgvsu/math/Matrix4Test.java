package tests.com.cgvsu.math;
import org.junit.jupiter.api.Test;

import src.main.java.com.cgvsu.math.Matrix4;
import src.main.java.com.cgvsu.math.Vector4;

import static org.junit.jupiter.api.Assertions.*;

public class Matrix4Test {
    private static final float EPS = 1e-9f;

    @Test
    void identity_zero() {
        Matrix4 I = Matrix4.identity();
        Matrix4 Z = Matrix4.zero();

        Vector4 v = new Vector4(1,2,3,4);
        Vector4 Iv = I.multiply(v);
        assertEquals(v.x, Iv.x, EPS);
        assertEquals(v.y, Iv.y, EPS);
        assertEquals(v.z, Iv.z, EPS);
        assertEquals(v.w, Iv.w, EPS);

        Vector4 Zv = Z.multiply(v);
        assertEquals(0, Zv.x, EPS);
        assertEquals(0, Zv.y, EPS);
        assertEquals(0, Zv.z, EPS);
        assertEquals(0, Zv.w, EPS);
    }

    @Test
    void add_sub_mul_transpose() {
        Matrix4 A = new Matrix4(new float[][]{
                {1,2,3,4},
                {5,6,7,8},
                {9,10,11,12},
                {13,14,15,16}
        });
        Matrix4 B = new Matrix4(new float[][]{
                {16,15,14,13},
                {12,11,10,9},
                {8,7,6,5},
                {4,3,2,1}
        });

        Matrix4 S = A.add(B);
        assertEquals(17, S.get(0,0), EPS);
        assertEquals(17, S.get(1,1), EPS);
        assertEquals(17, S.get(2,2), EPS);
        assertEquals(17, S.get(3,3), EPS);

        Matrix4 T = A.transpose().transpose();
        assertTrue(T.approxEquals(A, EPS));

        Matrix4 M = A.multiply(B);
        assertEquals(80, M.get(0,0), EPS);
        assertEquals(70, M.get(0,1), EPS);
        assertEquals(60, M.get(0,2), EPS);
        assertEquals(50, M.get(0,3), EPS);
    }

    @Test
    void det_inverse_solve() {
        Matrix4 A = new Matrix4(new float[][]{
                {4,  7,  2, 3},
                {0,  5,  0, 1},
                {0,  0,  3, 0},
                {2,  0,  1, 1}
        });

        float det = A.determinant();
        assertNotEquals(0.0f, det, 1e-12f);

        Matrix4 Inv = A.inverse();
        Matrix4 I = A.multiply(Inv);
        assertTrue(I.approxEquals(Matrix4.identity(), 1e-8f));

        Vector4 b = new Vector4(1,2,3,4);
        Vector4 x = A.solve(b);
        Vector4 check = A.multiply(x);
        assertEquals(b.x, check.x, 1e-8f);
        assertEquals(b.y, check.y, 1e-8f);
        assertEquals(b.z, check.z, 1e-8f);
        assertEquals(b.w, check.w, 1e-8f);

        Matrix4 singular = new Matrix4(new float[][]{
                {1,2,3,4},
                {2,4,6,8},
                {0,0,0,0},
                {5,10,15,20}
        });
        assertEquals(0.0f, singular.determinant(), 1e-8f);
        assertThrows(ArithmeticException.class, singular::inverse);
        assertThrows(ArithmeticException.class, () -> singular.solve(new Vector4(1,2,3,4)));
    }
}