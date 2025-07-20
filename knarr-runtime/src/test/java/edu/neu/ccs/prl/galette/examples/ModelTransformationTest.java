package edu.neu.ccs.prl.galette.examples;

import static org.junit.jupiter.api.Assertions.*;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathConditionWrapper;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils;
import edu.neu.ccs.prl.galette.examples.models.source.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.target.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.examples.transformation.BrakeDiscTransformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for model transformation concolic execution example.
 *
 * These tests validate that:
 * 1. Both execution paths work correctly (thickness ≤ 10 vs > 10)
 * 2. Symbolic values are properly tracked through transformations
 * 3. Path constraints are collected during execution
 * 4. Geometric calculations produce expected results
 * 5. Conditional logic behaves correctly
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class ModelTransformationTest {

    private BrakeDiscSource sampleSource;
    private static final double STIFFNESS_THRESHOLD = 10.0;
    private static final double DELTA = 0.001; // For floating point comparisons

    @BeforeEach
    public void setUp() {
        // Reset symbolic execution state before each test
        GaletteSymbolicator.reset();

        // Create a standard test brake disc
        sampleSource = new BrakeDiscSource(300.0, "cast iron", 20);
    }

    @Test
    public void testBasicTransformation() {
        // Test basic transformation functionality
        double thickness = 8.0;
        BrakeDiscTarget result = BrakeDiscTransformation.transform(sampleSource, thickness);

        assertNotNull(result, "Transformation should produce a result");
        assertEquals(sampleSource.getDiameter(), result.getDiameter(), DELTA);
        assertEquals(sampleSource.getMaterial(), result.getMaterial());
        assertEquals(sampleSource.getCoolingVanes(), result.getCoolingVanes());
        assertEquals(thickness, result.getThickness(), DELTA);
    }

    @Test
    public void testGeometricCalculations() {
        // Test that geometric calculations are correct
        double thickness = 5.0;
        BrakeDiscTarget result = BrakeDiscTransformation.transform(sampleSource, thickness);

        // Verify surface area calculation: π * (diameter/2)²
        double expectedSurfaceArea = Math.PI * Math.pow(sampleSource.getDiameter() / 2.0, 2);
        assertEquals(expectedSurfaceArea, result.getSurfaceArea(), DELTA);

        // Verify volume calculation: surface area * thickness
        double expectedVolume = expectedSurfaceArea * thickness;
        assertEquals(expectedVolume, result.getVolume(), DELTA);

        // Verify weight calculation is positive and reasonable
        assertTrue(result.getEstimatedWeight() > 0, "Weight should be positive");
        assertTrue(result.getEstimatedWeight() < 50000, "Weight should be reasonable (< 50kg)");
    }

    @Test
    public void testConditionalLogicPath1_NoAdditionalStiffness() {
        // Test path 1: thickness ≤ 10 → additionalStiffness = false
        double thickness = 8.0; // Below threshold

        BrakeDiscTarget result = BrakeDiscTransformation.transform(sampleSource, thickness);

        assertFalse(
                result.hasAdditionalStiffness(),
                "Thickness " + thickness + " ≤ " + STIFFNESS_THRESHOLD + " should result in no additional stiffness");
    }

    @Test
    public void testConditionalLogicPath2_WithAdditionalStiffness() {
        // Test path 2: thickness > 10 → additionalStiffness = true
        double thickness = 15.0; // Above threshold

        BrakeDiscTarget result = BrakeDiscTransformation.transform(sampleSource, thickness);

        assertTrue(
                result.hasAdditionalStiffness(),
                "Thickness " + thickness + " > " + STIFFNESS_THRESHOLD + " should result in additional stiffness");
    }

    @Test
    public void testBoundaryConditions() {
        // Test exact boundary conditions

        // Test exactly at threshold
        BrakeDiscTarget resultAt = BrakeDiscTransformation.transform(sampleSource, STIFFNESS_THRESHOLD);
        assertFalse(
                resultAt.hasAdditionalStiffness(),
                "Thickness exactly at " + STIFFNESS_THRESHOLD + " should not have additional stiffness");

        // Test just below threshold
        BrakeDiscTarget resultBelow = BrakeDiscTransformation.transform(sampleSource, STIFFNESS_THRESHOLD - 0.1);
        assertFalse(
                resultBelow.hasAdditionalStiffness(),
                "Thickness just below threshold should not have additional stiffness");

        // Test just above threshold
        GaletteSymbolicator.reset(); // Reset for second test
        BrakeDiscTarget resultAbove = BrakeDiscTransformation.transform(sampleSource, STIFFNESS_THRESHOLD + 0.1);
        assertTrue(
                resultAbove.hasAdditionalStiffness(),
                "Thickness just above threshold should have additional stiffness");
    }

    @Test
    public void testSymbolicValueCreation() {
        // Test that symbolic values are created during transformation
        double thickness = 12.0;

        // Capture initial state
        String initialStats = GaletteSymbolicator.getStatistics();

        BrakeDiscTarget result = BrakeDiscTransformation.transform(sampleSource, thickness);

        // Capture final state
        String finalStats = GaletteSymbolicator.getStatistics();

        assertNotNull(result, "Transformation should succeed");
        assertNotEquals(initialStats, finalStats, "Symbolic execution state should change during transformation");

        // The statistics should show that symbolic values were created
        assertTrue(finalStats.contains("Symbolic values:"), "Statistics should mention symbolic values");
    }

    @Test
    public void testPathConstraintCollection() {
        // Test that path constraints are collected during execution
        double thickness = 15.0; // Above threshold to trigger conditional

        BrakeDiscTarget result = BrakeDiscTransformation.transform(sampleSource, thickness);

        assertNotNull(result, "Transformation should succeed");
        assertTrue(result.hasAdditionalStiffness(), "Should have additional stiffness for thickness > 10");

        // Check if path constraints were collected
        PathConditionWrapper pc = PathUtils.getCurPC();
        assertNotNull(pc, "Path condition wrapper should exist");

        // Note: The exact constraint collection depends on the symbolic execution implementation
        // This test verifies the infrastructure is working
    }

    @Test
    public void testDifferentMaterials() {
        // Test transformation with different materials
        String[] materials = {"cast iron", "carbon ceramic", "steel"};
        double thickness = 12.0;

        for (String material : materials) {
            GaletteSymbolicator.reset(); // Reset for each material test

            BrakeDiscSource source = new BrakeDiscSource(300.0, material, 20);
            BrakeDiscTarget result = BrakeDiscTransformation.transform(source, thickness);

            assertNotNull(result, "Transformation should work for material: " + material);
            assertEquals(material, result.getMaterial(), "Material should be preserved");
            assertTrue(result.hasAdditionalStiffness(), "Should have additional stiffness for thickness > 10");
            assertTrue(result.getEstimatedWeight() > 0, "Weight should be positive for material: " + material);
        }
    }

    @Test
    public void testMultipleTransformations() {
        // Test multiple transformations to ensure state management works correctly
        double[] thicknessValues = {5.0, 12.0, 8.0, 15.0};
        boolean[] expectedStiffness = {false, true, false, true};

        for (int i = 0; i < thicknessValues.length; i++) {
            GaletteSymbolicator.reset(); // Reset state for each transformation

            double thickness = thicknessValues[i];
            boolean expected = expectedStiffness[i];

            BrakeDiscTarget result = BrakeDiscTransformation.transform(sampleSource, thickness);

            assertNotNull(result, "Transformation " + i + " should succeed");
            assertEquals(
                    expected,
                    result.hasAdditionalStiffness(),
                    String.format(
                            "Transformation %d with thickness %.1f should have additionalStiffness=%s",
                            i, thickness, expected));
        }
    }

    @Test
    public void testTransformationConsistency() {
        // Test that the same input produces the same output
        double thickness = 11.5;

        BrakeDiscTarget result1 = BrakeDiscTransformation.transform(sampleSource, thickness);

        GaletteSymbolicator.reset();
        BrakeDiscTarget result2 = BrakeDiscTransformation.transform(sampleSource, thickness);

        // Results should be identical
        assertEquals(result1.getDiameter(), result2.getDiameter(), DELTA);
        assertEquals(result1.getThickness(), result2.getThickness(), DELTA);
        assertEquals(result1.getSurfaceArea(), result2.getSurfaceArea(), DELTA);
        assertEquals(result1.getVolume(), result2.getVolume(), DELTA);
        assertEquals(result1.getEstimatedWeight(), result2.getEstimatedWeight(), DELTA);
        assertEquals(result1.hasAdditionalStiffness(), result2.hasAdditionalStiffness());
    }

    @Test
    public void testVolumeWeightRelationship() {
        // Test that larger volumes result in larger weights (for same material)
        double thinThickness = 5.0;
        double thickThickness = 20.0;

        BrakeDiscTarget thinResult = BrakeDiscTransformation.transform(sampleSource, thinThickness);

        GaletteSymbolicator.reset();
        BrakeDiscTarget thickResult = BrakeDiscTransformation.transform(sampleSource, thickThickness);

        assertTrue(thickResult.getVolume() > thinResult.getVolume(), "Thicker disc should have larger volume");
        assertTrue(
                thickResult.getEstimatedWeight() > thinResult.getEstimatedWeight(),
                "Thicker disc should have larger weight");

        // Verify the proportional relationship
        double volumeRatio = thickResult.getVolume() / thinResult.getVolume();
        double weightRatio = thickResult.getEstimatedWeight() / thinResult.getEstimatedWeight();
        assertEquals(volumeRatio, weightRatio, 0.01, "Volume and weight should scale proportionally");
    }

    @Test
    public void testEdgeCaseInputs() {
        // Test edge cases and unusual inputs

        // Very small thickness
        BrakeDiscTarget smallResult = BrakeDiscTransformation.transform(sampleSource, 0.1);
        assertNotNull(smallResult, "Should handle very small thickness");
        assertFalse(smallResult.hasAdditionalStiffness(), "Small thickness should not have additional stiffness");

        GaletteSymbolicator.reset();

        // Very large thickness
        BrakeDiscTarget largeResult = BrakeDiscTransformation.transform(sampleSource, 100.0);
        assertNotNull(largeResult, "Should handle very large thickness");
        assertTrue(largeResult.hasAdditionalStiffness(), "Large thickness should have additional stiffness");
    }
}
