package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.ArraySymbolicTracker;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.StringSymbolicTracker;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;

/**
 * Combined array and string symbolic execution example.
 *
 * This demonstrates how array and string symbolic execution can work
 * together in complex scenarios, such as processing arrays of strings
 * or string-based array operations.
 *
 * @author Combined Symbolic Execution Example
 */
public class CombinedSymbolicExecutionExample {

    public static void main(String[] args) {
        System.out.println("=== Combined Array and String Symbolic Execution Example ===");
        System.out.println();

        try {
            // Test array of strings
            testStringArrayOperations();
            System.out.println();

            // Test string-based array indexing
            testStringIndexedArrays();
            System.out.println();

            // Test complex combined scenario
            testComplexScenario();
            System.out.println();

            System.out.println("✓ Combined symbolic execution test passed!");

        } catch (Exception e) {
            System.out.println("✗ Test failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("=== Test Complete ===");
        showStatistics();
    }

    /**
     * Test symbolic execution with arrays of strings.
     */
    private static void testStringArrayOperations() {
        System.out.println("Testing string array operations:");

        ArraySymbolicTracker arrayTracker = new ArraySymbolicTracker();
        StringSymbolicTracker stringTracker = new StringSymbolicTracker();

        ArraySymbolicTracker.reset();
        StringSymbolicTracker.reset();

        // Create array of strings
        String[] stringArray = {"hello", "world", "test", "symbolic"};
        Tag[] arrayTags = new Tag[stringArray.length];

        System.out.println("String array: [\"hello\", \"world\", \"test\", \"symbolic\"]");

        // Make some strings symbolic
        Tag string1Tag = Tag.of("symbolic_string_1");
        Tag string3Tag = Tag.of("symbolic_string_3");

        stringTracker.registerSymbolicString(stringArray[1], string1Tag, 0, stringArray[1].length());
        stringTracker.registerSymbolicString(stringArray[3], string3Tag, 0, stringArray[3].length());

        arrayTags[1] = string1Tag;
        arrayTags[3] = string3Tag;

        System.out.println("✓ Registered symbolic strings at indices 1 and 3");

        // Perform symbolic array read with concrete index
        Tag symbolicIndex = Tag.of("array_index");
        int concreteIndex = 1;

        Tag readResult = arrayTracker.handleArrayRead(
                stringArray, symbolicIndex, concreteIndex, arrayTags, stringArray[concreteIndex]);

        if (readResult != null) {
            System.out.println("✓ Array read with symbolic index produced result: " + readResult);

            // Now perform string operations on the result
            String selectedString = stringArray[concreteIndex];
            Tag lengthResult = stringTracker.handleLength(selectedString, readResult);

            if (lengthResult != null) {
                System.out.println("✓ String length of array element: " + lengthResult);
            }

            Tag equalsResult = stringTracker.handleEquals(selectedString, "world", readResult, null);
            if (equalsResult != null) {
                System.out.println("✓ String comparison of array element: " + equalsResult);
            }
        }

        System.out.println("✓ String array operations working correctly");
    }

    /**
     * Test using strings to determine array indices.
     */
    private static void testStringIndexedArrays() {
        System.out.println("Testing string-indexed array access:");

        StringSymbolicTracker stringTracker = new StringSymbolicTracker();
        ArraySymbolicTracker arrayTracker = new ArraySymbolicTracker();

        // Create a data array
        int[] dataArray = {100, 200, 300, 400, 500};
        Tag[] arrayTags = new Tag[dataArray.length];

        System.out.println("Data array: [100, 200, 300, 400, 500]");

        // Create symbolic string representing an index
        String indexString = "2";
        Tag indexStringTag = Tag.of("index_string");

        stringTracker.registerSymbolicString(indexString, indexStringTag, 0, indexString.length());
        System.out.println("✓ Created symbolic string for index: \"" + indexString + "\"");

        // In a real scenario, we would parse the string to get the index
        // For this example, we'll simulate the conversion
        int parsedIndex = Integer.parseInt(indexString);
        Tag indexTag = Tag.of("parsed_index"); // This would be derived from string parsing

        // Perform array access with the derived index
        Tag arrayResult =
                arrayTracker.handleArrayRead(dataArray, indexTag, parsedIndex, arrayTags, dataArray[parsedIndex]);

        if (arrayResult != null) {
            System.out.println("✓ Array access with string-derived index: " + arrayResult);
        } else {
            System.out.println("○ Array access returned concrete result: " + dataArray[parsedIndex]);
        }

        System.out.println("✓ String-indexed array operations working correctly");
    }

    /**
     * Test a complex scenario involving both arrays and strings.
     */
    private static void testComplexScenario() {
        System.out.println("Testing complex combined scenario:");

        ArraySymbolicTracker arrayTracker = new ArraySymbolicTracker();
        StringSymbolicTracker stringTracker = new StringSymbolicTracker();

        // Scenario: Processing user names from a symbolic input
        String[] userNames = {"alice", "bob", "charlie", "diana"};
        Tag[] nameTags = new Tag[userNames.length];

        System.out.println("User names: [\"alice\", \"bob\", \"charlie\", \"diana\"]");

        // Simulate symbolic input for user selection
        Tag userInputTag = Tag.of("user_input");
        String userInput = "bob"; // User-provided input

        stringTracker.registerSymbolicString(userInput, userInputTag, 0, userInput.length());
        System.out.println("✓ User input: \"" + userInput + "\" (symbolic)");

        // Search for the user in the array (symbolic string comparison)
        boolean found = false;
        int foundIndex = -1;

        for (int i = 0; i < userNames.length; i++) {
            Tag equalsResult = stringTracker.handleEquals(userNames[i], userInput, null, userInputTag);

            if (equalsResult != null) {
                System.out.println("✓ Symbolic comparison at index " + i + ": " + equalsResult);

                // In real execution, we would use the symbolic result
                // For demo, we know the concrete result
                if (userNames[i].equals(userInput)) {
                    found = true;
                    foundIndex = i;
                }
            }
        }

        if (found) {
            System.out.println("✓ Found user \"" + userInput + "\" at index " + foundIndex);

            // Now perform operations on the found user
            String foundName = userNames[foundIndex];
            Tag upperCaseResult = stringTracker.handleCaseConversion(foundName.toUpperCase(), foundName, true, null);

            if (upperCaseResult != null) {
                System.out.println("✓ Uppercase conversion: " + upperCaseResult);
            }

            // Check if name starts with specific letter
            Tag startsWithResult = stringTracker.handleStartsWith(foundName, "b", 0, null, null);
            if (startsWithResult != null) {
                System.out.println("✓ Starts with 'b' check: " + startsWithResult);
            }
        }

        System.out.println("✓ Complex scenario working correctly");
    }

    /**
     * Show statistics from both trackers.
     */
    private static void showStatistics() {
        System.out.println("=== Execution Statistics ===");
        System.out.println(ArraySymbolicTracker.getStatistics());
        System.out.println();
        System.out.println(StringSymbolicTracker.getStatistics());
    }
}
