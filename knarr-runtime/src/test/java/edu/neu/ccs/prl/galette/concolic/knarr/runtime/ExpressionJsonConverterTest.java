package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import za.ac.sun.cs.green.expr.*;

/**
 * Tests for ExpressionJsonConverter - JSON serialization and deserialization
 * of Green Expression objects for GreenServer communication.
 */
public class ExpressionJsonConverterTest {

    @Test
    public void testIntConstantToJson() {
        IntConstant expr = new IntConstant(42);
        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"intconst\""));
        assertTrue(json.contains("\"value\":42"));
    }

    @Test
    public void testRealConstantToJson() {
        RealConstant expr = new RealConstant(3.14159);
        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"realconst\""));
        assertTrue(json.contains("3.14159"));
    }

    @Test
    public void testNegativeRealConstantToJson() {
        RealConstant expr = new RealConstant(-80.0);
        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"realconst\""));
        assertTrue(json.contains("-80.0"));
    }

    @Test
    public void testStringConstantToJson() {
        StringConstant expr = new StringConstant("hello");
        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"strconst\""));
        assertTrue(json.contains("\"value\":\"hello\""));
    }

    @Test
    public void testStringConstantWithEscapesToJson() {
        StringConstant expr = new StringConstant("line1\nline2\ttab");
        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"strconst\""));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
    }

    @Test
    public void testIntVariableToJson() {
        IntVariable expr = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"intvar\""));
        assertTrue(json.contains("\"name\":\"x\""));
    }

    @Test
    public void testRealVariableToJson() {
        RealVariable expr = new RealVariable("temperature", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"realvar\""));
        assertTrue(json.contains("\"name\":\"temperature\""));
    }

    @Test
    public void testStringVariableToJson() {
        StringVariable expr = new StringVariable("message");
        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"strvar\""));
        assertTrue(json.contains("\"name\":\"message\""));
    }

    @Test
    public void testBinaryOperationToJson() {
        IntVariable left = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        IntConstant right = new IntConstant(10);
        BinaryOperation expr = new BinaryOperation(Operation.Operator.LT, left, right);

        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"binary\""));
        assertTrue(json.contains("\"op\":\"LT\""));
        assertTrue(json.contains("\"left\":{"));
        assertTrue(json.contains("\"right\":{"));
    }

    @Test
    public void testNestedBinaryOperationToJson() {
        // (x < 10) AND (y > 5)
        IntVariable x = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        IntVariable y = new IntVariable("y", Integer.MIN_VALUE, Integer.MAX_VALUE);
        IntConstant ten = new IntConstant(10);
        IntConstant five = new IntConstant(5);

        BinaryOperation left = new BinaryOperation(Operation.Operator.LT, x, ten);
        BinaryOperation right = new BinaryOperation(Operation.Operator.GT, y, five);
        BinaryOperation expr = new BinaryOperation(Operation.Operator.AND, left, right);

        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"op\":\"AND\""));
        assertTrue(json.contains("\"op\":\"LT\""));
        assertTrue(json.contains("\"op\":\"GT\""));
    }

    @Test
    public void testUnaryOperationToJson() {
        IntVariable x = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        UnaryOperation expr = new UnaryOperation(Operation.Operator.NOT, x);

        String json = ExpressionJsonConverter.toJson(expr);

        assertTrue(json.contains("\"type\":\"unary\""));
        assertTrue(json.contains("\"op\":\"NOT\""));
        assertTrue(json.contains("\"operand\":{"));
    }

    @Test
    public void testNullToJson() {
        String json = ExpressionJsonConverter.toJson(null);
        assertEquals("null", json);
    }

    // ==================== fromJson tests ====================

    @Test
    public void testIntConstantFromJson() {
        String json = "{\"type\":\"intconst\",\"value\":42}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof IntConstant);
        assertEquals(42, ((IntConstant) expr).getValueLong());
    }

    @Test
    public void testRealConstantFromJson() {
        String json = "{\"type\":\"realconst\",\"value\":3.14159}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof RealConstant);
        assertEquals(3.14159, ((RealConstant) expr).getValue(), 0.00001);
    }

    @Test
    public void testNegativeRealConstantFromJson() {
        String json = "{\"type\":\"realconst\",\"value\":-80.0}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof RealConstant);
        assertEquals(-80.0, ((RealConstant) expr).getValue(), 0.00001);
    }

    @Test
    public void testStringConstantFromJson() {
        String json = "{\"type\":\"strconst\",\"value\":\"hello\"}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof StringConstant);
        assertEquals("hello", ((StringConstant) expr).getValue());
    }

    @Test
    public void testIntVariableFromJson() {
        String json = "{\"type\":\"intvar\",\"name\":\"x\"}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof IntVariable);
        assertEquals("x", ((IntVariable) expr).getName());
    }

    @Test
    public void testRealVariableFromJson() {
        String json = "{\"type\":\"realvar\",\"name\":\"temperature\"}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof RealVariable);
        assertEquals("temperature", ((RealVariable) expr).getName());
    }

    @Test
    public void testStringVariableFromJson() {
        String json = "{\"type\":\"strvar\",\"name\":\"message\"}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof StringVariable);
        assertEquals("message", ((StringVariable) expr).getName());
    }

    @Test
    public void testBinaryOperationFromJson() {
        String json =
                "{\"type\":\"binary\",\"op\":\"LT\",\"left\":{\"type\":\"intvar\",\"name\":\"x\"},\"right\":{\"type\":\"intconst\",\"value\":10}}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof BinaryOperation);
        BinaryOperation binOp = (BinaryOperation) expr;

        assertEquals(Operation.Operator.LT, binOp.getOperator());
        assertTrue(binOp.left instanceof IntVariable);
        assertTrue(binOp.right instanceof IntConstant);
    }

    @Test
    public void testUnaryOperationFromJson() {
        String json = "{\"type\":\"unary\",\"op\":\"NOT\",\"operand\":{\"type\":\"intvar\",\"name\":\"x\"}}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof UnaryOperation);
        UnaryOperation unOp = (UnaryOperation) expr;

        assertEquals(Operation.Operator.NOT, unOp.getOperator());
    }

    @Test
    public void testNullFromJson() {
        Expression expr = ExpressionJsonConverter.fromJson(null);
        assertNull(expr);

        expr = ExpressionJsonConverter.fromJson("null");
        assertNull(expr);
    }

    // ==================== Roundtrip tests ====================

    @Test
    public void testIntConstantRoundtrip() {
        IntConstant original = new IntConstant(12345);
        String json = ExpressionJsonConverter.toJson(original);
        Expression parsed = ExpressionJsonConverter.fromJson(json);

        assertNotNull(parsed);
        assertTrue(parsed instanceof IntConstant);
        assertEquals(original.getValueLong(), ((IntConstant) parsed).getValueLong());
    }

    @Test
    public void testRealConstantRoundtrip() {
        RealConstant original = new RealConstant(80.0);
        String json = ExpressionJsonConverter.toJson(original);
        Expression parsed = ExpressionJsonConverter.fromJson(json);

        assertNotNull(parsed);
        assertTrue(parsed instanceof RealConstant);
        assertEquals(original.getValue(), ((RealConstant) parsed).getValue(), 0.00001);
    }

    @Test
    public void testRealVariableRoundtrip() {
        RealVariable original = new RealVariable("brakePressure", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        String json = ExpressionJsonConverter.toJson(original);
        Expression parsed = ExpressionJsonConverter.fromJson(json);

        assertNotNull(parsed);
        assertTrue(parsed instanceof RealVariable);
        assertEquals(original.getName(), ((RealVariable) parsed).getName());
    }

    @Test
    public void testComplexExpressionRoundtrip() {
        // brakePressure < 80.0
        RealVariable brakePressure =
                new RealVariable("brakePressure", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        RealConstant threshold = new RealConstant(80.0);
        BinaryOperation original = new BinaryOperation(Operation.Operator.LT, brakePressure, threshold);

        String json = ExpressionJsonConverter.toJson(original);
        Expression parsed = ExpressionJsonConverter.fromJson(json);

        assertNotNull(parsed);
        assertTrue(parsed instanceof BinaryOperation);
        BinaryOperation binOp = (BinaryOperation) parsed;

        assertEquals(Operation.Operator.LT, binOp.getOperator());
        assertTrue(binOp.left instanceof RealVariable);
        assertTrue(binOp.right instanceof RealConstant);
        assertEquals("brakePressure", ((RealVariable) binOp.left).getName());
        assertEquals(80.0, ((RealConstant) binOp.right).getValue(), 0.00001);
    }

    @Test
    public void testNestedExpressionRoundtrip() {
        // (x > 0) AND (x < 100)
        IntVariable x = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        IntConstant zero = new IntConstant(0);
        IntConstant hundred = new IntConstant(100);

        BinaryOperation left = new BinaryOperation(Operation.Operator.GT, x, zero);
        BinaryOperation right = new BinaryOperation(Operation.Operator.LT, x, hundred);
        BinaryOperation original = new BinaryOperation(Operation.Operator.AND, left, right);

        String json = ExpressionJsonConverter.toJson(original);
        Expression parsed = ExpressionJsonConverter.fromJson(json);

        assertNotNull(parsed);
        assertTrue(parsed instanceof BinaryOperation);
        BinaryOperation and = (BinaryOperation) parsed;

        assertEquals(Operation.Operator.AND, and.getOperator());
        assertTrue(and.left instanceof BinaryOperation);
        assertTrue(and.right instanceof BinaryOperation);

        BinaryOperation parsedLeft = (BinaryOperation) and.left;
        BinaryOperation parsedRight = (BinaryOperation) and.right;

        assertEquals(Operation.Operator.GT, parsedLeft.getOperator());
        assertEquals(Operation.Operator.LT, parsedRight.getOperator());
    }

    @Test
    public void testComparisonOperatorsRoundtrip() {
        IntVariable x = new IntVariable("x", Integer.MIN_VALUE, Integer.MAX_VALUE);
        IntConstant val = new IntConstant(50);

        // Test all comparison operators
        Operation.Operator[] ops = {
            Operation.Operator.EQ,
            Operation.Operator.NE,
            Operation.Operator.LT,
            Operation.Operator.LE,
            Operation.Operator.GT,
            Operation.Operator.GE
        };

        for (Operation.Operator op : ops) {
            BinaryOperation original = new BinaryOperation(op, x, val);
            String json = ExpressionJsonConverter.toJson(original);
            Expression parsed = ExpressionJsonConverter.fromJson(json);

            assertNotNull(parsed, "Parsed expression should not be null for op: " + op);
            assertTrue(parsed instanceof BinaryOperation, "Should be BinaryOperation for op: " + op);
            assertEquals(op, ((BinaryOperation) parsed).getOperator(), "Operator should match for: " + op);
        }
    }

    @Test
    public void testWhitespaceHandling() {
        // JSON with extra whitespace
        String json = "  {  \"type\"  :  \"intconst\"  ,  \"value\"  :  42  }  ";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof IntConstant);
        assertEquals(42, ((IntConstant) expr).getValueLong());
    }

    @Test
    public void testScientificNotation() {
        String json = "{\"type\":\"realconst\",\"value\":1.5e-10}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof RealConstant);
        assertEquals(1.5e-10, ((RealConstant) expr).getValue(), 1e-15);
    }

    @Test
    public void testNegativeExponent() {
        String json = "{\"type\":\"realconst\",\"value\":-1.23E+5}";
        Expression expr = ExpressionJsonConverter.fromJson(json);

        assertNotNull(expr);
        assertTrue(expr instanceof RealConstant);
        assertEquals(-123000.0, ((RealConstant) expr).getValue(), 0.1);
    }
}
