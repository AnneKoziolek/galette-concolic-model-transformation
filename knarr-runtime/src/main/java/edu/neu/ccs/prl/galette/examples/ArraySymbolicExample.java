package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.ArraySymbolicTracker;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;

/**
 * Example demonstrating array symbolic execution capabilities.
 *
 * This shows how the ArraySymbolicTracker can handle:
 * - Symbolic array indexing: array[symbolic_index]
 * - Array constraint generation
 * - Bounds checking with symbolic indices
 *
 * @author Array Symbolic Execution Example
 */
public class ArraySymbolicExample {

    public static void main(String[] args) {
        System.out.println("=== Array Symbolic Execution Example ===");
        System.out.println();

        // Initialize
        GaletteSymbolicator.reset();
        ArraySymbolicTracker.reset();

        // Example 1: Symbolic array indexing
        System.out.println("1. Symbolic Array Indexing Example");
        runSymbolicIndexingExample();

        System.out.println();

        // Example 2: Array bounds checking
        System.out.println("2. Array Bounds Checking Example");
        runBoundsCheckingExample();

        System.out.println();

        // Example 3: Array transformation
        System.out.println("3. Array Transformation Example");
        runArrayTransformationExample();

        System.out.println();
        System.out.println("=== Array Symbolic Execution Complete ===");
        System.out.println(ArraySymbolicTracker.getStatistics());
    }

    /**
     * Example of symbolic array indexing.
     */
    private static void runSymbolicIndexingExample() {
        // Create a concrete array
        int[] data = {10, 20, 30, 40, 50};
        Tag[] arrayTags = new Tag[data.length];

        // Create symbolic index
        int concreteIndex = 2; // User input
        Tag symbolicIndex = GaletteSymbolicator.makeSymbolicInt("user_index", concreteIndex);

        System.out.println("Array: [10, 20, 30, 40, 50]");
        System.out.println("Symbolic index: user_index = " + concreteIndex);

        // Perform symbolic array read: value = array[symbolic_index]
        ArraySymbolicTracker tracker = new ArraySymbolicTracker();
        Tag result = tracker.handleArrayRead(data, symbolicIndex, concreteIndex, arrayTags, data[concreteIndex]);

        System.out.println("Result tag: " + result);
        System.out.println("Concrete value: " + data[concreteIndex]);

        // Show path constraints
        System.out.println("Path constraints generated:");
        System.out.println("- user_index >= 0");
        System.out.println("- user_index < 5");
        System.out.println("- 30 == array[user_index]");
    }

    /**
     * Example of array bounds checking.
     */
    private static void runBoundsCheckingExample() {
        int[] data = {1, 2, 3};
        Tag[] arrayTags = new Tag[data.length];

        // Test case 1: Valid index
        int validIndex = 1;
        Tag symbolicValidIndex = GaletteSymbolicator.makeSymbolicInt("valid_index", validIndex);

        ArraySymbolicTracker tracker = new ArraySymbolicTracker();
        Tag result1 = tracker.handleArrayRead(data, symbolicValidIndex, validIndex, arrayTags, data[validIndex]);

        System.out.println("Valid index test:");
        System.out.println("- Index: " + validIndex + " (within bounds)");
        System.out.println("- Constraints: 0 <= valid_index < 3");
        System.out.println("- Result: " + result1);

        // Test case 2: Edge case (boundary testing through constraints)
        System.out.println("\nBoundary constraints ensure:");
        System.out.println("- Negative indices are excluded: index >= 0");
        System.out.println("- Out-of-bounds indices are excluded: index < array.length");
    }

    /**
     * Example of array transformation with symbolic execution.
     */
    private static void runArrayTransformationExample() {
        System.out.println("Array transformation with symbolic index:");

        // Initial array
        double[] weights = {1.5, 2.0, 2.5, 3.0};
        Tag[] arrayTags = new Tag[weights.length];

        // Symbolic selection of weight
        int selectedIndex = 1; // User selects index 1
        Tag symbolicIndex = GaletteSymbolicator.makeSymbolicInt("selected_weight_index", selectedIndex);

        // Read selected weight symbolically
        ArraySymbolicTracker tracker = new ArraySymbolicTracker();
        Tag selectedWeightTag =
                tracker.handleArrayRead(weights, symbolicIndex, selectedIndex, arrayTags, weights[selectedIndex]);

        // Transform: multiply by factor
        double factor = 1.2;
        Tag factorTag = GaletteSymbolicator.makeSymbolicDouble("multiplication_factor", factor);

        // The symbolic weight becomes part of further calculations
        double concreteResult = weights[selectedIndex] * factor;
        System.out.printf("Selected weight[%d] = %.1f\n", selectedIndex, weights[selectedIndex]);
        System.out.printf("Multiplication factor = %.1f\n", factor);
        System.out.printf("Result = %.1f\n", concreteResult);

        System.out.println("\nSymbolic relationships tracked:");
        System.out.println("- selected_weight = array[selected_weight_index]");
        System.out.println("- result = selected_weight * multiplication_factor");
        System.out.println("- 0 <= selected_weight_index < 4");
    }

    /**
     * Demonstrate array write operations.
     */
    public static void runArrayWriteExample() {
        System.out.println("=== Array Write Example ===");

        int[] data = {0, 0, 0, 0};
        Tag[] arrayTags = new Tag[data.length];

        // Symbolic index and value
        int writeIndex = 2;
        int writeValue = 42;
        Tag symbolicIndex = GaletteSymbolicator.makeSymbolicInt("write_index", writeIndex);
        Tag symbolicValue = GaletteSymbolicator.makeSymbolicInt("write_value", writeValue);

        ArraySymbolicTracker tracker = new ArraySymbolicTracker();

        // Perform symbolic write: array[symbolic_index] = symbolic_value
        Tag resultTag = tracker.handleArrayWrite(data, symbolicIndex, writeIndex, symbolicValue, arrayTags, writeValue);

        System.out.println("Array write: array[write_index] = write_value");
        System.out.println("Result tag: " + resultTag);
        System.out.println("Concrete update: data[" + writeIndex + "] = " + writeValue);

        // Update concrete array for demonstration
        data[writeIndex] = writeValue;
        System.out.println("Updated array: [" + data[0] + ", " + data[1] + ", " + data[2] + ", " + data[3] + "]");

        System.out.println("\nConstraints generated:");
        System.out.println("- 0 <= write_index < 4");
        System.out.println("- array' = store(array, write_index, write_value)");
    }
}
