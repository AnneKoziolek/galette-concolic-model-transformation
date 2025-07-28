package edu.neu.ccs.prl.galette.internal.runtime;

import java.util.*;

/**
 * Runtime path constraint collection using replacement strategy for bytecode operations.
 * Provides instrumented versions of comparison operations that collect constraints
 * for symbolic execution and path analysis.
 *
 * This class implements the replacement strategy from the comparison interception plan,
 * avoiding complex stack manipulation in favor of direct method replacement.
 *
 * @author Implementation based on claude-copilot-combined-comparison-interception-plan-3.md
 */
public final class PathUtils {

    // ===== CONFIGURATION =====

    // HARDCODED: Always enabled to eliminate system property dependency during transformation
    private static boolean isEnabled() {
        return true; // Always enabled - no system property dependency
    }

    static {
        try {
            System.out.println("🔧 PathUtils static initializer: isEnabled() = " + isEnabled() + " (HARDCODED)");
            System.out.println("🔧 System property galette.concolic.interception.enabled = "
                    + System.getProperty("galette.concolic.interception.enabled") + " (IGNORED)");
        } catch (Throwable t) {
            System.err.println("❌ PathUtils static initializer failed: " + t);
            t.printStackTrace();
        }
    }

    private static final boolean DEBUG = Boolean.getBoolean("galette.concolic.interception.debug");

    private static final boolean PERFORMANCE_MODE = Boolean.getBoolean("galette.concolic.interception.performance");

    // ===== THREAD-LOCAL STORAGE =====

    private static final ThreadLocal<List<Constraint>> PATH_CONDITIONS = new ThreadLocal<List<Constraint>>() {
        @Override
        protected List<Constraint> initialValue() {
            return new ArrayList<>();
        }
    };

    // ===== SYMBOLIC VALUE DETECTION =====

    /**
     * Simple heuristic to determine if values might be symbolic.
     * Avoids complex reflection in performance-critical code.
     */
    private static boolean mightBeSymbolic(Object value1, Object value2) {
        if (!PERFORMANCE_MODE) {
            return true; // Always log when performance mode is disabled
        }

        // Heuristic: values are likely symbolic if they're not simple constants
        return !isSimpleConstant(value1) || !isSimpleConstant(value2);
    }

    private static boolean isSimpleConstant(Object value) {
        if (value instanceof Integer) {
            int val = (Integer) value;
            return val >= -1 && val <= 10;
        }
        if (value instanceof Long) {
            long val = (Long) value;
            return val >= -1L && val <= 10L;
        }
        if (value instanceof Float) {
            float val = (Float) value;
            return val >= -1.0f && val <= 10.0f && val == (int) val;
        }
        if (value instanceof Double) {
            double val = (Double) value;
            return val >= -1.0 && val <= 10.0 && val == (int) val;
        }
        return false;
    }

    // ===== INSTRUMENTED COMPARISON OPERATIONS =====

    /**
     * Instrumented version of LCMP instruction.
     */
    public static int instrumentedLcmp(long value1, long value2) {
        int result = Long.compare(value1, value2);

        if (isEnabled() && mightBeSymbolic(value1, value2)) {
            List<Constraint> conditions = PATH_CONDITIONS.get();
            if (conditions == null) {
                conditions = new ArrayList<>();
                PATH_CONDITIONS.set(conditions);
            }
            conditions.add(new Constraint(value1, value2, "LCMP", result));

            if (DEBUG) {
                System.out.println("PathUtils: " + value1 + " LCMP " + value2 + " -> " + result);
            }
        }

        return result;
    }

    /**
     * Instrumented version of FCMPL instruction.
     */
    public static int instrumentedFcmpl(float value1, float value2) {
        int result;
        if (Float.isNaN(value1) || Float.isNaN(value2)) {
            result = -1; // FCMPL returns -1 for NaN
        } else {
            result = Float.compare(value1, value2);
        }

        if (isEnabled() && mightBeSymbolic(value1, value2)) {
            PATH_CONDITIONS.get().add(new Constraint(value1, value2, "FCMPL", result));

            if (DEBUG) {
                System.out.println("PathUtils: " + value1 + " FCMPL " + value2 + " -> " + result);
            }
        }

        return result;
    }

    /**
     * Instrumented version of FCMPG instruction.
     */
    public static int instrumentedFcmpg(float value1, float value2) {
        int result;
        if (Float.isNaN(value1) || Float.isNaN(value2)) {
            result = 1; // FCMPG returns 1 for NaN
        } else {
            result = Float.compare(value1, value2);
        }

        if (isEnabled() && mightBeSymbolic(value1, value2)) {
            PATH_CONDITIONS.get().add(new Constraint(value1, value2, "FCMPG", result));

            if (DEBUG) {
                System.out.println("PathUtils: " + value1 + " FCMPG " + value2 + " -> " + result);
            }
        }

        return result;
    }

    /**
     * Instrumented version of DCMPL instruction.
     */
    public static int instrumentedDcmpl(double value1, double value2) {
        System.out.println("🔍 PathUtils.instrumentedDcmpl called: " + value1 + " vs " + value2);
        int result;
        if (Double.isNaN(value1) || Double.isNaN(value2)) {
            result = -1; // DCMPL returns -1 for NaN
        } else {
            result = Double.compare(value1, value2);
        }

        if (isEnabled() && mightBeSymbolic(value1, value2)) {
            PATH_CONDITIONS.get().add(new Constraint(value1, value2, "DCMPL", result));
            System.out.println("✅ DCMPL constraint added: " + value1 + " DCMPL " + value2 + " -> " + result);

            if (DEBUG) {
                System.out.println("PathUtils: " + value1 + " DCMPL " + value2 + " -> " + result);
            }
        } else {
            System.out.println("⚠️ DCMPL not enabled or not symbolic: ENABLED=" + isEnabled() + ", mightBeSymbolic="
                    + mightBeSymbolic(value1, value2));
        }

        return result;
    }

    /**
     * Instrumented version of DCMPG instruction.
     */
    public static int instrumentedDcmpg(double value1, double value2) {
        System.out.println("🔍 PathUtils.instrumentedDcmpg called: " + value1 + " vs " + value2);
        int result;
        if (Double.isNaN(value1) || Double.isNaN(value2)) {
            result = 1; // DCMPG returns 1 for NaN
        } else {
            result = Double.compare(value1, value2);
        }

        if (isEnabled() && mightBeSymbolic(value1, value2)) {
            PATH_CONDITIONS.get().add(new Constraint(value1, value2, "DCMPG", result));
            System.out.println("✅ DCMPG constraint added: " + value1 + " DCMPG " + value2 + " -> " + result);

            if (DEBUG) {
                System.out.println("PathUtils: " + value1 + " DCMPG " + value2 + " -> " + result);
            }
        } else {
            System.out.println("⚠️ DCMPG not enabled or not symbolic: ENABLED=" + isEnabled() + ", mightBeSymbolic="
                    + mightBeSymbolic(value1, value2));
        }

        return result;
    }

    /**
     * Instrumented version of IF_ICMP* instructions.
     */
    public static boolean instrumentedIcmpJump(int value1, int value2, String operation) {
        boolean result;

        switch (operation) {
            case "EQ":
                result = value1 == value2;
                break;
            case "NE":
                result = value1 != value2;
                break;
            case "LT":
                result = value1 < value2;
                break;
            case "GE":
                result = value1 >= value2;
                break;
            case "GT":
                result = value1 > value2;
                break;
            case "LE":
                result = value1 <= value2;
                break;
            default:
                result = false;
        }

        if (isEnabled() && mightBeSymbolic(value1, value2)) {
            List<Constraint> conditions = PATH_CONDITIONS.get();
            if (conditions == null) {
                conditions = new ArrayList<>();
                PATH_CONDITIONS.set(conditions);
            }
            conditions.add(new Constraint(value1, value2, operation, result ? 1 : 0));

            if (DEBUG) {
                System.out.println("PathUtils: " + value1 + " " + operation + " " + value2 + " -> " + result);
            }
        }

        return result;
    }

    /**
     * Instrumented version of IF_ACMP* instructions.
     */
    public static boolean instrumentedAcmpJump(Object value1, Object value2, String operation) {
        boolean result;

        switch (operation) {
            case "ACMP_EQ":
                result = value1 == value2;
                break;
            case "ACMP_NE":
                result = value1 != value2;
                break;
            default:
                result = false;
        }

        if (isEnabled() && mightBeSymbolic(value1, value2)) {
            List<Constraint> conditions = PATH_CONDITIONS.get();
            if (conditions == null) {
                conditions = new ArrayList<>();
                PATH_CONDITIONS.set(conditions);
            }
            conditions.add(new Constraint(value1, value2, operation, result ? 1 : 0));

            if (DEBUG) {
                System.out.println("PathUtils: " + value1 + " " + operation + " " + value2 + " -> " + result);
            }
        }

        return result;
    }

    // ===== ACCESS METHODS =====

    /**
     * Retrieve and clear all collected path conditions.
     */
    public static List<Constraint> flush() {
        List<Constraint> constraints = new ArrayList<>(PATH_CONDITIONS.get());
        PATH_CONDITIONS.get().clear();
        return constraints;
    }

    /**
     * Clear all path conditions and reset state.
     */
    public static void reset() {
        PATH_CONDITIONS.get().clear();
    }

    /**
     * Get current path conditions without clearing.
     */
    public static List<Constraint> getCurrent() {
        return new ArrayList<>(PATH_CONDITIONS.get());
    }

    /**
     * Get the count of collected constraints.
     */
    public static int getConstraintCount() {
        return PATH_CONDITIONS.get().size();
    }

    // ===== DATA STRUCTURES =====

    public static class Constraint {
        public final Object value1;
        public final Object value2;
        public final String operation;
        public final int result;
        public final long timestamp;

        public Constraint(Object value1, Object value2, String operation, int result) {
            this.value1 = value1;
            this.value2 = value2;
            this.operation = operation;
            this.result = result;
            this.timestamp = System.nanoTime();
        }

        @Override
        public String toString() {
            return String.format("Constraint{%s %s %s -> %d}", value1, operation, value2, result);
        }

        /**
         * Convert to human-readable constraint expression.
         */
        public String toExpression() {
            switch (operation) {
                case "EQ":
                    return value1 + " == " + value2;
                case "NE":
                    return value1 + " != " + value2;
                case "LT":
                    return value1 + " < " + value2;
                case "GE":
                    return value1 + " >= " + value2;
                case "GT":
                    return value1 + " > " + value2;
                case "LE":
                    return value1 + " <= " + value2;
                case "LCMP":
                    return value1 + " cmp " + value2 + " = " + result;
                case "FCMPL":
                case "FCMPG":
                    return value1 + " fcmp " + value2 + " = " + result;
                case "DCMPL":
                case "DCMPG":
                    return value1 + " dcmp " + value2 + " = " + result;
                case "ACMP_EQ":
                    return value1 + " === " + value2;
                case "ACMP_NE":
                    return value1 + " !== " + value2;
                default:
                    return value1 + " " + operation + " " + value2 + " -> " + result;
            }
        }
    }
}
