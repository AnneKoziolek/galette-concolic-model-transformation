package edu.neu.ccs.prl.galette.internal.runtime.mask;

import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import edu.neu.ccs.prl.galette.internal.runtime.TagFrame;
import edu.neu.ccs.prl.galette.internal.runtime.symbolic.SymbolicListener;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Symbolic model for decimal arithmetic on a double through {@link BigDecimal}
 * (CoCoPath/UncertainTree addition, 2026-11-10): {@code BigDecimal.valueOf(x)}, then
 * {@code round(mc)}, {@code multiply}/{@code divide} by a factor, {@code setScale(0, HALF_UP)},
 * and back with {@code doubleValue()} or {@code intValueExact()}/{@code intValue()}.
 *
 * <p>{@code BigDecimal.valueOf(double)} goes through {@code Double.toString}: the digits of the
 * decimal representation carry character-level expressions, and {@code doubleValue()} rebuilds a
 * double from them ({@code (d1 * 10 + d2) / 100.0}). Those expressions are bit-vector arithmetic
 * over a string encoding that no solver can relate back to {@code x}. The model: the double that
 * comes out of such a chain carries {@code x}'s tag, i.e. decimal rounding is the identity
 * (exact up to the rounding precision, 12 significant digits in the demonstrator's
 * {@code SysmlModels.setNumber}); multiplication and division are real arithmetic on the tag
 * (the demonstrator's unit conversions, {@code AttributeMappings.scale}); rounding to an integer
 * is {@code Math.round}'s model (half up, as {@code HALF_UP} for the values seen).
 *
 * <p>The tag travels with the BigDecimal objects in a small identity table (weak references,
 * newest first), filled only while a symbolic listener is installed; equal values from other
 * sources never inherit it.
 */
public final class BigDecimalSymbolicMasks {
    private static final int SIZE = 64;

    @SuppressWarnings("unchecked")
    private static final WeakReference<Object>[] KEYS = new WeakReference[SIZE];

    private static final Tag[] TAGS = new Tag[SIZE];
    /** Whether the entry is an integral value from {@code setScale(0, ...)}; its tag is the value before rounding. */
    private static final boolean[] ROUNDED = new boolean[SIZE];

    private static int next;

    private BigDecimalSymbolicMasks() {
        throw new AssertionError();
    }

    private static void remember(Object key, Tag tag) {
        remember(key, tag, false);
    }

    private static synchronized void remember(Object key, Tag tag, boolean rounded) {
        KEYS[next] = new WeakReference<>(key);
        TAGS[next] = tag;
        ROUNDED[next] = rounded;
        next = (next + 1) % SIZE;
    }

    private static synchronized int index(Object key) {
        if (key == null) {
            return -1;
        }
        for (int i = 0; i < SIZE; i++) {
            int k = (next - 1 - i + SIZE) % SIZE;
            WeakReference<Object> ref = KEYS[k];
            if (ref != null && ref.get() == key) {
                return k;
            }
        }
        return -1;
    }

    private static synchronized Tag lookup(Object key) {
        int k = index(key);
        return k < 0 || ROUNDED[k] ? null : TAGS[k];
    }

    private static synchronized Tag lookupRounded(Object key) {
        int k = index(key);
        return k < 0 || !ROUNDED[k] ? null : TAGS[k];
    }

    /** {@code receiver op other} as real arithmetic on the tags, for a result in the table. */
    private static void arith(int opcode, BigDecimal returnValue, BigDecimal receiver, BigDecimal other) {
        if (returnValue == null || receiver == null || other == null || !SymbolicListener.isSymbolic()) {
            return;
        }
        Tag a = lookup(receiver);
        Tag b = lookup(other);
        if (a == null && b == null) {
            return;
        }
        Tag t = SymbolicListener.onDoubleArith(
                opcode,
                receiver.doubleValue(),
                other.doubleValue(),
                a == null ? Tag.emptyTag() : a,
                b == null ? Tag.emptyTag() : b);
        if (!Tag.isEmpty(t)) {
            remember(returnValue, t);
        }
    }

    @Mask(owner = "java/math/BigDecimal", name = "valueOf", isStatic = true, type = MaskType.POST_PROCESS)
    public static BigDecimal valueOf(BigDecimal returnValue, double value, TagFrame frame) {
        Tag tag = frame.get(0);
        if (returnValue != null && SymbolicListener.isSymbolic() && !Tag.isEmpty(tag)) {
            remember(returnValue, tag);
        }
        return returnValue;
    }

    @Mask(owner = "java/math/BigDecimal", name = "round", type = MaskType.POST_PROCESS)
    public static BigDecimal round(BigDecimal returnValue, BigDecimal receiver, MathContext mc, TagFrame frame) {
        if (returnValue != null && SymbolicListener.isSymbolic()) {
            Tag tag = lookup(receiver);
            if (tag != null) {
                remember(returnValue, tag);
            }
        }
        return returnValue;
    }

    @Mask(owner = "java/math/BigDecimal", name = "multiply", type = MaskType.POST_PROCESS)
    public static BigDecimal multiply(BigDecimal returnValue, BigDecimal receiver, BigDecimal other, TagFrame frame) {
        arith(edu.neu.ccs.prl.galette.internal.runtime.symbolic.SymbolicOpcodes.DMUL, returnValue, receiver, other);
        return returnValue;
    }

    @Mask(owner = "java/math/BigDecimal", name = "divide", type = MaskType.POST_PROCESS)
    public static BigDecimal divide(
            BigDecimal returnValue, BigDecimal receiver, BigDecimal other, MathContext mc, TagFrame frame) {
        arith(edu.neu.ccs.prl.galette.internal.runtime.symbolic.SymbolicOpcodes.DDIV, returnValue, receiver, other);
        return returnValue;
    }

    @Mask(owner = "java/math/BigDecimal", name = "setScale", type = MaskType.POST_PROCESS)
    public static BigDecimal setScale(
            BigDecimal returnValue, BigDecimal receiver, int newScale, java.math.RoundingMode mode, TagFrame frame) {
        if (returnValue != null && SymbolicListener.isSymbolic()) {
            Tag tag = lookup(receiver);
            if (tag != null) {
                // scale 0: an integer (rounded at intValueExact); other scales: decimal rounding as the identity
                remember(returnValue, tag, newScale == 0);
            }
        }
        return returnValue;
    }

    @Mask(owner = "java/math/BigDecimal", name = "intValueExact", type = MaskType.POST_PROCESS)
    @Mask(owner = "java/math/BigDecimal", name = "intValue", type = MaskType.POST_PROCESS)
    public static int intValue(int returnValue, BigDecimal receiver, TagFrame frame) {
        if (SymbolicListener.isSymbolic()) {
            Tag tag = lookupRounded(receiver);
            if (tag != null) {
                frame.setReturnTag(SymbolicListener.onMathRound(true, returnValue, tag));
            }
        }
        return returnValue;
    }

    @Mask(owner = "java/math/BigDecimal", name = "doubleValue", type = MaskType.POST_PROCESS)
    public static double doubleValue(double returnValue, BigDecimal receiver, TagFrame frame) {
        if (SymbolicListener.isSymbolic()) {
            Tag tag = lookup(receiver);
            if (tag != null) {
                frame.setReturnTag(tag);
            }
        }
        return returnValue;
    }
}
