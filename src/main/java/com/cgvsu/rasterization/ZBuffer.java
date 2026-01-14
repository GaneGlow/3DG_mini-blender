package com.cgvsu.rasterization;

/**
 * Z-buffer (буфер глубины).
 *
 * Хранит минимальное значение глубины (z) для каждого пикселя.
 * Используется алгоритмами растеризации для скрытия задних примитивов.
 *
 * Методичка:
 *  - инициализация значениями +∞
 *  - если новый z < текущего, пиксель перерисовывается
 */
public final class ZBuffer {

    private final int width;
    private final int height;
    private final double[][] buffer;

    /**
     * Создаёт Z-buffer под заданный размер экрана.
     */
    public ZBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.buffer = new double[height][width];
        clear();
    }

    /**
     * Очистка буфера перед новым кадром.
     * Все значения глубины устанавливаются в +∞.
     */
    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = Double.POSITIVE_INFINITY;
            }
        }
    }

    /**
     * Возвращает текущее значение глубины в пикселе (x, y).
     */
    public double get(int x, int y) {
        return buffer[y][x];
    }

    /**
     * Устанавливает новое значение глубины в пикселе (x, y).
     * ЛОГИКИ СРАВНЕНИЯ ТУТ НЕТ — она находится в растеризации.
     */
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
