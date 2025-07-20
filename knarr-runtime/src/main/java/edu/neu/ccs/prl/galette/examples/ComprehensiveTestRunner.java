package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.*;
import edu.neu.ccs.prl.galette.concolic.knarr.testing.SymbolicExecutionTestFramework;

/**
 * Comprehensive test runner for all symbolic execution components.
 *
 * This example demonstrates the testing infrastructure and validates
 * the correctness of array symbolic execution, string symbolic execution,
 * coverage tracking, and their integration.
 *
 * @author Comprehensive Test Runner
 */
public class ComprehensiveTestRunner {

    public static void main(String[] args) {
        System.out.println("=== Galette Symbolic Execution Comprehensive Test Runner ===");
        System.out.println();

        try {
            // Initialize components
            initializeComponents();

            // Run comprehensive test suite
            SymbolicExecutionTestFramework.TestResults results = SymbolicExecutionTestFramework.runComprehensiveTests();

            // Display detailed results
            displayDetailedResults(results);

            // Run performance benchmarks
            runPerformanceBenchmarks();

            // Display final summary
            displayFinalSummary(results);

        } catch (Exception e) {
            System.out.println("✗ Test execution failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("=== Test Runner Complete ===");
    }

    /**
     * Initialize symbolic execution components.
     */
    private static void initializeComponents() {
        System.out.println("Initializing symbolic execution components...");

        // Reset all trackers
        ArraySymbolicTracker.reset();
        StringSymbolicTracker.reset();
        CoverageTracker.instance.reset();
        PathUtils.resetPC();

        // Configure coverage
        CoverageTracker.setCoverageConfig(true, java.util.Optional.empty());

        System.out.println("✓ Components initialized");
        System.out.println();
    }

    /**
     * Display detailed test results.
     */
    private static void displayDetailedResults(SymbolicExecutionTestFramework.TestResults results) {
        System.out.println("=== Detailed Test Results ===");

        System.out.printf("Total Tests: %d\n", results.totalTests);
        System.out.printf("Passed: %d\n", results.passedTests);
        System.out.printf("Failed: %d\n", results.failedTests);
        System.out.printf("Pass Rate: %.1f%%\n", results.getPassRate() * 100);
        System.out.println();

        if (!results.getFailures().isEmpty()) {
            System.out.println("Failed Tests:");
            for (String failure : results.getFailures()) {
                System.out.println("  ✗ " + failure);
            }
            System.out.println();
        }

        System.out.println("Test Execution Times:");
        results.getTimings().entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> System.out.printf("  %s: %dms\n", entry.getKey(), entry.getValue()));
        System.out.println();
    }

    /**
     * Run performance benchmarks.
     */
    private static void runPerformanceBenchmarks() {
        System.out.println("=== Performance Benchmarks ===");

        // Array operations benchmark
        benchmarkArrayOperations();

        // String operations benchmark
        benchmarkStringOperations();

        // Coverage tracking benchmark
        benchmarkCoverageTracking();

        System.out.println();
    }

    /**
     * Benchmark array symbolic execution performance.
     */
    private static void benchmarkArrayOperations() {
        System.out.println("Array Operations Benchmark:");

        ArraySymbolicTracker tracker = new ArraySymbolicTracker();
        ArraySymbolicTracker.reset();

        int[] testArray = new int[1000];
        for (int i = 0; i < testArray.length; i++) {
            testArray[i] = i * 2;
        }

        long startTime = System.currentTimeMillis();
        int operations = 10000;

        for (int i = 0; i < operations; i++) {
            edu.neu.ccs.prl.galette.internal.runtime.Tag indexTag =
                    edu.neu.ccs.prl.galette.internal.runtime.Tag.of("bench_index_" + i);
            edu.neu.ccs.prl.galette.internal.runtime.Tag[] arrayTags =
                    new edu.neu.ccs.prl.galette.internal.runtime.Tag[testArray.length];

            int index = i % testArray.length;
            tracker.handleArrayRead(testArray, indexTag, index, arrayTags, testArray[index]);
        }

        long duration = System.currentTimeMillis() - startTime;
        double opsPerSecond = (double) operations / (duration / 1000.0);

        System.out.printf("  Array reads: %d ops in %dms (%.1f ops/sec)\n", operations, duration, opsPerSecond);
        System.out.println("  " + ArraySymbolicTracker.getStatistics());
    }

    /**
     * Benchmark string symbolic execution performance.
     */
    private static void benchmarkStringOperations() {
        System.out.println("String Operations Benchmark:");

        StringSymbolicTracker tracker = new StringSymbolicTracker();
        StringSymbolicTracker.reset();

        String[] testStrings = {"hello", "world", "test", "symbolic", "execution"};

        long startTime = System.currentTimeMillis();
        int operations = 5000;

        for (int i = 0; i < operations; i++) {
            String str = testStrings[i % testStrings.length];
            edu.neu.ccs.prl.galette.internal.runtime.Tag strTag =
                    edu.neu.ccs.prl.galette.internal.runtime.Tag.of("bench_string_" + i);

            tracker.handleLength(str, strTag);
            tracker.handleEquals(str, "test", strTag, null);
            tracker.handleStartsWith(str, "te", 0, strTag, null);
        }

        long duration = System.currentTimeMillis() - startTime;
        double opsPerSecond = (double) (operations * 3) / (duration / 1000.0); // 3 ops per iteration

        System.out.printf(
                "  String operations: %d ops in %dms (%.1f ops/sec)\n", operations * 3, duration, opsPerSecond);
        System.out.println("  " + StringSymbolicTracker.getStatistics());
    }

    /**
     * Benchmark coverage tracking performance.
     */
    private static void benchmarkCoverageTracking() {
        System.out.println("Coverage Tracking Benchmark:");

        CoverageTracker tracker = new CoverageTracker();
        tracker.reset();

        long startTime = System.currentTimeMillis();
        int operations = 100000;

        for (int i = 0; i < operations; i++) {
            tracker.recordCodeCoverage(i);
            tracker.recordPathCoverage(i * 2);

            if (i % 100 == 0) {
                tracker.recordMethodEntry("BenchmarkClass", "method" + (i / 100), "()V");
            }

            if (i % 50 == 0) {
                tracker.recordBranchCoverage(i / 50, i % 2 == 0);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        double opsPerSecond = (double) operations / (duration / 1000.0);

        System.out.printf("  Coverage tracking: %d ops in %dms (%.1f ops/sec)\n", operations, duration, opsPerSecond);
        System.out.println("  " + tracker.getCoverageStatistics());
    }

    /**
     * Display final summary.
     */
    private static void displayFinalSummary(SymbolicExecutionTestFramework.TestResults results) {
        System.out.println("=== Final Summary ===");

        boolean allTestsPassed = results.failedTests == 0;
        String status = allTestsPassed ? "✓ ALL TESTS PASSED" : "✗ SOME TESTS FAILED";

        System.out.println(status);
        System.out.println(results.getSummary());

        // Display component statistics
        System.out.println();
        System.out.println("Component Statistics:");
        System.out.println(ArraySymbolicTracker.getStatistics());
        System.out.println();
        System.out.println(StringSymbolicTracker.getStatistics());
        System.out.println();
        System.out.println(CoverageTracker.instance.getCoverageStatistics());

        // Path condition information
        PathConditionWrapper pc = PathUtils.getCurPC();
        System.out.println();
        System.out.printf("Path Condition: %d constraints collected\n", pc.size());

        if (allTestsPassed) {
            System.out.println();
            System.out.println("🎉 Galette Symbolic Execution Migration: SUCCESS");
            System.out.println("All components are working correctly with Galette APIs!");
        } else {
            System.out.println();
            System.out.println("⚠️  Some tests failed - please review the failures above");
        }
    }
}
