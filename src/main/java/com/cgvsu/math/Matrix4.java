package com.cgvsu.math;

import java.util.Arrays;
import java.util.Objects;

public final class Matrix4 {
    private final double[][] m;
    private static final double EPS = 1e-10;

    public Matrix4(double[][] values) {
        Objects.requireNonNull(values, "Массив значений нулевой!");
        if (values.length != 4 || values[0].length != 4 || values[1].length != 4 || values[2].length != 4 || values[3].length != 4) {
            throw new IllegalArgumentException("Матрица должна быть задана как 4x4!");
        }
        this.m = new double[4][4];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(values[i], 0, this.m[i], 0, 4);
        }
    }

    public static Matrix4 zero() {
        return new Matrix4(new double[][]{
                {0,0,0,0},
                {0,0,0,0},
                {0,0,0,0},
                {0,0,0,0}
        });
    }

    public static Matrix4 identity() {
        return new Matrix4(new double[][]{
                {1,0,0,0},
                {0,1,0,0},
                {0,0,1,0},
                {0,0,0,1}
        });
    }

    public double get(int row, int col) {
        return m[row][col];
    }

    public Matrix4 add(Matrix4 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        double[][] r = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r[i][j] = m[i][j] + o.m[i][j];
            }
        }
        return new Matrix4(r);
    }

    public Matrix4 subtract(Matrix4 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        double[][] r = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r[i][j] = m[i][j] - o.m[i][j];
            }
        }
        return new Matrix4(r);
    }

    public Matrix4 multiply(Matrix4 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        double[][] r = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += m[i][k] * o.m[k][j];
                }
                r[i][j] = sum;
            }
        }
        return new Matrix4(r);
    }

    public Vector4 multiply(Vector4 v) {
        Objects.requireNonNull(v);
        return new Vector4(
                m[0][0]*v.x + m[0][1]*v.y + m[0][2]*v.z + m[0][3]*v.w,
                m[1][0]*v.x + m[1][1]*v.y + m[1][2]*v.z + m[1][3]*v.w,
                m[2][0]*v.x + m[2][1]*v.y + m[2][2]*v.z + m[2][3]*v.w,
                m[3][0]*v.x + m[3][1]*v.y + m[3][2]*v.z + m[3][3]*v.w
        );
    }

    public Matrix4 transpose() {
        double[][] r = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r[i][j] = m[j][i];
            }
        }
        return new Matrix4(r);
    }

    public double determinant() {
        int n = 4;
        double[][] a = copy(m);
        int swaps = 0;
        double det = 1.0;

        for (int col = 0; col < n; col++) {
            int pivot = col;
            double max = Math.abs(a[pivot][col]);
            for (int r = col + 1; r < n; r++) {
                double val = Math.abs(a[r][col]);
                if (val > max) {
                    max = val; pivot = r;
                }
            }
            if (max < EPS) {
                return 0.0;
            }

            if (pivot != col) {
                swapRows(a, pivot, col);
                swaps++;
            }

            double pivVal = a[col][col];
            det *= pivVal;

            for (int r = col + 1; r < n; r++) {
                double factor = a[r][col] / pivVal;
                for (int j = col; j < n; j++) {
                    a[r][j] -= factor * a[col][j];
                }
            }
        }

        if (swaps % 2 != 0) {
            det = -det;
        }
        return det;
    }

    public Matrix4 inverse() {
        int n = 4;
        double[][] a = copy(m);
        double[][] inv = identity().m;

        for (int col = 0; col < n; col++) {
            int pivot = col;
            double max = Math.abs(a[pivot][col]);
            for (int r = col + 1; r < n; r++) {
                double val = Math.abs(a[r][col]);
                if (val > max) {
                    max = val; pivot = r;
                }
            }
            if (max < EPS) {
                throw new ArithmeticException("Матрица вырожденная!");
            }

            if (pivot != col) {
                swapRows(a, pivot, col);
                swapRows(inv, pivot, col);
            }

            double pivVal = a[col][col];
            for (int j = 0; j < n; j++) {
                a[col][j] /= pivVal;
                inv[col][j] /= pivVal;
            }

            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double factor = a[r][col];
                if (Math.abs(factor) < EPS) continue;
                for (int j = 0; j < n; j++) {
                    a[r][j] -= factor * a[col][j];
                    inv[r][j] -= factor * inv[col][j];
                }
            }
        }
        return new Matrix4(inv);
    }

    public Vector4 solve(Vector4 b) {
        Objects.requireNonNull(b, "Переданный в качестве аргумента вектор нулевой!");
        int n = 4;
        double[][] a = copy(m);
        double[] rhs = new double[]{b.x, b.y, b.z, b.w};

        for (int col = 0; col < n; col++) {
            int pivot = col;
            double max = Math.abs(a[pivot][col]);
            for (int r = col + 1; r < n; r++) {
                double val = Math.abs(a[r][col]);
                if (val > max) {
                    max = val; pivot = r;
                }
            }
            if (max < EPS) {
                throw new ArithmeticException("Система вырожденная!");
            }

            if (pivot != col) {
                swapRows(a, pivot, col);
                double t = rhs[pivot]; rhs[pivot] = rhs[col]; rhs[col] = t;
            }

            for (int r = col + 1; r < n; r++) {
                double factor = a[r][col] / a[col][col];
                for (int j = col; j < n; j++) {
                    a[r][j] -= factor * a[col][j];
                }
                rhs[r] -= factor * rhs[col];
            }
        }

        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = rhs[i];
            for (int j = i + 1; j < n; j++) sum -= a[i][j] * x[j];
            double diag = a[i][i];
            if (Math.abs(diag) < EPS) {
                throw new ArithmeticException("Система вырожденная!");
            }
            x[i] = sum / diag;
        }
        return new Vector4(x[0], x[1], x[2], x[3]);
    }

    private static double[][] copy(double[][] src) {
        double[][] r = new double[src.length][src[0].length];
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, r[i], 0, src[i].length);
        }
        return r;
    }

    private static void swapRows(double[][] a, int r1, int r2) {
        double[] t = a[r1]; a[r1] = a[r2]; a[r2] = t;
    }

    public boolean approxEquals(Matrix4 o, double eps) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (Math.abs(m[i][j] - o.m[i][j]) > eps) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Matrix4 other)) return false;
        for (int i=0;i<4;i++) for (int j=0;j<4;j++) {
            if (Double.doubleToLongBits(m[i][j]) != Double.doubleToLongBits(other.m[i][j])) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int r = 1;
        for (int i=0;i<4;i++) for (int j=0;j<4;j++) r = 31*r + Double.hashCode(m[i][j]);
        return r;
    }

    @Override
    public String toString() {
        return "Matrix4" + Arrays.deepToString(m);
    }
}