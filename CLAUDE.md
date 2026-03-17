# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Galette is a dynamic taint tracking system for the JVM that uses bytecode instrumentation to track information flow. This repository extends Galette with **Knarr integration** for concolic (combined concrete and symbolic) execution, primarily targeting model transformation analysis.

**Key capability**: Automatic path constraint collection via bytecode instrumentation - tracks which conditions symbolic values pass through during execution.

## Build Commands

### Full Build
```bash
mvn -DskipTests install       # Build all modules
mvn clean install             # Build with tests
```

### Single Module Build
```bash
mvn -pl :knarr-runtime clean install
mvn -pl :galette-integration-tests verify
```

### Running the Example Application
```bash
cd knarr-runtime
./run-example.sh              # Main demo with 7 execution modes
```

### Running Tests
```bash
mvn -pl :galette-integration-tests verify     # Integration tests
mvn -Dit.test=AssignmentITCase verify         # Single integration test
mvn -Dmaven.failsafe.debug -Dit.test=AssignmentITCase verify  # Debug
```

## Critical Instrumentation Requirement

Path constraints are **only collected when running with Galette instrumentation**. Without it, symbolic values are created but constraints are not captured.

**Required components**:
1. Instrumented Java Runtime (created by Galette Maven plugin)
2. Galette Agent (`-javaagent` and `-Xbootclasspath/a` arguments)

**When modifying Galette agent classes** (GaletteTransformer, PathUtils, ComparisonInterceptorVisitor):
```bash
cd knarr-runtime
./rebuild-instrumented-java.sh  # Embeds updated classes
./run-example.sh                # Test changes
```

The instrumented Java caches agent classes during jlink. Changes to core instrumentation classes require rebuilding.

## Architecture

### Maven Modules

- **galette-agent**: Core Java agent for runtime bytecode transformation. Contains `Tainter` (tag manipulation API), `PathUtils` (constraint collection), and bytecode visitors like `ComparisonInterceptorVisitor`.

- **galette-instrument**: Creates instrumented Java installations via jlink. Embeds agent classes into JDK modules.

- **galette-maven-plugin**: Maven integration for creating instrumented Java as part of builds.

- **knarr-runtime**: Symbolic execution engine migrated from Phosphor to Galette APIs. Contains `GaletteSymbolicator`, `ArraySymbolicTracker`, `StringSymbolicTracker`, and example transformations.

- **galette-integration-tests**: Test suite demonstrating Galette usage patterns.

### Bytecode Instrumentation Flow

```
Class load → GaletteAgent (premain) → GaletteTransformer
  → ComparisonInterceptorVisitor (intercepts comparisons)
  → PathUtils.instrumentedDcmpl() etc (collects constraints at runtime)
```

### Tag-Based Constraint Filtering

Only constraints involving **tagged (symbolic) values** are collected. This eliminates noise from utility code (loop counters, HashMap internals) while preserving meaningful constraints when tagged values flow through complex operations like `Arrays.sort()`.

## Branch: comparison-interception-internal

This branch implements two key architectural features:

### Internal Comparison Interception

The `ComparisonInterceptorVisitor` intercepts JVM comparison bytecode instructions (DCMPL, DCMPG, FCMPL, FCMPG, LCMP) and replaces them with calls to instrumented methods in `PathUtils`:

```
Original bytecode:    DCMPL (compare two doubles)
Instrumented:         INVOKESTATIC PathUtils.instrumentedDcmpl(DD)I
```

This enables **zero-code-change constraint collection** - comparisons in user code automatically trigger constraint recording without modifying the source.

### External GreenServer with JSON Protocol

To avoid Galette instrumenting the Green/Z3 solver bytecode (which breaks Java serialization), constraint solving runs in a **separate non-instrumented JVM process**:

```
┌─────────────────────────┐         JSON over TCP          ┌─────────────────────────┐
│  Instrumented JVM       │ ───────────────────────────────▶ │  GreenServer (Java 21)  │
│  (Galette + User Code)  │         Port 9408              │  (Non-instrumented)     │
│                         │ ◀─────────────────────────────── │  Green + Z3             │
└─────────────────────────┘    {"sat":true,"model":{...}}  └─────────────────────────┘
```

**Protocol**:
- Client sends Green Expression as JSON via [ExpressionJsonConverter.java](knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/runtime/ExpressionJsonConverter.java)
- Server responds with `{"sat":true/false,"model":{"var1":value1,...}}`
- GreenServer source is in `green-solver/greenserver/` (separate repository)

**Running with GreenServer**:
```bash
cd knarr-runtime
./run-example-with-greenserver.sh              # Local
./run-example-with-greenserver.sh --codespaces # GitHub Codespaces
```

Configuration flags in the script:
- `USE_INSTRUMENTED_JAVA=true` - Enable Galette bytecode instrumentation
- `USE_GREEN_SOLVER=true` - Enable constraint solving
- `USE_EXTERNAL_GREEN_SERVER=true` - Use external GreenServer process

## Key Classes

### In galette-agent
- [Tainter.java](galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/Tainter.java) - Main API for tag manipulation
- [PathUtils.java](galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/PathUtils.java) - Runtime path constraint collection
- [ComparisonInterceptorVisitor.java](galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/transform/ComparisonInterceptorVisitor.java) - Bytecode visitor for comparison interception

### In knarr-runtime
- [GaletteSymbolicator.java](knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/runtime/GaletteSymbolicator.java) - Symbolic execution engine with Green solver integration
- [ExpressionJsonConverter.java](knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/runtime/ExpressionJsonConverter.java) - JSON serialization for GreenServer protocol
- [GalettePathConstraintBridge.java](knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/runtime/GalettePathConstraintBridge.java) - Converts Galette constraints to Green expressions
- [SymbolicExecutionWrapper.java](knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/examples/transformation/SymbolicExecutionWrapper.java) - Non-invasive wrapper pattern for adding symbolic execution
- [ModelTransformationExample.java](knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.java) - Main demo application

## Usage Pattern

```java
// Make value symbolic
double thickness = 12.5;
Tag tag = GaletteSymbolicator.makeSymbolicDouble("thickness", thickness);

// Use in transformation (constraints collected automatically)
if (thickness > 10.0) {  // Creates constraint: thickness > 10.0
    model.setStiffness(true);
}

// Retrieve collected constraints
PathConditionWrapper pc = PathUtils.getCurPC();
```

## Requirements

- **JDK 17** for building (can run on Java 8-21 at runtime)
- **JDK 21** for GreenServer (Z3 model extraction requires newer class version)
- **Maven 3.6.0+**
- **Green solver** repository at `../green-solver/` (contains GreenServer in `greenserver/`)
- **Z3-turnkey** from Maven for native Z3 support
