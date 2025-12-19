# Variable Name Preservation Plan for Galette Concolic Execution

## Problem Statement

The concolic execution system generates path constraints with concrete values instead of variable names:
- **Current:** `((12.0<80.0)&&(0<1))`
- **Desired:** `((thickness_1<80.0)&&(0<1))`

This prevents the Green solver from identifying which variables to mutate for path exploration.

## Analysis: Phosphor/Knarr vs Galette Architecture

### How Knarr/Phosphor Preserves Variable Names

1. **Taint Objects Carry Expressions:**
   ```java
   // Symbolicator.java line 556
   ret.taint = new ExpressionTaint(new BVVariable(label, 32));
   ```
   The variable name is stored in the Expression within the taint.

2. **Comparisons Receive Both Values and Taints:**
   ```java
   // PathUtils.java line 926
   public static TaintedIntWithObjTag DCMPL(
       Taint<Expression> rVal, double v1,
       Taint<Expression> lVal, double v2,
       TaintedIntWithObjTag ret)
   ```

3. **Constraints Built with Expressions:**
   ```java
   Expression l = lVal.getSingleLabel();  // Gets BVVariable("thickness")
   Expression r = new RealConstant(80.0);
   getCurPC()._addDet(Operator.GT, l, r);  // Adds: thickness > 80.0
   ```

### Why Galette Can't Access Variable Names

1. **Different Taint Architecture:**
   - Galette uses `Tag` objects that store labels (strings), not Expression objects
   - Tags are stored in shadow memory, separate from values

2. **No TagFrame in Agent Code:**
   - `PathUtils.instrumentedDcmpl()` is part of the Galette agent (not instrumented)
   - `Tainter.getTag(value)` without TagFrame returns null (it's a placeholder)
   - Only `Tainter.getTag(value, TagFrame)` works, but TagFrame isn't available in agent code

3. **Comparison Methods Don't Receive Tags:**
   ```java
   // Current Galette approach
   public static int instrumentedDcmpl(double value1, double value2)
   // vs Phosphor approach which receives taints
   ```

4. **Fundamental Limitation:**
   - Agent code (PathUtils) runs at instrumentation level
   - Can't access runtime tags without TagFrame
   - Architectural difference from Phosphor's approach

## Proposed Solution: Value-to-Name Mapping

Since we control symbolic value creation, we can track the value-to-name mappings at the application level.

### Implementation Plan

#### Step 1: Track Mappings in ModelTransformationExample

Add tracking for symbolic values:
```java
private static final Map<Double, String> symbolicValueNames = new HashMap<>();

private static ConcolicResult executeConcolic(BrakeDiscSource source, double thickness, String label) {
    symbolicValueNames.clear();
    Tag symbolicTag = GaletteSymbolicator.makeSymbolicDouble(label, thickness);
    symbolicValueNames.put(thickness, label);  // Track the mapping
    // ...
}

public static String getSymbolicVariableName(double value) {
    return symbolicValueNames.get(value);
}
```

#### Step 2: Update GalettePathConstraintBridge

Modify `convertValue()` to look up variable names:
```java
private static Expression convertValue(Object value, String variablePrefix) {
    if (value instanceof Double || value instanceof Float) {
        double doubleVal = value instanceof Float ? (Float) value : (Double) value;

        String variableName = null;
        try {
            Class<?> exampleClass = Class.forName("edu.neu.ccs.prl.galette.examples.ModelTransformationExample");
            Method getNameMethod = exampleClass.getMethod("getSymbolicVariableName", double.class);
            variableName = (String) getNameMethod.invoke(null, doubleVal);
        } catch (Exception e) {
            // Continue without variable name
        }

        if (variableName != null) {
            return new RealVariable(variableName, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        } else {
            return new RealConstant(doubleVal);
        }
    }
    // ... handle other types similarly ...
}
```

### Why This Solution Works

1. **Sidesteps architectural limitation** - Doesn't need TagFrame access
2. **Simple and clean** - Just a map tracking value-to-name associations
3. **Scoped per execution** - Map cleared for each test run, avoiding collisions
4. **No agent modifications** - Works with existing Galette architecture
5. **Easy to test** - Can verify immediately with existing examples

### Alternative Solutions Considered

1. **Pass TagFrame through call chain** - Too complex, requires accessing method parameters via reflection
2. **Modify agent to store Expressions in Constraints** - Requires changing Galette agent code
3. **Use bytecode manipulation to access tags** - Complex and fragile

### Testing Strategy

1. Run `ModelTransformationExample` with option 2 (concolic execution)
2. Check that constraints show variable names (e.g., `thickness_1`) instead of values (e.g., `12.0`)
3. Verify Green solver can identify variables for mutation
4. Confirm path exploration discovers alternative branches

### Expected Results

**Before:**
```
Path constraints: ((12.0<80.0)&&(0<1))
Green solver: Cannot identify variables to mutate
Exploration: Fails to find alternative paths
```

**After:**
```
Path constraints: ((thickness_1<80.0)&&(0<1))
Green solver: Identifies thickness_1 as mutable variable
Exploration: Generates thickness_1 != 12.0 to find new paths
```

## Implementation Status

- [x] Analyzed Phosphor/Knarr variable handling
- [x] Identified Galette architectural limitations
- [x] Designed value-to-name mapping solution
- [ ] Implement value tracking in ModelTransformationExample
- [ ] Update GalettePathConstraintBridge
- [ ] Test with analyzeBoundaryConditions
- [ ] Integrate into main exploration loop

## Files to Modify

1. `/home/anne/concolic-execution/galette-concolic-model-transformation/knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.java`
2. `/home/anne/concolic-execution/galette-concolic-model-transformation/knarr-runtime/src/main/java/edu/neu/ccs/prl/galette/concolic/knarr/runtime/GalettePathConstraintBridge.java`

## Notes

- The collision risk (two variables with same value) exists but is manageable for testing scenarios
- Future enhancement could use composite keys (value + context) if needed
- This approach could be generalized to support multiple examples beyond ModelTransformationExample