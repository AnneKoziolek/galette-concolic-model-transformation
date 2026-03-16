#!/bin/bash

# Script to run the ModelTransformationExample with Galette instrumentation
# and an EXTERNAL GreenServer process for constraint solving.
# This avoids Galette instrumenting the Green/Z3 bytecode.

set -e  # Exit on any error

# Ensure Java 17 is used for builds and execution
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$PATH"

echo "GreenServer + Galette Knarr Runtime Example"
echo "============================================"
echo "Java Configuration:"
echo "   JAVA_HOME: $JAVA_HOME"
echo "   Java version: $(java -version 2>&1 | head -1)"
echo ""

# ============================================================================
# Build Configuration Constants
# ============================================================================
FORCE_CLEAN_BUILD=false        # Force complete clean rebuild (overrides everything)
FORCE_REBUILD_AGENT=false      # Force rebuild galette-agent JAR only
FORCE_REBUILD_CLASSES=false    # Force rebuild knarr-runtime Java classes only (intelligent detection by default)
FORCE_REBUILD_JAVA=false       # Force rebuild instrumented Java installation only (intelligent detection by default)

# ============================================================================
# Runtime Configuration
# ============================================================================
USE_GREEN_SOLVER=true          # Enable Green solver integration
USE_EXTERNAL_GREEN_SERVER=true # Use external GreenServer process

# ============================================================================
# GreenServer Configuration
# ============================================================================
GREEN_SERVER_PORT=9408
GREEN_SERVER_DIR="/home/anne/CocoPath/green-solver/greenserver"
GREEN_SERVER_GREEN_LIB="/home/anne/CocoPath/green-solver/green/lib"
GREEN_SERVER_PID=""
# GreenServer requires Java 21 (green.jar compiled with class version 65.0)
GREEN_SERVER_JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
# Z3-turnkey from Maven (includes native libraries for Linux)
Z3_TURNKEY_JAR="$HOME/.m2/repository/io/github/tudo-aqua/z3-turnkey/4.8.14/z3-turnkey-4.8.14.jar"

# Cleanup strategy for instrumented Java (when rebuilding)
CLEANUP_STRATEGY="safe"

# ============================================================================
# GreenServer Management Functions
# ============================================================================

check_green_server_running() {
    # Check if port is in use
    if command -v nc &> /dev/null; then
        nc -z localhost $GREEN_SERVER_PORT 2>/dev/null
        return $?
    elif command -v ss &> /dev/null; then
        ss -tuln | grep -q ":$GREEN_SERVER_PORT " 2>/dev/null
        return $?
    else
        # Fallback: try to connect with bash
        (echo > /dev/tcp/localhost/$GREEN_SERVER_PORT) 2>/dev/null
        return $?
    fi
}

build_green_server() {
    echo "Building GreenServer with Java 21..."
    local original_dir=$(pwd)
    cd "$GREEN_SERVER_DIR"

    # Compile with Java 21 against Maven green.jar (same version as knarr-runtime)
    local MAVEN_GREEN_JAR="$HOME/.m2/repository/za/ac/sun/cs/green/green/1.0-SNAPSHOT/green-1.0-SNAPSHOT.jar"
    "$GREEN_SERVER_JAVA_HOME/bin/javac" -cp "$MAVEN_GREEN_JAR" -d bin \
        src/za/ac/sun/cs/green/server/GreenServer.java 2>&1
    local result=$?
    cd "$original_dir"

    if [ $result -eq 0 ]; then
        echo "   GreenServer built successfully (using Maven green.jar)"
    else
        echo "   Error: GreenServer build failed"
    fi
    return $result
}

start_green_server() {
    if check_green_server_running; then
        echo "GreenServer already running on port $GREEN_SERVER_PORT"
        return 0
    fi

    echo "Starting GreenServer on port $GREEN_SERVER_PORT..."

    # Check if GreenServer is built
    if [ ! -d "$GREEN_SERVER_DIR/bin" ] || [ -z "$(ls -A $GREEN_SERVER_DIR/bin 2>/dev/null)" ]; then
        build_green_server || return 1
    fi

    # Start GreenServer with NON-INSTRUMENTED Java 21 (critical!)
    # GreenServer requires Java 21 due to green.jar class version
    local original_dir=$(pwd)
    cd "$GREEN_SERVER_DIR"

    # Build classpath with Z3-turnkey (includes native libs) and other dependencies
    # IMPORTANT: Use the same green.jar from Maven that knarr-runtime uses to avoid serialVersionUID mismatch
    local MAVEN_GREEN_JAR="$HOME/.m2/repository/za/ac/sun/cs/green/green/1.0-SNAPSHOT/green-1.0-SNAPSHOT.jar"
    local GREEN_CP="bin:$MAVEN_GREEN_JAR:$Z3_TURNKEY_JAR"
    GREEN_CP="$GREEN_CP:$GREEN_SERVER_GREEN_LIB/slf4j-api-1.7.12.jar"
    GREEN_CP="$GREEN_CP:$GREEN_SERVER_GREEN_LIB/slf4j-simple-1.7.12.jar"

    echo "   Starting with: $GREEN_SERVER_JAVA_HOME/bin/java (Java 21 required)"
    echo "   Classpath includes Z3-turnkey for native Z3 support"
    "$GREEN_SERVER_JAVA_HOME/bin/java" -cp "$GREEN_CP" \
        za.ac.sun.cs.green.server.GreenServer \
        > /tmp/greenserver.log 2>&1 &
    GREEN_SERVER_PID=$!

    cd "$original_dir"

    # Wait for server to be ready
    echo "   Waiting for GreenServer to be ready..."
    local attempts=0
    local max_attempts=30
    while [ $attempts -lt $max_attempts ]; do
        if check_green_server_running; then
            echo "   GreenServer started (PID: $GREEN_SERVER_PID)"
            return 0
        fi
        sleep 0.5
        attempts=$((attempts + 1))
    done

    echo "   Error: GreenServer failed to start within ${max_attempts} attempts"
    echo "   Check /tmp/greenserver.log for details"
    if [ -f /tmp/greenserver.log ]; then
        echo "   Last lines of log:"
        tail -10 /tmp/greenserver.log
    fi
    return 1
}

stop_green_server() {
    if [ -n "$GREEN_SERVER_PID" ]; then
        echo ""
        echo "Stopping GreenServer (PID: $GREEN_SERVER_PID)..."

        # Try graceful shutdown first via QUIT command
        if check_green_server_running; then
            if command -v nc &> /dev/null; then
                echo "QUIT" | nc localhost $GREEN_SERVER_PORT 2>/dev/null || true
            fi
        fi

        # Wait briefly for graceful shutdown
        sleep 1

        # Force kill if still running
        if kill -0 $GREEN_SERVER_PID 2>/dev/null; then
            echo "   Sending SIGTERM..."
            kill $GREEN_SERVER_PID 2>/dev/null || true
            sleep 0.5
        fi

        if kill -0 $GREEN_SERVER_PID 2>/dev/null; then
            echo "   Sending SIGKILL..."
            kill -9 $GREEN_SERVER_PID 2>/dev/null || true
        fi

        wait $GREEN_SERVER_PID 2>/dev/null || true
        echo "   GreenServer stopped"
    fi
}

# Register cleanup handler
trap stop_green_server EXIT

# ============================================================================
# Instrumented Java Cleanup Functions (from original script)
# ============================================================================

clean_instrumented_java() {
    local java_dir="$1"
    local strategy="${2:-safe}"

    if [ ! -d "$java_dir" ]; then
        return 0
    fi

    case "$strategy" in
        "aggressive")
            echo "   Using aggressive cleanup (find-based deletion)..."
            find "$java_dir" -type f -delete 2>/dev/null
            find "$java_dir" -type d -delete 2>/dev/null
            rm -rf "$java_dir" 2>/dev/null
            ;;
        "nuclear")
            echo "   Using nuclear cleanup (recursive deletion)..."
            for i in {1..3}; do
                rm -rf "$java_dir" 2>/dev/null
                if [ ! -d "$java_dir" ]; then
                    break
                fi
                sleep 0.5
            done
            ;;
        *)
            echo "   Using safe cleanup (rm -rf with verification)..."
            rm -rf "$java_dir" 2>/dev/null
            if [ -d "$java_dir" ]; then
                echo "   Warning: Directory still exists after deletion, retrying..."
                sleep 1
                rm -rf "$java_dir" 2>/dev/null
            fi
            ;;
    esac

    if [ -d "$java_dir" ]; then
        echo "   Warning: Could not fully delete $java_dir (may cause rebuild issues)"
        return 1
    fi
    return 0
}

needs_build() {
    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "FORCE_CLEAN_BUILD enabled - forcing complete rebuild"
        return 0
    fi

    if [ "$FORCE_REBUILD_CLASSES" = "true" ]; then
        echo "FORCE_REBUILD_CLASSES enabled - rebuilding Java classes"
        return 0
    fi

    if [ "$FORCE_REBUILD_JAVA" = "true" ]; then
        echo "FORCE_REBUILD_JAVA enabled - rebuilding instrumented Java"
        return 0
    fi

    local target_dir="target/classes"
    local galette_java="target/galette/java"
    local main_class="$target_dir/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.class"

    if [ ! -d "$target_dir" ] || [ ! -f "$main_class" ] || [ ! -d "$galette_java" ]; then
        echo "Target directory, main class, or instrumented Java not found - build needed"
        return 0
    fi

    local current_time=$(date +%s)
    local file_time=$(stat -c %Y "$main_class" 2>/dev/null || echo 0)
    local time_diff=$((current_time - file_time))

    if [ $time_diff -lt 300 ]; then
        echo "Main class compiled $time_diff seconds ago (< 5 minutes) - using existing build"
        return 1
    fi

    local src_dir="src/main/java"
    if [ -d "$src_dir" ]; then
        local newest_src=$(find "$src_dir" -name "*.java" -newer "$main_class" | head -1)
        if [ -n "$newest_src" ]; then
            echo "Source file $newest_src is newer than compiled class - build needed"
            return 0
        fi
    fi

    echo "Build is up-to-date - using existing compiled classes and instrumented Java"
    return 1
}

# ============================================================================
# Main Execution
# ============================================================================

# Start GreenServer first (before any instrumented code runs)
start_green_server || {
    echo "Error: Failed to start GreenServer. Cannot continue."
    exit 1
}

echo ""

# Rebuild galette-agent if requested
if [ "$FORCE_CLEAN_BUILD" = "true" ] || [ "$FORCE_REBUILD_AGENT" = "true" ]; then
    echo "Rebuilding galette-agent..."
    (cd ../galette-agent && mvn clean install -DskipTests -q)
    if [ $? -ne 0 ]; then
        echo "Error: Failed to rebuild galette-agent!"
        exit 1
    fi
    echo "galette-agent rebuilt successfully"
    echo ""
fi

# Build project with instrumentation if needed
if needs_build; then
    echo "Building project with Galette instrumentation..."

    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "FORCE_CLEAN_BUILD enabled - removing all build artifacts"
        clean_instrumented_java "target/galette/java" "$CLEANUP_STRATEGY"
        echo "Cleaning Maven target directory..."
        mvn clean -q
    elif [ "$FORCE_REBUILD_JAVA" = "true" ]; then
        echo "FORCE_REBUILD_JAVA enabled - removing only instrumented Java"
        clean_instrumented_java "target/galette/java" "$CLEANUP_STRATEGY"
    else
        echo "Cleaning Maven target directory..."
        mvn clean -q
    fi

    if [ "$FORCE_CLEAN_BUILD" = "true" ] || [ "$FORCE_REBUILD_CLASSES" = "true" ] || [ ! -f "target/classes/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.class" ]; then
        echo "Compiling Java classes..."
        mvn compile -q
    fi

    echo "Creating instrumented Java installation via process-resources phase..."
    mvn process-resources -q

    if [ $? -ne 0 ]; then
        echo "Error: Build failed!"
        exit 1
    fi
    echo "Build completed successfully with instrumentation"
else
    echo "Using existing build and instrumentation"
fi

# Verify instrumented Java exists
INSTRUMENTED_JAVA="target/galette/java"
if [ ! -f "$INSTRUMENTED_JAVA/bin/java" ]; then
    echo "Error: Instrumented Java not found at: $INSTRUMENTED_JAVA"
    echo "   Building now with process-resources phase..."
    mvn process-resources -q || {
        echo "Error: Failed to build instrumented Java"
        exit 1
    }
    if [ ! -f "$INSTRUMENTED_JAVA/bin/java" ]; then
        echo "Error: Instrumented Java creation failed"
        exit 1
    fi
    echo "Instrumented Java created successfully"
fi

# Find Galette agent JAR
GALETTE_AGENT=""
if [ -f "../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar" ]; then
    GALETTE_AGENT="../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"
elif [ -f "$HOME/.m2/repository/edu/neu/ccs/prl/galette/galette-agent/1.0.0-SNAPSHOT/galette-agent-1.0.0-SNAPSHOT.jar" ]; then
    GALETTE_AGENT="$HOME/.m2/repository/edu/neu/ccs/prl/galette/galette-agent/1.0.0-SNAPSHOT/galette-agent-1.0.0-SNAPSHOT.jar"
else
    echo "Error: Galette agent JAR not found!"
    echo "   Expected locations:"
    echo "   - ../galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"
    echo "   - ~/.m2/repository/edu/neu/ccs/prl/galette/galette-agent/1.0.0-SNAPSHOT/galette-agent-1.0.0-SNAPSHOT.jar"
    echo "   Run 'mvn install' in the parent galette directory"
    exit 1
fi

echo ""
echo "Configuration:"
echo "   Instrumented Java: $INSTRUMENTED_JAVA/bin/java"
echo "   Galette Agent: $GALETTE_AGENT"
echo "   Use Green Solver: $USE_GREEN_SOLVER"
echo "   External GreenServer: $USE_EXTERNAL_GREEN_SERVER (port $GREEN_SERVER_PORT)"

# Generate classpath using Maven (only if needed)
if [ ! -f cp.txt ] || [ $(find cp.txt -mmin +60 2>/dev/null | wc -l) -eq 1 ]; then
    echo "Generating classpath..."
    mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q

    if [ ! -f cp.txt ]; then
        echo "Error: Failed to generate classpath file!"
        exit 1
    fi
else
    echo "Using cached classpath (cp.txt)"
fi

# Create classpath with compiled classes and all dependencies
CP="target/classes:target/test-classes:$(cat cp.txt)"

echo "Using classpath with $(echo $CP | tr ':' '\n' | wc -l) entries"
echo ""

# Run with instrumented Java and Galette agent
echo "Running ModelTransformationExample with Galette instrumentation + External GreenServer..."
echo "   Expected: Constraints will be sent to external GreenServer for solving"
echo ""

# Create cache directory if it doesn't exist
mkdir -p target/galette/cache

# Build JVM arguments
OPTIONAL_JVM_ARGS=""
if [ "$USE_GREEN_SOLVER" = "true" ]; then
    OPTIONAL_JVM_ARGS="$OPTIONAL_JVM_ARGS -Dgalette.useGreenSolver=true"
    echo "   Green Solver: ENABLED"
fi
if [ "$USE_EXTERNAL_GREEN_SERVER" = "true" ]; then
    OPTIONAL_JVM_ARGS="$OPTIONAL_JVM_ARGS -Dgalette.useExternalGreenServer=true"
    OPTIONAL_JVM_ARGS="$OPTIONAL_JVM_ARGS -DSATPort=$GREEN_SERVER_PORT"
    echo "   External GreenServer: ENABLED (port $GREEN_SERVER_PORT)"
fi

# Run with automated input: option 2 (concolic execution), then 3 (exit)
# This tests the GreenServer integration automatically
echo -e "2\n3" | "$INSTRUMENTED_JAVA/bin/java" \
  -cp "$CP" \
  -Xbootclasspath/a:"$GALETTE_AGENT" \
  -javaagent:"$GALETTE_AGENT" \
  -Dgalette.cache=target/galette/cache \
  -Dgalette.coverage=true \
  -Dsymbolic.execution.debug=true \
  -Dgalette.debug=true \
  $OPTIONAL_JVM_ARGS \
  -verbose:javaagent \
  edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"

echo ""
echo "Execution completed"
echo "   GreenServer log available at: /tmp/greenserver.log"
