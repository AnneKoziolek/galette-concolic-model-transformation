package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import edu.neu.ccs.prl.galette.concolic.knarr.green.GaletteGreenBridge;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import java.util.HashSet;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.expr.Operation.Operator;

/**
 * Galette-based path constraint utilities.
 *
 * This class replaces the original Phosphor-based PathUtils with
 * Galette-compatible APIs for managing symbolic execution constraints.
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class PathUtils {

    /**
     * Constants for bit vector operations.
     */
    private static IntConstant O000FFFF;

    public static BVConstant BV0_32;

    /**
     * Current path condition wrapper.
     */
    private static PathConditionWrapper curPC;

    /**
     * Configuration flags.
     */
    public static final boolean IGNORE_SHIFTS = true;

    public static final String INTERNAL_NAME = "edu/neu/ccs/prl/galette/concolic/knarr/runtime/PathUtils";

    /**
     * JPF initialization flag.
     */
    static boolean JPFInited = false;

    /**
     * Set of used labels to prevent duplicates.
     */
    static HashSet<String> usedLabels = new HashSet<String>();

    /**
     * Get the current path condition.
     *
     * @return Current path condition wrapper
     */
    public static PathConditionWrapper getCurPC() {
        if (curPC == null) {
            curPC = new PathConditionWrapper();
        }
        return curPC;
    }

    /**
     * Reset the PathUtils state for testing.
     */
    public static void reset() {
        usedLabels.clear();
        JPFInited = false;
        curPC = null;
    }

    /**
     * Check label validity and initialize JPF if needed.
     *
     * @param label The label to check
     */
    public static void checkLabelAndInitJPF(String label) {
        if (label == null || usedLabels.contains(label)) {
            throw new IllegalArgumentException("Invalid (dup?) label: \"" + label + "\"");
        }
        if (label.contains(" ")) {
            throw new IllegalArgumentException("label has spaces, but must not: \"" + label + "\"");
        }
        // Note: In Galette, we don't have the same taint checking as Phosphor
        // TODO: Add equivalent Galette tag checking if needed
        usedLabels.add(label);
        if (!JPFInited) {
            initJPF();
        }
    }

    /**
     * Initialize JPF (Java PathFinder) compatibility.
     */
    private static void initJPF() {
        // TODO: Implement JPF initialization for Galette
        JPFInited = true;
    }

    /**
     * Perform a long operation with symbolic tracking.
     *
     * @param lTag Tag for left operand
     * @param lVal Left operand value
     * @param rTag Tag for right operand
     * @param rVal Right operand value
     * @param op Operation code
     * @param result Result container
     * @return Result with updated tag
     */
    public static GaletteTaintedLong performLongOp(
            Tag lTag, long lVal, Tag rTag, long rVal, int op, GaletteTaintedLong result) {
        if (lTag == null && rTag == null) {
            result.tag = null;
            result.value = performConcreteLongOp(lVal, rVal, op);
        } else {
            Expression l = (lTag != null)
                    ? GaletteGreenBridge.tagToGreenExpression(lTag, lVal)
                    : new IntConstant((int) lVal); // Truncate for Green solver

            Expression r =
                    (rTag != null) ? GaletteGreenBridge.tagToGreenExpression(rTag, rVal) : new IntConstant((int) rVal);

            Operator operator = getOperatorForOpcode(op);
            if (operator != null) {
                Expression expr = GaletteGreenBridge.createBinaryOp(l, operator, r);
                result.tag = createTagFromExpression(expr);
                result.value = performConcreteLongOp(lVal, rVal, op);

                // Add constraint if this creates a new symbolic relationship
                getCurPC().addConstraint(expr);
            } else {
                // Unsupported operation, treat as concrete
                result.tag = null;
                result.value = performConcreteLongOp(lVal, rVal, op);
            }
        }
        return result;
    }

    /**
     * Perform a concrete long operation.
     *
     * @param lVal Left operand
     * @param rVal Right operand
     * @param op Operation code
     * @return Result of the operation
     */
    private static long performConcreteLongOp(long lVal, long rVal, int op) {
        switch (op) {
            case 97: // LADD
                return lVal + rVal;
            case 101: // LSUB
                return lVal - rVal;
            case 105: // LMUL
                return lVal * rVal;
            case 109: // LDIV
                return rVal != 0 ? lVal / rVal : 0; // Avoid division by zero
            case 113: // LREM
                return rVal != 0 ? lVal % rVal : 0;
            case 121: // LSHL
                return lVal << rVal;
            case 123: // LSHR
                return lVal >> rVal;
            case 125: // LUSHR
                return lVal >>> rVal;
            case 127: // LAND
                return lVal & rVal;
            case 129: // LOR
                return lVal | rVal;
            case 131: // LXOR
                return lVal ^ rVal;
            default:
                return lVal; // Unknown operation, return left operand
        }
    }

    /**
     * Convert bytecode operation to Green operator.
     *
     * @param opcode The bytecode operation
     * @return Corresponding Green operator, or null if not supported
     */
    private static Operator getOperatorForOpcode(int opcode) {
        switch (opcode) {
            case 96: // IADD
            case 97: // LADD
            case 98: // FADD
            case 99: // DADD
                return Operator.ADD;
            case 100: // ISUB
            case 101: // LSUB
            case 102: // FSUB
            case 103: // DSUB
                return Operator.SUB;
            case 104: // IMUL
            case 105: // LMUL
            case 106: // FMUL
            case 107: // DMUL
                return Operator.MUL;
            case 108: // IDIV
            case 109: // LDIV
            case 110: // FDIV
            case 111: // DDIV
                return Operator.DIV;
            case 112: // IREM
            case 113: // LREM
            case 114: // FREM
            case 115: // DREM
                return Operator.MOD;
            case 126: // IAND
            case 127: // LAND
                return Operator.BIT_AND;
            case 128: // IOR
            case 129: // LOR
                return Operator.BIT_OR;
            case 130: // IXOR
            case 131: // LXOR
                return Operator.BIT_XOR;
            default:
                return null; // Unsupported operation
        }
    }

    /**
     * Create a Galette tag from a Green expression.
     *
     * @param expr The Green expression
     * @return Galette tag representing the expression
     */
    private static Tag createTagFromExpression(Expression expr) {
        // For now, create a tag with the expression string as label
        // TODO: Implement more sophisticated tag creation
        return Tag.of("expr_" + expr.toString().hashCode());
    }

    /**
     * Simple container for tagged long values.
     */
    public static class GaletteTaintedLong {
        public Tag tag;
        public long value;

        public GaletteTaintedLong() {
            this.tag = null;
            this.value = 0L;
        }

        public GaletteTaintedLong(long value, Tag tag) {
            this.value = value;
            this.tag = tag;
        }
    }
}
