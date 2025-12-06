# Branch Overview

This document explains the differences between the three main branches in the galette-concolic-model-transformation repository and their purposes.

## Branch Descriptions

### `main` Branch
**Status**: Active, Stable  
**Purpose**: Primary working branch with explicit comparison interception via `SymbolicComparison.greaterThan()` API calls

**Key Characteristics**:
- Uses **explicit method calls** for symbolic comparisons instead of bytecode interception
- Application code manually calls `SymbolicComparison.greaterThan()`, `SymbolicComparison.lessThan()`, etc.
- PathUtils with null-safe guards for ThreadLocal operations
- Model transformation code (`BrakeDiscTransformation`) is in the same module
- Uses instrumented Java 17 runtime via Galette agent
- This is the **recommended branch** for current development

**Example Usage**:
```java
SymbolicComparison.greaterThan(thickness, 10.0)
```

**Status**: used to work, now test again

---

### `comparison-interception` Branch
**Status**: Experimental  
**Purpose**: Attempted to move model transformation to a separate Maven module (closer to real-world Vitruv setup)

**Key Characteristics**:
- Tried to replicate real application structure where model transformation code is in a separate module
- Comparison interception still uses explicit `SymbolicComparison` API calls
- Goal was to test if the framework works with separated modules (as would be the case in real Vitruv projects)
- **Issue**: Did not complete successful implementation of separated module structure

**What Was Attempted**:
- Move `BrakeDiscTransformation` and related model code to `galette-concolic-examples` module
- Keep framework code in main `galette-concolic-model-transformation` module
- Test that agent instrumentation works across module boundaries

**Status**: Incomplete/Not Fully Tested

---

### `comparison-interception-internal` Branch
**Status**: Experimental, Issues with Over-Interception  
**Purpose**: Full automatic bytecode-level comparison interception without explicit API calls

**Key Characteristics**:
- Attempted to automatically intercept **all** bytecode comparison instructions (DCMPL, DCMPG, LCMP, ICMP, etc.)
- Uses `ComparisonMethodVisitor` to replace comparison bytecode with calls to `PathUtils.instrumentedDcmpl()`, etc.
- PathUtils stores all collected constraints in ThreadLocal storage
- **Major Issue**: Over-intercepts comparisons - collects constraints from comparisons that shouldn't be tracked
- Tag-based filtering was incomplete, so comparison interception was too broad

**Problems Encountered**:
- ThreadLocal NullPointerException when `PATH_CONDITIONS.get()` returns null (fixed with null-safe guards)
- Too many comparisons collected because tag-based filtering doesn't properly distinguish symbolic from concrete values
- Would need proper tag propagation through all operations to filter correctly

**What Was Fixed**:
- Added null-safe checks in `PathUtils.instrumentedDcmpl()`, `instrumentedDcmpg()`, `instrumentedLcmp()`, `instrumentedFcmpl()`, `instrumentedFcmpg()`
- Code compiles and runs without crashing
- But still over-intercepts due to lack of proper tag filtering

**Status**: Technically Working but Incomplete (too many constraints collected)

---

## Comparison Table

| Aspect | main | comparison-interception | comparison-interception-internal |
|--------|------|------------------------|----------------------------------|
| Comparison Method | Explicit `SymbolicComparison` API | Explicit API | Automatic bytecode interception |
| Module Structure | Monolithic | Separated modules | Monolithic |
| Tag Filtering | Manual | Manual | Attempted (incomplete) |
| Over-Interception | ❌ No | ❌ No | ⚠️ Yes |
| Working Status | ✅ Yes | ❓ Partial | ⚠️ Compiles but over-intercepts |
| Production Ready | ✅ Yes | ❌ No | ❌ No |

---

## Next Steps & Recommendations

### For Continuing on `main`:
1. **Current Task**: Fix the NullPointerException by rebuilding the agent JAR or using the null-safe guards from `comparison-interception-internal`
2. Stay on `main` branch which has proven explicit comparison tracking
3. If separated module structure is needed, consider backporting only that change from `comparison-interception`

### For Future Work:
1. **Bytecode Interception (comparison-interception-internal)**: Would require:
   - Proper tag propagation mechanism
   - Smart filtering to distinguish symbolic vs. concrete comparisons
   - More sophisticated constraint collection

2. **Separated Modules (comparison-interception)**: Would require:
   - Verify agent instrumentation across module boundaries
   - Test with Vitruv-like separation
   - Combine with explicit comparison API from `main`

### Branch Maintenance:
- Keep `main` as the stable, working version
- Archive `comparison-interception` and `comparison-interception-internal` for reference
- Document lessons learned for future automatic instrumentation attempts
