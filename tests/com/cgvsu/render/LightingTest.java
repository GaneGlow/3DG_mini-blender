package com.cgvsu.render;

import com.cgvsu.math.Vector3;
import com.cgvsu.render_engine.Lighting;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LightingTest {

    private static final float EPS = 1e-6f;

    private static void assertColorEquals(Color expected, Color actual, double eps) {
        assertEquals(expected.getRed(), actual.getRed(), eps, "red");
        assertEquals(expected.getGreen(), actual.getGreen(), eps, "green");
        assertEquals(expected.getBlue(), actual.getBlue(), eps, "blue");
        assertEquals(expected.getOpacity(), actual.getOpacity(), eps, "opacity");
    }

    @Test
    void calculateLightingCoefficient_alignedVectors_returnsOne() {
        Vector3 n = new Vector3(0, 0, 1);
        Vector3 lightDir = new Vector3(0, 0, 10); // не нормализован специально

        float c = Lighting.calculateLightingCoefficient(n, lightDir);

        assertEquals(1.0f, c, EPS);
    }

    @Test
    void calculateLightingCoefficient_oppositeVectors_returnsZero() {
        Vector3 n = new Vector3(0, 0, 1);
        Vector3 lightDir = new Vector3(0, 0, -1);

        float c = Lighting.calculateLightingCoefficient(n, lightDir);

        assertEquals(0.0f, c, EPS);
    }

    @Test
    void calculateLightingCoefficient_45deg_isCos45() {
        Vector3 n = new Vector3(0, 0, 1);
        Vector3 lightDir = new Vector3(0, 1, 1); // угол 45° к оси Z

        float c = Lighting.calculateLightingCoefficient(n, lightDir);

        float expected = (float)(1.0 / Math.sqrt(2.0));
        assertEquals(expected, c, 1e-5f);
    }

    @Test
    void applySmoothLighting_whenFullyLit_returnsBaseColor() {
        // при coefficient=1 и intensity=1: ambient=0.2 => итоговая intensity=1
        // значит цвет не должен измениться
        Color base = new Color(0.5, 0.2, 0.1, 1.0);

        Lighting.Light light = new Lighting.Light(new Vector3(0, 0, 1), Color.WHITE, 1.0f);
        Vector3 normal = new Vector3(0, 0, 1);

        Color out = Lighting.applySmoothLighting(base, normal, light);

        assertColorEquals(base, out, 1e-9);
    }

    @Test
    void applySmoothLighting_whenNoDiffuse_onlyAmbientRemains() {
        // coefficient=0 => intensity = ambient = 0.2
        Color base = new Color(0.5, 0.2, 0.1, 1.0);

        Lighting.Light light = new Lighting.Light(new Vector3(0, 0, -1), Color.WHITE, 1.0f);
        Vector3 normal = new Vector3(0, 0, 1);

        Color out = Lighting.applySmoothLighting(base, normal, light);

        Color expected = new Color(0.5 * 0.2, 0.2 * 0.2, 0.1 * 0.2, 1.0);
        assertColorEquals(expected, out, 1e-8);
    }

    @Test
    void createCameraLight_buildsNormalizedDirection() {
        Vector3 cam = new Vector3(10, 0, 0);
        Vector3 target = new Vector3(0, 0, 0);

        Lighting.Light light = Lighting.createCameraLight(cam, target);

        Vector3 expectedDir = cam.subtract(target).normalized();
        assertTrue(light.direction.approxEquals(expectedDir, 1e-6f));
        assertEquals(1.0f, light.intensity, EPS);
        assertEquals(Color.WHITE, light.color);
    }

    @Test
    void interpolateNormal_returnsNormalizedWeightedSum() {
        Vector3 n1 = new Vector3(1, 0, 0);
        Vector3 n2 = new Vector3(0, 1, 0);
        Vector3 n3 = new Vector3(0, 0, 1);

        Vector3 out = Lighting.interpolateNormal(n1, n2, n3, 0.5f, 0.5f, 0.0f);
        // до нормализации это (0.5, 0.5, 0)
        Vector3 expected = new Vector3(0.5f, 0.5f, 0.0f).normalized();

        assertTrue(out.approxEquals(expected, 1e-6f));
        assertEquals(1.0f, out.length(), 1e-6f);
    }
}
