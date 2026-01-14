package com.cgvsu.render_engine;

import javafx.scene.paint.Color;
import com.cgvsu.math.Vector3;

public class Lighting {

    /**
     * Источник света.
     * Содержит направление (для направленного света) и цвет.
     */
    public static class Light {
        public final Vector3 direction; // Направление света (должно быть нормализовано)
        public final Color color;
        public final float intensity;

        public Light(Vector3 direction, Color color, float intensity) {
            this.direction = direction.normalized();
            this.color = color;
            this.intensity = intensity;
        }

        public Light(Vector3 direction, Color color) {
            this(direction, color, 1.0f);
        }
    }

    /**
     * Вычисляет коэффициент освещенности по методичке (страница 10).
     * Яркость цвета в точке зависит от того, под каким углом на неё падает луч света.
     *
     * Формула из методички:
     * l = -n⃗ · r⃗ a y
     * где n⃗ - нормаль в точке, r⃗ - направление луча света
     *
     * После нормализации получаем коэффициент l в диапазоне [0, 1]
     * Если l отрицательный, это означает, что луч приходит изнутри модели
     */
    public static float calculateLightingCoefficient(Vector3 normal, Vector3 lightDirection) {
        // Нормализуем векторы
        normal = normal.normalized();
        lightDirection = lightDirection.normalized();

        // Скалярное произведение (косинус угла между нормалью и направлением света)
        float dotProduct = normal.dot(lightDirection);

        // Согласно методичке, если dotProduct отрицательный, это означает,
        // что луч приходит изнутри модели
        if (dotProduct < 0) {
            return 0; // Не освещаем
        }

        return dotProduct;
    }

    /**
     * Простейшая модель освещения согласно методичке.
     * Яркость цвета = базовый цвет * коэффициент освещенности.
     */
    public static Color applySimpleLighting(Color baseColor, Vector3 normal, Light light) {
        float coefficient = calculateLightingCoefficient(normal, light.direction);

        // Применяем интенсивность света
        coefficient *= light.intensity;

        // Создаем новый цвет с учетом освещения
        return new Color(
                Math.min(1.0, baseColor.getRed() * coefficient),
                Math.min(1.0, baseColor.getGreen() * coefficient),
                Math.min(1.0, baseColor.getBlue() * coefficient),
                baseColor.getOpacity()
        );
    }

    /**
     * Улучшенная модель освещения с фоном (ambient lighting).
     * Согласно методичке, в реальном мире всегда есть фоновое освещение.
     *
     * Формула из методички:
     * rgb′ = rgb(1 - k) + rgb * k * l
     * где k - коэффициент от 0 до 1, показывает долю света для настройки тени
     */
    public static Color applyLightingWithAmbient(Color baseColor, Vector3 normal, Light light, float ambientCoefficient) {
        // Ограничиваем коэффициент фона от 0 до 1
        ambientCoefficient = Math.max(0, Math.min(1, ambientCoefficient));

        // Вычисляем коэффициент освещенности
        float lightingCoefficient = calculateLightingCoefficient(normal, light.direction);

        // Формула из методички:
        // rgb′ = rgb(1 - k) + rgb * k * l
        // где (1 - k) - фоновая неизменная яркость, k*l - освещенная часть

        float k = light.intensity;
        float l = lightingCoefficient;

        // Если l = 0 (точка не освещена), остается только фоновая составляющая
        if (l <= 0) {
            l = 0;
        }

        // Вычисляем итоговые компоненты цвета
        double r = baseColor.getRed() * (1 - k + k * l);
        double g = baseColor.getGreen() * (1 - k + k * l);
        double b = baseColor.getBlue() * (1 - k + k * l);

        // Добавляем фоновое освещение
        r = Math.min(1.0, r + baseColor.getRed() * ambientCoefficient);
        g = Math.min(1.0, g + baseColor.getGreen() * ambientCoefficient);
        b = Math.min(1.0, b + baseColor.getBlue() * ambientCoefficient);

        // Ограничиваем значения
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
        // Простая модель с небольшим фоновым освещением для плавности
        float ambient = 0.2f;
        float coefficient = calculateLightingCoefficient(normal, light.direction);

        // Комбинируем фоновое и диффузное освещение
        float intensity = ambient + (1 - ambient) * coefficient * light.intensity;

        // Ограничиваем интенсивность
        intensity = Math.max(0, Math.min(1, intensity));

        return new Color(
                Math.min(1.0, baseColor.getRed() * intensity),
                Math.min(1.0, baseColor.getGreen() * intensity),
                Math.min(1.0, baseColor.getBlue() * intensity),
                baseColor.getOpacity()
        );
    }

    /**
     * Создает источник света, привязанный к камере (как в методичке).
     * Направление света - от камеры к объекту.
     */
    public static Light createCameraLight(Vector3 cameraPosition, Vector3 targetPosition) {
        // Направление от объекта к камере
        Vector3 direction = cameraPosition.subtract(targetPosition).normalized();

        // Белый свет полной интенсивности
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