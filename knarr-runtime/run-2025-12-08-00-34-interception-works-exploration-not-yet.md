TODO: need to find upper bound / know when to stop. 

Avoid that output: → Generating value 82.0 for testing > 81.0


anne@LAPTOP-9T3FG2C8:~/concolic-execution/galette-concolic-model-transformation/knarr-runtime$ ./run-example.sh 
🚀 Enhanced Galette Knarr Runtime Example
==========================================
☕ Java Configuration:
   JAVA_HOME: /usr/lib/jvm/java-17-openjdk-amd64
   Java version: openjdk version "17.0.17" 2025-10-21

📦 Green solver rebuild needed
📦 Galette agent rebuild needed
📦 Java classes rebuild needed
📦 Building required components...
🔨 Building and installing Green solver...
✅ Green solver installed
🔨 Installing all Galette modules into local Maven repo...
   Building: Galette project modules
🔧 PathUtils static initializer: isEnabled() = true (HARDCODED)
🔧 System property galette.concolic.interception.enabled = null (IGNORED)
/usr/lib/jvm/java-17-openjdk-amd64/bin/jlink -J-javaagent:/home/anne/concolic-execution/galette-concolic-model-transformation/galette-instrument/target/galette-instrument-1.0.0-SNAPSHOT.jar -J--class-path=/home/anne/concolic-execution/galette-concolic-model-transformation/galette-instrument/target/galette-instrument-1.0.0-SNAPSHOT.jar -J--add-reads=edu.neu.ccs.prl.galette.instrument=ALL-UNNAMED -J--module-path=/home/anne/concolic-execution/galette-concolic-model-transformation/galette-instrument/target/galette-instrument-1.0.0-SNAPSHOT.jar -J--add-modules=edu.neu.ccs.prl.galette.instrument --pack=x:type=edu.neu.ccs.prl.galette.instrument.GaletteInstrumentation:options=/tmp/instrument-3946349303249616552.properties --instrument=x:type=edu.neu.ccs.prl.galette.instrument.GaletteInstrumentation:options=/tmp/instrument-5418267415291573787.properties --output=/home/anne/concolic-execution/galette-concolic-model-transformation/knarr-runtime/target/galette/java --add-modules ALL-MODULE-PATH,jdk.unsupported,jdk.jdwp.agent,java.base,java.instrument
WARNING: Using incubator modules: jdk.incubator.vector, jdk.incubator.foreign
✅ Galette modules installed
🔨 Building and installing galette-agent...
🔧 PathUtils static initializer: isEnabled() = true (HARDCODED)
🔧 System property galette.concolic.interception.enabled = null (IGNORED)
✅ Galette agent built and installed successfully
🔨 Compiling Java classes...
✅ Java classes compiled successfully
⚡ Using existing instrumented Java
✅ All required builds completed successfully

🔧 Configuration:
   Instrumented Java: target/galette/java/bin/java
   Galette Agent: ../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar
📋 Generating classpath...
📚 Using classpath with 17 entries

🚀 Running ModelTransformationExample with Galette instrumentation...
   Expected: Path constraints will be collected (not 'no constraints')

🔍 Debug Information:
   Command: target/galette/java/bin/java
   Agent arguments: -Xbootclasspath/a:../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar -javaagent:../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar
   Galette cache directory: target/galette/cache
🚀 GaletteAgent.premain() called at Mon Dec 08 00:29:39 CET 2025
📍 Agent args: null
💾 Cache path: target/galette/cache
✅ Adding TransformerWrapper to instrumentation
✅ GaletteAgent initialization complete
GALETTE CONCOLIC EXECUTION DEMO: MODEL TRANSFORMATION

This example demonstrates how Galette can track symbolic values
through model transformations to analyze the impact of external inputs.

Initializing Galette symbolic execution environment...
🔍 ComparisonInterceptorVisitor created for class transformation
🎯 Intercepting jump instruction: GE
🎯 Intercepting comparison instruction: 152
🎯 Intercepting comparison instruction: 152
🎯 Intercepting comparison instruction: 152
🎯 Intercepting comparison instruction: 152
🎯 Intercepting comparison instruction: 152
🎯 Intercepting jump instruction: NE
🎯 Intercepting jump instruction: NE
🎯 Intercepting jump instruction: GE
🔧 PathUtils static initializer: isEnabled() = true (HARDCODED)
🔧 System property galette.concolic.interception.enabled = true (IGNORED)
Created sample brake disc: BrakeDiscSource{diameter=350.0mm, material='cast iron', coolingVanes=24}

Available options:
1. Standard transformation (concrete execution, no path exploration)
2. Concolic execution with automated path constraint collection and exploration
3. Exit

Select an option (1-3): 2

=== TRUE CONCOLIC EXECUTION WITH PATH EXPLORATION ===

This demonstrates proper concolic execution using Galette and Knarr:
1. Start with initial input and collect path constraints
2. Use constraint solver to generate inputs for unexplored paths
3. Automatically discover boundary conditions

CONCOLIC EXECUTION ANALYSIS

=== ITERATION 1: Initial Execution ===
Starting concolic analysis with initial value = 12.0
🏷️ PathUtils: Added user symbolic label: thickness_1
ModelTransformationExample: Created symbolic value: thickness_1 = 12.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
🔍 GaletteTransformer.transform() called for: edu/neu/ccs/prl/galette/examples/transformation/BrakeDiscTransformation
🎯 ComparisonInterceptorVisitor HARDCODED ENABLED for BrakeDiscTransformation
🎯 Adding ComparisonInterceptorVisitor to BrakeDiscTransformation (HARDCODED ENABLED)
🔍 ComparisonInterceptorVisitor created for class transformation
🎯 Intercepting comparison instruction: 151
🎯 Intercepting jump instruction: GE
🎯 Intercepting comparison instruction: 151
🎯 Intercepting comparison instruction: 152
🔍 TransformInternal completed for BrakeDiscTransformation, result length: 18408
🔍 TransformerWrapper.transform() called for: edu/neu/ccs/prl/galette/examples/transformation/BrakeDiscTransformation. classBeingRedefined: null. classFileBuffer length: 18408. loader: jdk.internal.loader.ClassLoaders$AppClassLoader@71f2a7d5. protectionDomain: ProtectionDomain  (file:/home/anne/concolic-execution/galette-concolic-model-transformation/knarr-runtime/target/classes/ <no signer certificates>)
 jdk.internal.loader.ClassLoaders$AppClassLoader@71f2a7d5
 <no principals>
 java.security.Permissions@69930714 (
 ("java.io.FilePermission" "/home/anne/concolic-execution/galette-concolic-model-transformation/knarr-runtime/target/classes/-" "read")
 ("java.lang.RuntimePermission" "exitVM")
)

. GaletteTransformer: edu.neu.ccs.prl.galette.internal.transform.GaletteTransformer@78047b92
🔍 About to call transformer.transform()
🔍 Transformer class: edu.neu.ccs.prl.galette.internal.transform.GaletteTransformer
🔍 Transformer classloader: null
🔍 Transformer location: null
🔍 GaletteTransformer.transform() called for: edu/neu/ccs/prl/galette/examples/transformation/BrakeDiscTransformation
🔍 Found GaletteInstrumented annotation on BrakeDiscTransformation
⚠️ BrakeDiscTransformation already has shadow instrumentation - skipping
🔍 TransformInternal completed for BrakeDiscTransformation, result length: null
🔍 transformer.transform() returned: null
🔍 TransformerWrapper.transform() result for edu/neu/ccs/prl/galette/examples/transformation/BrakeDiscTransformation: null (no transformation)
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 12.0 vs 80.0
🔍 mightBeSymbolic(double 12.0, 80.0) -> true (collecting all)
❌ DCMPL: PATH_CONDITIONS.get() returned null! Initializing new list...
✅ DCMPL constraint added: 12.0 DCMPL 80.0 -> -1
PathUtils: 12.0 DCMPL 80.0 -> -1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge: Attempting to load Galette PathConstraintAPI...
✅ Successfully loaded PathConstraintAPI: edu.neu.ccs.prl.galette.PathConstraintAPI
✅ Found getCurrentConstraints method: public static java.util.List edu.neu.ccs.prl.galette.PathConstraintAPI.getCurrentConstraints()
✅ Found flushConstraints method: public static java.util.List edu.neu.ccs.prl.galette.PathConstraintAPI.flushConstraints()
🎉 GalettePathConstraintBridge initialization complete!
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 1 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 1
🔧 Merged constraints: 1
Path constraints: 12.0<80.0
Initial path constraint: 12.0<80.0
Result: additionalStiffness = false

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 1 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 1
🔧 Merged constraints: 1
🔍 generateAlternativeInput: Analyzing 1 constraints
  Extracted thresholds: [80.0, 12.0]
🔍 Using discovered thresholds: [80.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=false
  → Generating value 81.0 for testing > 80.0

=== ITERATION 2: Alternative Path ===
Exploring with generated input: 81.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_2
ModelTransformationExample: Created symbolic value: thickness_2 = 81.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 81.0 vs 80.0
🔍 mightBeSymbolic(double 81.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 81.0 DCMPL 80.0 -> 1
PathUtils: 81.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 2 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 2
🔧 Merged constraints: 2
Path constraints: (12.0<80.0)&&(81.0>80.0)
Path constraint: (12.0<80.0)&&(81.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 2 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 2
🔧 Merged constraints: 2
🔍 generateAlternativeInput: Analyzing 2 constraints
  Extracted thresholds: [80.0, 12.0]
  Extracted thresholds: [81.0, 80.0]
🔍 Using discovered thresholds: [80.0, 81.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=true
  Threshold 81.0: hasLow=true, hasHigh=false
  → Generating value 82.0 for testing > 81.0

=== ITERATION 3: Alternative Path ===
Exploring with generated input: 82.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_3
ModelTransformationExample: Created symbolic value: thickness_3 = 82.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 82.0 vs 80.0
🔍 mightBeSymbolic(double 82.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 82.0 DCMPL 80.0 -> 1
PathUtils: 82.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 3 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 3
🔧 Merged constraints: 3
Path constraints: ((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0)
Path constraint: ((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 3 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 3
🔧 Merged constraints: 3
🔍 generateAlternativeInput: Analyzing 3 constraints
  Extracted thresholds: [80.0, 12.0]
  Extracted thresholds: [81.0, 80.0]
  Extracted thresholds: [82.0, 80.0]
🔍 Using discovered thresholds: [80.0, 81.0, 82.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=true
  Threshold 81.0: hasLow=true, hasHigh=true
  Threshold 82.0: hasLow=true, hasHigh=false
  → Generating value 83.0 for testing > 82.0

=== ITERATION 4: Alternative Path ===
Exploring with generated input: 83.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_4
ModelTransformationExample: Created symbolic value: thickness_4 = 83.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 83.0 vs 80.0
🔍 mightBeSymbolic(double 83.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 83.0 DCMPL 80.0 -> 1
PathUtils: 83.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 4 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 4
🔧 Merged constraints: 4
Path constraints: (((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0)
Path constraint: (((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 4 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 4
🔧 Merged constraints: 4
🔍 generateAlternativeInput: Analyzing 4 constraints
  Extracted thresholds: [80.0, 12.0]
  Extracted thresholds: [81.0, 80.0]
  Extracted thresholds: [82.0, 80.0]
  Extracted thresholds: [83.0, 80.0]
🔍 Using discovered thresholds: [80.0, 81.0, 82.0, 83.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=true
  Threshold 81.0: hasLow=true, hasHigh=true
  Threshold 82.0: hasLow=true, hasHigh=true
  Threshold 83.0: hasLow=true, hasHigh=false
  → Generating value 84.0 for testing > 83.0

=== ITERATION 5: Alternative Path ===
Exploring with generated input: 84.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_5
ModelTransformationExample: Created symbolic value: thickness_5 = 84.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 84.0 vs 80.0
🔍 mightBeSymbolic(double 84.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 84.0 DCMPL 80.0 -> 1
PathUtils: 84.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 5 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 5
🔧 Merged constraints: 5
Path constraints: ((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0)
Path constraint: ((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 5 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 5
🔧 Merged constraints: 5
🔍 generateAlternativeInput: Analyzing 5 constraints
  Extracted thresholds: [80.0, 12.0]
  Extracted thresholds: [81.0, 80.0]
  Extracted thresholds: [82.0, 80.0]
  Extracted thresholds: [83.0, 80.0]
  Extracted thresholds: [80.0, 84.0]
🔍 Using discovered thresholds: [80.0, 81.0, 82.0, 83.0, 84.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=true
  Threshold 81.0: hasLow=true, hasHigh=true
  Threshold 82.0: hasLow=true, hasHigh=true
  Threshold 83.0: hasLow=true, hasHigh=true
  Threshold 84.0: hasLow=true, hasHigh=false
  → Generating value 85.0 for testing > 84.0

=== ITERATION 6: Alternative Path ===
Exploring with generated input: 85.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_6
ModelTransformationExample: Created symbolic value: thickness_6 = 85.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 85.0 vs 80.0
🔍 mightBeSymbolic(double 85.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 85.0 DCMPL 80.0 -> 1
PathUtils: 85.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 6 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 6
🔧 Merged constraints: 6
Path constraints: (((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0)
Path constraint: (((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 6 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 6
🔧 Merged constraints: 6
🔍 generateAlternativeInput: Analyzing 6 constraints
  Extracted thresholds: [80.0, 12.0]
  Extracted thresholds: [81.0, 80.0]
  Extracted thresholds: [82.0, 80.0]
  Extracted thresholds: [83.0, 80.0]
  Extracted thresholds: [80.0, 84.0]
  Extracted thresholds: [80.0, 85.0]
🔍 Using discovered thresholds: [80.0, 81.0, 82.0, 83.0, 84.0, 85.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=true
  Threshold 81.0: hasLow=true, hasHigh=true
  Threshold 82.0: hasLow=true, hasHigh=true
  Threshold 83.0: hasLow=true, hasHigh=true
  Threshold 84.0: hasLow=true, hasHigh=true
  Threshold 85.0: hasLow=true, hasHigh=false
  → Generating value 86.0 for testing > 85.0

=== ITERATION 7: Alternative Path ===
Exploring with generated input: 86.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_7
ModelTransformationExample: Created symbolic value: thickness_7 = 86.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 86.0 vs 80.0
🔍 mightBeSymbolic(double 86.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 86.0 DCMPL 80.0 -> 1
PathUtils: 86.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 7 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 7
🔧 Merged constraints: 7
Path constraints: ((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0)
Path constraint: ((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 7 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 7
🔧 Merged constraints: 7
🔍 generateAlternativeInput: Analyzing 7 constraints
  Extracted thresholds: [80.0, 12.0]
  Extracted thresholds: [81.0, 80.0]
  Extracted thresholds: [82.0, 80.0]
  Extracted thresholds: [83.0, 80.0]
  Extracted thresholds: [80.0, 84.0]
  Extracted thresholds: [80.0, 85.0]
  Extracted thresholds: [80.0, 86.0]
🔍 Using discovered thresholds: [80.0, 81.0, 82.0, 83.0, 84.0, 85.0, 86.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=true
  Threshold 81.0: hasLow=true, hasHigh=true
  Threshold 82.0: hasLow=true, hasHigh=true
  Threshold 83.0: hasLow=true, hasHigh=true
  Threshold 84.0: hasLow=true, hasHigh=true
  Threshold 85.0: hasLow=true, hasHigh=true
  Threshold 86.0: hasLow=true, hasHigh=false
  → Generating value 87.0 for testing > 86.0

=== ITERATION 8: Alternative Path ===
Exploring with generated input: 87.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_8
ModelTransformationExample: Created symbolic value: thickness_8 = 87.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 87.0 vs 80.0
🔍 mightBeSymbolic(double 87.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 87.0 DCMPL 80.0 -> 1
PathUtils: 87.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 8 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 8
🔧 Merged constraints: 8
Path constraints: (((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0)
Path constraint: (((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 8 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 8
🔧 Merged constraints: 8
🔍 generateAlternativeInput: Analyzing 8 constraints
  Extracted thresholds: [80.0, 12.0]
  Extracted thresholds: [81.0, 80.0]
  Extracted thresholds: [82.0, 80.0]
  Extracted thresholds: [83.0, 80.0]
  Extracted thresholds: [80.0, 84.0]
  Extracted thresholds: [80.0, 85.0]
  Extracted thresholds: [80.0, 86.0]
  Extracted thresholds: [80.0, 87.0]
🔍 Using discovered thresholds: [80.0, 81.0, 82.0, 83.0, 84.0, 85.0, 86.0, 87.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=true
  Threshold 81.0: hasLow=true, hasHigh=true
  Threshold 82.0: hasLow=true, hasHigh=true
  Threshold 83.0: hasLow=true, hasHigh=true
  Threshold 84.0: hasLow=true, hasHigh=true
  Threshold 85.0: hasLow=true, hasHigh=true
  Threshold 86.0: hasLow=true, hasHigh=true
  Threshold 87.0: hasLow=true, hasHigh=false
  → Generating value 88.0 for testing > 87.0

=== ITERATION 9: Alternative Path ===
Exploring with generated input: 88.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_9
ModelTransformationExample: Created symbolic value: thickness_9 = 88.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 88.0 vs 80.0
🔍 mightBeSymbolic(double 88.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 88.0 DCMPL 80.0 -> 1
PathUtils: 88.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 9 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 9
🔧 Merged constraints: 9
Path constraints: ((((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0))&&(88.0>80.0)
Path constraint: ((((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0))&&(88.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!

=== Generating Alternative Inputs ===
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 9 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 9
🔧 Merged constraints: 9
🔍 generateAlternativeInput: Analyzing 9 constraints
  Extracted thresholds: [80.0, 12.0]
  Extracted thresholds: [81.0, 80.0]
  Extracted thresholds: [82.0, 80.0]
  Extracted thresholds: [83.0, 80.0]
  Extracted thresholds: [80.0, 84.0]
  Extracted thresholds: [80.0, 85.0]
  Extracted thresholds: [80.0, 86.0]
  Extracted thresholds: [80.0, 87.0]
  Extracted thresholds: [80.0, 88.0]
🔍 Using discovered thresholds: [80.0, 81.0, 82.0, 83.0, 84.0, 85.0, 86.0, 87.0, 88.0, 12.0]
  Threshold 80.0: hasLow=true, hasHigh=true
  Threshold 81.0: hasLow=true, hasHigh=true
  Threshold 82.0: hasLow=true, hasHigh=true
  Threshold 83.0: hasLow=true, hasHigh=true
  Threshold 84.0: hasLow=true, hasHigh=true
  Threshold 85.0: hasLow=true, hasHigh=true
  Threshold 86.0: hasLow=true, hasHigh=true
  Threshold 87.0: hasLow=true, hasHigh=true
  Threshold 88.0: hasLow=true, hasHigh=false
  → Generating value 89.0 for testing > 88.0

=== ITERATION 10: Alternative Path ===
Exploring with generated input: 89.0 mm
🏷️ PathUtils: Added user symbolic label: thickness_10
ModelTransformationExample: Created symbolic value: thickness_10 = 89.0 (tag: no tag)
🔧 About to call BrakeDiscTransformation.transform() with tagged thickness
BrakeDiscTransformation: encounter tag: no tag)
BrakeDiscTransformation: tag in transformation result: no tag)
🔍 PathUtils.instrumentedDcmpl called: 89.0 vs 80.0
🔍 mightBeSymbolic(double 89.0, 80.0) -> true (collecting all)
✅ DCMPL constraint added: 89.0 DCMPL 80.0 -> 1
PathUtils: 89.0 DCMPL 80.0 -> 1
🔧 BrakeDiscTransformation.transform() completed
🔧 PathUtils.getCurPCWithGalette() called
🔧 Existing constraints: 0
🔧 GalettePathConstraintBridge.getGaletteConstraints() called, isAvailable=true
🔧 Retrieved 10 raw constraints from Galette PathUtils
🔧 Galette constraints retrieved: 10
🔧 Merged constraints: 10
Path constraints: (((((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0))&&(88.0>80.0))&&(89.0>80.0)
Path constraint: (((((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0))&&(88.0>80.0))&&(89.0>80.0)
Result: additionalStiffness = true
✓ NEW EXECUTION PATH DISCOVERED!
CONCOLIC ANALYSIS SUMMARY
Total iterations: 10
Inputs explored: 10
Unique path constraints: 10

Explored inputs and their path constraints:
  Input 12.0 mm → 12.0<80.0
  Input 81.0 mm → (12.0<80.0)&&(81.0>80.0)
  Input 82.0 mm → ((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0)
  Input 83.0 mm → (((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0)
  Input 84.0 mm → ((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0)
  Input 85.0 mm → (((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0)
  Input 86.0 mm → ((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0)
  Input 87.0 mm → (((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0)
  Input 88.0 mm → ((((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0))&&(88.0>80.0)
  Input 89.0 mm → (((((((((12.0<80.0)&&(81.0>80.0))&&(82.0>80.0))&&(83.0>80.0))&&(84.0>80.0))&&(85.0>80.0))&&(86.0>80.0))&&(87.0>80.0))&&(88.0>80.0))&&(89.0>80.0)

=== BOUNDARY CONDITION ANALYSIS ===
=== Dynamic Boundary Analysis (using Galette/Knarr) ===
No constraint solution available, analyzing input patterns...
  Input distribution: [12.0, 81.0, 82.0, 83.0, 84.0, 85.0, 86.0, 87.0, 88.0, 89.0]
  Range: 12.0 to 89.0
  → Potential boundary around 46.5 mm (gap: 69.0)
✓ Boundary analysis complete - using dynamic constraint discovery
Available options:
1. Standard transformation (concrete execution, no path exploration)
2. Concolic execution with automated path constraint collection and exploration
3. Exit

✅ Execution completed
   If you see 'Path constraints: no constraints', verify:
   1. Galette agent is properly configured
   2. Instrumented Java is being used
   3. Both -Xbootclasspath/a and -javaagent arguments are present