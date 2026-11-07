package edu.neu.ccs.prl.galette.internal.runtime.mask;

import edu.neu.ccs.prl.galette.internal.runtime.TagFrame;
import edu.neu.ccs.prl.galette.internal.runtime.symbolic.SymbolicListener;

/**
 * Symbolic models for {@code java.lang.Math} functions whose JDK implementation works on
 * the bit pattern of its argument (CoCoPath/UncertainTree addition). The original method
 * runs (POST_PROCESS); the listener then supplies the tag of the result.
 */
public final class MathSymbolicMasks {
    private MathSymbolicMasks() {
        throw new AssertionError();
    }

    @Mask(owner = "java/lang/Math", name = "round", isStatic = true, type = MaskType.POST_PROCESS)
    public static int roundFloat(int returnValue, float value, TagFrame frame) {
        frame.setReturnTag(SymbolicListener.onMathRound(true, value, frame.get(0)));
        return returnValue;
    }

    @Mask(owner = "java/lang/Math", name = "round", isStatic = true, type = MaskType.POST_PROCESS)
    public static long roundDouble(long returnValue, double value, TagFrame frame) {
        frame.setReturnTag(SymbolicListener.onMathRound(false, value, frame.get(0)));
        return returnValue;
    }

    // X.compare(a, b): the JDK computes -1/0/1 with branches whose results are untagged
    // constants, so a caller's "compare(a, b) < 0" would lose the comparison. The
    // listener supplies a tag for the result instead (e.g. Knarr's CmpDifference).

    @Mask(owner = "java/lang/Integer", name = "compare", isStatic = true, type = MaskType.POST_PROCESS)
    public static int integerCompare(int returnValue, int a, int b, TagFrame frame) {
        frame.setReturnTag(SymbolicListener.onIntegralCompare(false, a, b, frame.get(0), frame.get(1)));
        return returnValue;
    }

    @Mask(owner = "java/lang/Long", name = "compare", isStatic = true, type = MaskType.POST_PROCESS)
    public static int longCompare(int returnValue, long a, long b, TagFrame frame) {
        frame.setReturnTag(SymbolicListener.onIntegralCompare(true, a, b, frame.get(0), frame.get(1)));
        return returnValue;
    }

    @Mask(owner = "java/lang/Float", name = "compare", isStatic = true, type = MaskType.POST_PROCESS)
    public static int floatCompare(int returnValue, float a, float b, TagFrame frame) {
        frame.setReturnTag(SymbolicListener.onRealCompare(true, a, b, frame.get(0), frame.get(1)));
        return returnValue;
    }

    @Mask(owner = "java/lang/Double", name = "compare", isStatic = true, type = MaskType.POST_PROCESS)
    public static int doubleCompare(int returnValue, double a, double b, TagFrame frame) {
        frame.setReturnTag(SymbolicListener.onRealCompare(false, a, b, frame.get(0), frame.get(1)));
        return returnValue;
    }
}
