#!/bin/bash

# Test script to run with REGULAR Java (not instrumented) to verify agent works correctly

set -e

echo "🧪 TEST: Running with REGULAR Java + Galette Agent"
echo "=================================================="
echo ""
echo "This test bypasses the instrumented Java to verify if the embedded"
echo "GaletteTransformer in the instrumented Java is the problem."
echo ""

# Find regular Java
REGULAR_JAVA="/usr/lib/jvm/java-17-openjdk-amd64"
if [ ! -f "$REGULAR_JAVA/bin/java" ]; then
    echo "❌ Regular Java not found at: $REGULAR_JAVA"
    echo "   Please update the script with your Java installation path"
    exit 1
fi

# Find Galette agent JAR
GALETTE_AGENT="../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"
if [ ! -f "$GALETTE_AGENT" ]; then
    echo "❌ Galette agent JAR not found at: $GALETTE_AGENT"
    echo "   Run 'mvn package' in galette-agent directory"
    exit 1
fi

# Build classes if needed
if [ ! -d "target/classes" ]; then
    echo "📦 Building Java classes..."
    mvn compile -q
fi

# Generate classpath
if [ ! -f cp.txt ]; then
    echo "📋 Generating classpath..."
    mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q
fi

CP="target/classes:target/test-classes:$(cat cp.txt)"

echo "🔧 Configuration:"
echo "   Regular Java: $REGULAR_JAVA/bin/java"
echo "   Galette Agent: $GALETTE_AGENT"
echo ""

# Create simple test class that just loads BrakeDiscTransformation
cat > target/classes/SimpleTransformTest.java << 'EOF'
public class SimpleTransformTest {
    public static void main(String[] args) throws Exception {
        System.out.println("🧪 SimpleTransformTest: Loading BrakeDiscTransformation...");
        Class<?> clazz = Class.forName("edu.neu.ccs.prl.galette.examples.transformation.BrakeDiscTransformation");
        System.out.println("✅ Class loaded: " + clazz.getName());
        System.out.println("   ClassLoader: " + clazz.getClassLoader());
        System.out.println("   Location: " + clazz.getProtectionDomain().getCodeSource());
    }
}
EOF

# Compile test class
echo "🔨 Compiling test class..."
"$REGULAR_JAVA/bin/javac" -cp "$CP" -d target/classes target/classes/SimpleTransformTest.java

echo ""
echo "🚀 Running with REGULAR Java + Agent..."
echo "=========================================="
echo ""

# Run with regular Java and agent
"$REGULAR_JAVA/bin/java" \
  -cp "$CP" \
  -javaagent:"$GALETTE_AGENT" \
  -Dgalette.debug=true \
  SimpleTransformTest

echo ""
echo "✅ Test completed"
echo ""
echo "EXPECTED RESULT:"
echo "- You SHOULD see GaletteTransformer.transform() debug output"
echo "- This proves the agent works with regular Java"
echo ""
echo "If you see the debug output here but not with instrumented Java,"
echo "it confirms that the instrumented Java's embedded GaletteTransformer"
echo "is overriding the agent's version."