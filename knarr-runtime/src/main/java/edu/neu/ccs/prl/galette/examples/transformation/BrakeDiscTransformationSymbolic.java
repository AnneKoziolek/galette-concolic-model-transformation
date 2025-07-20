package edu.neu.ccs.prl.galette.examples.transformation;

import edu.neu.ccs.prl.galette.examples.models.source.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.target.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.examples.transformation.SymbolicExecutionWrapper.SymbolicValue;
import java.util.Scanner;

/**
 * Symbolic execution enhanced version of the brake disc transformation.
 *
 * This class demonstrates how to add symbolic execution capabilities to an
 * existing transformation by using the SymbolicExecutionWrapper. The core
 * business logic is delegated to the clean transformation, while this class
 * handles the symbolic execution concerns.
 *
 * This approach provides:
 * - Separation of concerns between business logic and symbolic execution
 * - Ability to run transformations with or without symbolic execution
 * - Clean integration of Galette into existing systems
 * - Demonstration of layered architecture
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class BrakeDiscTransformationSymbolic {

    /**
     * Transform with symbolic execution capabilities.
     *
     * This method wraps the clean transformation with symbolic execution,
     * enabling path constraint collection and analysis.
     */
    public static BrakeDiscTarget transformSymbolic(
            BrakeDiscSource source, double thicknessValue, String thicknessLabel) {
        System.out.println("=== Symbolic Model Transformation ===");
        System.out.println("Source model: " + source);
        System.out.println("User input thickness: " + thicknessValue + " mm");

        // Create symbolic value for the external input
        SymbolicValue<Double> symbolicThickness =
                SymbolicExecutionWrapper.makeSymbolicDouble(thicknessLabel, thicknessValue);
        System.out.println("Created symbolic value: " + symbolicThickness);

        // Use the clean transformation for the core logic
        BrakeDiscTarget target = BrakeDiscTransformationClean.transform(source, symbolicThickness.getValue());

        // Add symbolic execution analysis for the conditional logic
        System.out.println("\n=== Symbolic Condition Analysis ===");
        analyzeConditionalLogic(symbolicThickness, target);

        // Display path constraint analysis
        SymbolicExecutionWrapper.displayPathConstraintAnalysis();

        System.out.println("=== Symbolic Transformation Complete ===");
        System.out.println("Target model: " + target);

        return target;
    }

    /**
     * Analyze the conditional logic with symbolic execution.
     * This recreates the thickness comparison to collect path constraints.
     */
    private static void analyzeConditionalLogic(SymbolicValue<Double> thickness, BrakeDiscTarget target) {
        double threshold = 10.0;

        System.out.println("Analyzing condition: thickness > " + threshold);

        // Perform symbolic comparison to collect path constraints
        boolean condition = SymbolicExecutionWrapper.evaluateThicknessCondition(thickness, threshold);

        System.out.println("Condition result: " + condition);
        System.out.println("Actual additionalStiffness value: " + target.hasAdditionalStiffness());

        // Verify consistency
        if (condition == target.hasAdditionalStiffness()) {
            System.out.println("✓ Symbolic analysis consistent with transformation result");
        } else {
            System.out.println("⚠ Inconsistency detected between symbolic analysis and result");
        }
    }

    /**
     * Interactive transformation with symbolic execution.
     */
    public static BrakeDiscTarget transformInteractiveSymbolic(BrakeDiscSource source) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== Interactive Symbolic Model Transformation ===");
        System.out.println("Source brake disc: " + source);
        System.out.print("Please enter the brake disc thickness (mm): ");

        double thickness = scanner.nextDouble();

        return transformSymbolic(source, thickness, "user_thickness");
    }

    /**
     * Demonstrate path exploration by running multiple transformations.
     */
    public static void demonstratePathExploration(BrakeDiscSource source) {
        System.out.println("\n" + repeatString("=", 70));
        System.out.println("SYMBOLIC PATH EXPLORATION DEMONSTRATION");
        System.out.println(repeatString("=", 70));

        double[] testValues = {8.0, 15.0};
        String[] pathDescriptions = {"thickness ≤ 10 (no additional stiffness)", "thickness > 10 (additional stiffness)"
        };

        for (int i = 0; i < testValues.length; i++) {
            System.out.println("\n### Path " + (i + 1) + ": " + pathDescriptions[i] + " ###");

            // Reset symbolic execution state for each path
            SymbolicExecutionWrapper.reset();

            BrakeDiscTarget result = transformSymbolic(source, testValues[i], "path_" + (i + 1) + "_thickness");

            System.out.println("Result: additionalStiffness = " + result.hasAdditionalStiffness());
            System.out.println(SymbolicExecutionWrapper.getExecutionSummary());
        }

        System.out.println("\n" + repeatString("=", 70));
        System.out.println("PATH EXPLORATION COMPLETE");
        System.out.println("Demonstrated how different inputs create different path constraints");
        System.out.println(repeatString("=", 70));
    }

    /**
     * Compare symbolic vs. clean transformation results.
     */
    public static void compareTransformationMethods(BrakeDiscSource source, double thickness) {
        System.out.println("\n=== Comparing Transformation Methods ===");

        // Clean transformation (no symbolic execution)
        System.out.println("\n1. Clean Transformation (Business Logic Only):");
        BrakeDiscTarget cleanResult = BrakeDiscTransformationClean.transform(source, thickness);
        System.out.println("Result: " + cleanResult);

        // Reset and run symbolic transformation
        SymbolicExecutionWrapper.reset();
        System.out.println("\n2. Symbolic Transformation (With Path Constraints):");
        BrakeDiscTarget symbolicResult = transformSymbolic(source, thickness, "comparison_thickness");

        // Compare results
        System.out.println("\n3. Comparison:");
        boolean resultsMatch = compareResults(cleanResult, symbolicResult);
        System.out.println("Results match: " + (resultsMatch ? "✓ YES" : "✗ NO"));

        if (SymbolicExecutionWrapper.isSymbolicExecutionActive()) {
            System.out.println("Symbolic execution provides additional analysis capabilities:");
            System.out.println("- Path constraints collected for solver-based test generation");
            System.out.println("- Ability to explore alternative execution paths");
            System.out.println("- Impact analysis of external inputs on model properties");
        }
    }

    /**
     * Compare two transformation results for equality.
     */
    private static boolean compareResults(BrakeDiscTarget result1, BrakeDiscTarget result2) {
        return Math.abs(result1.getDiameter() - result2.getDiameter()) < 0.001
                && Math.abs(result1.getThickness() - result2.getThickness()) < 0.001
                && Math.abs(result1.getSurfaceArea() - result2.getSurfaceArea()) < 0.001
                && Math.abs(result1.getVolume() - result2.getVolume()) < 0.001
                && Math.abs(result1.getEstimatedWeight() - result2.getEstimatedWeight()) < 0.001
                && result1.hasAdditionalStiffness() == result2.hasAdditionalStiffness()
                && result1.getMaterial().equals(result2.getMaterial())
                && result1.getCoolingVanes() == result2.getCoolingVanes();
    }

    /**
     * Utility method to repeat strings (Java 8 compatible).
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Get detailed analysis of a symbolic transformation.
     */
    public static String getSymbolicAnalysis(BrakeDiscSource source, double thickness) {
        SymbolicExecutionWrapper.reset();

        BrakeDiscTarget result = transformSymbolic(source, thickness, "analysis_thickness");

        StringBuilder analysis = new StringBuilder();
        analysis.append("=== Symbolic Transformation Analysis ===\n");
        analysis.append("Input: ").append(source).append("\n");
        analysis.append("Thickness: ").append(thickness).append(" mm\n");
        analysis.append("Output: ").append(result).append("\n");
        analysis.append("\n").append(SymbolicExecutionWrapper.analyzePathConstraints());

        return analysis.toString();
    }
}
