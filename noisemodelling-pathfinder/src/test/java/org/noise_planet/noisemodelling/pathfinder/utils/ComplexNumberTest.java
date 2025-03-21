package org.noise_planet.noisemodelling.pathfinder.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.noise_planet.noisemodelling.pathfinder.utils.ComplexNumber;

/**
 * Unit test class for testing various operations and methods on the ComplexNumber class.
 * This class provides test cases for validating the correctness of arithmetic operations,
 * trigonometric functions, formatting, and utility methods of the ComplexNumber class.
 */
public class ComplexNumberTest {
    private static final double EPSILON = 1e-6;

    /**
     * Tests for the `add` method.
     */
    @Test
    void testAdd() {
        ComplexNumber complex1 = new ComplexNumber(3.0, 4.0);
        ComplexNumber complex2 = new ComplexNumber(2.0, 5.0);

        ComplexNumber result = ComplexNumber.add(complex1, complex2);

        assertEquals(5.0, result.getRe(), EPSILON);
        assertEquals(9.0, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `subtract` method.
     */
    @Test
    void testSubtract() {
        ComplexNumber complex1 = new ComplexNumber(5.0, 7.0);
        ComplexNumber complex2 = new ComplexNumber(2.0, 3.0);

        ComplexNumber result = ComplexNumber.subtract(complex1, complex2);

        assertEquals(3.0, result.getRe(), EPSILON);
        assertEquals(4.0, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `multiply` method.
     */
    @Test
    void testMultiply() {
        ComplexNumber complex1 = new ComplexNumber(3.0, 4.0);
        ComplexNumber complex2 = new ComplexNumber(2.0, 5.0);

        ComplexNumber result = ComplexNumber.multiply(complex1, complex2);

        assertEquals(-14.0, result.getRe(), EPSILON);
        assertEquals(23.0, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `divide` method.
     */
    @Test
    void testDivide() {
        ComplexNumber complex1 = new ComplexNumber(3.0, 4.0);
        ComplexNumber complex2 = new ComplexNumber(1.0, -1.0);

        ComplexNumber result = ComplexNumber.divide(complex1, complex2);

        assertEquals(-0.5, result.getRe(), EPSILON);
        assertEquals(3.5, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `conjugate` method.
     */
    @Test
    void testConjugate() {
        ComplexNumber complex = new ComplexNumber(3.0, 4.0);

        ComplexNumber result = complex.conjugate();

        assertEquals(3.0, result.getRe(), EPSILON);
        assertEquals(-4.0, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `mod` method.
     */
    @Test
    void testMod() {
        ComplexNumber complex = new ComplexNumber(3.0, 4.0);

        double result = complex.mod();

        assertEquals(5.0, result, EPSILON);
    }

    /**
     * Tests for the `square` method.
     */
    @Test
    void testSquare() {
        ComplexNumber complex = new ComplexNumber(3.0, 4.0);

        ComplexNumber result = complex.square();

        assertEquals(-7.0, result.getRe(), EPSILON);
        assertEquals(24.0, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `exp` method.
     */
    @Test
    void testExp() {
        ComplexNumber complex = new ComplexNumber(0.0, Math.PI);

        ComplexNumber result = ComplexNumber.exp(complex);

        assertEquals(-1.0, result.getRe(), EPSILON);
        assertEquals(0.0, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `pow` method.
     */
    @Test
    void testPow() {
        ComplexNumber complex = new ComplexNumber(2.0, 3.0);

        ComplexNumber result = ComplexNumber.pow(complex, 2);

        assertEquals(-5.0, result.getRe(), EPSILON);
        assertEquals(12.0, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `sin` method.
     */
    @Test
    void testSin() {
        ComplexNumber complex = new ComplexNumber(2.0, 3.0);

        ComplexNumber result = ComplexNumber.sin(complex);

        assertEquals(9.15449914691143, result.getRe(), EPSILON);
        assertEquals(-4.168906959966565, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `cos` method.
     */
    @Test
    void testCos() {
        ComplexNumber complex = new ComplexNumber(2.0, 3.0);

        ComplexNumber result = ComplexNumber.cos(complex);

        assertEquals(-4.189625690968807, result.getRe(), EPSILON);
        assertEquals(-9.109227893755337, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `tan` method.
     */
    @Test
    void testTan() {
        ComplexNumber complex = new ComplexNumber(1.0, 2.0);

        ComplexNumber result = ComplexNumber.tan(complex);

        assertEquals(0.0338128260798967, result.getRe(), EPSILON);
        assertEquals(1.014793616146633, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `toString` method.
     */
    @Test
    void testToString() {
        ComplexNumber complex = new ComplexNumber(3.0, 4.0);

        String result = complex.toString();

        assertEquals("3.0+4.0i", result);
    }

    /**
     * Tests for the `equals` method.
     */
    @Test
    void testEquals() {
        ComplexNumber complex1 = new ComplexNumber(3.0, 4.0);
        ComplexNumber complex2 = new ComplexNumber(3.0, 4.0);
        ComplexNumber complex3 = new ComplexNumber(5.0, 4.0);

        assertEquals(complex1, complex2);
        assertNotEquals(complex1, complex3);
    }

    /**
     * Tests for the `parseComplex` method.
     */
    @Test
    void testParseComplex() {
        ComplexNumber result1 = ComplexNumber.parseComplex("3+4i");
        ComplexNumber result2 = ComplexNumber.parseComplex("-5-6i");
        ComplexNumber result3 = ComplexNumber.parseComplex("7");

        assertEquals(new ComplexNumber(3.0, 4.0), result1);
        assertEquals(new ComplexNumber(-5.0, -6.0), result2);
        assertEquals(new ComplexNumber(7.0, 0.0), result3);
    }

    /**
     * Tests for the `inverse` method.
     */
    @Test
    void testInverse() {
        ComplexNumber complex = new ComplexNumber(1.0, -1.0);

        ComplexNumber result = complex.inverse();

        assertEquals(0.5, result.getRe(), EPSILON);
        assertEquals(0.5, result.getIm(), EPSILON);
    }

    /**
     * Tests for the `format` method.
     */
    @Test
    void testFormat() {
        ComplexNumber complex = new ComplexNumber(3.0, 4.0);

        String resultXY = complex.format(ComplexNumber.XY);
        String resultRCIS = complex.format(ComplexNumber.RCIS);

        assertEquals("3.0+4.0i", resultXY);
        assertEquals("5.0 cis(0.9272952180016122)", resultRCIS);
    }
}