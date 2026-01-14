package com.cgvsu.rasterization;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Comparator;

public class Rasterization {

    /**
     * Заполнение треугольника одним цветом (быстрый scanline)
     *
     */
    public static void fillTriangle(
            final GraphicsContext graphicsContext,
            double x1, double y1,
            double x2, double y2,
            double x3, double y3,
            Color color) {
        final PixelWriter pixelWriter = graphicsContext.getPixelWriter();
        fillTriangleScanline(pixelWriter, x1, y1, x2, y2, x3, y3, color);
    }

    /**
     * Заполнение треугольника с интерполяцией цвета (барицентрические координаты),
     * суть в том, что это не совсем эффективная реализация
     *
     */
    public static void fillTriangle(
            final GraphicsContext graphicsContext,
            double x1, double y1,
            double x2, double y2,
            double x3, double y3,
            Color color1, Color color2, Color color3) {

        final PixelWriter pixelWriter = graphicsContext.getPixelWriter();

        // --- scanline с интерполяцией цвета ---

        // сортировка вершин по y
        if (y2 < y1) {
            double tx = x1; x1 = x2; x2 = tx;
            double ty = y1; y1 = y2; y2 = ty;
            Color tc = color1; color1 = color2; color2 = tc;
        }
        if (y3 < y1) {
            double tx = x1; x1 = x3; x3 = tx;
            double ty = y1; y1 = y3; y3 = ty;
            Color tc = color1; color1 = color3; color3 = tc;
        }
        if (y3 < y2) {
            double tx = x2; x2 = x3; x3 = tx;
            double ty = y2; y2 = y3; y3 = ty;
            Color tc = color2; color2 = color3; color3 = tc;
        }

        if (Math.abs(y3 - y1) < 1e-10) {
            return;
        }

        int startY = (int) Math.ceil(y1);
        int endY   = (int) Math.floor(y3);

        for (int y = startY; y <= endY; y++) {
            if (y < 0) continue;

            // длинное ребро (1 -> 3)
            double t13 = (y - y1) / (y3 - y1);
            double x13 = interpolate(x1, x3, t13);
            Color c13 = new Color(
                    interpolate(color1.getRed(),   color3.getRed(),   t13),
                    interpolate(color1.getGreen(), color3.getGreen(), t13),
                    interpolate(color1.getBlue(),  color3.getBlue(),  t13),
                    interpolate(color1.getOpacity(), color3.getOpacity(), t13)
            );

            double xA;
            Color cA;

            if (y < y2) {
                if (Math.abs(y2 - y1) < 1e-10) continue;
                double t12 = (y - y1) / (y2 - y1);
                xA = interpolate(x1, x2, t12);
                cA = new Color(
                        interpolate(color1.getRed(),   color2.getRed(),   t12),
                        interpolate(color1.getGreen(), color2.getGreen(), t12),
                        interpolate(color1.getBlue(),  color2.getBlue(),  t12),
                        interpolate(color1.getOpacity(), color2.getOpacity(), t12)
                );
            } else {
                if (Math.abs(y3 - y2) < 1e-10) continue;
                double t23 = (y - y2) / (y3 - y2);
                xA = interpolate(x2, x3, t23);
                cA = new Color(
                        interpolate(color2.getRed(),   color3.getRed(),   t23),
                        interpolate(color2.getGreen(), color3.getGreen(), t23),
                        interpolate(color2.getBlue(),  color3.getBlue(),  t23),
                        interpolate(color2.getOpacity(), color3.getOpacity(), t23)
                );
            }

            double xLeft = xA;
            double xRight = x13;
            Color cLeft = cA;
            Color cRight = c13;

            if (xLeft > xRight) {
                double tx = xLeft; xLeft = xRight; xRight = tx;
                Color tc = cLeft; cLeft = cRight; cRight = tc;
            }

            int startX = (int) Math.ceil(xLeft);
            int endX   = (int) Math.floor(xRight);

            for (int x = startX; x <= endX; x++) {
                if (x < 0) continue;
                double t = (xRight == xLeft) ? 0 : (x - xLeft) / (xRight - xLeft);

                pixelWriter.setColor(x, y, new Color(
                        interpolate(cLeft.getRed(),   cRight.getRed(),   t),
                        interpolate(cLeft.getGreen(), cRight.getGreen(), t),
                        interpolate(cLeft.getBlue(),  cRight.getBlue(),  t),
                        interpolate(cLeft.getOpacity(), cRight.getOpacity(), t)
                ));
            }
        }
    }

    /**
     * Быстрое заполнение треугольника одним цветом используя scanline
     */
    private static void fillTriangleScanline(
            PixelWriter pixelWriter,
            double x1, double y1,
            double x2, double y2,
            double x3, double y3,
            Color color) {

        double[][] vertices = {{x1, y1}, {x2, y2}, {x3, y3}};
        Arrays.sort(vertices, Comparator.comparingDouble(v -> v[1]));

        double[] top = vertices[0];
        double[] middle = vertices[1];
        double[] bottom = vertices[2];

        if (bottom[1] != top[1]) {
            double t = (middle[1] - top[1]) / (bottom[1] - top[1]);
            double xOnLongSide = interpolate(top[0], bottom[0], t);
            fillScanlinePart(pixelWriter, top, middle, new double[]{xOnLongSide, middle[1]}, color);
            fillScanlinePart(pixelWriter, middle, bottom, new double[]{xOnLongSide, middle[1]}, color);
        }
    }

    /**
     * Заполняет часть треугольника между двумя y-координатами
     */
    private static void fillScanlinePart(
            PixelWriter pixelWriter,
            double[] start, double[] end, double[] splitPoint,
            Color color) {

        double dy = end[1] - start[1];
        if (dy == 0) return;

        int startY = (int) Math.ceil(start[1]);
        int endY = (int) Math.floor(end[1]);

        for (int y = startY; y <= endY; y++) {
            if (y < 0) continue;

            double t = (y - start[1]) / dy;
            t = Math.max(0, Math.min(1, t));

            double xLeft, xRight;
            if (y <= splitPoint[1]) {
                xLeft = interpolate(start[0], splitPoint[0], t);
            } else {
                xLeft = interpolate(splitPoint[0], end[0], t);
            }
            xRight = interpolate(start[0], end[0], t);

            if (xLeft > xRight) {
                double temp = xLeft;
                xLeft = xRight;
                xRight = temp;
            }

            fillHorizontalLine(pixelWriter, xLeft, xRight, y, color);
        }
    }

    /**
     * Линейная интерполяция
     */
    private static double interpolate(double a, double b, double t) {
        return a + t * (b - a);
    }

    /**
     * Заполняет горизонтальную линию
     */
    private static void fillHorizontalLine(
            PixelWriter pixelWriter,
            double x1, double x2, int y, Color color) {

        if (x1 > x2) {
            double temp = x1;
            x1 = x2;
            x2 = temp;
        }
        int startX = (int) Math.ceil(x1);
        int endX = (int) Math.floor(x2);

        for (int x = startX; x <= endX; x++) {
            if (x < 0) continue;
            pixelWriter.setColor(x, y, color);
        }
    }

    /**
     * Заполнение треугольника с интерполяцией цвета через барицентрические координаты
     * Используя ограничивающий прямоугольник
     */
    private static void fillTriangleBarycentric(
            PixelWriter pixelWriter,
            double x1, double y1,
            double x2, double y2,
            double x3, double y3,
            Color color1, Color color2, Color color3) {

        int minX = (int) Math.max(0, Math.floor(Math.min(x1, Math.min(x2, x3))));
        int maxX = (int) Math.ceil(Math.max(x1, Math.max(x2, x3)));
        int minY = (int) Math.max(0, Math.floor(Math.min(y1, Math.min(y2, y3))));
        int maxY = (int) Math.ceil(Math.max(y1, Math.max(y2, y3)));

        BarycentricConstants constants = new BarycentricConstants(x1, y1, x2, y2, x3, y3);

        if (constants.isDegenerate) {
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double[] lambdas = computeBarycentricCoordinates(x, y, constants);

                if (lambdas[0] >= 0 && lambdas[1] >= 0 && lambdas[2] >= 0) {
                    Color color = interpolateColorBarycentric(
                            lambdas[0], lambdas[1], lambdas[2],
                            color1, color2, color3
                    );
                    pixelWriter.setColor(x, y, color);
                }
            }
        }
    }

    /**
     * Структура для хранения вычисленных констант барицентрических координат
     */
    private static class BarycentricConstants {
        final double x1, y1, x2, y2, x3, y3;
        final double det;
        final double invDet;
        final boolean isDegenerate;

        BarycentricConstants(double x1, double y1, double x2, double y2, double x3, double y3) {
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.x3 = x3; this.y3 = y3;

            this.det = (x1 - x3) * (y2 - y3) - (x2 - x3) * (y1 - y3);
            this.isDegenerate = Math.abs(det) < 1e-10;
            this.invDet = isDegenerate ? 0 : 1.0 / det;
        }
    }

    /**
     * Вычисление барицентрических координат для точки (x,y)
     */
    private static double[] computeBarycentricCoordinates(
            int x, int y, BarycentricConstants constants) {

        if (constants.isDegenerate) {
            return new double[]{-1, -1, -1}; // Вне треугольника
        }

        double lambda1 = ((x - constants.x3) * (constants.y2 - constants.y3) -
                (constants.x2 - constants.x3) * (y - constants.y3)) * constants.invDet;
        double lambda2 = ((constants.x1 - constants.x3) * (y - constants.y3) -
                (x - constants.x3) * (constants.y1 - constants.y3)) * constants.invDet;
        double lambda3 = 1 - lambda1 - lambda2;

        return new double[]{lambda1, lambda2, lambda3};
    }

    /**
     * Интерполяция цвета через барицентрические координаты
     */
    private static Color interpolateColorBarycentric(
            double lambda1, double lambda2, double lambda3,
            Color color1, Color color2, Color color3) {

        double r = lambda1 * color1.getRed() + lambda2 * color2.getRed() + lambda3 * color3.getRed();
        double g = lambda1 * color1.getGreen() + lambda2 * color2.getGreen() + lambda3 * color3.getGreen();
        double b = lambda1 * color1.getBlue() + lambda2 * color2.getBlue() + lambda3 * color3.getBlue();
        double a = lambda1 * color1.getOpacity() + lambda2 * color2.getOpacity() + lambda3 * color3.getOpacity();

        r = Math.max(0, Math.min(1, r));
        g = Math.max(0, Math.min(1, g));
        b = Math.max(0, Math.min(1, b));
        a = Math.max(0, Math.min(1, a));

        return new Color(r, g, b, a);
    }

    public static void drawLineWithZ(
            PixelWriter pw,
            ZBuffer zBuffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            Color color) {

        int ix1 = (int) Math.round(x1);
        int iy1 = (int) Math.round(y1);
        int ix2 = (int) Math.round(x2);
        int iy2 = (int) Math.round(y2);

        int dx = Math.abs(ix2 - ix1);
        int dy = Math.abs(iy2 - iy1);

        int sx = ix1 < ix2 ? 1 : -1;
        int sy = iy1 < iy2 ? 1 : -1;

        int err = dx - dy;

        int x = ix1;
        int y = iy1;

        int steps = Math.max(dx, dy);
        for (int i = 0; i <= steps; i++) {

            double t = steps == 0 ? 0.0 : (double) i / steps;
            double z = interpolate(z1, z2, t);

            if (z < zBuffer.get(x, y)) {
                zBuffer.set(x, y, z);
                pw.setColor(x, y, color);
            }

            if (x == ix2 && y == iy2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    public static void fillTriangleZ(
            GraphicsContext gc,
            ZBuffer zBuffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            Color color
    ) {
        PixelWriter pw = gc.getPixelWriter();

        int minX = (int) Math.floor(Math.min(x1, Math.min(x2, x3)));
        int maxX = (int) Math.ceil (Math.max(x1, Math.max(x2, x3)));
        int minY = (int) Math.floor(Math.min(y1, Math.min(y2, y3)));
        int maxY = (int) Math.ceil (Math.max(y1, Math.max(y2, y3)));

        double denom =
                (y2 - y3) * (x1 - x3) +
                        (x3 - x2) * (y1 - y3);

        if (Math.abs(denom) < 1e-10) return;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {

                if (x < 0 || y < 0 ||
                        x >= zBuffer.getWidth() ||
                        y >= zBuffer.getHeight())
                    continue;

                double l1 =
                        ((y2 - y3) * (x - x3) +
                                (x3 - x2) * (y - y3)) / denom;

                double l2 =
                        ((y3 - y1) * (x - x3) +
                                (x1 - x3) * (y - y3)) / denom;

                double l3 = 1.0 - l1 - l2;

                if (l1 < 0 || l2 < 0 || l3 < 0) continue;

                // 🔹 интерполяция глубины
                double z = l1 * z1 + l2 * z2 + l3 * z3;

                // 🔹 Z-test
                if (z < zBuffer.get(x, y)) {
                    zBuffer.set(x, y, z);
                    pw.setColor(x, y, color);
                }
            }
        }
    }

}
