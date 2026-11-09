package edu.neu.ccs.prl.galette.internal.runtime.symbolic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

/**
 * {@link SymbolicOpcodes} promises "values match the JVM specification exactly": every constant
 * must equal ASM's constant of the same name. D2F and D2L were swapped (found 2026-11-09 in
 * CoCoPath/UncertainTree: EMF's NotificationImpl.getNewFloatValue, a d2f, reached the listener as
 * D2L and turned a float attribute's expression into a truncation to long).
 */
class SymbolicOpcodesTest {
    @Test
    void everyConstantMatchesAsm() throws Exception {
        int checked = 0;
        for (Field f : SymbolicOpcodes.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != int.class) {
                continue;
            }
            Field asm = Opcodes.class.getField(f.getName());
            Assertions.assertEquals(asm.getInt(null), f.getInt(null), f.getName());
            checked++;
        }
        Assertions.assertTrue(checked > 80, "constants checked: " + checked);
    }
}
