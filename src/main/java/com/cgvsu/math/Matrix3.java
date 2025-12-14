package com.cgvsu.math;

import java.util.Arrays;
import java.util.Objects;

public final class Matrix3 {
    private final float[][] m;
    private static final float EPS = 1e-7f;

    public Matrix3(float[][] values) {
        Objects.requireNonNull(values, "Массив значений нулевой!");
        if (values.length != 3 || values[0].length != 3 || values[1].length != 3 || values[2].length != 3) {
            throw new IllegalArgumentException("Матрица должна быть задана как 3x3!");
        }
        this.m = new float[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(values[i], 0, this.m[i], 0, 3);
        }
    }

    public static Matrix3 zero() {
        return new Matrix3(new float[][]{
                {0,0,0},
                {0,0,0},
                {0,0,0}
        });
    }

    public static Matrix3 identity() {
        return new Matrix3(new float[][]{
                {1,0,0},
                {0,1,0},
                {0,0,1}
        });
    }

    public float get(int row, int col) {
        return m[row][col];
    }

    public Matrix3 add(Matrix3 o) {
        Objects.requireNonNull(o, "Переданная в качестве аргумента матрица нулевая!");
        float[][] r = new float[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = m[i][j] + o.m[i][j];
            }
        }
        return new Matrix3(r);
    }

    public Matrix3 subtract(Matrix3 o) {
        Objects.requireNonNull(o, "Переданная в качестве аргумента матрица нулевая!");
        float[][] r = new float[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = m[i][j] - o.m[i][j];
            }
        }
        return new Matrix3(r);
    }

    public Matrix3 multiply(Matrix3 o) {
        Objects.requireNonNull(o, "Переданная в качестве аргумента матрица нулевая!");
        float[][] r = new float[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                float sum = 0;
                for (int k = 0; k < 3; k++) {
                    sum += m[i][k] * o.m[k][j];
                }
                r[i][j] = sum;
            }
        }
        return new Matrix3(r);
    }

    public Vector3 multiply(Vector3 v) {
        Objects.requireNonNull(v, "Переданный в качестве аргумента вектор нулевой!");
        return new Vector3(
                m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z,
                m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z,
                m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z
        );
    }

    public Matrix3 transpose() {
        float[][] r = new float[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = m[j][i];
            }
        }
        return new Matrix3(r);
    }

    public float determinant() {
        float a = m[0][0], b = m[0][1], c = m[0][2];
        float d = m[1][0], e = m[1][1], f = m[1][2];
        float g = m[2][0], h = m[2][1], i = m[2][2];
        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
    }

    public Matrix3 inverse() {
        float[][] a = copy(m);
        float[][] inv = identity().m;

        for (int col = 0; col < 3; col++) {
            int pivot = col;
            float max = Math.abs(a[pivot][col]);
            for (int r = col + 1; r < 3; r++) {
                float val = Math.abs(a[r][col]);
                if (val > max) {
                    max = val;
                    pivot = r;
                }
            }
            if (max < EPS) {
                throw new ArithmeticException("Матрица вырожденная");
            }

            if (pivot != col) {
                swapRows(a, pivot, col);
                swapRows(inv, pivot, col);
            }

            float pivVal = a[col][col];
            for (int j = 0; j < 3; j++) {
                a[col][j] /= pivVal;
                inv[col][j] /= pivVal;
            }

            for (int r = 0; r < 3; r++) {
                if (r == col) continue;
                float factor = a[r][col];
                if (Math.abs(factor) < EPS) continue;
                for (int j = 0; j < 3; j++) {
                    a[r][j] -= factor * a[col][j];
                    inv[r][j] -= factor * inv[col][j];
                }
            }
        }
        return new Matrix3(inv);
    }

    public Vector3 solve(Vector3 b) {
        Objects.requireNonNull(b, "Переданный в качестве аргумента вектор нулевой!");
        int n = 3;
        float[][] a = copy(m);
        float[] rhs = new float[]{b.x, b.y, b.z};

        for (int col = 0; col < n; col++) {
            int pivot = col;
            float max = Math.abs(a[pivot][col]);
            for (int r = col + 1; r < n; r++) {
                float val = Math.abs(a[r][col]);
                if (val > max) {
                    max = val; pivot = r;
                }
            }
            if (max < EPS) {
                throw new ArithmeticException("Система вырожденная!");
            }

            if (pivot != col) {
                swapRows(a, pivot, col);
                float tmp = rhs[pivot]; rhs[pivot] = rhs[col]; rhs[col] = tmp;
            }

            for (int r = col + 1; r < n; r++) {
                float factor = a[r][col] / a[col][col];
                for (int j = col; j < n; j++) {
                    a[r][j] -= factor * a[col][j];
                }
                rhs[r] -= factor * rhs[col];
            }
        }

        float[] x = new float[n];
        for (int i = n - 1; i >= 0; i--) {
            float sum = rhs[i];
            for (int j = i + 1; j < n; j++) {
                sum -= a[i][j] * x[j];
            }
            float diag = a[i][i];
            if (Math.abs(diag) < EPS) {
                throw new ArithmeticException("Система вырожденная!");
            }
            x[i] = sum / diag;
        }
        return new Vector3(x[0], x[1], x[2]);
    }

    private static void swapRows(float[][] a, int r1, int r2) {
        float[] tmp = a[r1];
        a[r1] = a[r2];
        a[r2] = tmp;
    }

    private static float[][] copy(float[][] src) {
        float[][] r = new float[src.length][src[0].length];
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, r[i], 0, src[i].length);
        }
        return r;
    }

    public boolean approxEquals(Matrix3 o, float eps) {
        for (int i=0;i<3;i++) for (int j=0;j<3;j++) {
            if (Math.abs(m[i][j] - o.m[i][j]) > eps) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Matrix3 other)) return false;
        for (int i=0;i<3;i++) for (int j=0;j<3;j++) {
            if (Float.floatToIntBits(m[i][j]) != Float.floatToIntBits(other.m[i][j])) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int r = 1;
        for (int i=0;i<3;i++) for (int j=0;j<3;j++) r = 31*r + Float.hashCode(m[i][j]);
        return r;
    }

    @Override
    public String toString() {
        return "Matrix3" + Arrays.deepToString(m);
    }
}