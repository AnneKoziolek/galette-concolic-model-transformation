package edu.neu.ccs.prl.galette.internal.runtime.mask;

import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import edu.neu.ccs.prl.galette.internal.runtime.TagFrame;
import edu.neu.ccs.prl.galette.internal.runtime.TagFrameFactory;

public final class DoubleMasks {
    @Mask(owner = "java/lang/Double", name = "valueOf", isStatic = true)
    public static Double valueOf(double value, TagFrame frame) {
        Tag valueTag = frame.get(0);
        Double result;
        if (Tag.isEmpty(valueTag)) {
            result = BoxTypeAccessor.valueOf(value, TagFrameFactory.acquire(frame, Tag.emptyTag()));
        } else {
            result = BoxTypeAccessor.newDouble(value, TagFrameFactory.acquire(frame, Tag.emptyTag(), valueTag));
        }
        frame.setReturnTag(valueTag);
        return result;
    }

    @Mask(owner = "java/lang/Double", name = "doubleToRawLongBits", isStatic = true, type = MaskType.POST_PROCESS)
    @Mask(owner = "java/lang/Double", name = "doubleToLongBits", isStatic = true, type = MaskType.POST_PROCESS)
    public static long doubleToIntBits(long returnValue, double value, TagFrame frame) {
        Tag valueTag = frame.get(0);
        // CoCoPath/UncertainTree: the bit pattern of a symbolic floating-point value is
        // not expressible without floating-point bit-vector theory. Carrying the value's
        // expression onto the bits makes JDK internals (e.g. Math.round) branch on garbage.
        // While a symbolic listener is installed, the bits are therefore untagged; library
        // functions that need them get dedicated masks (MathSymbolicMasks).
        frame.setReturnTag(
                edu.neu.ccs.prl.galette.internal.runtime.symbolic.SymbolicListener.isSymbolic()
                        ? Tag.emptyTag()
                        : valueTag);
        return returnValue;
    }

    @Mask(owner = "java/lang/Double", name = "longBitsToDouble", isStatic = true, type = MaskType.POST_PROCESS)
    public static double longBitsToDouble(double returnValue, long bits, TagFrame frame) {
        Tag valueTag = frame.get(0);
        frame.setReturnTag(valueTag);
        return returnValue;
    }
}
