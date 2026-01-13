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
        fillTriangleBarycentric(pixelWriter, x1, y1, x2, y2, x3, y3, color1, color2, color3);
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

        BarycentricConstants constants = new BarycentricConstants(x1, y1, x2, y2, x3, y3);;

        if (constants.isDegenerate) {
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double[] lambdas = computeBarycentricCoordinates(x, y, constants);

                if (lambdas[0] >= 0 && lambdas[1] >= 0 && lambdas[2] >= 0) {
                    Color color = interpolateColorBarycentric(lambdas[0], lambdas[1], lambdas[2],
                            color1, color2, color3);
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
    private static double[] computeBarycentricCoordinates(int x, int y, BarycentricConstants constants) {
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



}