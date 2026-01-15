package com.cgvsu.rasterization;

import com.cgvsu.render_engine.Texture;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RasterizationTexturedTriangleTest {

    @BeforeAll
    static void initJavaFxToolkitIfNeeded() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
        } catch (Throwable t) {

        }
    }

    private static void assertColorEquals(Color expected, Color actual) {
        assertNotNull(actual, "Color is null (pixel was not drawn)");
        double eps = 1e-9;
        assertEquals(expected.getRed(), actual.getRed(), eps);
        assertEquals(expected.getGreen(), actual.getGreen(), eps);
        assertEquals(expected.getBlue(), actual.getBlue(), eps);
        assertEquals(expected.getOpacity(), actual.getOpacity(), eps);
    }

    private static Texture constantTexture(Color c) {
        WritableImage img = new WritableImage(1, 1);
        img.getPixelWriter().setColor(0, 0, c);
        return new Texture(img);
    }

    private static Texture gradientTexture(int w, int h) {
        WritableImage img = new WritableImage(w, h);
        var pw = img.getPixelWriter();

        // Уникальный цвет на каждый (x,y): r = x/(w-1), g = y/(h-1)
        // (b=0, a=1). Так проще проверять точность выбора texel.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double r = (w <= 1) ? 0.0 : (x / (double) (w - 1));
                double g = (h <= 1) ? 0.0 : (y / (double) (h - 1));
                pw.setColor(x, y, new Color(r, g, 0.0, 1.0));
            }
        }
        return new Texture(img);
    }

    @Test
    void fillTriangleTextured_fillsExactIntegerPixels_forRightTriangle() {
        ZBuffer zb = new ZBuffer(10, 10);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        Texture tex = constantTexture(Color.RED);

        // Простой прямоугольный треугольник:
        // (0,0), (4,0), (0,4)
        Rasterization.fillTriangleTextured(
                pw, zb,
                0, 0, 0,   0, 0,
                4, 0, 0,   1, 0,
                0, 4, 0,   0, 1,
                tex
        );

        // Ожидаем ровно все целочисленные точки с x>=0,y>=0,x+y<=4
        Set<RecordingPixelWriter.Pixel> expected = new HashSet<>();
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 4 - y; x++) {
                expected.add(new RecordingPixelWriter.Pixel(x, y));
            }
        }

        assertEquals(expected, pw.writtenPixels(), "Triangle coverage mismatch (holes or extra pixels?)");
        assertEquals(15, pw.writtenPixels().size()); // (N+1)(N+2)/2 = 15 для N=4

        // И цвета должны быть красные (текстура 1x1)
        for (RecordingPixelWriter.Pixel p : expected) {
            assertColorEquals(Color.RED, pw.get(p.x, p.y));
            assertEquals(0.0, zb.get(p.x, p.y), 1e-12);
        }

        // Вне треугольника — ничего не рисуем
        assertNull(pw.get(4, 4));
        assertNull(pw.get(3, 3)); // x+y=6>4
    }

    @Test
    void fillTriangleTextured_uvInterpolation_matchesExpectedColors_atSelectedPoints() {
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        Texture tex = gradientTexture(16, 16);

        // Треугольник (0,0)-(6,0)-(0,6)
        // UV задаём так, чтобы не попадать ровно в 1.0 (из-за wrap в Texture.getColor)
        double UMAX = 0.999;
        double VMAX = 0.999;

        Rasterization.fillTriangleTextured(
                pw, zb,
                0, 0, 0,   0,    0,
                6, 0, 0,   UMAX, 0,
                0, 6, 0,   0,    VMAX,
                tex
        );

        // Для такого треугольника внутри: u = (x/6)*UMAX, v=(y/6)*VMAX
        // Проверяем несколько точек по цветам.
        int[][] points = {
                {0, 0},
                {3, 1},
                {1, 3},
                {3, 3}, // на гипотенузе (x+y=6) — должна считаться внутри (>=0)
                {6, 0}, // вершина
                {0, 6}  // вершина
        };

        for (int[] p : points) {
            int x = p[0];
            int y = p[1];

            double u = (x / 6.0) * UMAX;
            double v = (y / 6.0) * VMAX;

            Color expected = tex.getColor(u, v);
            Color actual = pw.get(x, y);

            assertColorEquals(expected, actual);
        }

        // Точно вне треугольника
        assertNull(pw.get(6, 6));
    }

    @Test
    void fillTriangleTextured_respectsZBuffer_nearTriangleOverwritesFarTriangle() {
        // 1) Дальний (z=10) красный
        // 2) Ближний (z=5) зелёный -> должен перекрыть
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        Texture red = constantTexture(Color.RED);
        Texture green = constantTexture(Color.LIME);

        Rasterization.fillTriangleTextured(
                pw, zb,
                0, 0, 10,  0, 0,
                4, 0, 10,  1, 0,
                0, 4, 10,  0, 1,
                red
        );

        Rasterization.fillTriangleTextured(
                pw, zb,
                0, 0, 5,   0, 0,
                4, 0, 5,   1, 0,
                0, 4, 5,   0, 1,
                green
        );

        // Точка точно внутри
        assertColorEquals(Color.LIME, pw.get(1, 1));
        assertEquals(5.0, zb.get(1, 1), 1e-12);
        assertTrue(pw.writeCount(1, 1) >= 2, "Should be overwritten by nearer triangle");
    }

    @Test
    void fillTriangleTextured_respectsZBuffer_farTriangleDoesNotOverwriteNearTriangle() {
        // 1) Ближний (z=5) зелёный
        // 2) Дальний (z=10) красный -> НЕ должен перекрыть
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        Texture red = constantTexture(Color.RED);
        Texture green = constantTexture(Color.LIME);

        Rasterization.fillTriangleTextured(
                pw, zb,
                0, 0, 5,   0, 0,
                4, 0, 5,   1, 0,
                0, 4, 5,   0, 1,
                green
        );

        int writesAfterNear = pw.totalWrites();

        Rasterization.fillTriangleTextured(
                pw, zb,
                0, 0, 10,  0, 0,
                4, 0, 10,  1, 0,
                0, 4, 10,  0, 1,
                red
        );

        assertEquals(writesAfterNear, pw.totalWrites(), "Far triangle should not draw any pixel over near triangle");
        assertColorEquals(Color.LIME, pw.get(1, 1));
        assertEquals(5.0, zb.get(1, 1), 1e-12);
        assertEquals(1, pw.writeCount(1, 1), "Near pixel should be written exactly once");
    }

    @Test
    void fillTriangleTextured_doesNothing_whenTextureIsNull() {
        ZBuffer zb = new ZBuffer(10, 10);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        Rasterization.fillTriangleTextured(
                pw, zb,
                0, 0, 0,  0, 0,
                4, 0, 0,  1, 0,
                0, 4, 0,  0, 1,
                null
        );

        assertEquals(0, pw.totalWrites());
    }
}
