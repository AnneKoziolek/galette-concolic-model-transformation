package edu.neu.ccs.prl.galette.examples.transformation;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathConditionWrapper;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.SymbolicComparison;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import za.ac.sun.cs.green.expr.Expression;

/**
 * Wrapper class that handles symbolic execution concerns separately from business logic.
 *
 * This class provides a clean separation between the core model transformation
 * logic and the symbolic execution infrastructure. It can be used to "wrap"
 * existing transformations with symbolic execution capabilities without
 * modifying the original transformation code.
 *
 * Design benefits:
 * - Core transformation logic remains clean and focused
 * - Symbolic execution can be added/removed without changing business logic
 * - Makes it easier to integrate Galette into existing systems
 * - Demonstrates clean architecture principles
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class SymbolicExecutionWrapper {

    /**
     * Container for symbolic values and their concrete counterparts.
     */
    public static class SymbolicValue<T> {
        private final T concreteValue;
        private final Tag symbolicTag;
        private final String label;

        public SymbolicValue(String label, T concreteValue) {
            this.label = label;
            this.concreteValue = concreteValue;
            this.symbolicTag = createSymbolicTag(label, concreteValue);
        }

        public T getValue() {
            return concreteValue;
        }

        public Tag getTag() {
            return symbolicTag;
        }

        public String getLabel() {
            return label;
        }

        public boolean isSymbolic() {
            return symbolicTag != null && !symbolicTag.isEmpty();
        }

        @SuppressWarnings("unchecked")
        private Tag createSymbolicTag(String label, T value) {
            if (value instanceof Double) {
                return GaletteSymbolicator.makeSymbolicDouble(label, (Double) value);
            } else if (value instanceof Integer) {
                return GaletteSymbolicator.makeSymbolicInt(label, (Integer) value);
            } else if (value instanceof Long) {
                return GaletteSymbolicator.makeSymbolicLong(label, (Long) value);
            } else if (value instanceof String) {
                return GaletteSymbolicator.makeSymbolicString(label, (String) value);
            } else {
                System.err.println("Unsupported type for symbolic value: " + value.getClass());
                return null;
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "SymbolicValue{label='%s', value=%s, symbolic=%s}", label, concreteValue, isSymbolic());
        }
    }

    /**
     * Interface for conditional operations that can be tracked symbolically.
     */
    @FunctionalInterface
    public interface SymbolicCondition {
        boolean evaluate();
    }

    /**
     * Create a symbolic double value.
     */
    public static SymbolicValue<Double> makeSymbolicDouble(String label, double value) {
        return new SymbolicValue<>(label, value);
    }

    /**
     * Create a symbolic integer value.
     */
    public static SymbolicValue<Integer> makeSymbolicInt(String label, int value) {
        return new SymbolicValue<>(label, value);
    }

    /**
     * Create a symbolic string value.
     */
    public static SymbolicValue<String> makeSymbolicString(String label, String value) {
        return new SymbolicValue<>(label, value);
    }

    /**
     * Perform a symbolic comparison and collect path constraints.
     *
     * @param left Left operand (can be symbolic or concrete)
     * @param right Right operand (can be symbolic or concrete)
     * @param operation The comparison operation as a string
     * @return The concrete result of the comparison
     */
    public static boolean symbolicComparison(SymbolicValue<Double> left, double right, String operation) {
        switch (operation.toLowerCase()) {
            case ">":
            case "gt":
                return SymbolicComparison.greaterThan(left.getValue(), left.getTag(), right, null);
            case ">=":
            case "ge":
                return SymbolicComparison.greaterThanOrEqual(left.getValue(), left.getTag(), right, null);
            case "<":
            case "lt":
                return SymbolicComparison.lessThan(left.getValue(), left.getTag(), right, null);
            case "<=":
            case "le":
                return SymbolicComparison.lessThanOrEqual(left.getValue(), left.getTag(), right, null);
            case "==":
            case "eq":
                return SymbolicComparison.equal(left.getValue(), left.getTag(), right, null);
            default:
                throw new IllegalArgumentException("Unsupported comparison operation: " + operation);
        }
    }

    /**
     * Execute a conditional with symbolic tracking.
     * This method automatically collects path constraints when the condition is evaluated.
     */
    public static boolean executeConditional(String description, SymbolicCondition condition) {
        if (GaletteSymbolicator.DEBUG) {
            System.out.println("Evaluating symbolic condition: " + description);
        }

        boolean result = condition.evaluate();

        if (GaletteSymbolicator.DEBUG) {
            System.out.println("Condition result: " + result);
        }

        return result;
    }

    /**
     * Reset the symbolic execution state.
     * Useful for starting fresh between different executions.
     */
    public static void reset() {
        GaletteSymbolicator.reset();
    }

    /**
     * Collect and analyze the current path constraints.
     *
     * @return Analysis results as a formatted string
     */
    public static String analyzePathConstraints() {
        StringBuilder analysis = new StringBuilder();

        try {
            PathConditionWrapper pc = PathUtils.getCurPC();

            if (pc != null && !pc.isEmpty()) {
                analysis.append("=== Symbolic Execution Analysis ===\n");
                analysis.append("Path constraints collected: ")
                        .append(pc.size())
                        .append("\n");

                Expression constraint = pc.toSingleExpression();
                if (constraint != null) {
                    analysis.append("Consolidated constraint: ")
                            .append(constraint)
                            .append("\n");
                    analysis.append("Constraint type: ")
                            .append(constraint.getClass().getSimpleName())
                            .append("\n");
                }

                // Try to get solution
                GaletteSymbolicator.InputSolution solution = GaletteSymbolicator.solvePathCondition();
                if (solution != null) {
                    analysis.append("Constraint solver solution: ")
                            .append(solution)
                            .append("\n");
                }

                // Add statistics
                analysis.append("Symbolic execution statistics:\n");
                analysis.append(GaletteSymbolicator.getStatistics());

            } else {
                analysis.append("No path constraints collected\n");
                analysis.append("This may indicate that symbolic execution is not active\n");
            }

        } catch (Exception e) {
            analysis.append("Error analyzing path constraints: ")
                    .append(e.getMessage())
                    .append("\n");
        }

        return analysis.toString();
    }

    /**
     * Display path constraint analysis to console.
     */
    public static void displayPathConstraintAnalysis() {
        System.out.println("\n" + analyzePathConstraints());
    }

    /**
     * Get summary statistics about the current symbolic execution state.
     */
    public static String getExecutionSummary() {
        PathConditionWrapper pc = PathUtils.getCurPC();
        int constraintCount = (pc != null) ? pc.size() : 0;

        return String.format(
                "Symbolic Execution Summary: %d constraints collected, %s",
                constraintCount, constraintCount > 0 ? "active" : "inactive");
    }

    /**
     * Check if symbolic execution is currently active.
     */
    public static boolean isSymbolicExecutionActive() {
        PathConditionWrapper pc = PathUtils.getCurPC();
        return pc != null && !pc.isEmpty();
    }

    /**
     * Utility method to create a threshold-based condition for thickness checks.
     * This encapsulates the common pattern in our brake disc example.
     */
    public static boolean evaluateThicknessCondition(SymbolicValue<Double> thickness, double threshold) {
        return symbolicComparison(thickness, threshold, ">");
    }
}
