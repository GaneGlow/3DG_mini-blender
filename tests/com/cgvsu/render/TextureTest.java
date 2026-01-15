package com.cgvsu.render;

import com.cgvsu.render_engine.Texture;
import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureTest {

    @BeforeAll
    static void initJavaFx() {
        // На некоторых сборках JavaFX в тестах нужен запуск платформы.
        // Если у вас и так всё работает — этот блок не мешает.
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // уже запущена
        }
    }

    @Test
    void getColor_mapsCorners_withVFlip_andUsesFullWidthHeightRange() {
        WritableImage img = new WritableImage(4, 4);
        PixelWriter w = img.getPixelWriter();

        // y=0 — верхняя строка картинки (JavaFX)
        w.setColor(0, 0, Color.RED);      // top-left
        w.setColor(3, 0, Color.GREEN);    // top-right
        w.setColor(0, 3, Color.BLUE);     // bottom-left
        w.setColor(3, 3, Color.YELLOW);   // bottom-right

        Texture tex = new Texture(img);

        // v=0 -> после v=1-v попадаем в низ (bottom)
        assertEquals(Color.BLUE,   tex.getColor(0.0,   0.0));
        assertEquals(Color.YELLOW, tex.getColor(0.999, 0.0));

        // v≈1 -> после v=1-v попадаем в верх (top)
        assertEquals(Color.RED,    tex.getColor(0.0,   0.999));
        assertEquals(Color.GREEN,  tex.getColor(0.999, 0.999));
    }

    @Test
    void getColor_wrapsU_andNegativeU() {
        WritableImage img = new WritableImage(4, 4);
        PixelWriter w = img.getPixelWriter();

        // Ставим "маячок" в (x=1, y=0) = верхняя строка, 2-й столбец
        w.setColor(1, 0, Color.PURPLE);

        Texture tex = new Texture(img);

        // v≈1 -> y=0 (верх)
        // u=0.3  -> x=floor(0.3*4)=1
        assertEquals(Color.PURPLE, tex.getColor(0.3, 0.999));

        // wrap: 1.3 -> 0.3
        assertEquals(Color.PURPLE, tex.getColor(1.3, 0.999));

        // wrap: -0.7 -> 0.3 (т.к. -0.7 - floor(-0.7) = -0.7 - (-1) = 0.3)
        assertEquals(Color.PURPLE, tex.getColor(-0.7, 0.999));
    }

    @Test
    void getColor_wrapsV_andNegativeV() {
        WritableImage img = new WritableImage(4, 4);
        PixelWriter w = img.getPixelWriter();

        // Пиксель (x=0, y=2) — 3-я строка сверху
        w.setColor(0, 2, Color.ORANGE);

        Texture tex = new Texture(img);

        // Хотим получить y=2.
        // После flip: v' = 1 - v.
        // y=floor(v' * 4) => чтобы y=2, нужно v' в [0.5..0.75), например v'=0.6 => v=0.4
        assertEquals(Color.ORANGE, tex.getColor(0.0, 0.4));

        // wrap: 1.4 -> 0.4
        assertEquals(Color.ORANGE, tex.getColor(0.0, 1.4));

        // wrap: -0.6 -> 0.4
        assertEquals(Color.ORANGE, tex.getColor(0.0, -0.6));
    }
}
