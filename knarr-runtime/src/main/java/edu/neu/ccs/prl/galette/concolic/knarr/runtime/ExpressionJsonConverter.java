package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import za.ac.sun.cs.green.expr.*;

/**
 * Converts Green Expression objects to/from JSON strings.
 * This avoids Java serialization issues when communicating with external GreenServer.
 */
public class ExpressionJsonConverter {

    /**
     * Convert an Expression to a JSON string.
     */
    public static String toJson(Expression expr) {
        if (expr == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        toJsonInternal(expr, sb);
        return sb.toString();
    }

    private static void toJsonInternal(Expression expr, StringBuilder sb) {
        if (expr instanceof BinaryOperation) {
            BinaryOperation binOp = (BinaryOperation) expr;
            sb.append("{\"type\":\"binary\",\"op\":\"");
            sb.append(binOp.getOperator().name());
            sb.append("\",\"left\":");
            toJsonInternal(binOp.left, sb);
            sb.append(",\"right\":");
            toJsonInternal(binOp.right, sb);
            sb.append("}");
        } else if (expr instanceof UnaryOperation) {
            UnaryOperation unOp = (UnaryOperation) expr;
            sb.append("{\"type\":\"unary\",\"op\":\"");
            sb.append(unOp.getOperator().name());
            sb.append("\",\"operand\":");
            toJsonInternal(unOp.getOperand(0), sb);
            sb.append("}");
        } else if (expr instanceof IntVariable) {
            IntVariable var = (IntVariable) expr;
            sb.append("{\"type\":\"intvar\",\"name\":\"");
            sb.append(escapeJson(var.getName()));
            sb.append("\"}");
        } else if (expr instanceof RealVariable) {
            RealVariable var = (RealVariable) expr;
            sb.append("{\"type\":\"realvar\",\"name\":\"");
            sb.append(escapeJson(var.getName()));
            sb.append("\"}");
        } else if (expr instanceof StringVariable) {
            StringVariable var = (StringVariable) expr;
            sb.append("{\"type\":\"strvar\",\"name\":\"");
            sb.append(escapeJson(var.getName()));
            sb.append("\"}");
        } else if (expr instanceof IntConstant) {
            IntConstant c = (IntConstant) expr;
            sb.append("{\"type\":\"intconst\",\"value\":");
            sb.append(c.getValue());
            sb.append("}");
        } else if (expr instanceof RealConstant) {
            RealConstant c = (RealConstant) expr;
            sb.append("{\"type\":\"realconst\",\"value\":");
            sb.append(c.getValue());
            sb.append("}");
        } else if (expr instanceof StringConstant) {
            StringConstant c = (StringConstant) expr;
            sb.append("{\"type\":\"strconst\",\"value\":\"");
            sb.append(escapeJson(c.getValue()));
            sb.append("\"}");
        } else {
            // Fallback: use toString representation
            sb.append("{\"type\":\"unknown\",\"repr\":\"");
            sb.append(escapeJson(expr.toString()));
            sb.append("\"}");
        }
    }

    /**
     * Parse a JSON string back to an Expression.
     */
    public static Expression fromJson(String json) {
        if (json == null || json.equals("null")) {
            return null;
        }
        return parseExpression(json.trim(), new int[] {0});
    }

    private static Expression parseExpression(String json, int[] pos) {
        skipWhitespace(json, pos);
        if (pos[0] >= json.length()) return null;

        if (json.charAt(pos[0]) != '{') {
            throw new RuntimeException("Expected '{' at position " + pos[0]);
        }
        pos[0]++; // skip '{'

        String type = null;
        String op = null;
        String name = null;
        Object value = null;
        Expression left = null;
        Expression right = null;
        Expression operand = null;

        while (pos[0] < json.length() && json.charAt(pos[0]) != '}') {
            skipWhitespace(json, pos);
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            expect(json, pos, ':');
            skipWhitespace(json, pos);

            switch (key) {
                case "type":
                    type = parseString(json, pos);
                    break;
                case "op":
                    op = parseString(json, pos);
                    break;
                case "name":
                    name = parseString(json, pos);
                    break;
                case "value":
                    value = parseValue(json, pos);
                    break;
                case "left":
                    left = parseExpression(json, pos);
                    break;
                case "right":
                    right = parseExpression(json, pos);
                    break;
                case "operand":
                    operand = parseExpression(json, pos);
                    break;
                case "repr":
                    // Skip repr field for unknown types
                    parseString(json, pos);
                    break;
                default:
                    // Skip unknown fields
                    skipValue(json, pos);
            }

            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
            }
        }

        if (pos[0] < json.length() && json.charAt(pos[0]) == '}') {
            pos[0]++;
        }

        // Construct the appropriate Expression
        if (type == null) return null;

        switch (type) {
            case "binary":
                Operation.Operator binOp = Operation.Operator.valueOf(op);
                return new BinaryOperation(binOp, left, right);
            case "unary":
                Operation.Operator unOp = Operation.Operator.valueOf(op);
                return new UnaryOperation(unOp, operand);
            case "intvar":
                return new IntVariable(name, Integer.MIN_VALUE, Integer.MAX_VALUE);
            case "realvar":
                return new RealVariable(name, Double.MIN_VALUE, Double.MAX_VALUE);
            case "strvar":
                return new StringVariable(name);
            case "intconst":
                return new IntConstant(((Number) value).intValue());
            case "realconst":
                return new RealConstant(((Number) value).doubleValue());
            case "strconst":
                return new StringConstant((String) value);
            default:
                return null;
        }
    }

    private static void skipWhitespace(String json, int[] pos) {
        while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) {
            pos[0]++;
        }
    }

    private static void expect(String json, int[] pos, char c) {
        if (pos[0] >= json.length() || json.charAt(pos[0]) != c) {
            throw new RuntimeException("Expected '" + c + "' at position " + pos[0]);
        }
        pos[0]++;
    }

    private static String parseString(String json, int[] pos) {
        skipWhitespace(json, pos);
        if (json.charAt(pos[0]) != '"') {
            throw new RuntimeException("Expected '\"' at position " + pos[0]);
        }
        pos[0]++; // skip opening quote

        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length() && json.charAt(pos[0]) != '"') {
            char c = json.charAt(pos[0]);
            if (c == '\\' && pos[0] + 1 < json.length()) {
                pos[0]++;
                char escaped = json.charAt(pos[0]);
                switch (escaped) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    default:
                        sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
            pos[0]++;
        }
        pos[0]++; // skip closing quote
        return sb.toString();
    }

    private static Object parseValue(String json, int[] pos) {
        skipWhitespace(json, pos);
        char c = json.charAt(pos[0]);

        if (c == '"') {
            return parseString(json, pos);
        } else if (c == '-' || Character.isDigit(c)) {
            return parseNumber(json, pos);
        } else if (c == 't' || c == 'f') {
            return parseBoolean(json, pos);
        } else if (c == 'n') {
            // null
            pos[0] += 4;
            return null;
        }
        return null;
    }

    private static Number parseNumber(String json, int[] pos) {
        int start = pos[0];
        boolean isDouble = false;

        if (json.charAt(pos[0]) == '-') pos[0]++;
        while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) {
            pos[0]++;
        }
        if (pos[0] < json.length() && json.charAt(pos[0]) == '.') {
            isDouble = true;
            pos[0]++;
            while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) {
                pos[0]++;
            }
        }
        if (pos[0] < json.length() && (json.charAt(pos[0]) == 'e' || json.charAt(pos[0]) == 'E')) {
            isDouble = true;
            pos[0]++;
            if (pos[0] < json.length() && (json.charAt(pos[0]) == '+' || json.charAt(pos[0]) == '-')) {
                pos[0]++;
            }
            while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) {
                pos[0]++;
            }
        }

        String numStr = json.substring(start, pos[0]);
        if (isDouble) {
            return Double.parseDouble(numStr);
        } else {
            return Long.parseLong(numStr);
        }
    }

    private static Boolean parseBoolean(String json, int[] pos) {
        if (json.substring(pos[0]).startsWith("true")) {
            pos[0] += 4;
            return true;
        } else if (json.substring(pos[0]).startsWith("false")) {
            pos[0] += 5;
            return false;
        }
        return null;
    }

    private static void skipValue(String json, int[] pos) {
        skipWhitespace(json, pos);
        char c = json.charAt(pos[0]);

        if (c == '"') {
            parseString(json, pos);
        } else if (c == '{') {
            int depth = 1;
            pos[0]++;
            while (pos[0] < json.length() && depth > 0) {
                c = json.charAt(pos[0]);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                else if (c == '"') {
                    parseString(json, pos);
                    continue;
                }
                pos[0]++;
            }
        } else if (c == '[') {
            int depth = 1;
            pos[0]++;
            while (pos[0] < json.length() && depth > 0) {
                c = json.charAt(pos[0]);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                else if (c == '"') {
                    parseString(json, pos);
                    continue;
                }
                pos[0]++;
            }
        } else {
            parseValue(json, pos);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
