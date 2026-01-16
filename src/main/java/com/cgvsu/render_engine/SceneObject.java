package com.cgvsu.render_engine;

import com.cgvsu.model.Model;
import javafx.scene.paint.Color;

public class SceneObject {
    private String name;
    private Model model;
    private Texture texture;
    private boolean visible = true;

    // Добавляем цвета для отображения
    private Color wireframeColor = Color.WHITE;
    private Color modelColor = Color.GRAY;

    private RenderSettings renderSettings = null;

    public SceneObject(String name, Model model, Texture texture) {
        this.name = name;
        this.model = model;
        this.texture = texture;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Model getModel() { return model; }
    public void setModel(Model model) { this.model = model; }

    public Texture getTexture() { return texture; }
    public void setTexture(Texture texture) { this.texture = texture; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public Color getWireframeColor() { return wireframeColor; }
    public void setWireframeColor(Color wireframeColor) { this.wireframeColor = wireframeColor; }

    public Color getModelColor() { return modelColor; }
    public void setModelColor(Color modelColor) { this.modelColor = modelColor; }

    // Методы для настроек рендеринга
    public RenderSettings getRenderSettings() {
        return renderSettings;
    }

    public void setRenderSettings(RenderSettings settings) {
        this.renderSettings = settings;
    }

    public void applyRenderSettings(RenderSettings settings) {
        if (this.renderSettings == null) {
            this.renderSettings = new RenderSettings();
        }

        // Копируем только выбранные настройки
        this.renderSettings.drawWireframe = settings.drawWireframe;
        this.renderSettings.useTexture = settings.useTexture;
        this.renderSettings.useLighting = settings.useLighting;
        // Можно добавить копирование других настроек
    }

    // Сброс настроек объекта к глобальным
    public void resetRenderSettings() {
        this.renderSettings = null;
    }

    // Проверка, есть ли у объекта свои настройки
    public boolean hasCustomSettings() {
        return this.renderSettings != null;
    }
}