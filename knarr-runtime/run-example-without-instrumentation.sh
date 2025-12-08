#!/bin/bash

# Script to run the ModelTransformationExample with Galette instrumentation
# This script creates instrumented Java and runs with proper agent configuration

set -e  # Exit on any error

# Ensure Java 17 is used for builds and execution
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$PATH"

echo "🚀 Galette Knarr Runtime Example (SymbolicComparison Mode)"
echo "=========================================================="
echo "☕ Java Configuration:"
echo "   JAVA_HOME: $JAVA_HOME"
echo "   Java version: $(java -version 2>&1 | head -1)"
echo ""

# ============================================================================
# Build Configuration Constants
# ============================================================================
FORCE_CLEAN_BUILD=false        # Force complete clean rebuild (overrides everything)
FORCE_REBUILD_AGENT=false      # Force rebuild galette-agent JAR only
FORCE_REBUILD_CLASSES=false    # Force rebuild knarr-runtime Java classes only
USE_GALETTE_AGENT=false        # Use Galette agent for automatic interception (requires instrumented Java)

# Function to check if compilation is needed
needs_build() {
    # FORCE_CLEAN_BUILD overrides all other flags
    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "🧹 FORCE_CLEAN_BUILD enabled - forcing complete rebuild"
        return 0  # true - needs build
    fi
    
    # Check individual force flags
    if [ "$FORCE_REBUILD_CLASSES" = "true" ]; then
        echo "🧹 FORCE_REBUILD_CLASSES enabled - rebuilding Java classes"
        return 0  # true - needs build
    fi
    
    local target_dir="target/classes"
    local main_class="$target_dir/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.class"
    
    # If target directory doesn't exist, need build
    if [ ! -d "$target_dir" ] || [ ! -f "$main_class" ]; then
        echo "📦 Target directory or main class not found - build needed"
        return 0  # true - needs build
    fi
    
    # Check if main class was compiled within the last 5 minutes (300 seconds)
    local current_time=$(date +%s)
    local file_time=$(stat -c %Y "$main_class" 2>/dev/null || echo 0)
    local time_diff=$((current_time - file_time))
    
    if [ $time_diff -lt 300 ]; then
        echo "✅ Main class compiled $time_diff seconds ago (< 5 minutes) - using existing build"
        return 1  # false - no build needed
    fi
    
    # Check if any source files are newer than the compiled class
    local src_dir="src/main/java"
    if [ -d "$src_dir" ]; then
        local newest_src=$(find "$src_dir" -name "*.java" -newer "$main_class" | head -1)
        if [ -n "$newest_src" ]; then
            echo "📦 Source file $newest_src is newer than compiled class - build needed"
            return 0  # true - needs build
        fi
    fi
    
    echo "✅ Build is up-to-date - using existing compiled classes"
    return 1  # false - no build needed
}

# Rebuild galette-agent if requested
if [ "$FORCE_CLEAN_BUILD" = "true" ] || [ "$FORCE_REBUILD_AGENT" = "true" ]; then
    echo "🔨 Rebuilding galette-agent..."
    (cd ../galette-agent && mvn clean install -DskipTests -q)
    if [ $? -ne 0 ]; then
        echo "❌ Failed to rebuild galette-agent!"
        exit 1
    fi
    echo "✅ galette-agent rebuilt successfully"
    echo ""
fi

# Build project if needed
if needs_build; then
    echo "📦 Building project..."
    
    # Handle FORCE_CLEAN_BUILD
    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "🧹 FORCE_CLEAN_BUILD enabled - removing all build artifacts"
        echo "🧹 Cleaning Maven target directory..."
        mvn clean -q
    else
        # Normal rebuild - clean everything
        echo "🧹 Cleaning Maven target directory..."
        mvn clean -q
    fi
    
    # Compile classes
    echo "🔨 Compiling Java classes..."
    mvn compile -q
    
    if [ $? -ne 0 ]; then
        echo "❌ Build failed!"
        exit 1
    fi
    echo "✅ Build completed successfully"
else
    echo "⚡ Using existing build"
fi

# Generate classpath using Maven (only if needed)
if [ ! -f cp.txt ] || [ $(find cp.txt -mmin +60 2>/dev/null | wc -l) -eq 1 ]; then
    echo "📋 Generating classpath..."
    mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q
    
    if [ ! -f cp.txt ]; then
        echo "❌ Failed to generate classpath file!"
        exit 1
    fi
else
    echo "⚡ Using cached classpath (cp.txt)"
fi

# Create classpath with compiled classes and all dependencies
CP="target/classes:target/test-classes:$(cat cp.txt)"

echo "📚 Using classpath with $(echo $CP | tr ':' '\n' | wc -l) entries"
echo ""

# Determine execution mode
if [ "$USE_GALETTE_AGENT" = "true" ]; then
    echo "🚀 Running with Galette agent (automatic interception mode)..."
    
    # Need instrumented Java for agent mode
    INSTRUMENTED_JAVA="target/galette/java"
    if [ ! -f "$INSTRUMENTED_JAVA/bin/java" ]; then
        echo "❌ Instrumented Java not found at: $INSTRUMENTED_JAVA"
        echo "   Need to rebuild with instrumented Java for agent mode"
        echo "   Or set USE_GALETTE_AGENT=false to use SymbolicComparison"
        exit 1
    fi
    
    # Find Galette agent JAR
    GALETTE_AGENT=""
    if [ -f "../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar" ]; then
        GALETTE_AGENT="../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"
    elif [ -f "$HOME/.m2/repository/edu/neu/ccs/prl/galette/galette-agent/1.0.0-SNAPSHOT/galette-agent-1.0.0-SNAPSHOT.jar" ]; then
        GALETTE_AGENT="$HOME/.m2/repository/edu/neu/ccs/prl/galette/galette-agent/1.0.0-SNAPSHOT/galette-agent-1.0.0-SNAPSHOT.jar"
    else
        echo "❌ Galette agent JAR not found!"
        exit 1
    fi
    
    echo "🔧 Configuration:"
    echo "   Java: $INSTRUMENTED_JAVA/bin/java"
    echo "   Agent: $GALETTE_AGENT"
    echo ""
    
    mkdir -p target/galette/cache
    
    # AGENT MODE: Use instrumented Java with agent
    # - Instrumented Java: required to have bytecode instrumentation for taint tracking
    # - -Xbootclasspath/a: needed to ensure agent can instrument bootstrap classes
    # - -javaagent: enables automatic bytecode interception of comparison operations
    "$INSTRUMENTED_JAVA/bin/java" \
      -cp "$CP" \
      -Xbootclasspath/a:"$GALETTE_AGENT" \
      -javaagent:"$GALETTE_AGENT" \
      -Dgalette.cache=target/galette/cache \
      -Dgalette.coverage=true \
      -Dsymbolic.execution.debug=true \
      -Dgalette.debug=true \
      edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"
else
    echo "🚀 Running with SymbolicComparison (explicit API mode)..."
    echo "🔧 Configuration:"
    echo "   Java: $JAVA_HOME/bin/java (regular JDK)"
    echo "   Mode: Explicit SymbolicComparison API"
    echo ""
    
    # SYMBOLIC COMPARISON MODE: Use regular Java 17 (no agent needed)
    # - SymbolicComparison: application code explicitly calls comparison methods
    # - No agent: comparisons are NOT automatically intercepted
    # - No instrumented Java: uses standard JDK for faster builds and simpler execution
    #
    # NOTE: If switching to agent mode later (automatic interception):
    # - Will need to use instrumented Java (from target/galette/java)
    # - Will need -Xbootclasspath/a to instrument bootstrap classes (java.*, sun.* packages)
    #   This is required because the agent must intercept comparisons in the core JDK classes.
    # - Will need -javaagent to intercept comparison bytecode operations
    #   This triggers the Galette transformation on all loaded classes, not just application code.
    # - This mode catches comparisons WITHOUT explicit SymbolicComparison calls
    "$JAVA_HOME/bin/java" \
      -cp "$CP" \
      -Dgalette.debug=false \
      edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"
fi

echo ""
echo "✅ Execution completed"
