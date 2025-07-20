package edu.neu.ccs.prl.galette.examples.transformation;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathConditionWrapper;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils;
import edu.neu.ccs.prl.galette.examples.models.source.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.target.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import java.util.Scanner;
import za.ac.sun.cs.green.expr.Expression;

/**
 * Transformation logic for converting brake disc models from source to target format.
 *
 * This class demonstrates concolic execution in model transformations by:
 * 1. Accepting external input (thickness) from the user
 * 2. Marking the input as symbolic for tracking
 * 3. Performing geometric calculations with symbolic values
 * 4. Implementing conditional logic that affects model properties
 * 5. Collecting path constraints for analysis
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
    private static final double STIFFNESS_THRESHOLD = 10.0; // mm

    /**
     * Helper method to repeat a string (Java 8 compatible).
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Transform a source brake disc model to a target model with enhanced properties.
     *
     * This method demonstrates the core use case for concolic execution in model
     * transformations: external input (thickness) affects both computed values
     * and conditional model properties.
     *
     * @param source The source brake disc model
     * @param userInputThickness The thickness value provided by user (will be made symbolic)
     * @return Enhanced target model with computed properties
     */
    public static BrakeDiscTarget transform(BrakeDiscSource source, double userInputThickness) {
        System.out.println("=== Starting Brake Disc Model Transformation ===");
        System.out.println("Source model: " + source);
        System.out.println("User input thickness: " + userInputThickness + " mm");

        // Create symbolic representation of the user input
        // This is the key step for concolic execution
        Tag thicknessTag = GaletteSymbolicator.makeSymbolicDouble("user_thickness", userInputThickness);
        System.out.println("Created symbolic tag for thickness: " + (thicknessTag != null ? "SUCCESS" : "FAILED"));

        // Create target model and copy basic properties
        BrakeDiscTarget target = new BrakeDiscTarget();
        target.setDiameter(source.getDiameter());
        target.setMaterial(source.getMaterial());
        target.setCoolingVanes(source.getCoolingVanes());
        target.setThickness(userInputThickness);

        // Perform geometric calculations with symbolic values
        System.out.println("\n=== Performing Geometric Calculations ===");

        // Calculate surface area: π * (diameter/2)²
        double radius = source.getDiameter() / 2.0;
        double surfaceArea = Math.PI * radius * radius;
        target.setSurfaceArea(surfaceArea);
        System.out.println("Surface area: " + surfaceArea + " mm²");

        // Calculate volume: surface area * thickness (symbolic calculation)
        double volume = surfaceArea * userInputThickness;
        target.setVolume(volume);
        System.out.println("Volume: " + volume + " mm³");

        // Calculate estimated weight based on material density
        double density = getMaterialDensity(source.getMaterial());
        double volumeInM3 = volume / 1_000_000_000.0; // Convert mm³ to m³
        double weightInKg = volumeInM3 * density;
        double weightInGrams = weightInKg * 1000.0;
        target.setEstimatedWeight(weightInGrams);
        System.out.println("Estimated weight: " + weightInGrams + " g");

        // CRITICAL: Conditional logic that creates different execution paths
        System.out.println("\n=== Evaluating Conditional Logic ===");
        System.out.println("Checking if thickness (" + userInputThickness + ") > " + STIFFNESS_THRESHOLD);

        // This conditional creates two possible execution paths:
        // Path 1: thickness > 10 → additionalStiffness = true
        // Path 2: thickness ≤ 10 → additionalStiffness = false
        if (userInputThickness > STIFFNESS_THRESHOLD) {
            target.setAdditionalStiffness(true);
            System.out.println("→ Path taken: thickness > 10, setting additionalStiffness = true");
        } else {
            target.setAdditionalStiffness(false);
            System.out.println("→ Path taken: thickness ≤ 10, setting additionalStiffness = false");
        }

        // Collect and display path constraints
        collectAndDisplayPathConstraints();

        System.out.println("\n=== Transformation Complete ===");
        System.out.println("Target model: " + target);

        return target;
    }

    /**
     * Interactive transformation that prompts user for thickness input.
     *
     * @param source The source brake disc model
     * @return Enhanced target model with computed properties
     */
    public static BrakeDiscTarget transformInteractive(BrakeDiscSource source) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== Interactive Model Transformation ===");
        System.out.println("Source brake disc: " + source);
        System.out.print("Please enter the brake disc thickness (mm): ");

        double thickness = scanner.nextDouble();

        return transform(source, thickness);
    }

    /**
     * Get material density for weight calculations.
     *
     * @param material Material type
     * @return Density in kg/m³
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
     * Collect and display path constraints for analysis.
     *
     * This method demonstrates how to extract the symbolic execution
     * information that Galette has collected during the transformation.
     */
    private static void collectAndDisplayPathConstraints() {
        System.out.println("\n=== Path Constraint Analysis ===");

        try {
            // Get current path condition
            PathConditionWrapper pc = PathUtils.getCurPC();

            if (pc != null && !pc.isEmpty()) {
                System.out.println("Path constraints collected: " + pc.size());

                // Get the consolidated constraint expression
                Expression constraint = pc.toSingleExpression();
                if (constraint != null) {
                    System.out.println("Consolidated constraint: " + constraint);
                    System.out.println(
                            "Constraint type: " + constraint.getClass().getSimpleName());
                } else {
                    System.out.println("No consolidated constraint available");
                }

                // Display statistics
                System.out.println("Path constraint details:");
                System.out.println("  - Number of constraints: " + pc.size());
                System.out.println(
                        "  - Constraint satisfiable: " + (constraint != null ? "Unknown" : "No constraints"));

            } else {
                System.out.println("No path constraints collected");
                System.out.println("This may indicate that symbolic execution is not active");
            }

            // Try to get solution from constraint solver
            GaletteSymbolicator.InputSolution solution = GaletteSymbolicator.solvePathCondition();
            if (solution != null) {
                System.out.println("Constraint solver solution: " + solution);
            } else {
                System.out.println("No solution from constraint solver");
            }

        } catch (Exception e) {
            System.err.println("Error collecting path constraints: " + e.getMessage());
            if (GaletteSymbolicator.DEBUG) {
                e.printStackTrace();
            }
        }

        // Display overall symbolic execution statistics
        System.out.println("\nSymbolic execution statistics:");
        System.out.println(GaletteSymbolicator.getStatistics());
    }

    /**
     * Demonstrate different execution paths by running transformation with different inputs.
     *
     * @param source The source brake disc model
     */
    public static void demonstratePathExploration(BrakeDiscSource source) {
        System.out.println("\n" + repeatString("=", 60));
        System.out.println("DEMONSTRATING PATH EXPLORATION");
        System.out.println(repeatString("=", 60));

        // Test path 1: thickness ≤ 10 (no additional stiffness)
        System.out.println("\n### Testing Path 1: thickness ≤ 10 ###");
        GaletteSymbolicator.reset(); // Clear previous state
        BrakeDiscTarget result1 = transform(source, 8.0);
        System.out.println("Result 1 - Additional Stiffness: " + result1.hasAdditionalStiffness());

        // Test path 2: thickness > 10 (additional stiffness)
        System.out.println("\n### Testing Path 2: thickness > 10 ###");
        GaletteSymbolicator.reset(); // Clear previous state
        BrakeDiscTarget result2 = transform(source, 15.0);
        System.out.println("Result 2 - Additional Stiffness: " + result2.hasAdditionalStiffness());

        System.out.println("\n" + repeatString("=", 60));
        System.out.println("PATH EXPLORATION COMPLETE");
        System.out.println("Two different execution paths demonstrated:");
        System.out.println("1. thickness ≤ 10 → additionalStiffness = false");
        System.out.println("2. thickness > 10 → additionalStiffness = true");
        System.out.println(repeatString("=", 60));
    }
}
