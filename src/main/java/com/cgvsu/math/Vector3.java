package src.main.java.com.cgvsu.math;

import java.util.Objects;

public final class Vector3 {
    public final float x;
    public final float y;
    public final float z;

    private static final float EPS = 1e-7f;

    public Vector3(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Координаты должны быть конечными числами");
        }

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 add(Vector3 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return new Vector3(x + o.x, y + o.y, z + o.z);
    }

    public Vector3 subtract(Vector3 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return new Vector3(x - o.x, y - o.y, z - o.z);
    }

    public Vector3 scale(float s) {
        return new Vector3(x * s, y * s, z * s);
    }

    public Vector3 divide(float s) {
        if (Math.abs(s) < EPS) {
            throw new ArithmeticException("Деление на нулевой скаляр недопустимо!");
        }
        return new Vector3(x / s, y / s, z / s);
    }

    public float dot(Vector3 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return x * o.x + y * o.y + z * o.z;
    }

    public Vector3 cross(Vector3 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return new Vector3(
                y * o.z - z * o.y,
                z * o.x - x * o.z,
                x * o.y - y * o.x
        );
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public Vector3 normalized() {
        float len = length();
        if (len < EPS) {
            throw new ArithmeticException("Невозможно привести нулевой вектор к нормализованному виду!");
        }
        return divide(len);
    }

    public boolean approxEquals(Vector3 o, float eps) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return Math.abs(x - o.x) <= eps && Math.abs(y - o.y) <= eps && Math.abs(z - o.z) <= eps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector3 v)) return false;
        return Float.floatToIntBits(x) == Float.floatToIntBits(v.x) &&
                Float.floatToIntBits(y) == Float.floatToIntBits(v.y) &&
                Float.floatToIntBits(z) == Float.floatToIntBits(v.z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "Vector3(" + x + ", " + y + ", " + z + ")";
    }
}