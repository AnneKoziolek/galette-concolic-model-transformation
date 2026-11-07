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
}
