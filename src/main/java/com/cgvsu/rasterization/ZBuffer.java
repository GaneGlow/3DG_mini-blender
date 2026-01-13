package com.cgvsu.rasterization;

public class ZBuffer {
    private double[][] buffer;
    private int width, height;

    public ZBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        buffer = new double[width][height];
        clear();
    }

    public void clear() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                buffer[i][j] = Double.POSITIVE_INFINITY;
            }
        }
    }

    public boolean testAndSet(int x, int y, double z) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        if (z < buffer[x][y]) {
            buffer[x][y] = z;
            return true;
        }
        return false;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}