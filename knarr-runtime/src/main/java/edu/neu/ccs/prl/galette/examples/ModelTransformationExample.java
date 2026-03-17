package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathConditionWrapper;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils;
import edu.neu.ccs.prl.galette.examples.models.sourcemodel.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.targetmodel.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.examples.transformation.BrakeDiscTransformation;
import edu.neu.ccs.prl.galette.examples.transformation.SymbolicExecutionWrapper;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import za.ac.sun.cs.green.expr.Expression;

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

    public static void main(String[] args) {
        System.out.println("GALETTE CONCOLIC EXECUTION DEMO: MODEL TRANSFORMATION");
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

        // just one iteration for now
        // while (running) {
        System.out.print("\nSelect an option (1-3): ");

        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character
            System.out.println();

            switch (choice) {
                case 1:
                    runCleanTransformation(sourceModel, scanner);
                    break;
                case 2:
                    runConcolicPathExploration(sourceModel);
                    break;
                case 3:
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please select 1-3.");
            }

            if (running) {
                showMenu();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(); // Show full stack trace for debugging
            scanner.nextLine(); // Clear invalid input
            showMenu();
        }
        // }

        scanner.close();
    }

    /**
     * Display the main menu options.
     */
    private static void showMenu() {
        System.out.println("Available options:");
        System.out.println("1. Standard transformation (concrete execution, no path exploration)");
        System.out.println("2. Concolic execution with automated path constraint collection and exploration");
        System.out.println("3. Exit");
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
        BrakeDiscTarget result = BrakeDiscTransformation.transform(source, thickness);

        System.out.println("Transformation complete.");
        System.out.println("Additional stiffness: " + (result.hasAdditionalStiffness() ? "Yes" : "No"));

        System.out.println();
        System.out.println("=== TRANSFORMATION RESULTS ===");
        System.out.println(result.getGeometricSummary());
        System.out.println("\nNote: This version uses pure business logic without symbolic execution.");
    }

    /**
     * True concolic execution with automated path exploration.
     * This demonstrates the core Galette/Knarr functionality.
     */
    private static void runConcolicPathExploration(BrakeDiscSource source) {
        System.out.println("=== TRUE CONCOLIC EXECUTION WITH PATH EXPLORATION ===");
        System.out.println();
        System.out.println("This demonstrates proper concolic execution using Galette and Knarr:");
        System.out.println("1. Start with initial input and collect path constraints");
        System.out.println("2. Use constraint solver to generate inputs for unexplored paths");
        System.out.println("3. Automatically discover boundary conditions");
        System.out.println();

        performConcolicAnalysis(source);
    }

    /**
     * Perform true concolic execution analysis using Galette and Knarr.
     * This method implements the core concolic execution workflow:
     * 1. Execute with initial input and collect path constraints
     * 2. Use constraint solver to generate inputs for alternative paths
     * 3. Systematically explore all reachable execution paths
     */
    private static void performConcolicAnalysis(BrakeDiscSource source) {
        System.out.println("CONCOLIC EXECUTION ANALYSIS");

        List<Double> exploredInputs = new ArrayList<>();
        List<String> pathConstraints = new ArrayList<>();
        BranchCoverageTracker branchTracker = new BranchCoverageTracker();
        int maxIterations = 10; // Prevent infinite loops
        int iteration = 0;

        // Start with an initial input value
        double initialThickness = 12.0;

        System.out.println("\n=== ITERATION " + (++iteration) + ": Initial Execution ===");
        System.out.println("Starting concolic analysis with initial value = " + initialThickness);

        // Execute with initial input and collect path constraints
        ConcolicResult initialResult = executeConcolic(source, initialThickness, "thickness_" + iteration);
        exploredInputs.add(initialThickness);
        pathConstraints.add(initialResult.pathConstraint);
        branchTracker.analyzeConstraint(initialResult.pathConstraint, initialThickness);

        System.out.println("Initial path constraint: " + initialResult.pathConstraint);
        System.out.println("Result: additionalStiffness = " + initialResult.result.hasAdditionalStiffness());

        // Use constraint solver to generate alternative inputs
        while (iteration < maxIterations) {
            System.out.println("\n=== Generating Alternative Inputs ===");

            // Check if exploration is complete (all branches covered)
            if (branchTracker.isExplorationComplete()) {
                System.out.println("✅ All branches explored! Exploration complete.");
                System.out.println(branchTracker.getSummary());
                break;
            }

            // Try to generate input for unexplored branches
            Double alternativeInput =
                    generateAlternativeInputWithTracker(exploredInputs, pathConstraints, branchTracker);

            if (alternativeInput == null) {
                System.out.println("No more alternative paths found. Exploration complete.");
                break;
            }

            // Skip if we've already explored this input
            if (exploredInputs.contains(alternativeInput)) {
                System.out.println("Input " + alternativeInput + " already explored, trying boundary analysis...");
                alternativeInput = exploreBoundaryConditions(exploredInputs);
                if (alternativeInput == null) break;
            }

            System.out.println("\n=== ITERATION " + (++iteration) + ": Alternative Path ===");
            System.out.println("Exploring with generated input: " + alternativeInput + " mm");

            ConcolicResult altResult = executeConcolic(source, alternativeInput, "thickness_" + iteration);
            exploredInputs.add(alternativeInput);
            pathConstraints.add(altResult.pathConstraint);
            branchTracker.analyzeConstraint(altResult.pathConstraint, alternativeInput);

            System.out.println("Path constraint: " + altResult.pathConstraint);
            System.out.println("Result: additionalStiffness = " + altResult.result.hasAdditionalStiffness());

            // Check if we've found a different execution path
            if (!altResult.pathConstraint.equals(initialResult.pathConstraint)) {
                System.out.println("✓ NEW EXECUTION PATH DISCOVERED!");
            }
        }

        // Summary of concolic analysis
        System.out.println("CONCOLIC ANALYSIS SUMMARY");
        System.out.println("Total iterations: " + iteration);
        System.out.println("Inputs explored: " + exploredInputs.size());
        System.out.println("Unique path constraints: " + countUniqueConstraints(pathConstraints));

        System.out.println("\nExplored inputs and their path constraints:");
        for (int i = 0; i < exploredInputs.size(); i++) {
            System.out.println("  Input " + exploredInputs.get(i) + " mm → " + pathConstraints.get(i));
        }

        // Boundary analysis
        System.out.println("\n=== BOUNDARY CONDITION ANALYSIS ===");
        analyzeBoundaryConditions(exploredInputs);
    }

    /**
     * Container for concolic execution results.
     */
    private static class ConcolicResult {
        final BrakeDiscTarget result;
        final String pathConstraint;

        ConcolicResult(BrakeDiscTarget result, String pathConstraint, boolean hasConstraints) {
            this.result = result;
            this.pathConstraint = pathConstraint;
        }
    }

    /**
     * Tracks which branches (below/above threshold) have been explored for each comparison threshold.
     * This enables proper detection of exploration completeness.
     */
    private static class BranchCoverageTracker {
        // Maps threshold value to a pair of booleans: [belowExplored, aboveExplored]
        private final Map<Double, boolean[]> thresholdCoverage = new HashMap<>();

        /**
         * Analyze a path constraint string to extract threshold comparisons and their directions.
         * Parses constraints like "12.0<80.0" or "81.0>80.0" to determine which branch was taken.
         */
        void analyzeConstraint(String constraintStr, double inputValue) {
            if (constraintStr == null || constraintStr.isEmpty() || constraintStr.equals("no constraints")) {
                return;
            }

            // Parse individual comparisons from the constraint string
            // Format: "12.0<80.0" or "(12.0<80.0)&&(81.0>80.0)" etc.
            analyzeComparisonsInConstraint(constraintStr);
        }

        /**
         * Parse comparison expressions to identify threshold values and which branches are covered.
         */
        private void analyzeComparisonsInConstraint(String constraintStr) {
            // Split by && to get individual comparisons
            String[] parts = constraintStr.split("&&");

            for (String part : parts) {
                // Clean up parentheses
                String cleanPart = part.replace("(", "").replace(")", "").trim();

                // Parse comparisons like "12.0<80.0" or "81.0>80.0"
                parseComparison(cleanPart);
            }
        }

        /**
         * Parse a single comparison like "12.0<80.0", "81.0>80.0", or "80.0==80.0"
         */
        private void parseComparison(String comparison) {
            Double leftValue = null;
            Double rightValue = null;
            boolean isLessThan = false;
            boolean isGreaterThan = false;
            boolean isEqual = false;

            // Check multi-char operators first (==, !=, <=, >=) before single-char (< >)
            if (comparison.contains("==")) {
                String[] parts = comparison.split("==");
                if (parts.length == 2) {
                    leftValue = tryParseDouble(parts[0]);
                    rightValue = tryParseDouble(parts[1]);
                    isEqual = true;
                }
            } else if (comparison.contains("!=")) {
                String[] parts = comparison.split("!=");
                if (parts.length == 2) {
                    leftValue = tryParseDouble(parts[0]);
                    rightValue = tryParseDouble(parts[1]);
                    // != means we took the not-equal branch; record both directions as partially explored
                    isLessThan = true;
                    isGreaterThan = true;
                }
            } else if (comparison.contains("<=")) {
                String[] parts = comparison.split("<=");
                if (parts.length == 2) {
                    leftValue = tryParseDouble(parts[0]);
                    rightValue = tryParseDouble(parts[1]);
                    isLessThan = true;
                }
            } else if (comparison.contains(">=")) {
                String[] parts = comparison.split(">=");
                if (parts.length == 2) {
                    leftValue = tryParseDouble(parts[0]);
                    rightValue = tryParseDouble(parts[1]);
                    isGreaterThan = true;
                }
            } else if (comparison.contains("<")) {
                String[] parts = comparison.split("<");
                if (parts.length == 2) {
                    leftValue = tryParseDouble(parts[0]);
                    rightValue = tryParseDouble(parts[1]);
                    isLessThan = true;
                }
            } else if (comparison.contains(">")) {
                String[] parts = comparison.split(">");
                if (parts.length == 2) {
                    leftValue = tryParseDouble(parts[0]);
                    rightValue = tryParseDouble(parts[1]);
                    isGreaterThan = true;
                }
            }

            if (leftValue != null && rightValue != null) {
                // For ==, the DCMPL result was 0, meaning value equals threshold.
                // This is the "equal" case of the comparison; record it as the
                // above/equal branch (since the code checks thickness > threshold,
                // equal means we took the false/below branch).
                if (isEqual) {
                    isLessThan = true; // value == threshold means "not greater than"
                }
                recordBranchCoverage(rightValue, isLessThan, isGreaterThan);
            }
        }

        private Double tryParseDouble(String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /**
         * Record that a particular branch direction was taken for a threshold.
         * NOTE: Avoiding computeIfAbsent with lambda - Galette cannot instrument lambdas properly.
         */
        private void recordBranchCoverage(double threshold, boolean isLessThan, boolean isGreaterThan) {
            // Avoid using computeIfAbsent with lambda - Galette cannot instrument lambdas properly
            boolean[] coverage = thresholdCoverage.get(threshold);
            if (coverage == null) {
                coverage = new boolean[2];
                thresholdCoverage.put(threshold, coverage);
            }

            if (isLessThan) {
                coverage[0] = true; // Below/less-than branch explored
            }
            if (isGreaterThan) {
                coverage[1] = true; // Above/greater-than branch explored
            }
        }

        /**
         * Check if all discovered thresholds have both branches explored.
         */
        boolean isExplorationComplete() {
            if (thresholdCoverage.isEmpty()) {
                return false; // No thresholds discovered yet
            }

            for (Map.Entry<Double, boolean[]> entry : thresholdCoverage.entrySet()) {
                boolean[] coverage = entry.getValue();
                if (!coverage[0] || !coverage[1]) {
                    return false; // This threshold has unexplored branches
                }
            }
            return true; // All thresholds have both branches covered
        }

        /**
         * Get a threshold that still has unexplored branches.
         * Returns null if all branches are explored.
         */
        Double getUnexploredThreshold() {
            for (Map.Entry<Double, boolean[]> entry : thresholdCoverage.entrySet()) {
                boolean[] coverage = entry.getValue();
                if (!coverage[0] || !coverage[1]) {
                    return entry.getKey();
                }
            }
            return null;
        }

        /**
         * Determine which branch direction is unexplored for a given threshold.
         * Returns < 0 for below, > 0 for above, 0 if both explored.
         */
        int getUnexploredDirection(double threshold) {
            boolean[] coverage = thresholdCoverage.get(threshold);
            if (coverage == null) {
                return -1; // Default to exploring below first
            }
            if (!coverage[0]) {
                return -1; // Below branch unexplored
            }
            if (!coverage[1]) {
                return 1; // Above branch unexplored
            }
            return 0; // Both explored
        }

        /**
         * Get all discovered thresholds.
         */
        Set<Double> getDiscoveredThresholds() {
            return new HashSet<>(thresholdCoverage.keySet());
        }

        /**
         * Get a summary of branch coverage.
         */
        String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Branch Coverage:\n");
            for (Map.Entry<Double, boolean[]> entry : thresholdCoverage.entrySet()) {
                double threshold = entry.getKey();
                boolean[] coverage = entry.getValue();
                sb.append("  Threshold ").append(threshold).append(": ");
                sb.append("below=").append(coverage[0] ? "✓" : "✗");
                sb.append(", above=").append(coverage[1] ? "✓" : "✗");
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Execute transformation with concolic analysis.
     */
    private static ConcolicResult executeConcolic(BrakeDiscSource source, double thickness, String label) {
        // Reset symbolic execution state (both knarr-runtime and galette-agent constraints)
        GaletteSymbolicator.reset();
        PathUtils.resetPC();
        edu.neu.ccs.prl.galette.concolic.knarr.runtime.GalettePathConstraintBridge.resetGaletteConstraints();

        // Create symbolic value for thickness - we need to get the TAGGED VALUE, not just the tag
        Tag symbolicTag = GaletteSymbolicator.makeSymbolicDouble(label, thickness);

        // The problem: makeSymbolicDouble() creates a tagged value internally but only returns the tag!
        // We need to manually create the tagged value and use that
        double taggedThickness = edu.neu.ccs.prl.galette.internal.runtime.Tainter.setTag(thickness, symbolicTag);

        // Verify the tag was applied
        Tag verifyTag = edu.neu.ccs.prl.galette.internal.runtime.Tainter.getTag(taggedThickness);
        System.out.println("ModelTransformationExample: Created symbolic value: " + label + " = " + thickness
                + " (tag: " + (verifyTag != null ? verifyTag : "no tag") + ")");

        // Execute the transformation with the TAGGED value (this is the key fix!)
        System.out.println("🔧 About to call BrakeDiscTransformation.transform() with tagged thickness");
        BrakeDiscTarget result = BrakeDiscTransformation.transform(source, taggedThickness);
        System.out.println("🔧 BrakeDiscTransformation.transform() completed");

        // Collect path constraints
        PathConditionWrapper pc = PathUtils.getCurPCWithGalette();
        String constraintDescription = "no constraints";
        boolean hasConstraints = false;

        if (pc != null && !pc.isEmpty()) {
            hasConstraints = true;
            if (pc.toSingleExpression() != null) {
                constraintDescription = pc.toSingleExpression().toString();
            } else {
                constraintDescription = "constraints collected: " + pc.size();
            }
        }

        System.out.println("Path constraints: " + constraintDescription);

        return new ConcolicResult(result, constraintDescription, hasConstraints);
    }

    // Note: performSymbolicThicknessCheck method removed -
    // path constraints are now collected automatically during transformation execution

    /**
     * Generate alternative input values to explore different execution paths.
     * Uses the Z3 solver for input generation.
     */
    private static Double generateAlternativeInput(List<Double> exploredInputs, List<String> pathConstraints) {

        // Generate alternative inputs using Z3 solver based on discovered thresholds
        try {
            PathConditionWrapper pc = PathUtils.getCurPCWithGalette();
            Set<Double> discoveredThresholds = new HashSet<>();
            if (pc != null && !pc.isEmpty()) {
                List<Expression> constraints = pc.getConstraints();
                System.out.println("🔍 generateAlternativeInput: Analyzing " + constraints.size() + " constraints");
                for (Expression constraint : constraints) {
                    if (constraint != null) {
                        Set<Double> thresholds = SymbolicExecutionWrapper.extractThresholdsFromConstraint(constraint);
                        System.out.println("  Extracted thresholds: " + thresholds);
                        discoveredThresholds.addAll(thresholds);
                    }
                }
            } else {
                System.out.println("🔍 generateAlternativeInput: No path constraints available (pc=" + pc + ")");
            }

            // If we found thresholds from constraints, use Z3 solver to generate inputs
            if (!discoveredThresholds.isEmpty()) {
                System.out.println("🔍 Using Z3 solver with discovered thresholds: " + discoveredThresholds);
                for (Double threshold : discoveredThresholds) {
                    // Check which direction is unexplored
                    boolean hasLowValue = false;
                    boolean hasHighValue = false;
                    for (Double v : exploredInputs) {
                        if (v < threshold) hasLowValue = true;
                        if (v >= threshold) hasHighValue = true;
                    }

                    System.out.println(
                            "  Threshold " + threshold + ": hasLow=" + hasLowValue + ", hasHigh=" + hasHighValue);

                    if (!hasLowValue) {
                        // Use Z3 solver to generate value < threshold
                        System.out.println("  → Asking Z3 solver for value < " + threshold);
                        Double candidate = GaletteSymbolicator.generateInputForBranch("thickness", threshold, true);
                        if (candidate != null && !exploredInputs.contains(candidate)) {
                            System.out.println("  → Z3 generated: " + candidate);
                            return candidate;
                        }
                    } else if (!hasHighValue) {
                        // Use Z3 solver to generate value >= threshold
                        System.out.println("  → Asking Z3 solver for value >= " + threshold);
                        Double candidate = GaletteSymbolicator.generateInputForBranch("thickness", threshold, false);
                        if (candidate != null && !exploredInputs.contains(candidate)) {
                            System.out.println("  → Z3 generated: " + candidate);
                            return candidate;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not use Z3 solver: " + e.getMessage());
            if (e.getMessage() == null) {
                e.printStackTrace();
            }
        }

        // No solver result available
        System.out.println("🔍 Z3 solver could not generate alternative input");
        return null;
    }

    /**
     * Generate alternative input using the branch coverage tracker and Z3 solver.
     * This method uses the constraint solver to generate inputs that will explore
     * unexplored branches, stopping when all branches are covered.
     *
     * IMPORTANT: This now uses the Z3 solver for input generation, NOT heuristics.
     */
    private static Double generateAlternativeInputWithTracker(
            List<Double> exploredInputs, List<String> pathConstraints, BranchCoverageTracker tracker) {

        // First, check if exploration is already complete
        if (tracker.isExplorationComplete()) {
            System.out.println("🎯 Branch tracker: All branches already explored");
            return null;
        }

        // Get thresholds that have unexplored branches
        Set<Double> thresholds = tracker.getDiscoveredThresholds();
        System.out.println("🔍 Branch tracker: Discovered thresholds: " + thresholds);

        for (Double threshold : thresholds) {
            int direction = tracker.getUnexploredDirection(threshold);

            if (direction < 0) {
                // Need to explore below threshold - use solver
                System.out.println("  → Threshold " + threshold + ": need below branch, asking Z3 solver...");
                Double candidate = GaletteSymbolicator.generateInputForBranch("thickness", threshold, true);
                if (candidate != null && !exploredInputs.contains(candidate)) {
                    System.out.println("  → Z3 solver generated: " + candidate);
                    return candidate;
                } else if (candidate == null) {
                    System.out.println("  → Z3 solver returned null (constraint may be unsatisfiable)");
                }
            } else if (direction > 0) {
                // Need to explore above threshold - use solver
                System.out.println("  → Threshold " + threshold + ": need above branch, asking Z3 solver...");
                Double candidate = GaletteSymbolicator.generateInputForBranch("thickness", threshold, false);
                if (candidate != null && !exploredInputs.contains(candidate)) {
                    System.out.println("  → Z3 solver generated: " + candidate);
                    return candidate;
                } else if (candidate == null) {
                    System.out.println("  → Z3 solver returned null (constraint may be unsatisfiable)");
                }
            } else {
                System.out.println("  → Threshold " + threshold + ": both branches explored ✓");
            }
        }

        // If no thresholds found yet, fall back to the original method
        if (thresholds.isEmpty()) {
            System.out.println("🔍 No thresholds discovered yet, using fallback method");
            return generateAlternativeInput(exploredInputs, pathConstraints);
        }

        // All discovered thresholds have both branches explored
        System.out.println("🎯 All discovered threshold branches have been explored");
        return null;
    }

    /**
     * Explore boundary conditions around discovered threshold values.
     * Uses dynamic threshold discovery instead of hardcoded values.
     */
    private static Double exploreBoundaryConditions(List<Double> exploredInputs) {
        // Discover thresholds dynamically from path constraints
        PathConditionWrapper pc = PathUtils.getCurPCWithGalette();
        Set<Double> discoveredThresholds = new HashSet<>();
        if (pc != null && !pc.isEmpty()) {
            List<Expression> constraints = pc.getConstraints();
            for (Expression constraint : constraints) {
                discoveredThresholds.addAll(SymbolicExecutionWrapper.extractThresholdsFromConstraint(constraint));
            }
        }

        // Generate boundary values around discovered thresholds
        for (Double threshold : discoveredThresholds) {
            double[] boundaryValues = {threshold - 0.1, threshold, threshold + 0.1, threshold - 0.01, threshold + 0.01};

            for (double value : boundaryValues) {
                if (!exploredInputs.contains(value)) {
                    System.out.println("Exploring boundary condition around threshold " + threshold + ": " + value);
                    return value;
                }
            }
        }

        // Fallback: explore around input distribution patterns
        if (!exploredInputs.isEmpty() && exploredInputs.size() > 1) {
            List<Double> sortedInputs = new ArrayList<>(exploredInputs);
            Collections.sort(sortedInputs);

            for (int i = 0; i < sortedInputs.size() - 1; i++) {
                double gap = sortedInputs.get(i + 1) - sortedInputs.get(i);
                if (gap > 1.0) { // Significant gap might indicate boundary
                    double boundaryValue = (sortedInputs.get(i) + sortedInputs.get(i + 1)) / 2.0;
                    if (!exploredInputs.contains(boundaryValue)) {
                        System.out.println("Exploring potential boundary: " + boundaryValue);
                        return boundaryValue;
                    }
                }
            }
        }

        return null; // No more boundary conditions to explore
    }

    /**
     * Count unique path constraints to understand path coverage.
     */
    private static int countUniqueConstraints(List<String> constraints) {
        // Use traditional loop instead of stream to avoid instrumentation issues
        Set<String> unique = new HashSet<>(constraints);
        return unique.size();
    }

    /**
     * Analyze boundary conditions from explored inputs.
     * Let Galette/Knarr constraint solver determine thresholds dynamically.
     */
    private static void analyzeBoundaryConditions(List<Double> inputs) {
        System.out.println("=== Dynamic Boundary Analysis (using Galette/Knarr) ===");

        // Use Galette's constraint solver to analyze discovered thresholds
        // rather than hardcoded knowledge
        GaletteSymbolicator.InputSolution solution = GaletteSymbolicator.solvePathCondition();

        if (solution != null) {
            System.out.println("Constraint solver analysis:");
            System.out.println("  Solution variables: " + solution.getLabels());
            System.out.println("  Constraint solution: " + solution);

            // Extract threshold information from solver solution
            for (String label : solution.getLabels()) {
                Object value = solution.getValue(label);
                if (value instanceof Number && label.contains("thickness")) {
                    double threshold = ((Number) value).doubleValue();
                    System.out.println("  → Discovered threshold from constraints: " + threshold + " mm");

                    // Analyze inputs around this discovered threshold
                    analyzeInputsAroundThreshold(inputs, threshold);
                }
            }
        } else {
            // Fallback: analyze input distribution patterns
            System.out.println("No constraint solution available, analyzing input patterns...");
            analyzeInputPatterns(inputs);
        }

        System.out.println("✓ Boundary analysis complete - using dynamic constraint discovery");
    }

    /**
     * Analyze inputs around a discovered threshold value.
     */
    private static void analyzeInputsAroundThreshold(List<Double> inputs, double threshold) {
        // Use traditional loops instead of streams to avoid instrumentation issues
        Set<Double> belowSet = new TreeSet<>();
        Set<Double> aboveSet = new TreeSet<>();
        for (Double v : inputs) {
            if (v <= threshold) belowSet.add(v);
            if (v > threshold) aboveSet.add(v);
        }
        List<Double> belowThreshold = new ArrayList<>(belowSet);
        List<Double> aboveThreshold = new ArrayList<>(aboveSet);

        System.out.println("    Inputs ≤ " + threshold + ": " + belowThreshold);
        System.out.println("    Inputs > " + threshold + ": " + aboveThreshold);

        if (!belowThreshold.isEmpty() && !aboveThreshold.isEmpty()) {
            System.out.println("    ✓ Both execution paths explored");
        } else if (belowThreshold.isEmpty()) {
            System.out.println("    ⚠ Missing exploration of ≤ " + threshold + " path");
        } else if (aboveThreshold.isEmpty()) {
            System.out.println("    ⚠ Missing exploration of > " + threshold + " path");
        }
    }

    /**
     * Analyze input patterns when no constraints are available.
     */
    private static void analyzeInputPatterns(List<Double> inputs) {
        if (inputs.size() < 2) {
            System.out.println("  Insufficient inputs for pattern analysis");
            return;
        }

        List<Double> sortedInputs = new ArrayList<>(inputs);
        Collections.sort(sortedInputs);

        System.out.println("  Input distribution: " + sortedInputs);
        System.out.println("  Range: " + sortedInputs.get(0) + " to " + sortedInputs.get(sortedInputs.size() - 1));

        // Look for gaps that might indicate boundaries
        for (int i = 0; i < sortedInputs.size() - 1; i++) {
            double gap = sortedInputs.get(i + 1) - sortedInputs.get(i);
            if (gap > 2.0) { // Significant gap
                System.out.println("  → Potential boundary around "
                        + (sortedInputs.get(i) + sortedInputs.get(i + 1)) / 2.0 + " mm (gap: " + gap + ")");
            }
        }
    }
}
