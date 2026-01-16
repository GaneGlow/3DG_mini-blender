package com.cgvsu.rasterization;

import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RasterizationLineBresenhamTest {

    private static Method DRAW_LINE_PRIVATE;

    @BeforeAll
    static void lookupPrivateMethod() throws Exception {
        DRAW_LINE_PRIVATE = Rasterization.class.getDeclaredMethod(
                "drawLineWithZBuffer",
                PixelWriter.class,
                ZBuffer.class,
                double.class, double.class, double.class,
                double.class, double.class, double.class,
                Color.class
        );
        DRAW_LINE_PRIVATE.setAccessible(true);
    }

    private static void drawLine(
            PixelWriter pw, ZBuffer zb,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            Color color
    ) {
        try {
            DRAW_LINE_PRIVATE.invoke(null, pw, zb, x1, y1, z1, x2, y2, z2, color);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    @Test
    void singlePoint_drawsExactlyOnePixel() {
        ZBuffer zb = new ZBuffer(10, 10);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        drawLine(pw, zb, 3, 4, 7, 3, 4, 7, Color.RED);

        assertEquals(1, pw.totalWrites());
        assertEquals(Set.of(new RecordingPixelWriter.Pixel(3, 4)), pw.writtenPixels());
        assertColorEquals(Color.RED, pw.get(3, 4));
        assertEquals(7.0, zb.get(3, 4), 1e-12);
    }

    @Test
    void horizontalLine_includesEndpoints_andInterpolatesZ() {
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        drawLine(pw, zb, 1, 5, 10, 6, 5, 4, Color.BLUE);

        List<RecordingPixelWriter.Pixel> expected = List.of(
                new RecordingPixelWriter.Pixel(1, 5),
                new RecordingPixelWriter.Pixel(2, 5),
                new RecordingPixelWriter.Pixel(3, 5),
                new RecordingPixelWriter.Pixel(4, 5),
                new RecordingPixelWriter.Pixel(5, 5),
                new RecordingPixelWriter.Pixel(6, 5)
        );
        assertEquals(expected, pw.writeOrder(), "Bresenham pixel path mismatch");

        // Z: steps=5, dz=(4-10)/5=-1.2
        double[] expectedZ = {10.0, 8.8, 7.6, 6.4, 5.2, 4.0};
        for (int i = 0; i < expected.size(); i++) {
            var p = expected.get(i);
            assertColorEquals(Color.BLUE, pw.get(p.x, p.y));
            assertEquals(expectedZ[i], zb.get(p.x, p.y), 1e-12, "Z mismatch at " + p);
        }
    }

    @Test
    void verticalLine_includesEndpoints() {
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        drawLine(pw, zb, 3, 1, 0, 3, 6, 10, Color.GREEN);

        List<RecordingPixelWriter.Pixel> expected = List.of(
                new RecordingPixelWriter.Pixel(3, 1),
                new RecordingPixelWriter.Pixel(3, 2),
                new RecordingPixelWriter.Pixel(3, 3),
                new RecordingPixelWriter.Pixel(3, 4),
                new RecordingPixelWriter.Pixel(3, 5),
                new RecordingPixelWriter.Pixel(3, 6)
        );
        assertEquals(expected, pw.writeOrder());
    }

    @Test
    void diagonalSlopePlusOne() {
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        drawLine(pw, zb, 1, 1, 0, 6, 6, 0, Color.BLACK);

        List<RecordingPixelWriter.Pixel> expected = List.of(
                new RecordingPixelWriter.Pixel(1, 1),
                new RecordingPixelWriter.Pixel(2, 2),
                new RecordingPixelWriter.Pixel(3, 3),
                new RecordingPixelWriter.Pixel(4, 4),
                new RecordingPixelWriter.Pixel(5, 5),
                new RecordingPixelWriter.Pixel(6, 6)
        );
        assertEquals(expected, pw.writeOrder());
    }

    @Test
    void diagonalNegativeSlope() {
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        drawLine(pw, zb, 1, 6, 0, 6, 1, 0, Color.ORANGE);

        List<RecordingPixelWriter.Pixel> expected = List.of(
                new RecordingPixelWriter.Pixel(1, 6),
                new RecordingPixelWriter.Pixel(2, 5),
                new RecordingPixelWriter.Pixel(3, 4),
                new RecordingPixelWriter.Pixel(4, 3),
                new RecordingPixelWriter.Pixel(5, 2),
                new RecordingPixelWriter.Pixel(6, 1)
        );
        assertEquals(expected, pw.writeOrder());
    }

    @Test
    void steepLine_octant2_pixelsMatchExactly() {
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        // dx=2 dy=5
        drawLine(pw, zb, 2, 1, 0, 4, 6, 0, Color.PURPLE);

        List<RecordingPixelWriter.Pixel> expected = List.of(
                new RecordingPixelWriter.Pixel(2, 1),
                new RecordingPixelWriter.Pixel(2, 2),
                new RecordingPixelWriter.Pixel(3, 3),
                new RecordingPixelWriter.Pixel(3, 4),
                new RecordingPixelWriter.Pixel(4, 5),
                new RecordingPixelWriter.Pixel(4, 6)
        );
        assertEquals(expected, pw.writeOrder());
    }

    @Test
    void shallowLine_octant1_pixelsMatchExactly() {
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        // dx=5 dy=2
        drawLine(pw, zb, 1, 2, 0, 6, 4, 0, Color.BROWN);

        List<RecordingPixelWriter.Pixel> expected = List.of(
                new RecordingPixelWriter.Pixel(1, 2),
                new RecordingPixelWriter.Pixel(2, 2),
                new RecordingPixelWriter.Pixel(3, 3),
                new RecordingPixelWriter.Pixel(4, 3),
                new RecordingPixelWriter.Pixel(5, 4),
                new RecordingPixelWriter.Pixel(6, 4)
        );
        assertEquals(expected, pw.writeOrder());
    }

    @Test
    void respectsZBuffer_doesNotOverwriteWithFartherLine() {
        ZBuffer zb = new ZBuffer(20, 20);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        // Ближняя (z=0)
        drawLine(pw, zb, 1, 5, 0, 6, 5, 0, Color.RED);
        int writesAfterNear = pw.totalWrites();

        // Дальняя (z=10) — НЕ должна перерисовать
        drawLine(pw, zb, 1, 5, 10, 6, 5, 10, Color.BLUE);

        assertEquals(writesAfterNear, pw.totalWrites(), "Farther line should not draw any pixel");

        for (int x = 1; x <= 6; x++) {
            assertColorEquals(Color.RED, pw.get(x, 5));
            assertEquals(0.0, zb.get(x, 5), 1e-12);
            assertEquals(1, pw.writeCount(x, 5), "Should be written exactly once");
        }
    }

    @Test
    void clipsToBounds_onlyInRangePixelsAreWritten() {
        ZBuffer zb = new ZBuffer(5, 5);
        RecordingPixelWriter pw = new RecordingPixelWriter();

        // Линия (-2,2)->(2,2) пересекает границу
        drawLine(pw, zb, -2, 2, 0, 2, 2, 4, Color.CYAN);

        // Внутри буфера должны быть только x=0..2
        Set<RecordingPixelWriter.Pixel> expected = Set.of(
                new RecordingPixelWriter.Pixel(0, 2),
                new RecordingPixelWriter.Pixel(1, 2),
                new RecordingPixelWriter.Pixel(2, 2)
        );

        assertEquals(expected, pw.writtenPixels());

        // Проверим Z (ожидаем z=2,3,4 на x=0,1,2)
        assertEquals(2.0, zb.get(0, 2), 1e-12);
        assertEquals(3.0, zb.get(1, 2), 1e-12);
        assertEquals(4.0, zb.get(2, 2), 1e-12);
    }
}
