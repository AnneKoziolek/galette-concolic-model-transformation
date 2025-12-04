#!/bin/bash

# Script to run the ModelTransformationExample with Galette instrumentation
# This script creates instrumented Java and runs with proper agent configuration
#
# USAGE:
#   ./run-example.sh                    # Normal run (builds only if needed)
#   
# To force rebuild specific components, edit the flags below:
#   FORCE_REBUILD_AGENT=true           # Rebuild only galette-agent JAR (for agent changes)
#   FORCE_REBUILD_CLASSES=true         # Rebuild only knarr-runtime classes (for code changes)
#   FORCE_REBUILD_JAVA=true            # Rebuild only instrumented Java (for JDK issues)
#   FORCE_CLEAN_BUILD=true             # Full clean rebuild (everything)

set -e  # Exit on any error

echo "🚀 Enhanced Galette Knarr Runtime Example"
echo "=========================================="

# Build configuration flags - set to true to force rebuild of specific components
FORCE_CLEAN_BUILD=false        # Force complete clean rebuild (overrides everything)
FORCE_REBUILD_AGENT=false      # Force rebuild galette-agent JAR only
FORCE_REBUILD_CLASSES=false    # Force rebuild knarr-runtime Java classes only
FORCE_REBUILD_JAVA=false       # Force rebuild instrumented Java installation only

# Function to check if compilation and instrumentation is needed
needs_build() {
    # TEMPORARY: Force rebuild if flag is set
    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "🧹 FORCE_CLEAN_BUILD enabled - forcing complete rebuild"
        return 0  # true - needs build
    fi
    local target_dir="target/classes"
    local galette_java="target/galette/java"
    local main_class="$target_dir/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.class"
    
    # If target directory or instrumented Java doesn't exist, need build
    if [ ! -d "$target_dir" ] || [ ! -f "$main_class" ] || [ ! -d "$galette_java" ]; then
        echo "📦 Target directory, main class, or instrumented Java not found - build needed"
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
    
    echo "✅ Build is up-to-date - using existing compiled classes and instrumented Java"
    return 1  # false - no build needed
}

# Determine what needs to be built
need_agent_build=false
need_classes_build=false
need_java_build=false

# Check individual build requirements
if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
    echo "🧹 FORCE_CLEAN_BUILD enabled - forcing complete rebuild"
    need_agent_build=true
    need_classes_build=true
    need_java_build=true
else
    # Check if galette-agent needs rebuild
    if [ "$FORCE_REBUILD_AGENT" = "true" ] || [ ! -f "../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar" ]; then
        need_agent_build=true
        echo "📦 Galette agent rebuild needed"
    fi

    # Check if Java classes need rebuild
    if [ "$FORCE_REBUILD_CLASSES" = "true" ] || needs_build; then
        need_classes_build=true
        echo "📦 Java classes rebuild needed"
    fi

    # Check if instrumented Java needs rebuild
    if [ "$FORCE_REBUILD_JAVA" = "true" ] || [ ! -d "target/galette/java" ]; then
        need_java_build=true
        echo "📦 Instrumented Java rebuild needed"
    fi
fi

# Perform builds in correct order
if [ "$need_agent_build" = "true" ] || [ "$need_classes_build" = "true" ] || [ "$need_java_build" = "true" ]; then
    echo "📦 Building required components..."
    
    # Clean target if doing complete rebuild
    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "🧹 Cleaning Maven target directory..."
        mvn clean -q
        
        # Remove instrumented Java if it exists
        if [ -d "target/galette/java" ]; then
            echo "🧹 Removing existing instrumented Java directory"
            rm -rf target/galette/java
        fi
    fi
    
    # Step 1: Build galette-agent if needed
    if [ "$need_agent_build" = "true" ]; then
        echo "🔨 Building galette-agent..."
        (cd ../galette-agent && mvn clean package -q -DskipTests)
        if [ $? -ne 0 ]; then
            echo "❌ Galette agent build failed!"
            exit 1
        fi
        echo "✅ Galette agent built successfully"
    else
        echo "⚡ Using existing galette-agent JAR"
    fi
    
    # Step 2: Compile Java classes if needed
    if [ "$need_classes_build" = "true" ]; then
        echo "🔨 Compiling Java classes..."
        mvn compile -q
        if [ $? -ne 0 ]; then
            echo "❌ Java compilation failed!"
            exit 1
        fi
        echo "✅ Java classes compiled successfully"
    else
        echo "⚡ Using existing compiled classes"
    fi
    
    # Step 3: Create instrumented Java if needed
    if [ "$need_java_build" = "true" ]; then
        echo "⚙️ Creating instrumented Java installation..."
        mvn process-test-resources -q
        if [ $? -ne 0 ]; then
            echo "❌ Instrumented Java creation failed!"
            exit 1
        fi
        echo "✅ Instrumented Java created successfully"
    else
        echo "⚡ Using existing instrumented Java"
    fi
    
    echo "✅ All required builds completed successfully"
else
    echo "⚡ All components up-to-date - no builds needed"
fi

# Verify instrumented Java exists
INSTRUMENTED_JAVA="target/galette/java"
if [ ! -f "$INSTRUMENTED_JAVA/bin/java" ]; then
    echo "❌ Instrumented Java not found at: $INSTRUMENTED_JAVA"
    echo "   Run 'mvn process-resources' to create instrumented Java"
    exit 1
fi

# Find Galette agent JAR
GALETTE_AGENT=""
# Try parent directory first (standard Galette project structure)
if [ -f "../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar" ]; then
    GALETTE_AGENT="../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"
# Try Maven local repository as fallback
elif [ -f "$HOME/.m2/repository/edu/neu/ccs/prl/galette/galette-agent/1.0.0-SNAPSHOT/galette-agent-1.0.0-SNAPSHOT.jar" ]; then
    GALETTE_AGENT="$HOME/.m2/repository/edu/neu/ccs/prl/galette/galette-agent/1.0.0-SNAPSHOT/galette-agent-1.0.0-SNAPSHOT.jar"
else
    echo "❌ Galette agent JAR not found!"
    echo "   Expected locations:"
    echo "   - ../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"
    echo "   - ~/.m2/repository/edu/neu/ccs/prl/galette/galette-agent/1.0.0-SNAPSHOT/galette-agent-1.0.0-SNAPSHOT.jar"
    echo "   Run 'mvn install' in the parent galette directory"
    exit 1
fi

echo ""
echo "🔧 Configuration:"
echo "   Instrumented Java: $INSTRUMENTED_JAVA/bin/java"
echo "   Galette Agent: $GALETTE_AGENT"

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

# Create classpath with compiled classes and dependencies
# Note: Galette agent classes should be accessible via -Xbootclasspath/a, not regular classpath
CP="target/classes:target/test-classes:$(cat cp.txt)"

echo "📚 Using classpath with $(echo $CP | tr ':' '\n' | wc -l) entries"
echo ""

# Run with instrumented Java and Galette agent
echo "🚀 Running ModelTransformationExample with Galette instrumentation..."
echo "   Expected: Path constraints will be collected (not 'no constraints')"
echo ""

# CRITICAL: Use instrumented Java with both -Xbootclasspath/a and -javaagent
echo "🔍 Debug Information:"
echo "   Command: $INSTRUMENTED_JAVA/bin/java"
echo "   Agent arguments: -Xbootclasspath/a:$GALETTE_AGENT -javaagent:$GALETTE_AGENT"
echo "   Galette cache directory: target/galette/cache"

# Create cache directory if it doesn't exist
mkdir -p target/galette/cache

"$INSTRUMENTED_JAVA/bin/java" \
  -cp "$CP" \
  -Xbootclasspath/a:"$GALETTE_AGENT" \
  -javaagent:"$GALETTE_AGENT" \
  -Dgalette.cache=target/galette/cache \
  -Dgalette.coverage=true \
  -Dsymbolic.execution.debug=true \
  -Dgalette.debug=true \
  -Dgalette.concolic.interception.enabled=true \
  -Dgalette.concolic.interception.debug=true \
  -verbose:javaagent \
  edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"

echo ""
echo "✅ Execution completed"
echo "   If you see 'Path constraints: no constraints', verify:"
echo "   1. Galette agent is properly configured"
echo "   2. Instrumented Java is being used"
echo "   3. Both -Xbootclasspath/a and -javaagent arguments are present"