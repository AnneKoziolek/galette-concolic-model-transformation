package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.examples.models.source.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.target.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.examples.transformation.BrakeDiscTransformation;
import java.util.Scanner;

/**
 * Main application demonstrating concolic execution in model transformations.
 *
 * This example shows how Galette can be used to track symbolic values through
 * model transformation code, specifically demonstrating:
 *
 * 1. External input as symbolic values
 * 2. Propagation of symbolic values through calculations
 * 3. Conditional logic creating different execution paths
 * 4. Path constraint collection for impact analysis
 *
 * The example implements the use case described in the migration goals email:
 * analyzing the impact of external inputs in model-driven engineering scenarios.
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class ModelTransformationExample {

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

    public static void main(String[] args) {
        System.out.println(repeatString("=", 80));
        System.out.println("GALETTE CONCOLIC EXECUTION DEMO: MODEL TRANSFORMATION");
        System.out.println(repeatString("=", 80));
        System.out.println();
        System.out.println("This example demonstrates how Galette can track symbolic values");
        System.out.println("through model transformations to analyze the impact of external inputs.");
        System.out.println();

        // Initialize the symbolic execution environment
        System.out.println("Initializing Galette symbolic execution environment...");
        GaletteSymbolicator.reset(); // Start with clean state

        // Create a sample brake disc source model
        BrakeDiscSource sourceModel = createSampleBrakeDisc();
        System.out.println("Created sample brake disc: " + sourceModel);
        System.out.println();

        // Show menu options
        showMenu();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("\nSelect an option (1-5): ");

            try {
                int choice = scanner.nextInt();
                System.out.println();

                switch (choice) {
                    case 1:
                        runInteractiveTransformation(sourceModel);
                        break;
                    case 2:
                        runPathExplorationDemo(sourceModel);
                        break;
                    case 3:
                        runSymbolicAnalysisDemo(sourceModel);
                        break;
                    case 4:
                        showDetailedExample(sourceModel);
                        break;
                    case 5:
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid option. Please select 1-5.");
                }

                if (running) {
                    System.out.println("\n" + repeatString("-", 60));
                    showMenu();
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                scanner.nextLine(); // Clear invalid input
                showMenu();
            }
        }

        scanner.close();
    }

    /**
     * Display the main menu options.
     */
    private static void showMenu() {
        System.out.println("Available options:");
        System.out.println("1. Interactive transformation (enter thickness manually)");
        System.out.println("2. Path exploration demo (test both execution paths)");
        System.out.println("3. Symbolic analysis demo (detailed constraint tracking)");
        System.out.println("4. Show detailed example with explanations");
        System.out.println("5. Exit");
    }

    /**
     * Create a sample brake disc for demonstration.
     *
     * @return Sample brake disc source model
     */
    private static BrakeDiscSource createSampleBrakeDisc() {
        return new BrakeDiscSource(350.0, "cast iron", 24);
    }

    /**
     * Run interactive transformation where user enters thickness.
     *
     * @param source Source model to transform
     */
    private static void runInteractiveTransformation(BrakeDiscSource source) {
        System.out.println("=== INTERACTIVE TRANSFORMATION ===");
        System.out.println();

        GaletteSymbolicator.reset();
        BrakeDiscTarget result = BrakeDiscTransformation.transformInteractive(source);

        System.out.println();
        System.out.println("=== TRANSFORMATION RESULTS ===");
        System.out.println(result.getGeometricSummary());
    }

    /**
     * Demonstrate path exploration with different thickness values.
     *
     * @param source Source model to transform
     */
    private static void runPathExplorationDemo(BrakeDiscSource source) {
        System.out.println("=== PATH EXPLORATION DEMONSTRATION ===");
        System.out.println();
        System.out.println("This demo shows how different input values lead to different");
        System.out.println("execution paths in the transformation logic.");
        System.out.println();

        BrakeDiscTransformation.demonstratePathExploration(source);
    }

    /**
     * Run detailed symbolic analysis showing constraint collection.
     *
     * @param source Source model to transform
     */
    private static void runSymbolicAnalysisDemo(BrakeDiscSource source) {
        System.out.println("=== SYMBOLIC ANALYSIS DEMONSTRATION ===");
        System.out.println();
        System.out.println("This demo shows detailed symbolic execution tracking");
        System.out.println("and path constraint collection.");
        System.out.println();

        // Test with a thickness value that triggers the conditional
        double testThickness = 12.5;
        System.out.println("Using test thickness: " + testThickness + " mm");
        System.out.println("Expected: thickness > 10, so additionalStiffness should be true");
        System.out.println();

        GaletteSymbolicator.reset();
        BrakeDiscTarget result = BrakeDiscTransformation.transform(source, testThickness);

        System.out.println();
        System.out.println("=== ANALYSIS RESULTS ===");
        System.out.println("Transformation result:");
        System.out.println(result.getGeometricSummary());

        // Show symbolic execution statistics
        System.out.println();
        System.out.println("Final symbolic execution state:");
        System.out.println(GaletteSymbolicator.getStatistics());
    }

    /**
     * Show a detailed example with step-by-step explanations.
     *
     * @param source Source model to transform
     */
    private static void showDetailedExample(BrakeDiscSource source) {
        System.out.println("=== DETAILED EXAMPLE WITH EXPLANATIONS ===");
        System.out.println();

        System.out.println("MODEL TRANSFORMATION CONCOLIC EXECUTION WALKTHROUGH");
        System.out.println(repeatString("━", 55));
        System.out.println();

        System.out.println("1. PROBLEM CONTEXT:");
        System.out.println("   In model-driven engineering, transformations often require");
        System.out.println("   external input from users (e.g., design parameters).");
        System.out.println("   We want to analyze how these inputs affect the output models.");
        System.out.println();

        System.out.println("2. SOLUTION APPROACH:");
        System.out.println("   - Mark external inputs as symbolic values");
        System.out.println("   - Track their propagation through transformation logic");
        System.out.println("   - Collect path constraints showing input-output relationships");
        System.out.println();

        System.out.println("3. CONCRETE EXAMPLE:");
        System.out.println("   Source: Simple brake disc (diameter, material, cooling vanes)");
        System.out.println("   Input:  Thickness value (provided by user → made symbolic)");
        System.out.println("   Logic:  Geometric calculations + conditional stiffness rule");
        System.out.println("   Output: Enhanced model with computed properties");
        System.out.println();

        System.out.println("4. KEY CONDITIONAL LOGIC:");
        System.out.println("   if (thickness > 10mm) {");
        System.out.println("       additionalStiffness = true;");
        System.out.println("   } else {");
        System.out.println("       additionalStiffness = false;");
        System.out.println("   }");
        System.out.println();

        System.out.println("5. EXECUTION WITH SAMPLE VALUES:");
        System.out.println();

        // Demonstrate with two different values
        double[] testValues = {8.5, 12.0};
        String[] expectedPaths = {"thickness ≤ 10", "thickness > 10"};
        boolean[] expectedStiffness = {false, true};

        for (int i = 0; i < testValues.length; i++) {
            System.out.println("   Test " + (i + 1) + ": thickness = " + testValues[i] + " mm");
            System.out.println("   Expected path: " + expectedPaths[i]);
            System.out.println("   Expected additionalStiffness: " + expectedStiffness[i]);
            System.out.println();

            GaletteSymbolicator.reset();
            BrakeDiscTarget result = BrakeDiscTransformation.transform(source, testValues[i]);

            System.out.println("   Actual result:");
            System.out.println("   - Additional stiffness: " + result.hasAdditionalStiffness());
            System.out.println("   - Surface area: " + String.format("%.1f", result.getSurfaceArea()) + " mm²");
            System.out.println("   - Volume: " + String.format("%.1f", result.getVolume()) + " mm³");
            System.out.println("   - Weight: " + String.format("%.1f", result.getEstimatedWeight()) + " g");
            System.out.println();
        }

        System.out.println("6. BENEFITS FOR MODEL-DRIVEN ENGINEERING:");
        System.out.println("   ✓ Impact analysis: See how inputs affect outputs");
        System.out.println("   ✓ Constraint collection: Understand input-output relationships");
        System.out.println("   ✓ Path exploration: Identify different behavior scenarios");
        System.out.println("   ✓ Prioritization: Determine which inputs have most impact");
        System.out.println();

        System.out.println("This demonstrates the core value proposition from the migration goals:");
        System.out.println("Using Galette to track external inputs through complex model");
        System.out.println("transformations for impact analysis and consistency management.");
    }
}
