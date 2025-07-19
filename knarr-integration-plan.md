# Knarr Runtime Integration Plan

**Date:** July 19, 2025  
**Project:** Galette Concolic Model Transformation  
**Goal:** Integrate Knarr runtime components into Galette for concolic execution support

## Executive Summary

Based on correspondence with Jon Bell (author of both Knarr and Galette), we need to migrate Knarr's concolic execution capabilities from **Phosphor** (the predecessor) to **Galette** (the modern replacement). This integration will enable concolic execution for model transformations in recent Java versions.

### Key Insight from Jon Bell's Email
> "Unfortunately, this will not be compatible with Java > 8 either (the experience building and trying to apply this was part of the motivation to design and build Galette to replace Phosphor). However, some parts of it might provide useful inspiration for ways to proceed with collecting path constraints using Galette."

**Translation:** We need to port Knarr's Phosphor-based runtime to use Galette APIs instead.

## Current Status

### ✅ Completed Work
1. **Knarr Repository Analysis**
   - Cloned https://github.com/gmu-swe/knarr.git to `~/knarr`
   - Identified runtime components in `/knarr/src/main/java/edu/gmu/swe/knarr/runtime/`
   - Found 18 Java runtime files for concolic execution

2. **Git History Preservation**
   - Created filtered repository using `git-filter-repo` at `~/knarr-runtime-filtered`
   - Used `git subtree add` to integrate with preserved history into `galette-concolic-model-transformation/knarr-runtime/`
   - Maintained full commit history from original Knarr development

3. **Maven Build Verification**
   - Confirmed basic Maven build works: `mvn clean compile` exits with status 0
   - Knarr-runtime files are present but not yet integrated into build system

### 📋 Current File Structure
```
galette-concolic-model-transformation/
├── knarr-runtime/knarr/src/main/java/edu/gmu/swe/knarr/runtime/
│   ├── AFLCoverage.java
│   ├── Coverage.java
│   ├── PathUtils.java           # Main constraint utilities
│   ├── TaintListener.java       # Core concolic listener
│   ├── Symbolicator.java        # Symbolic execution engine
│   └── ... (15 more files)
└── green-solver/                # Our fork of Green SMT solver
```

## Core Challenge: Phosphor → Galette Migration

### Dependency Analysis
The Knarr runtime currently depends on **Phosphor APIs** that need to be replaced with **Galette equivalents**:

| **Phosphor Class** | **Usage** | **Galette Equivalent** |
|-------------------|-----------|------------------------|
| `edu.columbia.cs.psl.phosphor.runtime.Taint` | Core taint representation | `edu.neu.ccs.prl.galette.internal.runtime.Tag` |
| `edu.columbia.cs.psl.phosphor.runtime.DerivedTaintListener` | Concolic event callbacks | **Need to create interface** |
| `edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack` | Control flow tracking | **Need to implement** |
| `edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags` | Array taint storage | `edu.neu.ccs.prl.galette.internal.runtime.ArrayTagStore` |
| `edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag` | Primitive + taint pairs | **Direct Tag handling** |
| `edu.columbia.cs.psl.phosphor.org.objectweb.asm.*` | ASM bytecode manipulation | `org.objectweb.asm.*` (standard) |

### Green Solver Integration
- **Current:** `za.ac.sun.cs.green.expr.*` (unchanged)
- **Location:** `~/green-solver`
- **Status:** Our own fork already available, no changes needed to Green solver itself
- **Integration:** Need to adapt constraint generation to use Galette Tags instead of Phosphor Taints

## Implementation Plan

### Phase 1: Package Structure Migration
```
knarr-runtime/src/main/java/
└── edu/neu/ccs/prl/galette/concolic/knarr/
    ├── runtime/          # Core runtime classes (migrated from Phosphor)
    ├── listener/         # Concolic execution listeners
    ├── constraint/       # Path constraint generation
    └── green/           # Green solver integration bridge
```

### Phase 2: Core API Migration
1. **Update Package Declarations**
   ```java
   // FROM:
   package edu.gmu.swe.knarr.runtime;
   
   // TO:
   package edu.neu.ccs.prl.galette.concolic.knarr.runtime;
   ```

2. **Replace Phosphor Imports**
   ```java
   // FROM (Phosphor):
   import edu.columbia.cs.psl.phosphor.runtime.Taint;
   import edu.columbia.cs.psl.phosphor.runtime.DerivedTaintListener;
   
   // TO (Galette):
   import edu.neu.ccs.prl.galette.internal.runtime.Tag;
   import edu.neu.ccs.prl.galette.concolic.knarr.listener.ConcolicTaintListener;
   ```

3. **Method Signature Updates**
   ```java
   // FROM:
   public void handleBranch(Taint condition, boolean taken) { ... }
   
   // TO:
   public void handleBranch(Tag condition, boolean taken) { ... }
   ```

### Phase 3: Concolic-Specific Extensions
1. **Create Concolic Listener Interface**
   ```java
   package edu.neu.ccs.prl.galette.concolic.knarr.listener;
   
   public interface ConcolicTaintListener {
       void onBranch(Tag condition, boolean taken);
       void onArithmetic(Tag operand1, Tag operand2, Tag result);
       void onComparison(Tag left, Tag right, int opcode, boolean result);
       void onPathConstraint(Object constraint);
   }
   ```

2. **Control Flow Tracking**
   ```java
   package edu.neu.ccs.prl.galette.concolic.knarr.runtime;
   
   public class ConcolicControlStack {
       // Galette-compatible replacement for Phosphor's ControlTaintTagStack
       private static final ThreadLocal<Stack<Tag>> controlStack = 
           ThreadLocal.withInitial(Stack::new);
   }
   ```

### Phase 4: Maven Integration
1. **Add Module to Main POM**
   ```xml
   <modules>
       <module>galette-agent</module>
       <module>galette-instrument</module>
       <!-- ... existing modules ... -->
       <module>knarr-runtime</module>
   </modules>
   ```

2. **Create knarr-runtime/pom.xml**
   ```xml
   <dependencies>
       <dependency>
           <groupId>edu.neu.ccs.prl.galette</groupId>
           <artifactId>galette-agent</artifactId>
           <version>${project.version}</version>
       </dependency>
       <!-- Green solver dependency (local) -->
       <dependency>
           <groupId>za.ac.sun.cs.green</groupId>
           <artifactId>green</artifactId>
           <version>0.1</version>
           <scope>system</scope>
           <systemPath>${project.basedir}/../green-solver/green/target/green.jar</systemPath>
       </dependency>
   </dependencies>
   ```

### Phase 5: Green Solver Bridge
1. **Constraint Generation Adapter**
   ```java
   package edu.neu.ccs.prl.galette.concolic.knarr.green;
   
   public class GaletteGreenBridge {
       public static za.ac.sun.cs.green.expr.Expression 
           tagToGreenExpression(Tag tag, Object value) {
           // Convert Galette Tag to Green solver expression
       }
   }
   ```

2. **Path Constraint Collection**
   - Modify `PathUtils.java` to use Galette Tags
   - Update constraint serialization to work with Green solver
   - Ensure Z3 solver integration remains functional

## Technical Migration Details

### Critical Files Requiring Update
1. **`PathUtils.java`** (1760+ lines)
   - Core constraint manipulation utilities
   - Heavy Phosphor Taint usage → needs Tag conversion
   - Green solver integration points

2. **`TaintListener.java`** (485+ lines)
   - Main concolic execution listener
   - All Phosphor struct imports need replacement
   - Event handling methods need Tag parameters

3. **`Symbolicator.java`** (747+ lines)
   - Symbolic execution engine
   - Constraint generation and solving
   - Path condition management

### Green Solver Considerations
- **No changes needed** to Green solver itself (`green-solver/` directory)
- **Update constraint generation** to produce Green AST from Galette Tags
- **Maintain Z3 backend** compatibility
- **Test solver performance** with new Tag-based constraints

## Testing Strategy

### Phase 1: Unit Tests
```java
@Test
public void testTagToTaintMigration() {
    Tag galetteTag = Tag.of("test-label");
    // Verify equivalent behavior to old Phosphor Taint
}
```

### Phase 2: Integration Tests
- Test concolic execution on simple arithmetic operations
- Verify path constraint generation
- Validate Green solver integration

### Phase 3: Model Transformation Tests
- Test with Vitruv consistency preservation rules
- Verify external input parameter marking
- Validate constraint solving for model decisions

## Risk Assessment

### High Risk
- **API incompatibilities** between Phosphor and Galette
- **Performance degradation** from Tag vs Taint differences
- **Green solver integration** complexity

### Medium Risk
- **Build system complexity** with local Green solver dependency
- **Testing coverage** of migrated functionality

### Low Risk
- **Java version compatibility** (Galette supports Java 8-21)
- **Git history preservation** (already completed)

## Dependencies & Prerequisites

### External Dependencies
- ✅ **Galette** (already available in current workspace)
- ✅ **Green Solver** (available at `green-solver/`)
- ✅ **ASM 9.6** (managed by Galette)
- ✅ **Maven 3.8+** and **JDK 17**

### Build Requirements
1. **Galette agent JAR** must be built first
2. **Green solver JAR** must be available
3. **knarr-runtime module** integration into Maven build

## Success Criteria

### Milestone 1: Basic Migration
- [ ] All Knarr runtime files compile with Galette APIs
- [ ] Basic Tag operations work correctly
- [ ] Maven build includes knarr-runtime module

### Milestone 2: Functional Integration  
- [ ] Concolic execution works on simple programs
- [ ] Path constraints are generated correctly
- [ ] Green solver integration functional

### Milestone 3: Model Transformation Ready
- [ ] Integration with Vitruv model transformations
- [ ] External input parameter marking works
- [ ] Constraint solving for model decisions functional

## Next Steps

1. **Immediate:** Start Phase 1 package structure migration
2. **Short-term:** Implement core API replacements (Phase 2)
3. **Medium-term:** Create concolic-specific extensions (Phase 3)
4. **Long-term:** Full testing and model transformation integration

## Resources

### Documentation
- **Galette API:** `galette-agent/src/main/java/edu/neu/ccs/prl/galette/internal/runtime/`
- **Green Solver:** `green-solver/green/src/main/java/za/ac/sun/cs/green/`
- **Original Knarr:** https://github.com/gmu-swe/knarr
- **CONFETTI Paper:** https://jonbell.net/publications/confetti

### Contact
- **Jon Bell:** Available for consultation via https://fantastical.app/jon-LFd0/ff30

---

**Note:** This plan provides a roadmap for migrating Knarr's concolic execution capabilities from Phosphor to Galette, enabling modern Java support for concolic model transformations.
