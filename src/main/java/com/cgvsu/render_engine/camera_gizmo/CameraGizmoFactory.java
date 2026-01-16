package com.cgvsu.render_engine.camera_gizmo;

import com.cgvsu.math.Vector3;
import com.cgvsu.model.Model;
import com.cgvsu.model.Polygon;

public final class CameraGizmoFactory {

    private CameraGizmoFactory() {}

    public static Model create(float size) {
        Model m = new Model();
        float s = Math.max(size, 0.001f);

        // Основание
        m.vertices.add(new Vector3(-s, -s, 0));
        m.vertices.add(new Vector3( s, -s, 0));
        m.vertices.add(new Vector3( s,  s, 0));
        m.vertices.add(new Vector3(-s,  s, 0));

        // Вершина
        m.vertices.add(new Vector3(0, 0, 2 * s));

        m.polygons.add(tri(0,1,4));
        m.polygons.add(tri(1,2,4));
        m.polygons.add(tri(2,3,4));
        m.polygons.add(tri(3,0,4));
        m.polygons.add(tri(0,2,1));
        m.polygons.add(tri(0,3,2));

        return m;
    }

    private static Polygon tri(int a, int b, int c) {
        Polygon p = new Polygon();
        p.getVertexIndices().add(a);
        p.getVertexIndices().add(b);
        p.getVertexIndices().add(c);
        return p;
    }
}
