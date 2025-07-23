package edu.neu.ccs.prl.galette.examples.transformation;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.SymbolicComparison;
import edu.neu.ccs.prl.galette.examples.models.source.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.target.BrakeDiscTarget;
import java.util.Scanner;

/**
 * Model transformation logic for brake disc models with integrated symbolic execution support.
 *
 * This class contains business logic for transforming brake disc models from source to
 * target format, with integrated support for symbolic execution and path constraint collection.
 *
 * The transformation uses SymbolicComparison methods to automatically collect path constraints
 * when symbolic values are present, enabling concolic execution and automated test generation.
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class BrakeDiscTransformation {

    /**
     * Material density constants (kg/m³) for weight calculations.
     */
    private static final double CAST_IRON_DENSITY = 7200.0;

    private static final double CARBON_CERAMIC_DENSITY = 1600.0;
    private static final double DEFAULT_DENSITY = 7800.0; // Steel

    /**
     * Threshold for additional stiffness determination.
     */
    private static final double STIFFNESS_THRESHOLD = 60.0; // mm

    /**
     * Transform a source brake disc model to a target model with enhanced properties.
     *
     * This method contains business logic with integrated symbolic execution support.
     * When thickness values are tagged with Galette, path constraints are automatically
     * collected during comparisons for concolic execution analysis.
     *
     * @param source The source brake disc model
     * @param thickness The thickness value (may be Galette-tagged for symbolic execution)
     * @return Enhanced target model with computed properties
     */
    public static BrakeDiscTarget transform(BrakeDiscSource source, double thickness) {
        // Create target model and copy basic properties
        BrakeDiscTarget target = new BrakeDiscTarget();
        target.setDiameter(source.getDiameter());
        target.setMaterial(source.getMaterial());
        target.setCoolingVanes(source.getCoolingVanes());
        target.setThickness(thickness);

        // Perform geometric calculations
        calculateGeometricProperties(source, thickness, target);

        // Apply engineering rules
        applyEngineeringRules(thickness, target);

        return target;
    }

    /**
     * Calculate geometric properties based on diameter and thickness.
     */
    private static void calculateGeometricProperties(BrakeDiscSource source, double thickness, BrakeDiscTarget target) {
        // Calculate surface area: π * (diameter/2)²
        double radius = source.getDiameter() / 2.0;
        double surfaceArea = Math.PI * radius * radius;
        target.setSurfaceArea(surfaceArea);

        // Calculate volume: surface area * thickness
        double volume = surfaceArea * thickness;
        target.setVolume(volume);

        // Calculate estimated weight based on material density
        double density = getMaterialDensity(source.getMaterial());
        double volumeInM3 = volume / 1_000_000_000.0; // Convert mm³ to m³
        double weightInKg = volumeInM3 * density;
        double weightInGrams = weightInKg * 1000.0;
        target.setEstimatedWeight(weightInGrams);
    }

    /**
     * Apply engineering rules based on thickness and other properties.
     */
    private static void applyEngineeringRules(double thickness, BrakeDiscTarget target) {
        // Rule: If thickness > 10mm, then additional stiffness is present
        System.out.println("🔍 GALETTE DEBUG: About to perform comparison");
        System.out.println("   thickness = " + thickness);
        System.out.println("   STIFFNESS_THRESHOLD = " + STIFFNESS_THRESHOLD);

        // Check if the thickness value has a Galette tag
        edu.neu.ccs.prl.galette.internal.runtime.Tag thicknessTag =
                edu.neu.ccs.prl.galette.internal.runtime.Tainter.getTag(thickness);
        System.out.println("   thickness tag: " + (thicknessTag != null ? thicknessTag : "no tag"));

        // Use SymbolicComparison to ensure path constraints are collected for symbolic values
        boolean hasAdditionalStiffness = SymbolicComparison.greaterThan(
                thickness, thicknessTag, STIFFNESS_THRESHOLD, null // threshold is concrete, so no tag
                );
        System.out.println("   comparison result: " + hasAdditionalStiffness);

        target.setAdditionalStiffness(hasAdditionalStiffness);
    }

    /**
     * Get material density for weight calculations.
     */
    private static double getMaterialDensity(String material) {
        if (material == null) {
            return DEFAULT_DENSITY;
        }

        switch (material.toLowerCase()) {
            case "cast iron":
                return CAST_IRON_DENSITY;
            case "carbon ceramic":
                return CARBON_CERAMIC_DENSITY;
            default:
                return DEFAULT_DENSITY;
        }
    }

    /**
     * Interactive transformation that prompts user for thickness input.
     * This version has no symbolic execution - just pure business logic.
     */
    public static BrakeDiscTarget transformInteractive(BrakeDiscSource source) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== Clean Model Transformation (No Symbolic Execution) ===");
        System.out.println("Source brake disc: " + source);
        System.out.print("Please enter the brake disc thickness (mm): ");

        double thickness = scanner.nextDouble();

        System.out.println("Transforming with thickness: " + thickness + " mm");
        BrakeDiscTarget result = transform(source, thickness);

        System.out.println("Transformation complete.");
        System.out.println("Additional stiffness: " + (result.hasAdditionalStiffness() ? "Yes" : "No"));

        scanner.close();

        return result;
    }

    /**
     * Batch transformation for testing multiple values.
     */
    public static BrakeDiscTarget[] transformBatch(BrakeDiscSource source, double[] thicknessValues) {
        BrakeDiscTarget[] results = new BrakeDiscTarget[thicknessValues.length];

        for (int i = 0; i < thicknessValues.length; i++) {
            results[i] = transform(source, thicknessValues[i]);
        }

        return results;
    }

    /**
     * Validate transformation inputs.
     */
    public static boolean isValidThickness(double thickness) {
        return thickness > 0 && thickness < 100; // Reasonable bounds for brake disc thickness
    }

    /**
     * Get a summary of the transformation results.
     */
    public static String getTransformationSummary(BrakeDiscSource source, BrakeDiscTarget target) {
        return String.format(
                "Transformation Summary:\n" + "  Input: %s\n"
                        + "  Output: %s\n"
                        + "  Thickness: %.1f mm → Additional Stiffness: %s\n"
                        + "  Weight: %.1f g\n"
                        + "  Volume: %.1f mm³",
                source.toString(),
                target.toString(),
                target.getThickness(),
                target.hasAdditionalStiffness() ? "Yes" : "No",
                target.getEstimatedWeight(),
                target.getVolume());
    }
}
