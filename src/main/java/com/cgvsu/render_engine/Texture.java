package com.cgvsu.render_engine;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class Texture {
    private final Image image;
    private final int width;
    private final int height;
    private final PixelReader pixelReader;

    public Texture(Image image) {
        this.image = image;
        this.width = (int) image.getWidth();
        this.height = (int) image.getHeight();
        this.pixelReader = image.getPixelReader();
    }

    public Color getColor(double u, double v) {

        // WRAP координат (а не clamp!)
        u = u - Math.floor(u);
        v = v - Math.floor(v);

        // Инверсия V (OBJ -> Image)
        v = 1.0 - v;

        int x = (int) (u * (width - 1));
        int y = (int) (v * (height - 1));

        // Защита от выхода за границы
        x = Math.max(0, Math.min(width  - 1, x));
        y = Math.max(0, Math.min(height - 1, y));

        return pixelReader.getColor(x, y);
    }


    public boolean isValid() {
        return image != null;
    }
}