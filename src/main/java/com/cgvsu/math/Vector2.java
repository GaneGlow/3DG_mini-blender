package com.cgvsu.math;

import java.util.Objects;

public final class Vector2 {
    public final double x;
    public final double y;

    private static final double EPS = 1e-10;

    public Vector2(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Координаты должны быть конечными числами");
        }

        this.x = x;
        this.y = y;
    }

    public Vector2 add(Vector2 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return new Vector2(x + o.x, y + o.y);
    }

    public Vector2 subtract(Vector2 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return new Vector2(x - o.x, y - o.y);
    }

    public Vector2 scale(double s) {
        return new Vector2(x * s, y * s);
    }

    public Vector2 divide(double s) {
        if (Math.abs(s) < EPS) {
            throw new ArithmeticException("Деление на нулевой скаляр недопустимо!");
        }
        return new Vector2(x / s, y / s);
    }

    public double dot(Vector2 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return x * o.x + y * o.y;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public Vector2 normalized() {
        double len = length();
        if (len < EPS) {
            throw new ArithmeticException("Невозможно привести нулевой вектор к нормализованному виду!");
        }
        return divide(len);
    }

    public boolean approxEquals(Vector2 o, double eps) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return Math.abs(x - o.x) <= eps && Math.abs(y - o.y) <= eps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector2 v)) return false;
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(v.x)
                && Double.doubleToLongBits(y) == Double.doubleToLongBits(v.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Vector2(" + x + ", " + y + ")";
    }
}