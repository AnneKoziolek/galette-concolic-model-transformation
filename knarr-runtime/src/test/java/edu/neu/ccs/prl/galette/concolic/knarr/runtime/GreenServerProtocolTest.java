package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import za.ac.sun.cs.green.expr.*;

/**
 * Tests for the GreenServer JSON protocol.
 * Tests the JSON format used to communicate constraint solving results
 * including SAT status and model values.
 */
public class GreenServerProtocolTest {

    /**
     * Parse a GreenServer model response JSON into a map of variable values.
     * This is a test utility that mirrors the parsing logic in GaletteSymbolicator.
     */
    private Map<String, Object> parseModelResponse(String json) {
        Map<String, Object> result = new HashMap<>();

        if (json == null || json.isEmpty()) {
            return result;
        }

        // Parse "sat" field
        int satIdx = json.indexOf("\"sat\":");
        if (satIdx != -1) {
            int valueStart = satIdx + 6;
            if (json.substring(valueStart).trim().startsWith("true")) {
                result.put("sat", true);
            } else {
                result.put("sat", false);
            }
        }

        // Parse "model" field
        int modelIdx = json.indexOf("\"model\":");
        if (modelIdx != -1) {
            int braceStart = json.indexOf("{", modelIdx + 8);
            int braceEnd = findMatchingBrace(json, braceStart);
            if (braceStart != -1 && braceEnd != -1) {
                String modelJson = json.substring(braceStart + 1, braceEnd);
                parseModelValues(modelJson, result);
            }
        }

        // Parse "error" field
        int errorIdx = json.indexOf("\"error\":");
        if (errorIdx != -1) {
            int quoteStart = json.indexOf("\"", errorIdx + 8);
            int quoteEnd = json.indexOf("\"", quoteStart + 1);
            if (quoteStart != -1 && quoteEnd != -1) {
                result.put("error", json.substring(quoteStart + 1, quoteEnd));
            }
        }

        return result;
    }

    private int findMatchingBrace(String json, int start) {
        if (start == -1) return -1;
        int depth = 1;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private void parseModelValues(String modelJson, Map<String, Object> result) {
        // Simple parsing for key:value pairs
        int pos = 0;
        while (pos < modelJson.length()) {
            // Skip whitespace
            while (pos < modelJson.length() && Character.isWhitespace(modelJson.charAt(pos))) pos++;
            if (pos >= modelJson.length()) break;

            // Find key
            if (modelJson.charAt(pos) != '"') {
                pos++;
                continue;
            }
            int keyStart = pos + 1;
            int keyEnd = modelJson.indexOf('"', keyStart);
            if (keyEnd == -1) break;
            String key = modelJson.substring(keyStart, keyEnd);

            pos = keyEnd + 1;

            // Skip to value
            while (pos < modelJson.length() && modelJson.charAt(pos) != ':') pos++;
            pos++; // skip ':'
            while (pos < modelJson.length() && Character.isWhitespace(modelJson.charAt(pos))) pos++;

            // Parse value
            if (pos >= modelJson.length()) break;

            int valueStart = pos;
            char c = modelJson.charAt(pos);
            if (c == '"') {
                // String value
                int valueEnd = modelJson.indexOf('"', pos + 1);
                result.put(key, modelJson.substring(pos + 1, valueEnd));
                pos = valueEnd + 1;
            } else if (c == '-' || Character.isDigit(c)) {
                // Number value
                int valueEnd = pos;
                while (valueEnd < modelJson.length()
                        && (Character.isDigit(modelJson.charAt(valueEnd))
                                || modelJson.charAt(valueEnd) == '.'
                                || modelJson.charAt(valueEnd) == '-'
                                || modelJson.charAt(valueEnd) == 'e'
                                || modelJson.charAt(valueEnd) == 'E'
                                || modelJson.charAt(valueEnd) == '+')) {
                    valueEnd++;
                }
                String numStr = modelJson.substring(pos, valueEnd);
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    result.put(key, Double.parseDouble(numStr));
                } else {
                    result.put(key, Long.parseLong(numStr));
                }
                pos = valueEnd;
            } else if (c == 't') {
                result.put(key, true);
                pos += 4;
            } else if (c == 'f') {
                result.put(key, false);
                pos += 5;
            } else if (c == 'n') {
                result.put(key, null);
                pos += 4;
            }

            // Skip comma
            while (pos < modelJson.length()
                    && (modelJson.charAt(pos) == ',' || Character.isWhitespace(modelJson.charAt(pos)))) pos++;
        }
    }

    // ==================== SAT Response Tests ====================

    @Test
    public void testSatTrueWithEmptyModel() {
        String json = "{\"sat\":true,\"model\":{}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
    }

    @Test
    public void testSatFalse() {
        String json = "{\"sat\":false}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(false, result.get("sat"));
    }

    @Test
    public void testSatFalseWithError() {
        String json = "{\"sat\":false,\"error\":\"parse_error\"}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(false, result.get("sat"));
        assertEquals("parse_error", result.get("error"));
    }

    // ==================== Model Value Tests ====================

    @Test
    public void testModelWithIntegerValue() {
        String json = "{\"sat\":true,\"model\":{\"x\":42}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(42L, result.get("x"));
    }

    @Test
    public void testModelWithRealValue() {
        String json = "{\"sat\":true,\"model\":{\"temperature\":80.5}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(80.5, (Double) result.get("temperature"), 0.001);
    }

    @Test
    public void testModelWithNegativeRealValue() {
        String json = "{\"sat\":true,\"model\":{\"offset\":-12.34}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(-12.34, (Double) result.get("offset"), 0.001);
    }

    @Test
    public void testModelWithMultipleVariables() {
        String json = "{\"sat\":true,\"model\":{\"x\":10,\"y\":20.5,\"z\":-5}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(10L, result.get("x"));
        assertEquals(20.5, (Double) result.get("y"), 0.001);
        assertEquals(-5L, result.get("z"));
    }

    @Test
    public void testModelWithBooleanValue() {
        String json = "{\"sat\":true,\"model\":{\"flag\":true,\"enabled\":false}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(true, result.get("flag"));
        assertEquals(false, result.get("enabled"));
    }

    @Test
    public void testModelWithNullValue() {
        String json = "{\"sat\":true,\"model\":{\"optional\":null}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertNull(result.get("optional"));
    }

    @Test
    public void testModelWithStringValue() {
        String json = "{\"sat\":true,\"model\":{\"name\":\"test\"}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals("test", result.get("name"));
    }

    // ==================== Brake Disc Transformation Scenario Tests ====================

    @Test
    public void testBrakeDiscScenario_BelowThreshold() {
        // Scenario: brakePressure < 80.0, so additionalStiffness = false
        String json = "{\"sat\":true,\"model\":{\"brakePressure\":50.0}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        Double brakePressure = (Double) result.get("brakePressure");
        assertNotNull(brakePressure);
        assertTrue(brakePressure < 80.0, "Brake pressure should be below threshold");
    }

    @Test
    public void testBrakeDiscScenario_AboveThreshold() {
        // Scenario: brakePressure >= 80.0, so additionalStiffness = true
        String json = "{\"sat\":true,\"model\":{\"brakePressure\":85.0}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        Double brakePressure = (Double) result.get("brakePressure");
        assertNotNull(brakePressure);
        assertTrue(brakePressure >= 80.0, "Brake pressure should be at or above threshold");
    }

    @Test
    public void testBrakeDiscScenario_AtThreshold() {
        // Edge case: brakePressure exactly at 80.0
        String json = "{\"sat\":true,\"model\":{\"brakePressure\":80.0}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        Double brakePressure = (Double) result.get("brakePressure");
        assertNotNull(brakePressure);
        assertEquals(80.0, brakePressure, 0.001);
    }

    // ==================== Request JSON Generation Tests ====================

    @Test
    public void testSimpleConstraintToJson() {
        // brakePressure < 80.0
        RealVariable brakePressure =
                new RealVariable("brakePressure", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        RealConstant threshold = new RealConstant(80.0);
        BinaryOperation constraint = new BinaryOperation(Operation.Operator.LT, brakePressure, threshold);

        String json = ExpressionJsonConverter.toJson(constraint);

        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"binary\""));
        assertTrue(json.contains("\"op\":\"LT\""));
        assertTrue(json.contains("\"name\":\"brakePressure\""));
        assertTrue(json.contains("80.0"));
    }

    @Test
    public void testConjunctionConstraintToJson() {
        // (brakePressure >= 0.0) AND (brakePressure < 80.0)
        RealVariable brakePressure =
                new RealVariable("brakePressure", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        RealConstant zero = new RealConstant(0.0);
        RealConstant eighty = new RealConstant(80.0);

        BinaryOperation geZero = new BinaryOperation(Operation.Operator.GE, brakePressure, zero);
        BinaryOperation ltEighty = new BinaryOperation(Operation.Operator.LT, brakePressure, eighty);
        BinaryOperation conjunction = new BinaryOperation(Operation.Operator.AND, geZero, ltEighty);

        String json = ExpressionJsonConverter.toJson(conjunction);

        assertNotNull(json);
        assertTrue(json.contains("\"op\":\"AND\""));
        assertTrue(json.contains("\"op\":\"GE\""));
        assertTrue(json.contains("\"op\":\"LT\""));
    }

    @Test
    public void testNegatedConstraintToJson() {
        // NOT (brakePressure < 80.0)  equivalent to  brakePressure >= 80.0
        RealVariable brakePressure =
                new RealVariable("brakePressure", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        RealConstant threshold = new RealConstant(80.0);
        BinaryOperation ltConstraint = new BinaryOperation(Operation.Operator.LT, brakePressure, threshold);
        UnaryOperation negated = new UnaryOperation(Operation.Operator.NOT, ltConstraint);

        String json = ExpressionJsonConverter.toJson(negated);

        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"unary\""));
        assertTrue(json.contains("\"op\":\"NOT\""));
    }

    // ==================== Error Handling Tests ====================

    @Test
    public void testEmptyJsonResponse() {
        Map<String, Object> result = parseModelResponse("");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testNullJsonResponse() {
        Map<String, Object> result = parseModelResponse(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMalformedJsonResponseGraceful() {
        // Should not throw, just return partial results
        String json = "{\"sat\":true,\"model\":{"; // incomplete
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
    }

    // ==================== Whitespace and Formatting Tests ====================

    @Test
    public void testPrettyPrintedJson() {
        String json = "{\n" + "  \"sat\": true,\n"
                + "  \"model\": {\n"
                + "    \"x\": 42,\n"
                + "    \"y\": 3.14\n"
                + "  }\n"
                + "}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(42L, result.get("x"));
        assertEquals(3.14, (Double) result.get("y"), 0.001);
    }

    @Test
    public void testMinifiedJson() {
        String json = "{\"sat\":true,\"model\":{\"x\":42,\"y\":3.14}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(42L, result.get("x"));
        assertEquals(3.14, (Double) result.get("y"), 0.001);
    }

    // ==================== Edge Cases ====================

    @Test
    public void testVeryLargeNumber() {
        String json = "{\"sat\":true,\"model\":{\"big\":9999999999999}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(9999999999999L, result.get("big"));
    }

    @Test
    public void testScientificNotationInModel() {
        String json = "{\"sat\":true,\"model\":{\"tiny\":1.5e-10}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(1.5e-10, (Double) result.get("tiny"), 1e-15);
    }

    @Test
    public void testVariableNameWithUnderscore() {
        String json = "{\"sat\":true,\"model\":{\"brake_pressure\":75.5}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        assertEquals(75.5, (Double) result.get("brake_pressure"), 0.001);
    }

    @Test
    public void testMultipleConstraintsScenario() {
        // Simulating: x > 0 AND y < 100 AND x + y == 50
        String json = "{\"sat\":true,\"model\":{\"x\":30,\"y\":20}}";
        Map<String, Object> result = parseModelResponse(json);

        assertEquals(true, result.get("sat"));
        Long x = (Long) result.get("x");
        Long y = (Long) result.get("y");

        assertNotNull(x);
        assertNotNull(y);
        assertTrue(x > 0, "x should be greater than 0");
        assertTrue(y < 100, "y should be less than 100");
        assertEquals(50, x + y, "x + y should equal 50");
    }
}
