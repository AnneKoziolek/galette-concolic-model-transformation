package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GalettePathConstraintBridge;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathConditionWrapper;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils;
import edu.neu.ccs.prl.galette.examples.models.sourcemodel.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.targetmodel.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.examples.transformation.BrakeDiscTransformation;
import edu.neu.ccs.prl.galette.examples.transformation.SymbolicExecutionWrapper;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
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
     * Perform true concolic execution analysis using DART-style path exploration.
     *
     * The algorithm (following Godefroid et al., DART/SAGE):
     * 1. Execute with initial input, collect path condition [C1, C2, ..., Cn]
     * 2. For each constraint Ci (from last to first), build the negated prefix:
     *    C1 ∧ ... ∧ C(i-1) ∧ ¬Ci
     * 3. Solve the conjunction — if satisfiable, the model gives a new input
     *    that follows the same prefix but takes a different branch at position i
     * 4. Execute with the new input, discover new constraints, repeat
     *
     * This systematically explores all feasible execution paths without
     * any example-specific logic.
     */
    private static void performConcolicAnalysis(BrakeDiscSource source) {
        System.out.println("CONCOLIC EXECUTION ANALYSIS");

        List<Double> exploredInputs = new ArrayList<>();
        List<String> pathConstraintStrings = new ArrayList<>();
        Set<String> exploredPathSignatures = new HashSet<>();

        // Worklist of candidate conjunctions to solve for new inputs
        Queue<Expression> worklist = new LinkedList<>();
        // Track which conjunctions we've already submitted to the solver
        Set<String> triedCandidates = new HashSet<>();

        int maxIterations = 20;
        int iteration = 0;

        // Start with an initial input value
        double nextInput = 12.0;

        // Initialize path condition state before the loop —
        // Galette intercepts the loop comparison, which requires PATH_CONDITIONS to exist
        PathUtils.resetPC();

        while (iteration < maxIterations) {
            iteration++;
            System.out.println("\n=== ITERATION " + iteration + ": "
                    + (iteration == 1 ? "Initial Execution" : "Alternative Path") + " ===");
            System.out.println((iteration == 1
                            ? "Starting concolic analysis with initial value = "
                            : "Exploring with generated input: ")
                    + nextInput
                    + (iteration == 1 ? "" : " mm"));

            // Execute with concrete input and collect path constraints as Expression list
            ConcolicResult result = executeConcolic(source, nextInput, "thickness");
            exploredInputs.add(nextInput);
            pathConstraintStrings.add(result.pathConstraint);

            System.out.println("Path constraint: " + result.pathConstraint);
            System.out.println("Result: additionalStiffness = " + result.result.hasAdditionalStiffness());

            // Check if this is a new path
            String pathSig = result.pathConstraint;
            boolean isNew = exploredPathSignatures.add(pathSig);
            if (isNew && iteration > 1) {
                System.out.println("NEW EXECUTION PATH DISCOVERED!");
            }

            // Generate negation candidates from this path's constraints (DART strategy)
            // For each constraint Ci, build: C1 ∧ ... ∧ C(i-1) ∧ ¬Ci
            List<Expression> pathConstraints = result.constraints;
            for (int i = pathConstraints.size() - 1; i >= 0; i--) {
                Expression negated = GaletteSymbolicator.negateConstraint(pathConstraints.get(i));
                if (negated == null) continue;

                Expression candidate;
                if (i == 0) {
                    candidate = negated;
                } else {
                    List<Expression> prefix = new ArrayList<>();
                    for (int j = 0; j < i; j++) {
                        prefix.add(pathConstraints.get(j));
                    }
                    prefix.add(negated);
                    candidate = GaletteSymbolicator.conjoin(prefix);
                }

                String candidateSig = candidate.toString();
                if (!triedCandidates.contains(candidateSig)) {
                    triedCandidates.add(candidateSig);
                    worklist.add(candidate);
                }
            }

            // Find the next input from the worklist
            Double solvedInput = null;
            while (!worklist.isEmpty() && solvedInput == null) {
                Expression candidate = worklist.poll();
                String varName = GaletteSymbolicator.extractVariableName(candidate);
                if (varName == null) continue;

                System.out.println("\n=== Solving Candidate ===");
                System.out.println("Constraint: " + candidate);

                Double candidateValue = GaletteSymbolicator.solveConstraintForVariable(candidate, varName);

                if (candidateValue != null) {
                    // Skip values we've already explored
                    if (exploredInputs.contains(candidateValue)) {
                        System.out.println("Solver generated: " + candidateValue + " (already explored, skipping)");
                        continue;
                    }
                    System.out.println("Solver generated: " + varName + " = " + candidateValue);
                    solvedInput = candidateValue;
                } else {
                    System.out.println("UNSAT or solver failed");
                }
            }

            if (solvedInput == null) {
                System.out.println("\nAll candidate paths explored or unsatisfiable. Exploration complete.");
                break;
            }

            nextInput = solvedInput;
        }

        // Summary
        System.out.println("\nCONCOLIC ANALYSIS SUMMARY");
        System.out.println("Total iterations: " + iteration);
        System.out.println("Unique execution paths: " + exploredPathSignatures.size());

        System.out.println("\nExplored inputs and their path constraints:");
        for (int i = 0; i < exploredInputs.size(); i++) {
            System.out.println("  Input " + exploredInputs.get(i) + " mm -> " + pathConstraintStrings.get(i));
        }
    }

    /**
     * Container for concolic execution results.
     */
    private static class ConcolicResult {
        final BrakeDiscTarget result;
        final String pathConstraint;
        final List<Expression> constraints;

        ConcolicResult(BrakeDiscTarget result, String pathConstraint, List<Expression> constraints) {
            this.result = result;
            this.pathConstraint = pathConstraint;
            this.constraints = constraints;
        }
    }

    /**
     * Execute transformation with concolic analysis.
     * Returns the transformation result, the path constraint as a string,
     * and the individual constraint expressions for DART-style negation.
     */
    private static ConcolicResult executeConcolic(BrakeDiscSource source, double thickness, String label) {
        // Reset symbolic execution state (both knarr-runtime and galette-agent constraints)
        GaletteSymbolicator.reset();
        PathUtils.resetPC();
        GalettePathConstraintBridge.resetGaletteConstraints();

        // Create symbolic value for thickness
        Tag symbolicTag = GaletteSymbolicator.makeSymbolicDouble(label, thickness);
        double taggedThickness = edu.neu.ccs.prl.galette.internal.runtime.Tainter.setTag(thickness, symbolicTag);

        // Execute the transformation with the tagged value
        BrakeDiscTarget result = BrakeDiscTransformation.transform(source, taggedThickness);

        // Collect path constraints (individual expressions for negation)
        PathConditionWrapper pc = PathUtils.getCurPCWithGalette();
        String constraintDescription = "no constraints";
        List<Expression> constraintList = new ArrayList<>();

        if (pc != null && !pc.isEmpty()) {
            constraintList = pc.getConstraints();
            Expression conjunction = pc.toSingleExpression();
            if (conjunction != null) {
                constraintDescription = conjunction.toString();
            } else {
                constraintDescription = "constraints collected: " + pc.size();
            }
        }

        return new ConcolicResult(result, constraintDescription, constraintList);
    }
}
