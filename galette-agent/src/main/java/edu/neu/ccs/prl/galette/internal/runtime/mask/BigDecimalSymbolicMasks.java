package edu.neu.ccs.prl.galette.internal.runtime.mask;

import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import edu.neu.ccs.prl.galette.internal.runtime.TagFrame;
import edu.neu.ccs.prl.galette.internal.runtime.symbolic.SymbolicListener;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Symbolic model for rounding a double through {@link BigDecimal} (CoCoPath/UncertainTree
 * addition, 2026-11-10): {@code BigDecimal.valueOf(x).round(mc).doubleValue()}.
 *
 * <p>{@code BigDecimal.valueOf(double)} goes through {@code Double.toString}: the digits of the
 * decimal representation carry character-level expressions, and {@code doubleValue()} rebuilds a
 * double from them ({@code (d1 * 10 + d2) / 100.0}). Those expressions are bit-vector arithmetic
 * over a string encoding that no solver can relate back to {@code x}. The model: the double that
 * comes out of such a chain carries {@code x}'s tag, i.e. decimal rounding is the identity
 * (exact up to the rounding precision, 12 significant digits in the demonstrator's
 * {@code SysmlModels.setNumber}).
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
    private static int next;

    private BigDecimalSymbolicMasks() {
        throw new AssertionError();
    }

    private static synchronized void remember(Object key, Tag tag) {
        KEYS[next] = new WeakReference<>(key);
        TAGS[next] = tag;
        next = (next + 1) % SIZE;
    }

    private static synchronized Tag lookup(Object key) {
        for (int i = 0; i < SIZE; i++) {
            int k = (next - 1 - i + SIZE) % SIZE;
            WeakReference<Object> ref = KEYS[k];
            if (ref != null && ref.get() == key) {
                return TAGS[k];
            }
        }
        return null;
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
