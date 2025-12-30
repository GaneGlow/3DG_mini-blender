package src.main.java.com.cgvsu.math;

import java.util.Objects;

public final class Vector2 {
    public final float x;
    public final float y;

    private static final float EPS = 1e-7f;

    public Vector2(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
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

    public Vector2 scale(float s) {
        return new Vector2(x * s, y * s);
    }

    public Vector2 divide(float s) {
        if (Math.abs(s) < EPS) {
            throw new ArithmeticException("Деление на нулевой скаляр недопустимо!");
        }
        return new Vector2(x / s, y / s);
    }

    public float dot(Vector2 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return x * o.x + y * o.y;
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public float lengthSquared() {
        return x * x + y * y;
    }

    public Vector2 normalized() {
        float len = length();
        if (len < EPS) {
            throw new ArithmeticException("Невозможно привести нулевой вектор к нормализованному виду!");
        }
        return divide(len);
    }

    public boolean approxEquals(Vector2 o, float eps) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return Math.abs(x - o.x) <= eps && Math.abs(y - o.y) <= eps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector2 v)) return false;
        return Float.floatToIntBits(x) == Float.floatToIntBits(v.x)
                && Float.floatToIntBits(y) == Float.floatToIntBits(v.y);
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