package com.cgvsu.rasterization;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

public final class RecordingPixelWriter implements PixelWriter {

    public static final class Pixel {
        public final int x;
        public final int y;

        public Pixel(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pixel)) return false;
            Pixel p = (Pixel) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    private final Map<Long, Color> lastColorByPixel = new HashMap<>();
    private final Map<Long, Integer> writeCountByPixel = new HashMap<>();
    private final List<Pixel> writeOrder = new ArrayList<>();

    private static long key(int x, int y) {
        return (((long) x) << 32) | (y & 0xffffffffL);
    }

    public void clear() {
        lastColorByPixel.clear();
        writeCountByPixel.clear();
        writeOrder.clear();
    }

    public Color get(int x, int y) {
        return lastColorByPixel.get(key(x, y));
    }

    public int writeCount(int x, int y) {
        return writeCountByPixel.getOrDefault(key(x, y), 0);
    }

    public int totalWrites() {
        return writeOrder.size();
    }

    public List<Pixel> writeOrder() {
        return unmodifiableList(writeOrder);
    }

    public Set<Pixel> writtenPixels() {
        Set<Pixel> set = new HashSet<>();
        for (Long k : lastColorByPixel.keySet()) {
            int x = (int) (k >> 32);
            int y = (int) (long) k;
            set.add(new Pixel(x, y));
        }
        return unmodifiableSet(set);
    }

    @Override
    public void setColor(int x, int y, Color c) {
        long k = key(x, y);
        writeOrder.add(new Pixel(x, y));
        lastColorByPixel.put(k, c);
        writeCountByPixel.merge(k, 1, Integer::sum);
    }

    // Ниже — методы, которые Rasterization не использует.
    // Оставляем их, чтобы компилилось, и чтобы случайное использование в тестах сразу падало.

    @Override
    public PixelFormat getPixelFormat() {
        return null;
    }

    @Override
    public void setArgb(int x, int y, int argb) {
        throw new UnsupportedOperationException("Not used in these tests");
    }

    @Override
    public <T extends Buffer> void setPixels(
            int x, int y, int w, int h, PixelFormat<T> pixelformat, T buffer, int scanlineStride) {
        throw new UnsupportedOperationException("Not used in these tests");
    }

    @Override
    public void setPixels(
            int x, int y, int w, int h, PixelFormat<ByteBuffer> pixelformat, byte[] buffer, int offset, int scanlineStride) {
        throw new UnsupportedOperationException("Not used in these tests");
    }

    @Override
    public void setPixels(
            int x, int y, int w, int h, PixelFormat<IntBuffer> pixelformat, int[] buffer, int offset, int scanlineStride) {
        throw new UnsupportedOperationException("Not used in these tests");
    }

    @Override
    public void setPixels(
            int dstx, int dsty, int w, int h, PixelReader reader, int srcx, int srcy) {
        throw new UnsupportedOperationException("Not used in these tests");
    }
}
