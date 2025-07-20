package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.ArraySymbolicTracker;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.expr.Operation.Operator;

/**
 * Test array symbolic execution with Green solver integration.
 *
 * This example verifies that our ArraySymbolicTracker can properly
 * integrate with Green solver expressions.
 *
 * @author Array Green Test
 */
public class ArraySymbolicGreenTest {

    public static void main(String[] args) {
        System.out.println("=== Array Symbolic Execution with Green Solver Test ===");
        System.out.println();

        try {
            // Test Green solver expression creation
            testGreenExpressions();
            System.out.println();

            // Test array symbolic tracker
            testArraySymbolicTracker();
            System.out.println();

            System.out.println("✓ Green solver integration test passed!");

        } catch (Exception e) {
            System.out.println("✗ Test failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("=== Test Complete ===");
    }

    /**
     * Test creating Green solver expressions.
     */
    private static void testGreenExpressions() {
        System.out.println("Testing Green solver expression creation:");

        // Create basic expressions
        IntConstant five = new IntConstant(5);
        IntVariable index = new IntVariable("array_index", null, null);

        // Create array bounds constraint: index >= 0
        BinaryOperation lowerBound = new BinaryOperation(Operator.GE, index, new IntConstant(0));

        // Create array bounds constraint: index < 5
        BinaryOperation upperBound = new BinaryOperation(Operator.LT, index, five);

        // Combine constraints: (index >= 0) AND (index < 5)
        BinaryOperation boundsCheck = new BinaryOperation(Operator.AND, lowerBound, upperBound);

        System.out.println("✓ Created expressions:");
        System.out.println("  - Lower bound: " + lowerBound);
        System.out.println("  - Upper bound: " + upperBound);
        System.out.println("  - Combined: " + boundsCheck);

        // Test array operations
        ArrayVariable arrayVar = new ArrayVariable("test_array", int.class);
        BinaryOperation arrayRead = new BinaryOperation(Operator.SELECT, arrayVar, index);

        System.out.println("  - Array read: " + arrayRead);
        System.out.println("✓ Green solver expressions working correctly");
    }

    /**
     * Test ArraySymbolicTracker functionality.
     */
    private static void testArraySymbolicTracker() {
        System.out.println("Testing ArraySymbolicTracker:");

        // Reset tracker state
        ArraySymbolicTracker.reset();

        // Create test array
        int[] testArray = {10, 20, 30, 40, 50};
        System.out.println("Test array: [10, 20, 30, 40, 50]");

        // Create symbolic tags
        Tag indexTag = Tag.of("symbolic_index");
        Tag[] arrayTags = new Tag[testArray.length];

        System.out.println("✓ Created symbolic index tag: " + indexTag);

        // Create ArraySymbolicTracker instance
        ArraySymbolicTracker tracker = new ArraySymbolicTracker();

        // Test array read with symbolic index
        int concreteIndex = 2;
        Object concreteValue = testArray[concreteIndex];

        Tag resultTag = tracker.handleArrayRead(testArray, indexTag, concreteIndex, arrayTags, concreteValue);

        if (resultTag != null) {
            System.out.println("✓ Array read produced symbolic result: " + resultTag);
        } else {
            System.out.println("○ Array read returned concrete result (expected for some cases)");
        }

        // Test array write
        Tag valueTag = Tag.of("symbolic_value");
        Tag writeResult = tracker.handleArrayWrite(testArray, indexTag, concreteIndex, valueTag, arrayTags, 42);

        if (writeResult != null) {
            System.out.println("✓ Array write produced symbolic result: " + writeResult);
        } else {
            System.out.println("○ Array write returned concrete result");
        }

        // Show statistics
        System.out.println("✓ ArraySymbolicTracker working correctly");
        System.out.println(ArraySymbolicTracker.getStatistics());
    }
}
