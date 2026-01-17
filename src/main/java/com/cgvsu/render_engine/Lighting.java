package com.cgvsu.render_engine;

import javafx.scene.paint.Color;
import com.cgvsu.math.Vector3;

public class Lighting {

    public static class Light {
        public final Vector3 direction;
        public final Color color;
        public final float intensity;

        public Light(Vector3 direction, Color color, float intensity) {
            this.direction = direction.normalized();
            this.color = color;
            this.intensity = intensity;
        }

    }

    public static float calculateLightingCoefficient(Vector3 normal, Vector3 lightDirection) {
        normal = normal.normalized();
        lightDirection = lightDirection.normalized();

        float dotProduct = normal.dot(lightDirection);

        if (dotProduct < 0) {
            return 0;
        }

        return dotProduct;
    }

    public static Color applySimpleLighting(Color baseColor, Vector3 normal, Light light) {
        float coefficient = calculateLightingCoefficient(normal, light.direction);

        coefficient *= light.intensity;

        return new Color(
                Math.min(1.0, baseColor.getRed() * coefficient),
                Math.min(1.0, baseColor.getGreen() * coefficient),
                Math.min(1.0, baseColor.getBlue() * coefficient),
                baseColor.getOpacity()
        );
    }

    public static Color applyLightingWithAmbient(Color baseColor, Vector3 normal, Light light, float ambientCoefficient) {
        ambientCoefficient = Math.max(0, Math.min(1, ambientCoefficient));

        float lightingCoefficient = calculateLightingCoefficient(normal, light.direction);
        float k = light.intensity;
        float l = lightingCoefficient;

        if (l <= 0) {
            l = 0;
        }

        double r = baseColor.getRed() * (1 - k + k * l);
        double g = baseColor.getGreen() * (1 - k + k * l);
        double b = baseColor.getBlue() * (1 - k + k * l);

        r = Math.min(1.0, r + baseColor.getRed() * ambientCoefficient);
        g = Math.min(1.0, g + baseColor.getGreen() * ambientCoefficient);
        b = Math.min(1.0, b + baseColor.getBlue() * ambientCoefficient);

        r = Math.max(0, Math.min(1, r));
        g = Math.max(0, Math.min(1, g));
        b = Math.max(0, Math.min(1, b));

        return new Color(r, g, b, baseColor.getOpacity());
    }

    /**
     * Освещение с нормалями вершин (сглаживание по Гуро).
     * Использует интерполированные нормали для плавного освещения.
     */
    public static Color applySmoothLighting(Color baseColor, Vector3 normal, Light light) {
        float ambient = 0.2f;
        float coefficient = calculateLightingCoefficient(normal, light.direction);

        float intensity = ambient + (1 - ambient) * coefficient * light.intensity;

        intensity = Math.max(0, Math.min(1, intensity));

        return new Color(
                Math.min(1.0, baseColor.getRed() * intensity),
                Math.min(1.0, baseColor.getGreen() * intensity),
                Math.min(1.0, baseColor.getBlue() * intensity),
                baseColor.getOpacity()
        );
    }

    public static Light createCameraLight(Vector3 cameraPosition, Vector3 targetPosition) {
        Vector3 direction = cameraPosition.subtract(targetPosition).normalized();

        return new Light(direction, Color.WHITE, 1.0f);
    }

    /**
     * Создает направленный источник света (например, солнце).
     */
    public static Light createDirectionalLight(Vector3 direction, Color color, float intensity) {
        return new Light(direction, color, intensity);
    }

    /**
     * Интерполирует нормали вершин для текущей точки.
     * Используется для сглаживания нормалей (smooth shading).
     */
    public static Vector3 interpolateNormal(Vector3 n1, Vector3 n2, Vector3 n3,
                                            float lambda1, float lambda2, float lambda3) {
        return new Vector3(
                lambda1 * n1.x + lambda2 * n2.x + lambda3 * n3.x,
                lambda1 * n1.y + lambda2 * n2.y + lambda3 * n3.y,
                lambda1 * n1.z + lambda2 * n2.z + lambda3 * n3.z
        ).normalized();
    }
}