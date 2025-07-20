# Galette Model Transformation Concolic Execution Example

This document provides instructions for running the brake disc model transformation example that demonstrates using Galette for concolic execution in model-driven engineering scenarios.

## Overview

The example demonstrates:
- **External input as symbolic values**: User-provided thickness marked as symbolic
- **Path exploration**: Two execution paths based on thickness threshold (≤10 vs >10)  
- **Impact analysis**: Shows how symbolic input affects computed geometric properties
- **Conditional logic**: `additionalStiffness` attribute set based on thickness > 10mm
- **Constraint infrastructure**: Framework for collecting path constraints

## Prerequisites

1. **Navigate to the correct directory**:
   ```bash
   cd /home/anne/galette-concolic-model-transformation/knarr-runtime
   ```

2. **Set JAVA_HOME** (if not already set):
   ```bash
   export JAVA_HOME=$(readlink -f $(which java) | sed "s:/bin/java::")
   ```

3. **Verify setup**:
   ```bash
   echo $JAVA_HOME
   java -version
   ```

## Running the Example

### Method 1: Using the Automated Script (Recommended)

The easiest way to run the example is using the provided script:

```bash
./run-example.sh
```

This script automatically:
- Sets JAVA_HOME if needed
- Builds the project
- Generates the complete Maven classpath
- Runs the example with all dependencies

**Interactive Mode**: The script runs in interactive mode by default. You'll see a menu with options 1-5.

**Non-Interactive Mode**: You can also pass input directly:
```bash
echo "2" | ./run-example.sh    # Run path exploration demo
echo "3" | ./run-example.sh    # Run symbolic analysis demo  
echo "4" | ./run-example.sh    # Run detailed explanations
```

### Method 2: Manual Command Line (Advanced)

If you prefer to run manually:

```bash
# Build and generate classpath
mvn compile -q
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q

# Run interactively
java -cp "target/classes:target/test-classes:$(cat cp.txt)" edu.neu.ccs.prl.galette.examples.ModelTransformationExample

# Run specific demo non-interactively
echo "2" | java -cp "target/classes:target/test-classes:$(cat cp.txt)" edu.neu.ccs.prl.galette.examples.ModelTransformationExample
```

## Demo Options

When you run the example, you'll see this menu:

```
Available options:
1. Interactive transformation (enter thickness manually)
2. Path exploration demo (test both execution paths)
3. Symbolic analysis demo (detailed constraint tracking)
4. Show detailed example with explanations
5. Exit
```

### Option 1: Interactive Transformation
- Prompts you to enter a brake disc thickness value
- Shows the complete transformation process with your input
- Demonstrates symbolic value creation and geometric calculations
- Shows which execution path is taken based on your input

### Option 2: Path Exploration Demo
- Automatically runs the transformation with two different thickness values (8.0mm and 15.0mm)
- Demonstrates both execution paths:
  - **Path 1**: thickness ≤ 10 → `additionalStiffness = false`
  - **Path 2**: thickness > 10 → `additionalStiffness = true`
- Shows how different inputs lead to different model properties

### Option 3: Symbolic Analysis Demo
- Runs a detailed analysis with thickness = 12.5mm
- Shows symbolic execution statistics
- Demonstrates constraint collection infrastructure
- Provides technical details about the symbolic execution process

### Option 4: Detailed Example with Explanations
- Comprehensive walkthrough of the concolic execution concept
- Explains the problem context and solution approach
- Shows the key conditional logic that creates different execution paths
- Demonstrates both paths with concrete examples
- Explains the benefits for model-driven engineering

## Example Output

When running successfully, you'll see output like:

```
=== Starting Brake Disc Model Transformation ===
Source model: BrakeDiscSource{diameter=350.0mm, material='cast iron', coolingVanes=24}
User input thickness: 12.0 mm
Created symbolic tag for thickness: SUCCESS

=== Performing Geometric Calculations ===
Surface area: 96211.3 mm²
Volume: 1154535.3 mm³
Estimated weight: 8312.7 g

=== Evaluating Conditional Logic ===
Checking if thickness (12.0) > 10.0
→ Path taken: thickness > 10, setting additionalStiffness = true

=== Path Constraint Analysis ===
Symbolic execution statistics:
GaletteSymbolicator Statistics:
  Symbolic values: 1
  Green expressions: 1
  Path constraints: 0
  Server connected: false
```

## Key Features Demonstrated

✅ **Symbolic Value Creation**: "Created symbolic tag for thickness: SUCCESS"  
✅ **Both Execution Paths**: thickness ≤ 10 vs > 10  
✅ **Geometric Calculations**: Surface area, volume, weight computations  
✅ **Conditional Logic**: `additionalStiffness` based on thickness threshold  
✅ **Impact Analysis**: Shows how input affects multiple output properties  
✅ **Path Constraint Infrastructure**: Framework ready for advanced constraint collection  

## Understanding the Model Transformation

### Source Model (`BrakeDiscSource`)
- Simple brake disc with basic properties: diameter, material, cooling vanes
- Represents the initial model state before transformation

### Target Model (`BrakeDiscTarget`)  
- Enhanced model with computed geometric properties
- Includes derived attributes: thickness, surface area, volume, estimated weight
- Contains engineering property: `additionalStiffness` (based on conditional logic)

### Transformation Logic (`BrakeDiscTransformation`)
- Accepts external input (thickness) from user
- Marks thickness as symbolic using `GaletteSymbolicator.makeSymbolicDouble()`
- Performs geometric calculations with symbolic values
- **Critical conditional logic**: `if (thickness > 10) additionalStiffness = true`
- Collects path constraints for analysis

## Migration Goals Achieved

This example directly addresses the use case from the migration goals email:

- **External input tracking**: Thickness parameter marked as symbolic
- **Impact propagation**: Shows how input affects multiple output properties  
- **Conditional analysis**: Demonstrates different behavior paths based on input values
- **Consistency management**: Framework for understanding input-output relationships

The example provides a concrete foundation for using Galette in model-driven engineering scenarios, enabling impact analysis of external inputs on model transformations.

## Troubleshooting

**"JAVA_HOME not defined"**: Use the script `./run-example.sh` which sets it automatically, or set manually:
```bash
export JAVA_HOME=$(readlink -f $(which java) | sed "s:/bin/java::")
```

**"NoClassDefFoundError"**: Ensure you're using the script or the complete classpath with Maven dependencies.

**"Build failed"**: Make sure you're in the `knarr-runtime` directory and all dependencies are available.

## Testing

To run the automated test suite:
```bash
mvn test -Dtest=ModelTransformationTest
```

This validates that all transformation logic works correctly for both execution paths.