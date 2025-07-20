package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.examples.models.source.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.target.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.examples.transformation.BrakeDiscTransformationClean;
import edu.neu.ccs.prl.galette.examples.transformation.SymbolicExecutionWrapper;
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
        SymbolicExecutionWrapper.reset(); // Start with clean state

        // Create a sample brake disc source model
        BrakeDiscSource sourceModel = createSampleBrakeDisc();
        System.out.println("Created sample brake disc: " + sourceModel);
        System.out.println();

        // Show menu options
        showMenu();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("\nSelect an option (1-7): ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character
                System.out.println();

                switch (choice) {
                    case 1:
                        runCleanTransformation(sourceModel, scanner);
                        break;
                    case 2:
                        runSymbolicTransformation(sourceModel, scanner);
                        break;
                    case 3:
                        runComparisonDemo(sourceModel, scanner);
                        break;
                    case 4:
                        runPathExplorationDemo(sourceModel);
                        break;
                    case 5:
                        runLegacySymbolicDemo(sourceModel, scanner);
                        break;
                    case 6:
                        showDetailedExample(sourceModel);
                        break;
                    case 7:
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid option. Please select 1-7.");
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
        System.out.println("1. Clean transformation (business logic only, no symbolic execution)");
        System.out.println("2. Symbolic transformation (with path constraint collection)");
        System.out.println("3. Compare clean vs symbolic approaches");
        System.out.println("4. Path exploration demo (test both execution paths)");
        System.out.println("5. Legacy symbolic demo (original implementation)");
        System.out.println("6. Show detailed example with explanations");
        System.out.println("7. Exit");
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
     * Run clean transformation (business logic only).
     */
    private static void runCleanTransformation(BrakeDiscSource source, Scanner scanner) {
        System.out.println("=== CLEAN TRANSFORMATION (NO SYMBOLIC EXECUTION) ===");
        System.out.println();
        System.out.println("Source brake disc: " + source);
        System.out.print("Please enter the brake disc thickness (mm): ");

        double thickness = scanner.nextDouble();

        System.out.println("Transforming with thickness: " + thickness + " mm");
        BrakeDiscTarget result = BrakeDiscTransformationClean.transform(source, thickness);

        System.out.println("Transformation complete.");
        System.out.println("Additional stiffness: " + (result.hasAdditionalStiffness() ? "Yes" : "No"));

        System.out.println();
        System.out.println("=== TRANSFORMATION RESULTS ===");
        System.out.println(result.getGeometricSummary());
        System.out.println("\nNote: This version uses pure business logic without symbolic execution.");
    }

    /**
     * Run symbolic transformation with path constraint collection.
     */
    private static void runSymbolicTransformation(BrakeDiscSource source, Scanner scanner) {
        System.out.println("=== SYMBOLIC TRANSFORMATION (WITH PATH CONSTRAINTS) ===");
        System.out.println();
        System.out.println("Source brake disc: " + source);
        System.out.print("Please enter the brake disc thickness (mm): ");

        double thickness = scanner.nextDouble();

        SymbolicExecutionWrapper.reset();
        BrakeDiscTarget result = SymbolicExecutionWrapper.transformSymbolic(source, thickness, "user_thickness");

        System.out.println();
        System.out.println("=== TRANSFORMATION RESULTS ===");
        System.out.println(result.getGeometricSummary());
        System.out.println("\nNote: This version collects path constraints for symbolic analysis.");
    }

    /**
     * Compare clean vs symbolic transformation approaches.
     */
    private static void runComparisonDemo(BrakeDiscSource source, Scanner scanner) {
        System.out.println("=== COMPARISON: CLEAN VS SYMBOLIC TRANSFORMATION ===");
        System.out.println();

        System.out.print("Enter thickness value for comparison: ");
        double thickness = scanner.nextDouble();

        SymbolicExecutionWrapper.compareTransformationMethods(source, thickness);
    }

    /**
     * Run legacy symbolic demo (original implementation).
     */
    private static void runLegacySymbolicDemo(BrakeDiscSource source, Scanner scanner) {
        System.out.println("=== LEGACY SYMBOLIC DEMO (ORIGINAL IMPLEMENTATION) ===");
        System.out.println();
        System.out.println("Source brake disc: " + source);
        System.out.print("Please enter the brake disc thickness (mm): ");

        double thickness = scanner.nextDouble();

        SymbolicExecutionWrapper.reset();
        BrakeDiscTarget result = SymbolicExecutionWrapper.runLegacyStyleDemo(source, thickness);

        System.out.println();
        System.out.println("=== TRANSFORMATION RESULTS ===");
        System.out.println(result.getGeometricSummary());
    }

    /**
     * Demonstrate path exploration with different thickness values.
     */
    private static void runPathExplorationDemo(BrakeDiscSource source) {
        System.out.println("=== PATH EXPLORATION DEMONSTRATION ===");
        System.out.println();
        System.out.println("This demo shows how different input values lead to different");
        System.out.println("execution paths with path constraint collection.");
        System.out.println();

        SymbolicExecutionWrapper.demonstratePathExploration(source);
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

        SymbolicExecutionWrapper.reset();
        BrakeDiscTarget result = SymbolicExecutionWrapper.runLegacyStyleDemo(source, testThickness);

        System.out.println();
        System.out.println("=== ANALYSIS RESULTS ===");
        System.out.println("Transformation result:");
        System.out.println(result.getGeometricSummary());

        // Show symbolic execution statistics
        System.out.println();
        System.out.println("Final symbolic execution state:");
        System.out.println(SymbolicExecutionWrapper.getExecutionSummary());
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

            SymbolicExecutionWrapper.reset();
            BrakeDiscTarget result = BrakeDiscTransformationClean.transform(source, testValues[i]);

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
