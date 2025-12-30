package src.main.java.com.cgvsu.math;

import java.util.Objects;

public final class Vector4 {
    public final float x, y, z, w;
    private static final float EPS = 1e-7f;

    public Vector4(float x, float y, float z, float w) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(w)) {
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

    public Vector4 scale(float s) {
        return new Vector4(x * s, y * s, z * s, w * s);
    }

    public Vector4 divide(float s) {
        if (Math.abs(s) < EPS) {
            throw new ArithmeticException("Деление на нулевой скаляр недопустимо!");
        }
        return new Vector4(x / s, y / s, z / s, w / s);
    }

    public float dot(Vector4 o) {
        Objects.requireNonNull(o, "Переданный в качестве аргумента вектор нулевой!");
        return x * o.x + y * o.y + z * o.z + w * o.w;
    }

    public float length() {
        return (float) Math.sqrt(x*x + y*y + z*z + w*w);
    }

    public float lengthSquared() {
        return x*x + y*y + z*z + w*w;
    }

    public Vector4 normalized() {
        float len = length();
        if (len < EPS) {
            throw new ArithmeticException("Невозможно привести нулевой вектор к нормализованному виду!");
        }
        return divide(len);
    }

    public boolean approxEquals(Vector4 o, float eps) {
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
        return Float.floatToIntBits(x) == Float.floatToIntBits(v.x) &&
                Float.floatToIntBits(y) == Float.floatToIntBits(v.y) &&
                Float.floatToIntBits(z) == Float.floatToIntBits(v.z) &&
                Float.floatToIntBits(w) == Float.floatToIntBits(v.w);
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