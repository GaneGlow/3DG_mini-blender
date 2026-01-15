package com.cgvsu.render_engine;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class RenderSettings {
    public boolean drawWireframe = false;
    public boolean useTexture = false;
    public boolean useLighting = false;

    public Color baseColor = Color.GRAY;
    //public Image texture = null;

    /*public RenderSettings copy() {
        RenderSettings copy = new RenderSettings();
        copy.drawWireframe = this.drawWireframe;
        copy.useTexture = this.useTexture;
        copy.useLighting = this.useLighting;
        copy.baseColor = this.baseColor;
        return copy;
    }*/
}
