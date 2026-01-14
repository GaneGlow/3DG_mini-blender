package com.cgvsu.model;

import com.cgvsu.triangulation.Triangulator;

public final class ModelPreparationUtils {

    private ModelPreparationUtils() {}

    public static TriangulatedModel prepare(final Model raw) {
        TriangulatedModel triangulated = Triangulator.triangulate(raw);
        triangulated.recalculateNormals();
        return triangulated;
    }
}
