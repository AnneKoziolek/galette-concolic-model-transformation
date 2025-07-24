package edu.neu.ccs.prl.galette;

import java.util.List;

/**
 * Public API for accessing path constraints collected by Galette's automatic comparison interception.
 * This class provides a bridge to the internal PathUtils without exposing internal implementation details.
 *
 * @author Implementation based on claude-copilot-combined-comparison-interception-plan-3.md
 */
public class PathConstraintAPI {

    /**
     * Get the current path constraints collected by automatic interception.
     *
     * @return List of constraints, or empty list if interception is not enabled
     */
    public static List<?> getCurrentConstraints() {
        try {
            Class<?> pathUtilsClass = Class.forName("edu.neu.ccs.prl.galette.internal.runtime.PathUtils");
            java.lang.reflect.Method getCurrentMethod = pathUtilsClass.getMethod("getCurrent");
            return (List<?>) getCurrentMethod.invoke(null);
        } catch (Exception e) {
            // PathUtils not available or reflection failed
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get and clear the current path constraints.
     *
     * @return List of constraints that were collected, or empty list if interception is not enabled
     */
    public static List<?> flushConstraints() {
        try {
            Class<?> pathUtilsClass = Class.forName("edu.neu.ccs.prl.galette.internal.runtime.PathUtils");
            java.lang.reflect.Method flushMethod = pathUtilsClass.getMethod("flush");
            return (List<?>) flushMethod.invoke(null);
        } catch (Exception e) {
            // PathUtils not available or reflection failed
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Check if automatic comparison interception is available.
     *
     * @return true if the internal PathUtils class is accessible
     */
    public static boolean isAvailable() {
        try {
            Class.forName("edu.neu.ccs.prl.galette.internal.runtime.PathUtils");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Get the number of constraints currently collected.
     *
     * @return Number of constraints, or 0 if not available
     */
    public static int getConstraintCount() {
        return getCurrentConstraints().size();
    }
}
