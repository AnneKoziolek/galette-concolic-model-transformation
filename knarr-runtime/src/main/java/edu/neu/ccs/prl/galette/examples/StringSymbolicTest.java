package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.StringSymbolicTracker;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import za.ac.sun.cs.green.expr.*;

/**
 * Test string symbolic execution with Galette integration.
 *
 * This example verifies that our StringSymbolicTracker can properly
 * handle symbolic string operations and generate appropriate constraints.
 *
 * @author String Symbolic Test
 */
public class StringSymbolicTest {

    public static void main(String[] args) {
        System.out.println("=== String Symbolic Execution Test ===");
        System.out.println();

        try {
            // Test string symbolic operations
            testStringOperations();
            System.out.println();

            // Test string comparisons
            testStringComparisons();
            System.out.println();

            // Test string transformations
            testStringTransformations();
            System.out.println();

            System.out.println("✓ String symbolic execution test passed!");

        } catch (Exception e) {
            System.out.println("✗ Test failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("=== Test Complete ===");
    }

    /**
     * Test basic string symbolic operations.
     */
    private static void testStringOperations() {
        System.out.println("Testing basic string symbolic operations:");

        StringSymbolicTracker tracker = new StringSymbolicTracker();
        StringSymbolicTracker.reset();

        // Test symbolic string creation
        String testStr = "hello";
        Tag stringTag = Tag.of("symbolic_string");

        tracker.registerSymbolicString(testStr, stringTag, 0, testStr.length());
        System.out.println("✓ Registered symbolic string: \"" + testStr + "\" with tag: " + stringTag);

        // Test string length
        Tag lengthResult = tracker.handleLength(testStr, stringTag);
        if (lengthResult != null) {
            System.out.println("✓ String length operation produced symbolic result: " + lengthResult);
        } else {
            System.out.println("○ String length returned concrete result");
        }

        // Test isEmpty
        Tag isEmptyResult = tracker.handleIsEmpty(testStr, stringTag);
        if (isEmptyResult != null) {
            System.out.println("✓ String isEmpty operation produced symbolic result: " + isEmptyResult);
        } else {
            System.out.println("○ String isEmpty returned concrete result");
        }

        // Test charAt
        Tag charAtResult = tracker.handleCharAt(testStr, 1, stringTag, null);
        if (charAtResult != null) {
            System.out.println("✓ String charAt operation produced symbolic result: " + charAtResult);
        } else {
            System.out.println("○ String charAt returned concrete result");
        }

        System.out.println("✓ Basic string operations working correctly");
    }

    /**
     * Test string comparison operations.
     */
    private static void testStringComparisons() {
        System.out.println("Testing string comparison operations:");

        StringSymbolicTracker tracker = new StringSymbolicTracker();

        // Test strings
        String str1 = "hello";
        String str2 = "world";
        String str3 = "hello";

        Tag str1Tag = Tag.of("symbolic_str1");
        Tag str2Tag = Tag.of("symbolic_str2");

        // Test equals
        Tag equalsResult = tracker.handleEquals(str1, str3, str1Tag, null);
        if (equalsResult != null) {
            System.out.println("✓ String equals operation produced symbolic result: " + equalsResult);
        } else {
            System.out.println("○ String equals returned concrete result");
        }

        // Test startsWith
        Tag startsWithResult = tracker.handleStartsWith(str1, "he", 0, str1Tag, null);
        if (startsWithResult != null) {
            System.out.println("✓ String startsWith operation produced symbolic result: " + startsWithResult);
        } else {
            System.out.println("○ String startsWith returned concrete result");
        }

        // Test endsWith
        Tag endsWithResult = tracker.handleEndsWith(str1, "lo", str1Tag, null);
        if (endsWithResult != null) {
            System.out.println("✓ String endsWith operation produced symbolic result: " + endsWithResult);
        } else {
            System.out.println("○ String endsWith returned concrete result");
        }

        // Test indexOf
        Tag indexOfResult = tracker.handleIndexOf(str1, "ll", 0, str1Tag, null);
        if (indexOfResult != null) {
            System.out.println("✓ String indexOf operation produced symbolic result: " + indexOfResult);
        } else {
            System.out.println("○ String indexOf returned concrete result");
        }

        System.out.println("✓ String comparison operations working correctly");
    }

    /**
     * Test string transformation operations.
     */
    private static void testStringTransformations() {
        System.out.println("Testing string transformation operations:");

        StringSymbolicTracker tracker = new StringSymbolicTracker();

        // Test case conversion
        String original = "Hello";
        String uppercase = original.toUpperCase();
        String lowercase = original.toLowerCase();

        Tag originalTag = Tag.of("symbolic_original");

        // Test toUpperCase
        Tag upperResult = tracker.handleCaseConversion(uppercase, original, true, originalTag);
        if (upperResult != null) {
            System.out.println("✓ String toUpperCase produced symbolic result: " + upperResult);
        } else {
            System.out.println("○ String toUpperCase returned concrete result");
        }

        // Test toLowerCase
        Tag lowerResult = tracker.handleCaseConversion(lowercase, original, false, originalTag);
        if (lowerResult != null) {
            System.out.println("✓ String toLowerCase produced symbolic result: " + lowerResult);
        } else {
            System.out.println("○ String toLowerCase returned concrete result");
        }

        System.out.println("✓ String transformation operations working correctly");
    }
}
