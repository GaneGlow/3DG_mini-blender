package com.cgvsu.objreader;

import com.cgvsu.render_engine.Transform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.vecmath.Vector3f;

import static org.junit.jupiter.api.Assertions.*;

class AfineTest {

    private Transform transform;

    @BeforeEach
    void setUp() {
        transform = new Transform();
    }

    @Test
    public void testDefaultConstructor() {
        Vector3f expectedTranslation = new Vector3f(0, 0, 0);
        Vector3f expectedRotation = new Vector3f(0, 0, 0);
        Vector3f expectedScale = new Vector3f(1, 1, 1);

        assertVectorsEqual(expectedTranslation, transform.getTranslation(), "Default translation should be (0,0,0)");
        assertVectorsEqual(expectedRotation, transform.getRotation(), "Default rotation should be (0,0,0)");
        assertVectorsEqual(expectedScale, transform.getScale(), "Default scale should be (1,1,1)");
    }

    @Test
    public void testSetAndGetTranslation() {
        Vector3f newTranslation = new Vector3f(10.5f, -3.2f, 7.8f);
        transform.setTranslation(newTranslation);

        Vector3f actualTranslation = transform.getTranslation();
        assertVectorsEqual(newTranslation, actualTranslation, "Translation should be set correctly");
    }

    @Test
    public void testSetAndGetRotation() {
        Vector3f newRotation = new Vector3f((float) Math.PI/2, (float) Math.PI/4, (float) Math.PI/6);
        transform.setRotation(newRotation);

        Vector3f actualRotation = transform.getRotation();
        assertVectorsEqual(newRotation, actualRotation, "Rotation should be set correctly");
    }

    @Test
    public void testSetAndGetScale() {
        Vector3f newScale = new Vector3f(2.0f, 0.5f, 3.0f);
        transform.setScale(newScale);

        Vector3f actualScale = transform.getScale();
        assertVectorsEqual(newScale, actualScale, "Scale should be set correctly");
    }

    @Test
    public void testTranslatePositiveValues() {
        transform.translate(5.0f, 3.0f, 2.0f);
        Vector3f expected = new Vector3f(5.0f, 3.0f, 2.0f);
        assertVectorsEqual(expected, transform.getTranslation(), "Positive translation should add correctly");
    }

    @Test
    public void testTranslateNegativeValues() {
        transform.translate(-2.0f, -5.0f, -1.0f);
        Vector3f expected = new Vector3f(-2.0f, -5.0f, -1.0f);
        assertVectorsEqual(expected, transform.getTranslation(), "Negative translation should subtract correctly");
    }

    @Test
    public void testTranslateMultipleTimes() {
        transform.translate(1.0f, 2.0f, 3.0f);
        transform.translate(4.0f, 5.0f, 6.0f);
        Vector3f expected = new Vector3f(5.0f, 7.0f, 9.0f);
        assertVectorsEqual(expected, transform.getTranslation(), "Multiple translations should accumulate");
    }

    @Test
    public void testRotatePositiveValues() {
        transform.rotate(0.5f, 1.0f, 1.5f);
        Vector3f expected = new Vector3f(0.5f, 1.0f, 1.5f);
        assertVectorsEqual(expected, transform.getRotation(), "Positive rotation should add correctly");
    }

    @Test
    public void testRotateNegativeValues() {
        transform.rotate(-0.3f, -0.7f, -1.2f);
        Vector3f expected = new Vector3f(-0.3f, -0.7f, -1.2f);
        assertVectorsEqual(expected, transform.getRotation(), "Negative rotation should subtract correctly");
    }

    @Test
    public void testScaleUniform() {
        transform.scale(2.0f);
        Vector3f expected = new Vector3f(2.0f, 2.0f, 2.0f);
        assertVectorsEqual(expected, transform.getScale(), "Uniform scale should multiply all axes");
    }

    @Test
    public void testScaleUniformMultipleTimes() {
        transform.scale(2.0f);
        transform.scale(1.5f);
        Vector3f expected = new Vector3f(3.0f, 3.0f, 3.0f); // 2.0 * 1.5 = 3.0
        assertVectorsEqual(expected, transform.getScale(), "Multiple uniform scales should multiply");
    }

    @Test
    public void testScaleX() {
        transform.scaleX(2.0f);
        Vector3f expected = new Vector3f(2.0f, 1.0f, 1.0f);
        assertVectorsEqual(expected, transform.getScale(), "ScaleX should only affect X axis");
    }

    @Test
    public void testScaleY() {
        transform.scaleY(0.5f);
        Vector3f expected = new Vector3f(1.0f, 0.5f, 1.0f);
        assertVectorsEqual(expected, transform.getScale(), "ScaleY should only affect Y axis");
    }

    @Test
    public void testScaleZ() {
        transform.scaleZ(3.0f);
        Vector3f expected = new Vector3f(1.0f, 1.0f, 3.0f);
        assertVectorsEqual(expected, transform.getScale(), "ScaleZ should only affect Z axis");
    }

    @Test
    public void testScaleCombinedAxes() {
        transform.scaleX(2.0f);
        transform.scaleY(0.5f);
        transform.scaleZ(3.0f);
        Vector3f expected = new Vector3f(2.0f, 0.5f, 3.0f);
        assertVectorsEqual(expected, transform.getScale(), "Individual axis scales should combine correctly");
    }

    @Test
    public void testReset() {
        // Изменяем все значения
        transform.setTranslation(new Vector3f(10, 20, 30));
        transform.setRotation(new Vector3f(1, 2, 3));
        transform.setScale(new Vector3f(5, 6, 7));

        transform.reset();

        Vector3f expectedTranslation = new Vector3f(0, 0, 0);
        Vector3f expectedRotation = new Vector3f(0, 0, 0);
        Vector3f expectedScale = new Vector3f(1, 1, 1);

        assertVectorsEqual(expectedTranslation, transform.getTranslation(), "Reset should set translation to (0,0,0)");
        assertVectorsEqual(expectedRotation, transform.getRotation(), "Reset should set rotation to (0,0,0)");
        assertVectorsEqual(expectedScale, transform.getScale(), "Reset should set scale to (1,1,1)");
    }

    @Test
    public void testResetAfterOperations() {
        transform.translate(5, 3, 1);
        transform.rotate(0.1f, 0.2f, 0.3f);
        transform.scale(2.0f);

        transform.reset();

        Vector3f expectedTranslation = new Vector3f(0, 0, 0);
        Vector3f expectedRotation = new Vector3f(0, 0, 0);
        Vector3f expectedScale = new Vector3f(1, 1, 1);

        assertVectorsEqual(expectedTranslation, transform.getTranslation(), "Reset should clear accumulated translation");
        assertVectorsEqual(expectedRotation, transform.getRotation(), "Reset should clear accumulated rotation");
        assertVectorsEqual(expectedScale, transform.getScale(), "Reset should clear accumulated scale");
    }

    @Test
    public void testAllTransformationsCombined() {
        // Проверяем, что все операции работают вместе
        transform.translate(10, 5, 3);
        transform.rotate((float)Math.PI/4, (float)Math.PI/3, 0);
        transform.scaleX(2.0f);
        transform.scaleY(1.5f);
        transform.scale(0.5f); // Универсальный масштаб умножает все оси

        // Проверяем перевод
        Vector3f expectedTranslation = new Vector3f(10, 5, 3);
        assertVectorsEqual(expectedTranslation, transform.getTranslation(), "Translation should be correct");

        // Проверяем вращение
        Vector3f expectedRotation = new Vector3f((float)Math.PI/4, (float)Math.PI/3, 0);
        assertVectorsEqual(expectedRotation, transform.getRotation(), "Rotation should be correct");

        // Проверяем масштаб: сначала X*2, потом Y*1.5, потом все *0.5
        Vector3f expectedScale = new Vector3f(1.0f, 0.75f, 0.5f); // X: 2*0.5=1, Y: 1.5*0.5=0.75, Z: 1*0.5=0.5
        assertVectorsEqual(expectedScale, transform.getScale(), "Scale should be correct after combined operations");
    }

    @Test
    public void testScaleWithZeroFactor() {
        transform.scale(0.0f);
        Vector3f expected = new Vector3f(0.0f, 0.0f, 0.0f);
        assertVectorsEqual(expected, transform.getScale(), "Scaling by zero should result in zero scale");

        // После этого попробуем еще масштабировать
        transform.scaleX(2.0f);
        Vector3f expectedAfter = new Vector3f(0.0f, 0.0f, 0.0f); // 0 * 2 = 0
        assertVectorsEqual(expectedAfter, transform.getScale(), "Scaling zero should remain zero");
    }

    @Test
    public void testScaleWithNegativeFactor() {
        transform.scale(-2.0f);
        Vector3f expected = new Vector3f(-2.0f, -2.0f, -2.0f);
        assertVectorsEqual(expected, transform.getScale(), "Negative scale should be allowed");

        transform.scale(-0.5f);
        Vector3f expectedAfter = new Vector3f(1.0f, 1.0f, 1.0f); // -2 * -0.5 = 1
        assertVectorsEqual(expectedAfter, transform.getScale(), "Negative scales should multiply correctly");
    }

    // Вспомогательный метод для сравнения векторов с учетом погрешности float
    private void assertVectorsEqual(Vector3f expected, Vector3f actual, String message) {
        final float EPSILON = 0.0001f;
        assertEquals(expected.x, actual.x, EPSILON, message + " (X component)");
        assertEquals(expected.y, actual.y, EPSILON, message + " (Y component)");
        assertEquals(expected.z, actual.z, EPSILON, message + " (Z component)");
    }
}