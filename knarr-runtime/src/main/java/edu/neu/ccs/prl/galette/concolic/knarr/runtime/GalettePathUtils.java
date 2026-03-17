package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

/**
 * Galette-specific path utilities - delegates to PathUtils.
 *
 * This class provides a Galette-specific naming convention while
 * reusing the implementation from PathUtils.
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class GalettePathUtils {

    /**
     * Check label validity and initialize JPF if needed.
     *
     * @param label The label to check
     */
    public static void checkLabelAndInitJPF(String label) {
        PathUtils.checkLabelAndInitJPF(label);
    }

    /**
     * Get the current path condition.
     *
     * @return Current path condition wrapper
     */
    public static PathConditionWrapper getCurPC() {
        return PathUtils.getCurPC();
    }

    /**
     * Reset the path utilities state.
     */
    public static void reset() {
        PathUtils.reset();
    }

    /**
     * Get the current path condition with Galette automatic interception constraints.
     *
     * @return Path condition wrapper containing both manual and automatic constraints
     */
    public static PathConditionWrapper getCurPCWithGalette() {
        return PathUtils.getCurPCWithGalette();
    }

    /**
     * Reset the current path condition.
     */
    public static void resetPC() {
        PathUtils.resetPC();
    }
}
