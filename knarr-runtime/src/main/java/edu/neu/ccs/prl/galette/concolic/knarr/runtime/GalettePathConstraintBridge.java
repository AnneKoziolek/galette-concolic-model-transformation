package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import za.ac.sun.cs.green.expr.*;

/**
 * Bridge between Galette's automatic comparison interception and knarr-runtime.
 *
 * This class provides integration between the Galette agent's automatic comparison
 * interception (PathUtils) and the knarr-runtime symbolic execution framework.
 * It uses reflection to access Galette's internal PathUtils class to avoid direct
 * dependencies between modules.
 *
 * @author Implementation based on claude-copilot-combined-comparison-interception-plan-3.md
 */
public class GalettePathConstraintBridge {

    private static Class<?> galettePathUtilsClass;
    private static Method getCurrentMethod;
    private static Method flushMethod;

    static {
        try {
            System.out.println("🔧 GalettePathConstraintBridge: Attempting to load Galette PathConstraintAPI...");
            galettePathUtilsClass = Class.forName("edu.neu.ccs.prl.galette.PathConstraintAPI");
            System.out.println("✅ Successfully loaded PathConstraintAPI: " + galettePathUtilsClass.getName());

            getCurrentMethod = galettePathUtilsClass.getMethod("getCurrentConstraints");
            System.out.println("✅ Found getCurrentConstraints method: " + getCurrentMethod);

            flushMethod = galettePathUtilsClass.getMethod("flushConstraints");
            System.out.println("✅ Found flushConstraints method: " + flushMethod);

            System.out.println("🎉 GalettePathConstraintBridge initialization complete!");
        } catch (Exception e) {
            System.out.println("❌ GalettePathConstraintBridge initialization failed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("   This means automatic comparison interception is disabled");
            // Galette PathConstraintAPI not available - automatic interception disabled
            galettePathUtilsClass = null;
        }
    }

    /**
     * Check if Galette automatic interception is available.
     */
    public static boolean isAvailable() {
        return galettePathUtilsClass != null;
    }

    /**
     * Retrieve path constraints from Galette's automatic interception.
     */
    public static List<Expression> getGaletteConstraints() {
        System.out.println(
                "🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=" + isAvailable());
        if (!isAvailable()) return new ArrayList<>();

        try {
            @SuppressWarnings("unchecked")
            List<Object> rawConstraints = (List<Object>) getCurrentMethod.invoke(null);
            System.out.println("🔧 Retrieved " + rawConstraints.size() + " raw constraints from Galette PathUtils");
            return convertToGreenExpressions(rawConstraints);
        } catch (Exception e) {
            System.out.println("⚠️ Exception in getGaletteConstraints: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Retrieve and clear path constraints from Galette.
     */
    public static List<Expression> flushGaletteConstraints() {
        if (!isAvailable()) return new ArrayList<>();

        try {
            @SuppressWarnings("unchecked")
            List<Object> rawConstraints = (List<Object>) flushMethod.invoke(null);
            return convertToGreenExpressions(rawConstraints);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Convert Galette Constraints to Green Expression objects.
     */
    private static List<Expression> convertToGreenExpressions(List<Object> rawConstraints) {
        List<Expression> expressions = new ArrayList<>();

        for (Object constraint : rawConstraints) {
            try {
                Expression expr = convertSingleConstraint(constraint);
                if (expr != null) {
                    expressions.add(expr);
                }
            } catch (Exception e) {
                // Skip invalid constraints
            }
        }

        return expressions;
    }

    /**
     * Convert a single Galette Constraint to a Green Expression.
     */
    private static Expression convertSingleConstraint(Object constraint) throws Exception {
        // Use reflection to access Constraint fields
        Class<?> constraintClass = constraint.getClass();
        Object value1 = constraintClass.getField("value1").get(constraint);
        Object value2 = constraintClass.getField("value2").get(constraint);
        String operation = (String) constraintClass.getField("operation").get(constraint);
        int result = (Integer) constraintClass.getField("result").get(constraint);

        // Convert operands to Green expressions
        Expression leftExpr = convertValue(value1, "left");
        Expression rightExpr = convertValue(value2, "right");

        if (leftExpr == null || rightExpr == null) {
            return null;
        }

        // Create appropriate Green operation
        return createGreenOperation(leftExpr, rightExpr, operation, result);
    }

    /**
     * Convert a value to a Green Expression.
     */
    private static Expression convertValue(Object value, String variablePrefix) {
        if (value instanceof Integer) {
            int intVal = (Integer) value;
            if (intVal >= -10 && intVal <= 10) {
                return new IntConstant(intVal);
            } else {
                return new IntVariable(
                        variablePrefix + "_" + Math.abs(intVal % 1000), Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
        } else if (value instanceof Long) {
            long longVal = (Long) value;
            int intVal = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, longVal));
            if (intVal >= -10 && intVal <= 10) {
                return new IntConstant(intVal);
            } else {
                return new IntVariable(
                        variablePrefix + "_" + Math.abs(intVal % 1000), Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
        } else if (value instanceof Float || value instanceof Double) {
            double doubleVal = value instanceof Float ? (Float) value : (Double) value;
            if (doubleVal >= -10.0 && doubleVal <= 10.0 && doubleVal == (int) doubleVal) {
                return new RealConstant(doubleVal);
            } else {
                return new RealVariable(
                        variablePrefix + "_" + Math.abs((int) (doubleVal * 100) % 1000),
                        Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY);
            }
        }

        return null;
    }

    /**
     * Create appropriate Green operation based on operation string and result.
     */
    private static Expression createGreenOperation(Expression left, Expression right, String operation, int result) {
        switch (operation) {
            case "EQ":
                return result == 1
                        ? new BinaryOperation(Operation.Operator.EQ, left, right)
                        : new BinaryOperation(Operation.Operator.NE, left, right);
            case "NE":
                return result == 1
                        ? new BinaryOperation(Operation.Operator.NE, left, right)
                        : new BinaryOperation(Operation.Operator.EQ, left, right);
            case "LT":
                return result == 1
                        ? new BinaryOperation(Operation.Operator.LT, left, right)
                        : new BinaryOperation(Operation.Operator.GE, left, right);
            case "GE":
                return result == 1
                        ? new BinaryOperation(Operation.Operator.GE, left, right)
                        : new BinaryOperation(Operation.Operator.LT, left, right);
            case "GT":
                return result == 1
                        ? new BinaryOperation(Operation.Operator.GT, left, right)
                        : new BinaryOperation(Operation.Operator.LE, left, right);
            case "LE":
                return result == 1
                        ? new BinaryOperation(Operation.Operator.LE, left, right)
                        : new BinaryOperation(Operation.Operator.GT, left, right);
            case "LCMP":
            case "FCMPL":
            case "FCMPG":
            case "DCMPL":
            case "DCMPG":
                if (result < 0) {
                    return new BinaryOperation(Operation.Operator.LT, left, right);
                } else if (result > 0) {
                    return new BinaryOperation(Operation.Operator.GT, left, right);
                } else {
                    return new BinaryOperation(Operation.Operator.EQ, left, right);
                }
            case "ACMP_EQ":
                return result == 1
                        ? new BinaryOperation(Operation.Operator.EQ, left, right)
                        : new BinaryOperation(Operation.Operator.NE, left, right);
            case "ACMP_NE":
                return result == 1
                        ? new BinaryOperation(Operation.Operator.NE, left, right)
                        : new BinaryOperation(Operation.Operator.EQ, left, right);
            default:
                return null;
        }
    }
}
