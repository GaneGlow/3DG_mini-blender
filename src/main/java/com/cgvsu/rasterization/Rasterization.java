package com.cgvsu.rasterization;

import com.cgvsu.math.Vector3;
import com.cgvsu.render_engine.Texture;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Comparator;

public class Rasterization {

    /**
     * Заполнение треугольника одним цветом с использованием Z-буфера
     */
    public static void fillTriangle(
            final GraphicsContext graphicsContext,
            final ZBuffer zBuffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            Color color) {
        final PixelWriter pixelWriter = graphicsContext.getPixelWriter();
        fillTriangleScanline(pixelWriter, zBuffer, x1, y1, z1, x2, y2, z2, x3, y3, z3, color);
    }

    /**
     * Заполнение треугольника с интерполяцией цвета и Z-буфером
     */
    public static void fillTriangle(
            final GraphicsContext graphicsContext,
            final ZBuffer zBuffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            Color color1, Color color2, Color color3) {
        final PixelWriter pixelWriter = graphicsContext.getPixelWriter();
        fillTriangleBarycentric(pixelWriter, zBuffer, x1, y1, z1, x2, y2, z2, x3, y3, z3, color1, color2, color3);
    }

    /**
     * Быстрое заполнение треугольника с Z-буфером (scanline)
     */
    private static void fillTriangleScanline(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            Color color) {

        double[][] vertices = {{x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        Arrays.sort(vertices, Comparator.comparingDouble(v -> v[1]));

        double[] top = vertices[0];
        double[] middle = vertices[1];
        double[] bottom = vertices[2];

        if (bottom[1] != top[1]) {
            double t = (middle[1] - top[1]) / (bottom[1] - top[1]);
            double xOnLongSide = interpolate(top[0], bottom[0], t);
            double zOnLongSide = interpolate(top[2], bottom[2], t);
            fillScanlinePart(pixelWriter, zBuffer, top, middle, new double[]{xOnLongSide, middle[1], zOnLongSide}, color);
            fillScanlinePart(pixelWriter, zBuffer, middle, bottom, new double[]{xOnLongSide, middle[1], zOnLongSide}, color);
        }
    }

    /**
     * Заполняет часть треугольника между двумя y-координатами с Z-буфером
     */
    private static void fillScanlinePart(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            double[] start, double[] end, double[] splitPoint,
            Color color) {

        double dy = end[1] - start[1];
        if (dy == 0) return;

        int startY = (int) Math.ceil(start[1]);
        int endY = (int) Math.floor(end[1]);

        for (int y = startY; y <= endY; y++) {
            if (y < 0 || y >= zBuffer.getHeight()) continue;

            double t = (y - start[1]) / dy;
            t = Math.max(0, Math.min(1, t));

            double xLeft, xRight, zLeft, zRight;
            if (y <= splitPoint[1]) {
                xLeft = interpolate(start[0], splitPoint[0], t);
                zLeft = interpolate(start[2], splitPoint[2], t);
            } else {
                xLeft = interpolate(splitPoint[0], end[0], t);
                zLeft = interpolate(splitPoint[2], end[2], t);
            }
            xRight = interpolate(start[0], end[0], t);
            zRight = interpolate(start[2], end[2], t);

            if (xLeft > xRight) {
                double temp = xLeft;
                xLeft = xRight;
                xRight = temp;
                temp = zLeft;
                zLeft = zRight;
                zRight = temp;
            }

            fillHorizontalLine(pixelWriter, zBuffer, xLeft, xRight, zLeft, zRight, y, color);
        }
    }

    /**
     * Заполняет горизонтальную линию с Z-буфером
     */
    private static void fillHorizontalLine(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            double x1, double x2, double z1, double z2, int y, Color color) {
        if (x1 > x2) {
            double temp = x1;
            x1 = x2;
            x2 = temp;
            temp = z1;
            z1 = z2;
            z2 = temp;
        }
        int startX = (int) Math.ceil(x1);
        int endX = (int) Math.floor(x2);

        double dx = x2 - x1;
        for (int x = startX; x <= endX; x++) {
            if (x < 0 || x >= zBuffer.getWidth()) continue;

            double t = (x - x1) / dx;
            double z = interpolate(z1, z2, t);

            // Проверка глубины
            if (z < zBuffer.get(x, y)) {
                pixelWriter.setColor(x, y, color);
                zBuffer.set(x, y, z);
            }
        }
    }

    /**
     * Заполнение треугольника с интерполяцией цвета через барицентрические координаты с Z-буфером
     */
    private static void fillTriangleBarycentric(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            Color color1, Color color2, Color color3) {
        int minX = (int) Math.max(0, Math.floor(Math.min(x1, Math.min(x2, x3))));
        int maxX = (int) Math.min(zBuffer.getWidth() - 1, Math.ceil(Math.max(x1, Math.max(x2, x3))));
        int minY = (int) Math.max(0, Math.floor(Math.min(y1, Math.min(y2, y3))));
        int maxY = (int) Math.min(zBuffer.getHeight() - 1, Math.ceil(Math.max(y1, Math.max(y2, y3))));

        BarycentricConstants constants = new BarycentricConstants(x1, y1, x2, y2, x3, y3);

        if (constants.isDegenerate) {
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double[] lambdas = computeBarycentricCoordinates(x, y, constants);

                if (lambdas[0] >= 0 && lambdas[1] >= 0 && lambdas[2] >= 0) {
                    double z = lambdas[0] * z1 + lambdas[1] * z2 + lambdas[2] * z3;

                    // Проверка глубины
                    if (z < zBuffer.get(x, y)) {
                        Color color = interpolateColorBarycentric(lambdas[0], lambdas[1], lambdas[2],
                                color1, color2, color3);
                        pixelWriter.setColor(x, y, color);
                        zBuffer.set(x, y, z);
                    }
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
    private static double[] computeBarycentricCoordinates(int x, int y, BarycentricConstants constants) {
        if (constants.isDegenerate) {
            return new double[]{-1, -1, -1};
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
    private static Color interpolateColorBarycentric(double lambda1, double lambda2, double lambda3,
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

    /**
     * Линейная интерполяция
     */
    private static double interpolate(double a, double b, double t) {
        return a + t * (b - a);
    }

    public static void fillTriangleTextured(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            double x1, double y1, double z1, double u1, double v1,
            double x2, double y2, double z2, double u2, double v2,
            double x3, double y3, double z3, double u3, double v3,
            Texture texture) {

        if (texture == null || !texture.isValid()) {
            return;
        }

        int minX = (int) Math.max(0, Math.floor(Math.min(x1, Math.min(x2, x3))));
        int maxX = (int) Math.min(zBuffer.getWidth() - 1,
                Math.ceil(Math.max(x1, Math.max(x2, x3))));
        int minY = (int) Math.max(0, Math.floor(Math.min(y1, Math.min(y2, y3))));
        int maxY = (int) Math.min(zBuffer.getHeight() - 1,
                Math.ceil(Math.max(y1, Math.max(y2, y3))));

        BarycentricConstants constants =
                new BarycentricConstants(x1, y1, x2, y2, x3, y3);

        if (constants.isDegenerate) {
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {

                double[] lambdas = computeBarycentricCoordinates(x, y, constants);
                double l1 = lambdas[0];
                double l2 = lambdas[1];
                double l3 = lambdas[2];

                if (l1 >= 0 && l2 >= 0 && l3 >= 0) {

                    double z = l1 * z1 + l2 * z2 + l3 * z3;

                    if (z < zBuffer.get(x, y)) {

                        double u = l1 * u1 + l2 * u2 + l3 * u3;
                        double v = l1 * v1 + l2 * v2 + l3 * v3;

                        Color color = texture.getColor(u, v);

                        pixelWriter.setColor(x, y, color);
                        zBuffer.set(x, y, z);
                    }
                }
            }
        }
    }

    /*public static void drawLineWithZBuffer(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            Color color) {

        // Приводим к целочисленным координатам для алгоритма Брезенхема
        int ix1 = (int)Math.round(x1);
        int iy1 = (int)Math.round(y1);
        int ix2 = (int)Math.round(x2);
        int iy2 = (int)Math.round(y2);

        int dx = Math.abs(ix2 - ix1);
        int dy = Math.abs(iy2 - iy1);

        int sx = (ix1 < ix2) ? 1 : -1;
        int sy = (iy1 < iy2) ? 1 : -1;

        int err = dx - dy;

        double currentX = x1;
        double currentY = y1;

        // Предварительные вычисления для интерполяции
        double totalLength = Math.sqrt(
                (x2 - x1) * (x2 - x1) +
                        (y2 - y1) * (y2 - y1)
        );

        boolean steep = dy > dx;

        while (true) {
            int ix = (int)Math.round(currentX);
            int iy = (int)Math.round(currentY);

            // Вычисляем t для линейной интерполяции Z
            double t = 0;
            if (totalLength > 0) {
                double currentLength = Math.sqrt(
                        (currentX - x1) * (currentX - x1) +
                                (currentY - y1) * (currentY - y1)
                );
                t = currentLength / totalLength;
            }

            double currentZ = z1 + t * (z2 - z1);

            // Проверяем границы буфера
            if (ix >= 0 && ix < zBuffer.getWidth() &&
                    iy >= 0 && iy < zBuffer.getHeight()) {

                // Проверяем глубину
                if (currentZ < zBuffer.get(ix, iy)) {
                    pixelWriter.setColor(ix, iy, color);
                    zBuffer.set(ix, iy, currentZ);
                }
            }

            // Проверяем достижение конечной точки
            if (Math.abs(ix - ix2) < 1 && Math.abs(iy - iy2) < 1) {
                break;
            }

            int e2 = err * 2;

            if (steep) {
                if (e2 > -dy) {
                    err -= dy;
                    currentX += sx;
                }
                if (e2 < dx) {
                    err += dx;
                    currentY += sy;
                }
            } else {
                if (e2 > -dy) {
                    err -= dy;
                    currentY += sy;
                }
                if (e2 < dx) {
                    err += dx;
                    currentX += sx;
                }
            }
        }
    }*/
    /**
     * Рисует линию с учетом Z-буфера
     */
    public static void drawLineWithZBuffer(
            PixelWriter pixelWriter,
            ZBuffer zBuffer,
            Vector3 start,
            Vector3 end,
            Color color) {

        drawLineWithZBuffer(pixelWriter, zBuffer,
                start.x, start.y, start.z,
                end.x, end.y, end.z,
                color);
    }

    /**
     * Рисует линию с учетом Z-буфера (алгоритм Брезенхема)
     */
    private static void drawLineWithZBuffer(
            PixelWriter pixelWriter,
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

        int sx = (ix1 < ix2) ? 1 : -1;
        int sy = (iy1 < iy2) ? 1 : -1;

        int err = dx - dy;

        double currentX = x1;
        double currentY = y1;

        // Длина линии для интерполяции Z
        double lineLength = Math.sqrt(
                (x2 - x1) * (x2 - x1) +
                        (y2 - y1) * (y2 - y1)
        );

        while (true) {
            int ix = (int) Math.round(currentX);
            int iy = (int) Math.round(currentY);

            // Интерполируем Z
            double t = 0;
            if (lineLength > 0) {
                double currentDist = Math.sqrt(
                        (currentX - x1) * (currentX - x1) +
                                (currentY - y1) * (currentY - y1)
                );
                t = currentDist / lineLength;
            }

            double currentZ = z1 + t * (z2 - z1);

            // Проверяем границы и глубину
            if (ix >= 0 && ix < zBuffer.getWidth() &&
                    iy >= 0 && iy < zBuffer.getHeight() &&
                    currentZ < zBuffer.get(ix, iy)) {

                pixelWriter.setColor(ix, iy, color);
                zBuffer.set(ix, iy, currentZ);
            }

            // Проверяем достижение конечной точки
            if (Math.abs(ix - ix2) < 1 && Math.abs(iy - iy2) < 1) {
                break;
            }

            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                currentX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currentY += sy;
            }
        }
    }
}