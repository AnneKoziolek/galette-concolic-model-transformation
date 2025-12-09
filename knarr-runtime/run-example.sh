#!/bin/bash

# Script to run the ModelTransformationExample with Galette instrumentation
# This script creates instrumented Java and runs with proper agent configuration
#
# USAGE:
#   ./run-example.sh                    # Normal run (builds only if needed)
#   
# To force rebuild specific components, edit the flags below:
#   FORCE_REBUILD_GREEN=true           # Rebuild only Green solver and dependencies
#   FORCE_REBUILD_AGENT=true           # Rebuild only galette-agent JAR (for agent changes)
#   FORCE_REBUILD_CLASSES=true         # Rebuild only knarr-runtime classes (for code changes)
#   FORCE_REBUILD_JAVA=true            # Rebuild only instrumented Java (for JDK issues)
#   FORCE_CLEAN_BUILD=true             # Full clean rebuild (everything)
#
# NOTE: On first run or after cloning, the script will automatically build and install
#       all required dependencies (Green solver and Galette modules) to a workspace-local
#       Maven repository for isolation between parallel VS Code instances.

set -e  # Exit on any error

echo "🚀 Enhanced Galette Knarr Runtime Example"
echo "=========================================="

# Ensure Java 17 is used for builds and execution
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$PATH"

echo "☕ Java Configuration:"
echo "   JAVA_HOME: $JAVA_HOME"
echo "   Java version: $(java -version 2>&1 | head -1)"
echo ""


# Build configuration flags - set to true to force rebuild of specific components
FORCE_CLEAN_BUILD=false        # Set to true for complete clean rebuild (overrides all others)
FORCE_REBUILD_GREEN=false       # Force rebuild Green solver and Galette modules dependencies
FORCE_REBUILD_AGENT=true       # Force rebuild galette-agent JAR only
FORCE_REBUILD_CLASSES=false     # Force rebuild knarr-runtime Java classes only
FORCE_REBUILD_JAVA=true       # Force rebuild instrumented Java installation only

# Use workspace-local Maven repository for isolation
MAVEN_REPO_LOCAL="../.m2repo"
# Convert to absolute path for Maven repository URL
MAVEN_REPO_ABSOLUTE="$(cd .. && pwd)/.m2repo"

# Check if this is a fresh clone (no local Maven repo)
if [ ! -d "$MAVEN_REPO_LOCAL" ]; then
    echo "📦 Fresh clone detected - initializing workspace-local Maven repository"
    echo "🔨 Building and installing all dependencies to $MAVEN_REPO_LOCAL"
    echo "   (This may take a few minutes on first run)"
    echo ""
    
    # Force full setup on fresh clone
    FORCE_CLEAN_BUILD=true
fi

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
need_green_build=false

# Check individual build requirements
    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "🧹 FORCE_CLEAN_BUILD enabled - forcing complete rebuild"
        # Clear negative cache for Maven to force re-resolution of artifacts
        rm -rf "$MAVEN_REPO_LOCAL" 2>/dev/null || true
        # Install all galette modules and dependencies into the local Maven repo for plugin resolution
        echo "🔨 Installing Green solver dependency into local Maven repo..."
        echo "   Building: green-solver/green"
        (cd ../../green-solver/green && mvn clean install -q -DskipTests -U -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE")
        if [ $? -ne 0 ]; then
            echo "❌ Green solver build failed!"
            exit 1
        fi
        echo "✅ Green solver installed"
        
        echo "🔨 Installing all Galette modules into local Maven repo..."
        echo "   Building: Galette project modules"
        (cd .. && mvn clean install -q -DskipTests -U -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE")
        if [ $? -ne 0 ]; then
            echo "❌ Galette modules build failed!"
            exit 1
        fi
        echo "✅ Galette modules installed"
        need_agent_build=true
        need_classes_build=true
        need_java_build=true
else
    # Check if Green solver needs rebuild
    if [ "$FORCE_REBUILD_GREEN" = "true" ]; then
        need_green_build=true
        echo "📦 Green solver rebuild needed"
    fi
    
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
if [ "$need_green_build" = "true" ] || [ "$need_agent_build" = "true" ] || [ "$need_classes_build" = "true" ] || [ "$need_java_build" = "true" ]; then
    echo "📦 Building required components..."
    
    # Clean target if doing complete rebuild
    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "🧹 Cleaning Maven target directory..."
        mvn clean -q -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE"
        
        # Remove instrumented Java if it exists
        if [ -d "target/galette/java" ]; then
            echo "🧹 Removing existing instrumented Java directory"
            rm -rf target/galette/java
        fi
    fi
    
    # Step 0: Build Green solver if needed
    if [ "$need_green_build" = "true" ]; then
        echo "🔨 Building and installing Green solver..."
        (cd ../../green-solver/green && mvn clean install -q -DskipTests -U -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE")
        if [ $? -ne 0 ]; then
            echo "❌ Green solver build failed!"
            exit 1
        fi
        echo "✅ Green solver installed"
        
        echo "🔨 Installing all Galette modules into local Maven repo..."
        echo "   Building: Galette project modules"
        (cd .. && mvn clean install -q -DskipTests -U -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE")
        if [ $? -ne 0 ]; then
            echo "❌ Galette modules build failed!"
            exit 1
        fi
        echo "✅ Galette modules installed"
    else
        echo "⚡ Using existing Green solver and Galette modules"
    fi
    
    # Step 1: Build galette-agent if needed
    if [ "$need_agent_build" = "true" ]; then
        echo "🔨 Building and installing galette-agent..."
        (cd ../galette-agent && mvn clean install -q -DskipTests -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE")
        if [ $? -ne 0 ]; then
            echo "❌ Galette agent build failed!"
            exit 1
        fi
        echo "✅ Galette agent built and installed successfully"
    else
        echo "⚡ Using existing galette-agent JAR"
    fi
    
    # Step 2: Compile Java classes if needed
    if [ "$need_classes_build" = "true" ]; then
        echo "🔨 Compiling Java classes..."
        mvn compile -q -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE"
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
        mvn process-test-resources -q -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE"
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

# ==================== GREEN SERVER SETUP ====================
GREEN_SERVER_PORT=9408
GREEN_SERVER_PID=""

# Function to check if GreenServer is already running
is_green_server_running() {
    nc -z localhost $GREEN_SERVER_PORT 2>/dev/null
    return $?
}

# Function to start the GreenServer in a separate non-instrumented JVM
start_green_server() {
    echo "🔧 Setting up GreenServer (non-instrumented JVM for solver isolation)..."
    
    if is_green_server_running; then
        echo "✅ GreenServer already running on port $GREEN_SERVER_PORT"
        return 0
    fi
    
    local GREEN_SOLVER_DIR="../../green-solver"
    local GREENSERVER_DIR="$GREEN_SOLVER_DIR/greenserver"
    local GREEN_JAR="$GREEN_SOLVER_DIR/green/target/green-1.0-SNAPSHOT.jar"
    local KNARR_Z3_LIB="../../knarr/z3-4.8.9-x64-ubuntu-16.04/bin"
    
    # Build green if JAR doesn't exist
    if [ ! -f "$GREEN_JAR" ]; then
        echo "🔨 Building green solver..."
        (cd "$GREEN_SOLVER_DIR/green" && mvn package -DskipTests -q)
    fi
    
    # Copy green JAR to greenserver lib if needed
    if [ ! -f "$GREENSERVER_DIR/lib/green.jar" ] || [ "$GREEN_JAR" -nt "$GREENSERVER_DIR/lib/green.jar" ]; then
        echo "📦 Updating greenserver/lib/green.jar..."
        cp "$GREEN_JAR" "$GREENSERVER_DIR/lib/green.jar"
    fi
    
    # Build greenserver using javac
    local GS_SRC="$GREENSERVER_DIR/src/za/ac/sun/cs/green/server/GreenServer.java"
    local GS_CLASS="$GREENSERVER_DIR/bin/za/ac/sun/cs/green/server/GreenServer.class"
    if [ ! -f "$GS_CLASS" ] || [ "$GS_SRC" -nt "$GS_CLASS" ]; then
        echo "🔨 Compiling greenserver..."
        mkdir -p "$GREENSERVER_DIR/bin/za/ac/sun/cs/green/server"
        javac -cp "$GREENSERVER_DIR/lib/green.jar" -d "$GREENSERVER_DIR/bin" "$GS_SRC" 2>&1
        if [ $? -ne 0 ]; then
            echo "⚠️ Failed to compile GreenServer - solver will use in-process fallback"
            return 1
        fi
    fi
    
    # Build classpath for greenserver
    local GREEN_LIB="$GREEN_SOLVER_DIR/green/lib"
    local SERVER_CP="$GREENSERVER_DIR/bin:$GREENSERVER_DIR/lib/green.jar"
    SERVER_CP="$SERVER_CP:$KNARR_Z3_LIB/com.microsoft.z3.jar"
    SERVER_CP="$SERVER_CP:$GREEN_LIB/slf4j-api-1.7.12.jar:$GREEN_LIB/slf4j-simple-1.7.12.jar"
    
    # Set Z3 native library path
    export LD_LIBRARY_PATH="$KNARR_Z3_LIB:$GREEN_LIB:$LD_LIBRARY_PATH"
    
    # Start the server in background using NON-instrumented Java
    echo "🚀 Starting GreenServer on port $GREEN_SERVER_PORT..."
    java -cp "$SERVER_CP" za.ac.sun.cs.green.server.GreenServer > /tmp/greenserver.log 2>&1 &
    GREEN_SERVER_PID=$!
    
    # Wait for server to start
    local MAX_WAIT=30
    local WAITED=0
    while ! is_green_server_running && [ $WAITED -lt $MAX_WAIT ]; do
        sleep 0.5
        WAITED=$((WAITED + 1))
        if ! kill -0 $GREEN_SERVER_PID 2>/dev/null; then
            echo "⚠️ GreenServer process died - check /tmp/greenserver.log"
            echo "   Continuing with in-process solver fallback"
            return 1
        fi
    done
    
    if is_green_server_running; then
        echo "✅ GreenServer started (PID: $GREEN_SERVER_PID)"
        return 0
    else
        echo "⚠️ GreenServer failed to start - continuing with in-process solver"
        return 1
    fi
}

# Function to stop the GreenServer
stop_green_server() {
    if [ -n "$GREEN_SERVER_PID" ] && kill -0 $GREEN_SERVER_PID 2>/dev/null; then
        echo "🛑 Stopping GreenServer (PID: $GREEN_SERVER_PID)..."
        kill $GREEN_SERVER_PID 2>/dev/null || true
        wait $GREEN_SERVER_PID 2>/dev/null || true
    fi
}

# Cleanup on exit
trap stop_green_server EXIT

# Start GreenServer before running the example
start_green_server
echo ""
# ==================== END GREEN SERVER SETUP ====================

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
    mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q -Dmaven.repo.local="$MAVEN_REPO_ABSOLUTE" -Dlocal.repo.path="$MAVEN_REPO_ABSOLUTE"
    
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
  -Dgalette.useGreenSolver=true \
  -Dgalette.useGreenServer=true \
  -DDEBUG=true \
  edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"

echo ""
echo "✅ Execution completed"
echo "   If you see 'Path constraints: no constraints', verify:"
echo "   1. Galette agent is properly configured"
echo "   2. Instrumented Java is being used"
echo "   3. Both -Xbootclasspath/a and -javaagent arguments are present"