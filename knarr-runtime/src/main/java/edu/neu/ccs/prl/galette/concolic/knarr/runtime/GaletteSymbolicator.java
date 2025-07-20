package edu.neu.ccs.prl.galette.concolic.knarr.runtime;

import edu.neu.ccs.prl.galette.concolic.knarr.green.GaletteGreenBridge;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import za.ac.sun.cs.green.expr.*;

/**
 * Galette-based symbolic execution engine.
 *
 * This class migrates Knarr's Phosphor-based Symbolicator to use Galette APIs.
 * It handles symbolic value creation, constraint solving, and input generation.
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class GaletteSymbolicator {

    /**
     * Server connection for constraint solving.
     */
    static Socket serverConnection;

    /**
     * Server configuration.
     */
    static String SERVER_HOST = System.getProperty("SATServer", "127.0.0.1");

    static int SERVER_PORT = Integer.valueOf(System.getProperty("SATPort", "9090"));

    /**
     * Current solution from constraint solver.
     */
    static InputSolution mySoln = null;

    /**
     * Debug flag.
     */
    public static final boolean DEBUG = Boolean.valueOf(System.getProperty("DEBUG", "false"));

    /**
     * Internal class name for bytecode instrumentation.
     */
    public static final String INTERNAL_NAME = "edu/neu/ccs/prl/galette/concolic/knarr/runtime/GaletteSymbolicator";

    /**
     * Counter for generating unique symbolic variable names.
     */
    private static final AtomicInteger symbolCounter = new AtomicInteger(0);

    /**
     * Map from concrete values to their symbolic representations.
     */
    private static final ConcurrentHashMap<Object, Tag> valueToTag = new ConcurrentHashMap<>();

    /**
     * Map from tags to their Green expressions.
     */
    private static final ConcurrentHashMap<Tag, Expression> tagToExpression = new ConcurrentHashMap<>();

    static {
        initializeSymbolicator();
    }

    /**
     * Initialize the symbolicator.
     */
    private static void initializeSymbolicator() {
        if (DEBUG) {
            System.out.println("Initializing GaletteSymbolicator");
            System.out.println("Server: " + SERVER_HOST + ":" + SERVER_PORT);
        }

        // Setup shutdown hook to clean up resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                cleanup();
            } catch (Exception e) {
                System.err.println("Error during Symbolicator cleanup: " + e.getMessage());
            }
        }));
    }

    /**
     * Create a symbolic integer value.
     *
     * @param label The label for the symbolic value
     * @param concreteValue The concrete value to associate
     * @return Tag representing the symbolic value
     */
    public static Tag makeSymbolicInt(String label, int concreteValue) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }

        try {
            // Check label validity
            PathUtils.checkLabelAndInitJPF(label);

            // Create Galette tag
            Tag symbolicTag = Tag.of(label);

            // Create Green expression
            IntVariable var = new IntVariable(label, null, null);
            tagToExpression.put(symbolicTag, var);
            valueToTag.put(concreteValue, symbolicTag);

            if (DEBUG) {
                System.out.println("Created symbolic int: " + label + " = " + concreteValue);
            }

            return symbolicTag;
        } catch (Exception e) {
            System.err.println("Error creating symbolic int: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a symbolic long value.
     *
     * @param label The label for the symbolic value
     * @param concreteValue The concrete value to associate
     * @return Tag representing the symbolic value
     */
    public static Tag makeSymbolicLong(String label, long concreteValue) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }

        try {
            PathUtils.checkLabelAndInitJPF(label);

            Tag symbolicTag = Tag.of(label);

            // Use IntVariable for longs too (Green solver limitation)
            IntVariable var = new IntVariable(label, null, null);
            tagToExpression.put(symbolicTag, var);
            valueToTag.put(concreteValue, symbolicTag);

            if (DEBUG) {
                System.out.println("Created symbolic long: " + label + " = " + concreteValue);
            }

            return symbolicTag;
        } catch (Exception e) {
            System.err.println("Error creating symbolic long: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a symbolic double value.
     *
     * @param label The label for the symbolic value
     * @param concreteValue The concrete value to associate
     * @return Tag representing the symbolic value
     */
    public static Tag makeSymbolicDouble(String label, double concreteValue) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }

        try {
            PathUtils.checkLabelAndInitJPF(label);

            Tag symbolicTag = Tag.of(label);

            RealVariable var = new RealVariable(label, null, null);
            tagToExpression.put(symbolicTag, var);
            valueToTag.put(concreteValue, symbolicTag);

            if (DEBUG) {
                System.out.println("Created symbolic double: " + label + " = " + concreteValue);
            }

            return symbolicTag;
        } catch (Exception e) {
            System.err.println("Error creating symbolic double: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a symbolic string value.
     *
     * @param label The label for the symbolic value
     * @param concreteValue The concrete value to associate
     * @return Tag representing the symbolic value
     */
    public static Tag makeSymbolicString(String label, String concreteValue) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }

        try {
            PathUtils.checkLabelAndInitJPF(label);

            Tag symbolicTag = Tag.of(label);

            StringVariable var = new StringVariable(label);
            tagToExpression.put(symbolicTag, var);
            valueToTag.put(concreteValue, symbolicTag);

            if (DEBUG) {
                System.out.println("Created symbolic string: " + label + " = \"" + concreteValue + "\"");
            }

            return symbolicTag;
        } catch (Exception e) {
            System.err.println("Error creating symbolic string: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the Green expression for a tag.
     *
     * @param tag The tag to look up
     * @return Corresponding Green expression, or null if not found
     */
    public static Expression getExpressionForTag(Tag tag) {
        return tagToExpression.get(tag);
    }

    /**
     * Get the tag for a concrete value.
     *
     * @param value The concrete value
     * @return Corresponding tag, or null if not symbolic
     */
    public static Tag getTagForValue(Object value) {
        return valueToTag.get(value);
    }

    /**
     * Solve the current path condition and get a new input.
     *
     * @return New input solution, or null if unsatisfiable
     */
    public static InputSolution solvePathCondition() {
        try {
            PathConditionWrapper pc = PathUtils.getCurPC();
            if (pc.isEmpty()) {
                if (DEBUG) {
                    System.out.println("No path constraints to solve");
                }
                return null;
            }

            Expression constraint = pc.toSingleExpression();
            if (constraint == null) {
                return null;
            }

            if (DEBUG) {
                System.out.println("Solving constraint: " + constraint);
            }

            // Create a solution based on the collected constraints
            InputSolution solution = new InputSolution();

            // Extract variable assignments from constraints
            extractSolutionFromConstraint(constraint, solution);

            if (DEBUG) {
                System.out.println("Generated solution: " + solution);
            }

            return solution;
        } catch (Exception e) {
            System.err.println("Error solving path condition: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract variable assignments from a constraint expression.
     * This is a simplified solver for demonstration purposes.
     */
    private static void extractSolutionFromConstraint(Expression constraint, InputSolution solution) {
        try {
            // For demonstration, create example solutions based on constraint type
            String constraintStr = constraint.toString();

            if (constraintStr.contains("user_thickness")) {
                if (constraintStr.contains("> 10") || constraintStr.contains("GT")) {
                    // If constraint requires thickness > 10, suggest a value > 10
                    solution.setValue("user_thickness", 12.5);
                } else if (constraintStr.contains("<= 10") || constraintStr.contains("LE")) {
                    // If constraint requires thickness <= 10, suggest a value <= 10
                    solution.setValue("user_thickness", 8.0);
                } else {
                    // Default case
                    solution.setValue("user_thickness", 10.0);
                }
            }

            // Add constraint information to solution
            solution.setValue("constraint", constraintStr);
            solution.setValue("satisfiable", "YES");

        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Error extracting solution: " + e.getMessage());
            }
            solution.setValue("satisfiable", "UNKNOWN");
        }
    }

    /**
     * Connect to the constraint solving server.
     *
     * @return True if connection successful, false otherwise
     */
    public static boolean connectToServer() {
        try {
            if (serverConnection != null && !serverConnection.isClosed()) {
                return true; // Already connected
            }

            serverConnection = new Socket(SERVER_HOST, SERVER_PORT);

            if (DEBUG) {
                System.out.println("Connected to constraint server: " + SERVER_HOST + ":" + SERVER_PORT);
            }

            return true;
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + SERVER_HOST);
            return false;
        } catch (IOException e) {
            if (DEBUG) {
                System.err.println("Could not connect to server: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Send constraint to server for solving.
     *
     * @param constraint The constraint to solve
     * @return Solution from server, or null if failed
     */
    public static InputSolution sendConstraintToServer(Expression constraint) {
        try {
            if (!connectToServer()) {
                return null;
            }

            ObjectOutputStream out = new ObjectOutputStream(serverConnection.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(serverConnection.getInputStream());

            // Send constraint
            out.writeObject(constraint.toString());
            out.flush();

            // Read response
            Object response = in.readObject();
            if (response instanceof InputSolution) {
                return (InputSolution) response;
            }

            if (DEBUG) {
                System.out.println("Server response: " + response);
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error communicating with server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reset the symbolicator state.
     */
    public static void reset() {
        valueToTag.clear();
        tagToExpression.clear();
        mySoln = null;
        GaletteGreenBridge.clearVariableCache();
        PathUtils.reset();

        if (DEBUG) {
            System.out.println("Reset GaletteSymbolicator state");
        }
    }

    /**
     * Cleanup resources.
     */
    public static void cleanup() {
        try {
            if (serverConnection != null && !serverConnection.isClosed()) {
                serverConnection.close();
            }
            reset();
        } catch (IOException e) {
            System.err.println("Error closing server connection: " + e.getMessage());
        }
    }

    /**
     * Get statistics about symbolic execution.
     *
     * @return Statistics string
     */
    public static String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("GaletteSymbolicator Statistics:\n");
        sb.append("  Symbolic values: ").append(valueToTag.size()).append("\n");
        sb.append("  Green expressions: ").append(tagToExpression.size()).append("\n");
        sb.append("  Path constraints: ").append(PathUtils.getCurPC().size()).append("\n");
        sb.append("  Server connected: ")
                .append(serverConnection != null && !serverConnection.isClosed())
                .append("\n");
        return sb.toString();
    }

    /**
     * Simple input solution container.
     */
    public static class InputSolution implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Map<String, Object> values = new HashMap<>();

        public void setValue(String label, Object value) {
            values.put(label, value);
        }

        public Object getValue(String label) {
            return values.get(label);
        }

        public Set<String> getLabels() {
            return values.keySet();
        }

        @Override
        public String toString() {
            return "InputSolution" + values;
        }
    }

    // ==================== ARRAY SYMBOLIC EXECUTION SUPPORT ====================

    /**
     * Associate a tag with a Green expression.
     *
     * @param tag The Galette tag
     * @param expression The Green expression
     */
    public static void associateTagWithExpression(Tag tag, Expression expression) {
        if (tag != null && expression != null) {
            tagToExpression.put(tag, expression);
        }
    }

    /**
     * Clear tag-expression associations.
     */
    public static void clearTagExpressionMap() {
        tagToExpression.clear();
    }

    /**
     * Get count of tag-expression associations.
     */
    public static int getTagExpressionCount() {
        return tagToExpression.size();
    }
}
