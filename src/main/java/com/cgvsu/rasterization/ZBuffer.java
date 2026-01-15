package com.cgvsu.rasterization;

public final class ZBuffer {

    private final int width;
    private final int height;
    private final double[][] buffer;

    public ZBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.buffer = new double[height][width];
        clear();
    }

    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = Double.POSITIVE_INFINITY;
            }
        }
    }

    public double get(int x, int y) {
        return buffer[y][x];
    }

    public void set(int x, int y, double z) {
        buffer[y][x] = z;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
