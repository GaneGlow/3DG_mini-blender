package com.cgvsu.rasterization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZBufferTest {

    @Test
    void testConstructor() {
        ZBuffer zBuffer = new ZBuffer(100, 200);
        assertEquals(100, zBuffer.getWidth());
        assertEquals(200, zBuffer.getHeight());
    }

    @Test
    void testInitialValuesArePositiveInfinity() {
        ZBuffer zBuffer = new ZBuffer(10, 10);

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                assertEquals(Double.POSITIVE_INFINITY, zBuffer.get(x, y));
            }
        }
    }

    @Test
    void testSetAndGet() {
        ZBuffer zBuffer = new ZBuffer(20, 30);

        zBuffer.set(5, 10, 0.5);
        zBuffer.set(15, 25, -1.2);

        assertEquals(0.5, zBuffer.get(5, 10), 1e-10);
        assertEquals(-1.2, zBuffer.get(15, 25), 1e-10);

        assertEquals(Double.POSITIVE_INFINITY, zBuffer.get(0, 0));
    }

    @Test
    void testClear() {
        ZBuffer zBuffer = new ZBuffer(5, 5);

        zBuffer.set(2, 2, 0.1);
        zBuffer.set(3, 3, 0.2);

        zBuffer.clear();

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                assertEquals(Double.POSITIVE_INFINITY, zBuffer.get(x, y));
            }
        }
    }

}