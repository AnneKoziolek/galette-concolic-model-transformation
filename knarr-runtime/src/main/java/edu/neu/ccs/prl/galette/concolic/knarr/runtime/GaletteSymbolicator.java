package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import edu.neu.ccs.prl.galette.concolic.knarr.green.GaletteGreenBridge;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.util.Configuration;

/**
 * Galette-based symbolic execution engine.
 *
 * This class migrates Knarr's Phosphor-based Symbolicator to use Galette APIs.
 * It handles symbolic value creation, constraint solving, and input generation.
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class GaletteSymbolicator {

    /**
     * Server connection for constraint solving.
     */
    static Socket serverConnection;

    /**
     * Server configuration.
     */
    static String SERVER_HOST = System.getProperty("SATServer", "127.0.0.1");

    static int SERVER_PORT = Integer.valueOf(System.getProperty("SATPort", "9408"));

    /**
     * Current solution from constraint solver.
     */
    static InputSolution mySoln = null;

    /**
     * Debug flag.
     */
    public static final boolean DEBUG = Boolean.valueOf(System.getProperty("DEBUG", "true"));

    /**
     * Toggle to enable solving with Green instead of the heuristic extractor.
     * Can be set via -Dgalette.useGreenSolver=true or env GALETTE_USE_GREEN_SOLVER=true.
     */
    private static final boolean USE_GREEN_SOLVER = Boolean.parseBoolean(System.getProperty(
            "galette.useGreenSolver", System.getenv().getOrDefault("GALETTE_USE_GREEN_SOLVER", "false")));

    /**
     * Toggle to use external GreenServer process instead of in-process Green solver.
     * This avoids instrumentation conflicts when Galette instruments the Green bytecode.
     * Can be set via -Dgalette.useExternalGreenServer=true.
     */
    private static final boolean USE_EXTERNAL_SERVER =
            Boolean.parseBoolean(System.getProperty("galette.useExternalGreenServer", "false"));

    /**
     * Lazy initialized Green solver instance for model queries.
     */
    private static volatile Green greenSolver;

    /**
     * Mutex to guard Green solver init.
     */
    private static final Object GREEN_INIT_LOCK = new Object();

    /**
     * Separate Green solver instance for model extraction (with Z3 model service).
     */
    private static volatile Green greenModelSolver;

    /**
     * Internal class name for bytecode instrumentation.
     */
    public static final String INTERNAL_NAME = "edu/neu/ccs/prl/galette/concolic/knarr/runtime/GaletteSymbolicator";

    /**
     * Counter for generating unique symbolic variable names.
     */
    private static final AtomicInteger symbolCounter = new AtomicInteger(0);

    /**
     * Map from concrete values to their symbolic representations.
     */
    private static final ConcurrentHashMap<Object, Tag> valueToTag = new ConcurrentHashMap<>();

    /**
     * Map from tags to their Green expressions.
     */
    private static final ConcurrentHashMap<Tag, Expression> tagToExpression = new ConcurrentHashMap<>();

    static {
        initializeSymbolicator();
    }

    /**
     * Initialize the symbolicator.
     */
    private static void initializeSymbolicator() {
        if (DEBUG) {
            System.out.println("Initializing GaletteSymbolicator");
            System.out.println("Server: " + SERVER_HOST + ":" + SERVER_PORT);
        }

        // Setup shutdown hook to clean up resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                cleanup();
            } catch (Exception e) {
                System.err.println("Error during Symbolicator cleanup: " + e.getMessage());
            }
        }));
    }

    /**
     * Create a symbolic integer value.
     *
     * @param label The label for the symbolic value
     * @param concreteValue The concrete value to associate
     * @return Tag representing the symbolic value
     */
    public static Tag makeSymbolicInt(String label, int concreteValue) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }

        try {
            // Check label validity
            GalettePathUtils.checkLabelAndInitJPF(label);

            // Create Galette tag
            Tag symbolicTag = Tag.of(label);

            // Create Green expression
            IntVariable var = new IntVariable(label, null, null);
            tagToExpression.put(symbolicTag, var);
            valueToTag.put(concreteValue, symbolicTag);

            if (DEBUG) {
                System.out.println("Created symbolic int: " + label + " = " + concreteValue);
            }

            return symbolicTag;
        } catch (Exception e) {
            System.err.println("Error creating symbolic int: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a symbolic long value.
     *
     * @param label The label for the symbolic value
     * @param concreteValue The concrete value to associate
     * @return Tag representing the symbolic value
     */
    public static Tag makeSymbolicLong(String label, long concreteValue) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }

        try {
            GalettePathUtils.checkLabelAndInitJPF(label);

            Tag symbolicTag = Tag.of(label);

            // Use IntVariable for longs too (Green solver limitation)
            IntVariable var = new IntVariable(label, null, null);
            tagToExpression.put(symbolicTag, var);
            valueToTag.put(concreteValue, symbolicTag);

            if (DEBUG) {
                System.out.println("Created symbolic long: " + label + " = " + concreteValue);
            }

            return symbolicTag;
        } catch (Exception e) {
            System.err.println("Error creating symbolic long: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a symbolic double value.
     *
     * @param label The label for the symbolic value
     * @param concreteValue The concrete value to associate
     * @return Tag representing the symbolic value
     */
    public static Tag makeSymbolicDouble(String label, double concreteValue) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }

        try {
            GalettePathUtils.checkLabelAndInitJPF(label);

            Tag symbolicTag = Tag.of(label);

            // Use Galette's Tainter to associate the tag with the value
            double taggedValue = edu.neu.ccs.prl.galette.internal.runtime.Tainter.setTag(concreteValue, symbolicTag);

            // Use proper bounds for serialization compatibility (needed for GreenServer)
            RealVariable var = new RealVariable(label, Double.MIN_VALUE, Double.MAX_VALUE);
            tagToExpression.put(symbolicTag, var);
            valueToTag.put(taggedValue, symbolicTag);

            if (DEBUG) {
                System.out.println("Created symbolic double with Galette tagging: " + label + " = " + concreteValue);
            }

            return symbolicTag;
        } catch (Exception e) {
            System.err.println("Error creating symbolic double: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a symbolic string value.
     *
     * @param label The label for the symbolic value
     * @param concreteValue The concrete value to associate
     * @return Tag representing the symbolic value
     */
    public static Tag makeSymbolicString(String label, String concreteValue) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }

        try {
            GalettePathUtils.checkLabelAndInitJPF(label);

            Tag symbolicTag = Tag.of(label);

            StringVariable var = new StringVariable(label);
            tagToExpression.put(symbolicTag, var);
            valueToTag.put(concreteValue, symbolicTag);

            if (DEBUG) {
                System.out.println("Created symbolic string: " + label + " = \"" + concreteValue + "\"");
            }

            return symbolicTag;
        } catch (Exception e) {
            System.err.println("Error creating symbolic string: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the Green expression for a tag.
     *
     * @param tag The tag to look up
     * @return Corresponding Green expression, or null if not found
     */
    public static Expression getExpressionForTag(Tag tag) {
        return tagToExpression.get(tag);
    }

    /**
     * Get the tag for a concrete value.
     *
     * @param value The concrete value
     * @return Corresponding tag, or null if not symbolic
     */
    public static Tag getTagForValue(Object value) {
        return valueToTag.get(value);
    }

    /**
     * Initialize Green solver when enabled. Uses Choco3 SAT solving without external dependencies.
     * This approach uses constraint satisfiability checking to validate thresholds
     * and generate alternative inputs based on negated path conditions.
     */
    private static Green ensureGreenSolver() {
        if (greenSolver != null) {
            return greenSolver;
        }

        synchronized (GREEN_INIT_LOCK) {
            if (greenSolver != null) {
                return greenSolver;
            }

            try {
                if (DEBUG) {
                    System.out.println("[Green] Initializing Green solver (Z3 Java)");
                }
                Green solver = new Green();

                Properties props = new Properties();
                // Use Z3 Java SAT pipeline (slice -> canonize -> z3java)
                props.setProperty("green.services", "sat");
                props.setProperty("green.service.sat", "(slice (canonize z3java))");
                props.setProperty("green.service.sat.slice", "za.ac.sun.cs.green.service.slicer.SATSlicerService");
                props.setProperty(
                        "green.service.sat.canonize", "za.ac.sun.cs.green.service.canonizer.SATCanonizerService");
                props.setProperty("green.service.sat.z3java", "za.ac.sun.cs.green.service.z3.SATZ3JavaService");
                // Optional: timeout in ms for Z3 Java service
                props.setProperty("green.z3java.timeout", "5000");

                Configuration config = new Configuration(solver, props);
                config.configure();

                greenSolver = solver;
                if (DEBUG) {
                    System.out.println("[Green] Solver configured (Z3 Java)");
                }
            } catch (Exception e) {
                System.err.println("Failed to configure Green solver: " + e.getMessage());
                if (DEBUG) {
                    e.printStackTrace();
                }
                greenSolver = null;
            }

            return greenSolver;
        }
    }

    /**
     * Initialize Green solver with MODEL service for extracting variable assignments.
     * This uses Z3's model extraction to get actual satisfying values.
     */
    private static Green ensureGreenModelSolver() {
        if (greenModelSolver != null) {
            return greenModelSolver;
        }

        synchronized (GREEN_INIT_LOCK) {
            if (greenModelSolver != null) {
                return greenModelSolver;
            }

            try {
                if (DEBUG) {
                    System.out.println("[Green Model] Initializing Green solver with Z3 model extraction");
                }
                Green solver = new Green();

                Properties props = new Properties();
                // Use Z3 Java MODEL pipeline (slice -> canonize -> z3java model)
                props.setProperty("green.services", "model");
                props.setProperty("green.service.model", "(slice (canonize z3javamodel))");
                props.setProperty("green.service.model.slice", "za.ac.sun.cs.green.service.slicer.SATSlicerService");
                props.setProperty(
                        "green.service.model.canonize", "za.ac.sun.cs.green.service.canonizer.ModelCanonizerService");
                props.setProperty(
                        "green.service.model.z3javamodel", "za.ac.sun.cs.green.service.z3.ModelZ3JavaService");
                // Timeout in ms for Z3 Java service
                props.setProperty("green.z3java.timeout", "5000");

                Configuration config = new Configuration(solver, props);
                config.configure();

                greenModelSolver = solver;
                if (DEBUG) {
                    System.out.println("[Green Model] Solver configured with Z3 model extraction");
                }
            } catch (Exception e) {
                System.err.println("Failed to configure Green model solver: " + e.getMessage());
                if (DEBUG) {
                    e.printStackTrace();
                }
                greenModelSolver = null;
            }

            return greenModelSolver;
        }
    }

    /**
     * Solve a constraint and return the value for a specific variable.
     * This is the primary method for solver-based value generation in the exploration loop.
     *
     * @param constraint The constraint to solve (e.g., "x > 80.0")
     * @param variableName The name of the variable to extract from the model
     * @return The solver-computed value for the variable, or null if unsolvable
     */
    public static Double solveConstraintForVariable(Expression constraint, String variableName) {
        if (constraint == null || variableName == null) {
            return null;
        }

        if (DEBUG) {
            System.out.println("[Solver] Solving constraint for variable '" + variableName + "': " + constraint);
        }

        // Try external server first if configured
        if (USE_EXTERNAL_SERVER) {
            InputSolution externalSolution = sendConstraintToServer(constraint);
            if (externalSolution != null) {
                Object value = externalSolution.getValue(variableName);
                if (value instanceof Number) {
                    double result = ((Number) value).doubleValue();
                    if (DEBUG) {
                        System.out.println("[Solver] External server returned: " + variableName + " = " + result);
                    }
                    return result;
                }
            }
        }

        // Try in-process Z3 model extraction
        InputSolution modelSolution = solveWithGreenModel(constraint);
        if (modelSolution != null) {
            Object value = modelSolution.getValue(variableName);
            if (value instanceof Number) {
                double result = ((Number) value).doubleValue();
                if (DEBUG) {
                    System.out.println("[Solver] Z3 model returned: " + variableName + " = " + result);
                }
                return result;
            }
            // Try base name without suffix
            for (String label : modelSolution.getLabels()) {
                if (label.startsWith(variableName)) {
                    Object labelValue = modelSolution.getValue(label);
                    if (labelValue instanceof Number) {
                        double result = ((Number) labelValue).doubleValue();
                        if (DEBUG) {
                            System.out.println("[Solver] Z3 model returned (via " + label + "): " + result);
                        }
                        return result;
                    }
                }
            }
        }

        if (DEBUG) {
            System.out.println("[Solver] Could not solve constraint for variable: " + variableName);
        }
        return null;
    }

    /**
     * Generate a constraint for exploring a specific branch direction at a threshold.
     * This creates the appropriate constraint for the unexplored path.
     *
     * @param variableName The symbolic variable name (e.g., "thickness")
     * @param threshold The threshold value discovered from path constraints
     * @param exploreBelow True to explore "var < threshold", false for "var >= threshold"
     * @return A constraint expression for the unexplored branch
     */
    public static Expression buildExplorationConstraint(String variableName, double threshold, boolean exploreBelow) {
        RealVariable var = new RealVariable(variableName, Double.MIN_VALUE, Double.MAX_VALUE);
        RealConstant thresholdConst = new RealConstant(threshold);

        Expression constraint;
        if (exploreBelow) {
            // Explore below: var < threshold
            constraint = new BinaryOperation(Operation.Operator.LT, var, thresholdConst);
        } else {
            // Explore above: var > threshold (strict GT to flip a > branch)
            constraint = new BinaryOperation(Operation.Operator.GT, var, thresholdConst);
        }

        if (DEBUG) {
            System.out.println("[Solver] Built exploration constraint: " + constraint);
        }
        return constraint;
    }

    /**
     * Generate input for an unexplored branch using the constraint solver.
     * This is the main entry point for solver-based exploration.
     *
     * @param variableName The variable name to generate input for
     * @param threshold The threshold value at the branch point
     * @param exploreBelow True to explore below threshold, false for above
     * @return A solver-computed value, or null if the solver fails
     */
    public static Double generateInputForBranch(String variableName, double threshold, boolean exploreBelow) {
        if (DEBUG) {
            System.out.println(
                    "[Solver] Generating input for " + variableName + (exploreBelow ? " < " : " > ") + threshold);
        }

        // Build the constraint for the target branch
        Expression constraint = buildExplorationConstraint(variableName, threshold, exploreBelow);

        // Solve and get the value
        Double solverValue = solveConstraintForVariable(constraint, variableName);

        if (solverValue != null) {
            if (DEBUG) {
                System.out.println("[Solver] Generated input from Z3: " + variableName + " = " + solverValue);
            }
            return solverValue;
        }

        // Solver failed - return null (no heuristic fallback)
        if (DEBUG) {
            System.out.println("[Solver] Z3 solver failed to generate input - NO HEURISTIC FALLBACK");
        }
        return null;
    }

    /**
     * Extract variable values from Z3 model using Green's model service.
     * This is the real constraint solving - no heuristics.
     *
     * @param constraint The constraint to solve
     * @return InputSolution with actual Z3 model values, or null if unsatisfiable
     */
    @SuppressWarnings("unchecked")
    private static InputSolution solveWithGreenModel(Expression constraint) {
        Green solver = ensureGreenModelSolver();
        if (solver == null) {
            if (DEBUG) {
                System.out.println("[Green Model] Model solver not available");
            }
            return null;
        }

        try {
            if (DEBUG) {
                System.out.println("[Green Model] Solving constraint with Z3 model extraction: " + constraint);
            }

            Instance instance = new Instance(solver, null, constraint);

            // Request model - returns Map<Variable, Object> with variable assignments
            Object result = instance.request("model");

            if (result == null) {
                if (DEBUG) {
                    System.out.println("[Green Model] Constraint is UNSAT or model extraction failed");
                }
                return null;
            }

            if (!(result instanceof Map)) {
                if (DEBUG) {
                    System.out.println("[Green Model] Unexpected result type: "
                            + result.getClass().getName());
                }
                return null;
            }

            Map<Variable, Object> model = (Map<Variable, Object>) result;

            if (DEBUG) {
                System.out.println("[Green Model] Z3 returned model with " + model.size() + " variable assignments:");
                for (Map.Entry<Variable, Object> entry : model.entrySet()) {
                    System.out.println("  " + entry.getKey().getName() + " = " + entry.getValue());
                }
            }

            // Convert model to InputSolution
            InputSolution solution = new InputSolution();
            solution.setValue("satisfiable", "YES");
            solution.setValue("solver", "z3-model-extraction");

            for (Map.Entry<Variable, Object> entry : model.entrySet()) {
                String varName = entry.getKey().getName();
                Object value = entry.getValue();

                // Store the actual Z3-computed value
                solution.setValue(varName, value);

                // Also try to match to the original variable name (without counter suffix)
                // Variables are created with names like "thickness_0", we need "thickness"
                int underscoreIdx = varName.lastIndexOf('_');
                if (underscoreIdx > 0) {
                    String baseName = varName.substring(0, underscoreIdx);
                    // Only add base name if it's not already present
                    if (solution.getValue(baseName) == null) {
                        solution.setValue(baseName, value);
                    }
                }
            }

            if (DEBUG) {
                System.out.println("[Green Model] Generated solution from Z3 model: " + solution);
            }

            return solution;

        } catch (Exception e) {
            System.err.println("[Green Model] Z3 model extraction failed: " + e.getMessage());
            if (DEBUG) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Attempt solving via Green with Z3 model extraction for real constraint solving.
     * NO HEURISTIC FALLBACK - only uses Z3 model extraction.
     * When USE_EXTERNAL_SERVER is true, delegates to external GreenServer process.
     */
    private static InputSolution solveWithGreen(Expression constraint) {
        if (!USE_GREEN_SOLVER) {
            if (DEBUG) {
                System.out.println("[Green] USE_GREEN_SOLVER is not set, solver disabled.");
            }
            return null;
        }

        // Validate constraint before attempting Green solving
        if (constraint == null) {
            if (DEBUG) {
                System.out.println("[Green] Cannot use solver: constraint is null");
            }
            return null;
        }

        // Use external GreenServer if configured (avoids Galette instrumentation of Green bytecode)
        if (USE_EXTERNAL_SERVER) {
            if (DEBUG) {
                System.out.println(
                        "[External Server] Using external GreenServer at " + SERVER_HOST + ":" + SERVER_PORT);
            }
            InputSolution externalResult = sendConstraintToServer(constraint);
            if (externalResult != null) {
                return externalResult;
            }
            if (DEBUG) {
                System.out.println("[External Server] External server failed, trying in-process Z3");
            }
            // Fall through to try in-process model extraction
        }

        // Use Z3 model extraction for REAL constraint solving - NO HEURISTICS
        InputSolution modelSolution = solveWithGreenModel(constraint);
        if (modelSolution != null) {
            if (DEBUG) {
                System.out.println("[Green] Z3 model extraction succeeded");
            }
            return modelSolution;
        }

        // Z3 model extraction failed - NO HEURISTIC FALLBACK
        if (DEBUG) {
            System.out.println("[Green] Z3 model extraction failed - returning null (no heuristic fallback)");
        }
        return null;
    }

    /**
     * @deprecated This method uses heuristics and should not be used.
     * Use {@link #generateInputForBranch(String, double, boolean)} for solver-based input generation.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static InputSolution solveWithGreenSatOnly(Expression constraint) {
        // SAT-only check without model extraction - DEPRECATED
        Green solver = ensureGreenSolver();
        if (solver == null) {
            return null;
        }

        try {
            Instance instance = new Instance(solver, null, constraint);
            if (instance == null || instance.getExpression() == null || instance.getFullExpression() == null) {
                return null;
            }

            Boolean isSatisfiable = (Boolean) instance.request("sat");

            if (isSatisfiable != null && isSatisfiable) {
                // SAT but no model - return empty solution indicating satisfiability
                InputSolution satOnlySolution = new InputSolution();
                satOnlySolution.setValue("satisfiable", "YES");
                satOnlySolution.setValue("solver", "green-sat-only");
                return satOnlySolution;
            }
            return null;
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("[Green SAT] Failed: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Solve the current path condition and get a new input.
     * Uses Z3 model extraction - NO HEURISTIC FALLBACK.
     *
     * @return New input solution, or null if unsatisfiable or solver unavailable
     */
    public static InputSolution solvePathCondition() {
        try {
            PathConditionWrapper pc = GalettePathUtils.getCurPC();
            if (pc == null || pc.isEmpty()) {
                if (DEBUG) {
                    System.out.println("[Solver] No path constraints to solve");
                }
                return null;
            }

            Expression constraint = pc.toSingleExpression();
            if (constraint == null) {
                if (DEBUG) {
                    System.out.println("[Solver] Path condition could not be converted to single expression");
                }
                return null;
            }

            if (DEBUG) {
                System.out.println("[Solver] Solving path condition: " + constraint);
            }

            // Use Z3 solver - NO HEURISTIC FALLBACK
            InputSolution solution = solveWithGreen(constraint);
            if (solution != null) {
                if (DEBUG) {
                    System.out.println("[Solver] Z3 solution: " + solution);
                }
                return solution;
            }

            // Z3 solver failed or disabled - return null (no heuristics)
            if (DEBUG) {
                System.out.println("[Solver] Z3 solver returned null - no heuristic fallback");
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error solving path condition: " + e.getMessage());
            if (DEBUG) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Extract variable assignments from a constraint expression.
     * Migrated from original Knarr's dynamic constraint solving approach.
     * @deprecated This method uses heuristics and should not be used.
     * Use Z3 model extraction via {@link #solveWithGreenModel(Expression)} instead.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static void extractSolutionFromConstraint(Expression constraint, InputSolution solution) {
        try {
            // Generate alternative values using original Knarr's VariableMutator pattern
            Map<String, Object> alternativeValues = generateAlternativeValues(constraint);

            for (Map.Entry<String, Object> entry : alternativeValues.entrySet()) {
                solution.setValue(entry.getKey(), entry.getValue());
            }

            // Add constraint information to solution
            solution.setValue("constraint", constraint.toString());
            solution.setValue("satisfiable", "YES");

        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Error extracting solution: " + e.getMessage());
            }
            solution.setValue("satisfiable", "UNKNOWN");
        }
    }

    /**
     * @deprecated This method uses heuristics (threshold ± 0.1) and should not be used.
     * Use {@link #generateInputForBranch(String, double, boolean)} for solver-based input generation.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static Map<String, Object> generateAlternativeValues(Expression constraint) {
        Map<String, Object> alternatives = new HashMap<>();

        // Extract all variables from the constraint
        Set<String> variables = extractVariableNames(constraint);

        for (String variable : variables) {
            // Find the threshold value from the constraint involving this variable
            Double thresholdValue = extractThresholdForVariable(constraint, variable);
            if (thresholdValue != null) {
                // Generate alternative value using original Knarr pattern
                Double alternativeValue = generateAlternativeValue(constraint, variable, thresholdValue);
                if (alternativeValue != null) {
                    alternatives.put(variable, alternativeValue);
                }
            }
        }

        return alternatives;
    }

    /**
     * Extract all variable names from a constraint expression.
     */
    private static Set<String> extractVariableNames(Expression expr) {
        Set<String> variables = new HashSet<>();
        extractVariableNamesRecursive(expr, variables);
        return variables;
    }

    /**
     * Recursively extract variable names from expression tree.
     */
    private static void extractVariableNamesRecursive(Expression expr, Set<String> variables) {
        if (expr instanceof Variable) {
            variables.add(((Variable) expr).getName());
        } else if (expr instanceof BinaryOperation) {
            BinaryOperation binOp = (BinaryOperation) expr;
            extractVariableNamesRecursive(binOp.left, variables);
            extractVariableNamesRecursive(binOp.right, variables);
        } else if (expr instanceof UnaryOperation) {
            UnaryOperation unOp = (UnaryOperation) expr;
            extractVariableNamesRecursive(unOp.getOperand(0), variables);
        }
    }

    /**
     * Extract threshold value for a specific variable from constraint.
     */
    private static Double extractThresholdForVariable(Expression expr, String targetVariable) {
        if (expr instanceof BinaryOperation) {
            BinaryOperation binOp = (BinaryOperation) expr;

            // Check if this is a comparison involving the target variable
            if (isComparisonOperator(binOp.getOperator())) {
                String variable = extractVariableName(binOp);
                if (targetVariable.equals(variable)) {
                    return extractConstantValue(binOp);
                }
            }

            // Recursively search in operands
            Double leftResult = extractThresholdForVariable(binOp.left, targetVariable);
            if (leftResult != null) return leftResult;

            return extractThresholdForVariable(binOp.right, targetVariable);
        }

        return null;
    }

    /**
     * @deprecated This method uses heuristics (threshold ± 0.1) and should not be used.
     * Use {@link #generateInputForBranch(String, double, boolean)} for solver-based input generation.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static Double generateAlternativeValue(Expression constraint, String variable, Double threshold) {
        Operation.Operator constraintOp = extractOperatorForVariable(constraint, variable);

        if (constraintOp == null) return null;

        // Generate alternative value by negating the constraint logic
        switch (constraintOp) {
            case GT: // Original: x > threshold → Generate: x <= threshold
                return threshold - 0.1;
            case GE: // Original: x >= threshold → Generate: x < threshold
                return threshold - 0.1;
            case LT: // Original: x < threshold → Generate: x >= threshold
                return threshold + 0.1;
            case LE: // Original: x <= threshold → Generate: x > threshold
                return threshold + 0.1;
            case EQ: // Original: x == threshold → Generate: x != threshold
                return threshold + 1.0;
            case NE: // Original: x != threshold → Generate: x == threshold
                return threshold;
            default:
                return threshold + 1.0; // Default: generate different value
        }
    }

    /**
     * Extract operator for a specific variable from constraint.
     */
    private static Operation.Operator extractOperatorForVariable(Expression expr, String targetVariable) {
        if (expr instanceof BinaryOperation) {
            BinaryOperation binOp = (BinaryOperation) expr;

            if (isComparisonOperator(binOp.getOperator())) {
                String variable = extractVariableName(binOp);
                if (targetVariable.equals(variable)) {
                    return binOp.getOperator();
                }
            }

            // Recursively search in operands
            Operation.Operator leftResult = extractOperatorForVariable(binOp.left, targetVariable);
            if (leftResult != null) return leftResult;

            return extractOperatorForVariable(binOp.right, targetVariable);
        }

        return null;
    }

    /**
     * Check if operator is a comparison operator.
     */
    private static boolean isComparisonOperator(Operation.Operator op) {
        return op == Operation.Operator.GT
                || op == Operation.Operator.GE
                || op == Operation.Operator.LT
                || op == Operation.Operator.LE
                || op == Operation.Operator.EQ
                || op == Operation.Operator.NE;
    }

    /**
     * Extract variable name from binary operation (assumes one operand is variable, other is constant).
     */
    private static String extractVariableName(BinaryOperation binOp) {
        Expression left = binOp.left;
        Expression right = binOp.right;

        if (left instanceof Variable) {
            return ((Variable) left).getName();
        } else if (right instanceof Variable) {
            return ((Variable) right).getName();
        }

        return null;
    }

    /**
     * Extract constant value from binary operation.
     */
    private static Double extractConstantValue(BinaryOperation binOp) {
        Expression left = binOp.left;
        Expression right = binOp.right;

        if (left instanceof RealConstant) {
            return ((RealConstant) left).getValue();
        } else if (right instanceof RealConstant) {
            return ((RealConstant) right).getValue();
        } else if (left instanceof IntConstant) {
            return (double) ((IntConstant) left).getValue();
        } else if (right instanceof IntConstant) {
            return (double) ((IntConstant) right).getValue();
        }

        return null;
    }

    /**
     * Connect to the constraint solving server.
     *
     * @return True if connection successful, false otherwise
     */
    public static boolean connectToServer() {
        try {
            if (serverConnection != null && !serverConnection.isClosed()) {
                return true; // Already connected
            }

            serverConnection = new Socket(SERVER_HOST, SERVER_PORT);

            if (DEBUG) {
                System.out.println("Connected to constraint server: " + SERVER_HOST + ":" + SERVER_PORT);
            }

            return true;
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + SERVER_HOST);
            return false;
        } catch (IOException e) {
            if (DEBUG) {
                System.err.println("Could not connect to server: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Send constraint to server for solving using GreenServer JSON protocol.
     * Protocol v2: JSON expression → JSON response with model values
     * Response format: {"sat":true/false,"model":{"var1":value1,"var2":value2,...}}
     * Legacy fallback: single char response ('1'=SAT, '0'=UNSAT, 'E'=Error)
     *
     * @param constraint The constraint to solve
     * @return Solution from server, or null if failed
     */
    public static InputSolution sendConstraintToServer(Expression constraint) {
        try {
            if (!connectToServer()) {
                if (DEBUG) {
                    System.out.println(
                            "[External Server] Could not connect to GreenServer at " + SERVER_HOST + ":" + SERVER_PORT);
                }
                return null;
            }

            // Convert Expression to JSON (avoids serialization version mismatch)
            String jsonExpr = ExpressionJsonConverter.toJson(constraint);

            // Send as text line (GreenServer reads lines with BufferedReader)
            PrintWriter out = new PrintWriter(serverConnection.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));

            if (DEBUG) {
                System.out.println("[External Server] Sending JSON constraint to GreenServer: "
                        + jsonExpr.substring(0, Math.min(100, jsonExpr.length())) + "...");
            }

            out.println(jsonExpr);

            // Read response line (new protocol returns JSON with model)
            String responseLine = in.readLine();

            if (responseLine == null || responseLine.isEmpty()) {
                if (DEBUG) {
                    System.out.println("[External Server] Empty response from server");
                }
                return null;
            }

            if (DEBUG) {
                System.out.println("[External Server] GreenServer response: " + responseLine);
            }

            // Check if response is JSON (new protocol) or single char (legacy)
            if (responseLine.startsWith("{")) {
                // New JSON protocol with model extraction
                return parseJsonModelResponse(responseLine, constraint);
            } else {
                // Legacy single-char protocol fallback
                char response = responseLine.charAt(0);
                return handleLegacyResponse(response, constraint);
            }

        } catch (Exception e) {
            System.err.println("[External Server] Error communicating with GreenServer: " + e.getMessage());
            if (DEBUG) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Parse JSON model response from GreenServer.
     * Format: {"sat":true,"model":{"var1":value1,"var2":value2,...}}
     */
    private static InputSolution parseJsonModelResponse(String json, Expression constraint) {
        try {
            // Simple JSON parsing for model response
            if (!json.contains("\"sat\"")) {
                if (DEBUG) {
                    System.out.println("[External Server] Invalid JSON response: missing 'sat' field");
                }
                return null;
            }

            boolean isSat = json.contains("\"sat\":true") || json.contains("\"sat\": true");

            if (!isSat) {
                if (DEBUG) {
                    System.out.println("[External Server] Constraint is UNSAT");
                }
                return null;
            }

            InputSolution solution = new InputSolution();
            solution.setValue("satisfiable", "YES");
            solution.setValue("solver", "greenserver-z3-model");

            // Extract model values from JSON
            int modelStart = json.indexOf("\"model\"");
            if (modelStart >= 0) {
                int braceStart = json.indexOf("{", modelStart);
                if (braceStart >= 0) {
                    int braceEnd = findMatchingBrace(json, braceStart);
                    if (braceEnd > braceStart) {
                        String modelJson = json.substring(braceStart, braceEnd + 1);
                        parseModelValues(modelJson, solution);
                    }
                }
            }

            // If no model values extracted, return solution with just SAT status (no heuristics)
            if (solution.getLabels().size() <= 2) { // only "satisfiable" and "solver"
                if (DEBUG) {
                    System.out.println("[External Server] No model values in response - NO HEURISTIC FALLBACK");
                }
                // Return null to indicate solver couldn't provide values
                return null;
            }

            if (DEBUG) {
                System.out.println("[External Server] Parsed solution with model values: " + solution);
            }
            return solution;

        } catch (Exception e) {
            System.err.println("[External Server] Error parsing JSON response: " + e.getMessage());
            if (DEBUG) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Find the matching closing brace for an opening brace.
     */
    private static int findMatchingBrace(String json, int openPos) {
        int depth = 1;
        for (int i = openPos + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            } else if (c == '"') {
                // Skip string content
                i++;
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\') i++;
                    i++;
                }
            }
        }
        return -1;
    }

    /**
     * Parse model values from JSON object string.
     * Format: {"var1":value1,"var2":value2,...}
     */
    private static void parseModelValues(String modelJson, InputSolution solution) {
        // Remove outer braces
        String content = modelJson.substring(1, modelJson.length() - 1).trim();
        if (content.isEmpty()) return;

        // Split by comma (simple parsing, assumes no nested objects in values)
        int pos = 0;
        while (pos < content.length()) {
            // Skip whitespace
            while (pos < content.length() && Character.isWhitespace(content.charAt(pos))) pos++;
            if (pos >= content.length()) break;

            // Parse key
            if (content.charAt(pos) != '"') break;
            int keyStart = pos + 1;
            int keyEnd = content.indexOf('"', keyStart);
            if (keyEnd < 0) break;
            String key = content.substring(keyStart, keyEnd);
            pos = keyEnd + 1;

            // Skip to colon
            while (pos < content.length() && content.charAt(pos) != ':') pos++;
            if (pos >= content.length()) break;
            pos++; // skip colon

            // Skip whitespace
            while (pos < content.length() && Character.isWhitespace(content.charAt(pos))) pos++;
            if (pos >= content.length()) break;

            // Parse value
            Object value = null;
            if (content.charAt(pos) == '"') {
                // String value
                int valStart = pos + 1;
                int valEnd = content.indexOf('"', valStart);
                if (valEnd >= 0) {
                    value = content.substring(valStart, valEnd);
                    pos = valEnd + 1;
                }
            } else {
                // Numeric or boolean value
                int valStart = pos;
                while (pos < content.length() && content.charAt(pos) != ',' && content.charAt(pos) != '}') {
                    pos++;
                }
                String valStr = content.substring(valStart, pos).trim();
                if (valStr.equals("true")) value = true;
                else if (valStr.equals("false")) value = false;
                else if (valStr.equals("null")) value = null;
                else {
                    try {
                        if (valStr.contains(".") || valStr.contains("e") || valStr.contains("E")) {
                            value = Double.parseDouble(valStr);
                        } else {
                            value = Long.parseLong(valStr);
                        }
                    } catch (NumberFormatException e) {
                        value = valStr;
                    }
                }
            }

            if (key != null && value != null) {
                solution.setValue(key, value);
                // Also add base name without suffix
                int underscoreIdx = key.lastIndexOf('_');
                if (underscoreIdx > 0) {
                    String baseName = key.substring(0, underscoreIdx);
                    if (solution.getValue(baseName) == null) {
                        solution.setValue(baseName, value);
                    }
                }
            }

            // Skip comma
            while (pos < content.length()
                    && (content.charAt(pos) == ',' || Character.isWhitespace(content.charAt(pos)))) {
                pos++;
            }
        }
    }

    /**
     * Handle legacy single-character response from GreenServer.
     */
    private static InputSolution handleLegacyResponse(char response, Expression constraint) {
        // Legacy protocol only returns SAT/UNSAT - no model values available
        // Without model values, we cannot generate inputs - return null (no heuristic fallback)
        if (response == '1') {
            // SAT - but legacy protocol doesn't provide model values
            if (DEBUG) {
                System.out.println("[External Server] Legacy SAT response - no model values available, NO HEURISTICS");
            }
            // Return null because we can't generate useful inputs without Z3 model values
            return null;
        } else if (response == '0') {
            // UNSAT - constraint is unsatisfiable
            if (DEBUG) {
                System.out.println("[External Server] Constraint is UNSAT (legacy protocol)");
            }
            return null;
        } else if (response == 'E') {
            // Error from server
            System.err.println("[External Server] GreenServer returned error for constraint (legacy protocol)");
            return null;
        } else {
            System.err.println("[External Server] Unexpected response from GreenServer: " + response);
            return null;
        }
    }

    /**
     * Reset the symbolicator state.
     */
    public static void reset() {
        valueToTag.clear();
        tagToExpression.clear();
        mySoln = null;
        GaletteGreenBridge.clearVariableCache();
        GalettePathUtils.reset();

        if (DEBUG) {
            System.out.println("Reset GaletteSymbolicator state");
        }
    }

    /**
     * Cleanup resources.
     */
    public static void cleanup() {
        try {
            if (serverConnection != null && !serverConnection.isClosed()) {
                serverConnection.close();
            }
            reset();
        } catch (IOException e) {
            System.err.println("Error closing server connection: " + e.getMessage());
        }
    }

    /**
     * Get statistics about symbolic execution.
     *
     * @return Statistics string
     */
    public static String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("GaletteSymbolicator Statistics:\n");
        sb.append("  Symbolic values: ").append(valueToTag.size()).append("\n");
        sb.append("  Green expressions: ").append(tagToExpression.size()).append("\n");
        sb.append("  Path constraints: ")
                .append(GalettePathUtils.getCurPC().size())
                .append("\n");
        sb.append("  Server connected: ")
                .append(serverConnection != null && !serverConnection.isClosed())
                .append("\n");
        return sb.toString();
    }

    /**
     * Simple input solution container.
     */
    public static class InputSolution implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Map<String, Object> values = new HashMap<>();

        public void setValue(String label, Object value) {
            values.put(label, value);
        }

        public Object getValue(String label) {
            return values.get(label);
        }

        public Set<String> getLabels() {
            return values.keySet();
        }

        @Override
        public String toString() {
            return "InputSolution" + values;
        }
    }

    // ==================== ARRAY SYMBOLIC EXECUTION SUPPORT ====================

    /**
     * Associate a tag with a Green expression.
     *
     * @param tag The Galette tag
     * @param expression The Green expression
     */
    public static void associateTagWithExpression(Tag tag, Expression expression) {
        if (tag != null && expression != null) {
            tagToExpression.put(tag, expression);
        }
    }

    /**
     * Clear tag-expression associations.
     */
    public static void clearTagExpressionMap() {
        tagToExpression.clear();
    }

    /**
     * Get count of tag-expression associations.
     */
    public static int getTagExpressionCount() {
        return tagToExpression.size();
    }
}
