package com.cgvsu.math;

import java.util.Objects;

public final class Vector4 {
    public final double x, y, z, w;
    private static final double EPS = 1e-10;

    public Vector4(double x, double y, double z, double w) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Double.isFinite(w)) {
            throw new IllegalArgumentException("Координаты должны быть конечными числами");
        }

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4 add(Vector4 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return new Vector4(x + o.x, y + o.y, z + o.z, w + o.w);
    }

    public Vector4 subtract(Vector4 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return new Vector4(x - o.x, y - o.y, z - o.z, w - o.w);
    }

    public Vector4 scale(double s) {
        return new Vector4(x * s, y * s, z * s, w * s);
    }

    public Vector4 divide(double s) {
        if (Math.abs(s) < EPS) {
            throw new ArithmeticException("Деление на нулевой скаляр недопустимо!");
        }
        return new Vector4(x / s, y / s, z / s, w / s);
    }

    public double dot(Vector4 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return x * o.x + y * o.y + z * o.z + w * o.w;
    }

    public double length() {
        return Math.sqrt(x*x + y*y + z*z + w*w);
    }

    public double lengthSquared() {
        return x*x + y*y + z*z + w*w;
    }

    public Vector4 normalized() {
        double len = length();
        if (len < EPS) {
            throw new ArithmeticException("Невозможно привести нулевой вектор к нормализованному виду!");
        }
        return divide(len);
    }

    public boolean approxEquals(Vector4 o, double eps) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return Math.abs(x - o.x) <= eps &&
                Math.abs(y - o.y) <= eps &&
                Math.abs(z - o.z) <= eps &&
                Math.abs(w - o.w) <= eps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector4 v)) return false;
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(v.x) &&
                Double.doubleToLongBits(y) == Double.doubleToLongBits(v.y) &&
                Double.doubleToLongBits(z) == Double.doubleToLongBits(v.z) &&
                Double.doubleToLongBits(w) == Double.doubleToLongBits(v.w);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }

    @Override
    public String toString() {
        return "Vector4(" + x + ", " + y + ", " + z + ", " + w + ")";
    }
}