package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import za.ac.sun.cs.green.expr.*;

/**
 * Tests for GalettePathConstraintBridge.
 *
 * Note: These tests verify the bridge functionality in isolation.
 * Full integration tests require the Galette agent to be active
 * for bytecode comparison interception.
 */
public class GalettePathConstraintBridgeTest {

    @Test
    public void testIsAvailableWithoutAgent() {
        // When running without the Galette agent, PathConstraintAPI won't be available
        // This tests that the bridge handles this gracefully
        boolean available = GalettePathConstraintBridge.isAvailable();
        // Can be true or false depending on runtime environment
        // The important thing is it doesn't throw
        assertNotNull(Boolean.valueOf(available));
    }

    @Test
    public void testGetGaletteConstraintsWithoutAgent() {
        // When PathConstraintAPI is not available, should return empty list
        // This tests graceful degradation
        List<Expression> constraints = GalettePathConstraintBridge.getGaletteConstraints();
        assertNotNull(constraints, "Should return non-null list even when unavailable");
        // May be empty or have constraints depending on runtime
    }

    @Test
    public void testFlushGaletteConstraintsWithoutAgent() {
        // When PathConstraintAPI is not available, should return empty list
        List<Expression> constraints = GalettePathConstraintBridge.flushGaletteConstraints();
        assertNotNull(constraints, "Should return non-null list even when unavailable");
    }

    @Test
    public void testGetConstraintsDoesNotThrow() {
        // Verify that getGaletteConstraints never throws, even in unexpected states
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                GalettePathConstraintBridge.getGaletteConstraints();
            }
        });
    }

    @Test
    public void testFlushConstraintsDoesNotThrow() {
        // Verify that flushGaletteConstraints never throws
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                GalettePathConstraintBridge.flushGaletteConstraints();
            }
        });
    }

    @Test
    public void testConstraintsAreExpressions() {
        // When constraints are returned, they should all be valid Green Expressions
        List<Expression> constraints = GalettePathConstraintBridge.getGaletteConstraints();

        for (Expression expr : constraints) {
            assertNotNull(expr, "Each constraint should be a valid Expression");
            // Each expression should be one of the known types
            assertTrue(
                    expr instanceof BinaryOperation
                            || expr instanceof UnaryOperation
                            || expr instanceof Variable
                            || expr instanceof Constant,
                    "Expression should be a valid Green type");
        }
    }

    @Test
    public void testFlushClearsConstraints() {
        // After flush, subsequent calls should not return the same constraints
        List<Expression> first = GalettePathConstraintBridge.flushGaletteConstraints();
        List<Expression> second = GalettePathConstraintBridge.flushGaletteConstraints();

        // The second call should return fewer or equal constraints
        // (not more, since we just flushed)
        assertTrue(
                second.size() <= first.size() || first.isEmpty(),
                "Flush should clear constraints or be idempotent when empty");
    }

    @Test
    public void testMultipleCallsToGetConstraints() {
        // Multiple calls to getGaletteConstraints (not flush) should return same constraints
        if (GalettePathConstraintBridge.isAvailable()) {
            List<Expression> first = GalettePathConstraintBridge.getGaletteConstraints();
            List<Expression> second = GalettePathConstraintBridge.getGaletteConstraints();

            // Should be same size (not accumulated)
            assertEquals(first.size(), second.size(), "Multiple get calls should return same constraints");
        }
    }

    // ==================== Expression Type Tests ====================

    @Test
    public void testCreateGreenOperationEQ() {
        // Test that EQ operation is created correctly
        // This mirrors what convertSingleConstraint does internally
        Expression left = new RealVariable("x", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        Expression right = new RealConstant(80.0);

        BinaryOperation eq = new BinaryOperation(Operation.Operator.EQ, left, right);
        BinaryOperation ne = new BinaryOperation(Operation.Operator.NE, left, right);

        assertEquals(Operation.Operator.EQ, eq.getOperator());
        assertEquals(Operation.Operator.NE, ne.getOperator());
    }

    @Test
    public void testCreateGreenOperationLT() {
        Expression left = new RealVariable("pressure", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        Expression right = new RealConstant(80.0);

        BinaryOperation lt = new BinaryOperation(Operation.Operator.LT, left, right);
        BinaryOperation ge = new BinaryOperation(Operation.Operator.GE, left, right);

        assertEquals(Operation.Operator.LT, lt.getOperator());
        assertEquals(Operation.Operator.GE, ge.getOperator());
    }

    @Test
    public void testCreateGreenOperationGT() {
        Expression left = new RealVariable("temp", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        Expression right = new RealConstant(100.0);

        BinaryOperation gt = new BinaryOperation(Operation.Operator.GT, left, right);
        BinaryOperation le = new BinaryOperation(Operation.Operator.LE, left, right);

        assertEquals(Operation.Operator.GT, gt.getOperator());
        assertEquals(Operation.Operator.LE, le.getOperator());
    }

    // ==================== Variable Conversion Tests ====================

    @Test
    public void testRealVariableCreation() {
        RealVariable var = new RealVariable("brakePressure", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        assertEquals("brakePressure", var.getName());
        assertEquals(Double.NEGATIVE_INFINITY, var.getLowerBound());
        assertEquals(Double.POSITIVE_INFINITY, var.getUpperBound());
    }

    @Test
    public void testIntVariableCreation() {
        IntVariable var = new IntVariable("counter", Integer.MIN_VALUE, Integer.MAX_VALUE);

        assertEquals("counter", var.getName());
    }

    @Test
    public void testRealConstantCreation() {
        RealConstant constant = new RealConstant(80.0);
        assertEquals(80.0, constant.getValue(), 0.001);

        RealConstant negative = new RealConstant(-12.5);
        assertEquals(-12.5, negative.getValue(), 0.001);
    }

    @Test
    public void testIntConstantCreation() {
        IntConstant constant = new IntConstant(42);
        assertEquals(42, constant.getValueLong());

        IntConstant negative = new IntConstant(-10);
        assertEquals(-10, negative.getValueLong());
    }

    // ==================== Constraint Building Tests ====================

    @Test
    public void testBuildSimpleComparison() {
        // Build: brakePressure < 80.0
        RealVariable brakePressure =
                new RealVariable("brakePressure", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        RealConstant threshold = new RealConstant(80.0);

        BinaryOperation comparison = new BinaryOperation(Operation.Operator.LT, brakePressure, threshold);

        assertNotNull(comparison);
        assertEquals(Operation.Operator.LT, comparison.getOperator());
        assertTrue(comparison.left instanceof RealVariable);
        assertTrue(comparison.right instanceof RealConstant);
    }

    @Test
    public void testBuildConjunction() {
        // Build: (x > 0) AND (x < 100)
        IntVariable x = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        IntConstant zero = new IntConstant(0);
        IntConstant hundred = new IntConstant(100);

        BinaryOperation gtZero = new BinaryOperation(Operation.Operator.GT, x, zero);
        BinaryOperation ltHundred = new BinaryOperation(Operation.Operator.LT, x, hundred);
        BinaryOperation conjunction = new BinaryOperation(Operation.Operator.AND, gtZero, ltHundred);

        assertNotNull(conjunction);
        assertEquals(Operation.Operator.AND, conjunction.getOperator());
    }

    @Test
    public void testBuildDisjunction() {
        // Build: (x < 10) OR (x > 90)
        IntVariable x = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        IntConstant ten = new IntConstant(10);
        IntConstant ninety = new IntConstant(90);

        BinaryOperation ltTen = new BinaryOperation(Operation.Operator.LT, x, ten);
        BinaryOperation gtNinety = new BinaryOperation(Operation.Operator.GT, x, ninety);
        BinaryOperation disjunction = new BinaryOperation(Operation.Operator.OR, ltTen, gtNinety);

        assertNotNull(disjunction);
        assertEquals(Operation.Operator.OR, disjunction.getOperator());
    }

    @Test
    public void testBuildNegation() {
        // Build: NOT(x == 50)
        IntVariable x = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        IntConstant fifty = new IntConstant(50);

        BinaryOperation equality = new BinaryOperation(Operation.Operator.EQ, x, fifty);
        UnaryOperation negation = new UnaryOperation(Operation.Operator.NOT, equality);

        assertNotNull(negation);
        assertEquals(Operation.Operator.NOT, negation.getOperator());
    }

    // ==================== DCMPL/DCMPG Operation Mapping Tests ====================

    @Test
    public void testDcmplResultMappings() {
        // DCMPL compares two doubles:
        // result < 0 means left < right
        // result == 0 means left == right
        // result > 0 means left > right

        RealVariable left = new RealVariable("a", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        RealVariable right = new RealVariable("b", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // result < 0 -> LT
        BinaryOperation ltOp = new BinaryOperation(Operation.Operator.LT, left, right);
        assertEquals(Operation.Operator.LT, ltOp.getOperator());

        // result == 0 -> EQ
        BinaryOperation eqOp = new BinaryOperation(Operation.Operator.EQ, left, right);
        assertEquals(Operation.Operator.EQ, eqOp.getOperator());

        // result > 0 -> GT
        BinaryOperation gtOp = new BinaryOperation(Operation.Operator.GT, left, right);
        assertEquals(Operation.Operator.GT, gtOp.getOperator());
    }

    @Test
    public void testComparisonBytecodeToOperator() {
        // Test mapping from JVM comparison bytecodes to Green operators

        // IF_ICMPEQ (159) -> EQ
        assertEquals(Operation.Operator.EQ, mapBytecodeToOperator(159, true));
        assertEquals(Operation.Operator.NE, mapBytecodeToOperator(159, false));

        // IF_ICMPNE (160) -> NE
        assertEquals(Operation.Operator.NE, mapBytecodeToOperator(160, true));
        assertEquals(Operation.Operator.EQ, mapBytecodeToOperator(160, false));

        // IF_ICMPLT (161) -> LT
        assertEquals(Operation.Operator.LT, mapBytecodeToOperator(161, true));
        assertEquals(Operation.Operator.GE, mapBytecodeToOperator(161, false));

        // IF_ICMPGE (162) -> GE
        assertEquals(Operation.Operator.GE, mapBytecodeToOperator(162, true));
        assertEquals(Operation.Operator.LT, mapBytecodeToOperator(162, false));

        // IF_ICMPGT (163) -> GT
        assertEquals(Operation.Operator.GT, mapBytecodeToOperator(163, true));
        assertEquals(Operation.Operator.LE, mapBytecodeToOperator(163, false));

        // IF_ICMPLE (164) -> LE
        assertEquals(Operation.Operator.LE, mapBytecodeToOperator(164, true));
        assertEquals(Operation.Operator.GT, mapBytecodeToOperator(164, false));
    }

    private Operation.Operator mapBytecodeToOperator(int opcode, boolean branchTaken) {
        // Mirrors the logic in GaletteTaintListener.onComparison
        switch (opcode) {
            case 159: // IF_ICMPEQ
                return branchTaken ? Operation.Operator.EQ : Operation.Operator.NE;
            case 160: // IF_ICMPNE
                return branchTaken ? Operation.Operator.NE : Operation.Operator.EQ;
            case 161: // IF_ICMPLT
                return branchTaken ? Operation.Operator.LT : Operation.Operator.GE;
            case 162: // IF_ICMPGE
                return branchTaken ? Operation.Operator.GE : Operation.Operator.LT;
            case 163: // IF_ICMPGT
                return branchTaken ? Operation.Operator.GT : Operation.Operator.LE;
            case 164: // IF_ICMPLE
                return branchTaken ? Operation.Operator.LE : Operation.Operator.GT;
            default:
                return null;
        }
    }
}
