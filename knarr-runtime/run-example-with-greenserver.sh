#!/bin/bash

# Script to run the ModelTransformationExample with an EXTERNAL GreenServer
# process for constraint solving. This avoids Galette instrumenting the
# Green/Z3 bytecode by running the solver in a separate non-instrumented JVM.
#
# Configuration Options (see below):
#   USE_INSTRUMENTED_JAVA - Enable/disable Galette bytecode instrumentation
#   USE_GREEN_SOLVER      - Enable/disable Green constraint solver
#   USE_EXTERNAL_GREEN_SERVER - Use external GreenServer (recommended)
#
# Usage:
#   ./run-example-with-greenserver.sh              # Local (Anne's machine)
#   ./run-example-with-greenserver.sh --codespaces # GitHub Codespaces

set -e  # Exit on any error

# ============================================================================
# Environment Detection: --codespaces flag or auto-detect
# ============================================================================
CODESPACES_MODE=false
for arg in "$@"; do
    if [ "$arg" = "--codespaces" ]; then
        CODESPACES_MODE=true
        # Remove --codespaces from args so it doesn't get passed to the Java app
        set -- "${@/--codespaces/}"
        break
    fi
done

# Auto-detect Codespaces if not explicitly set
if [ "$CODESPACES_MODE" = "false" ] && [ -n "$CODESPACE_NAME" ]; then
    echo "Auto-detected GitHub Codespaces environment"
    CODESPACES_MODE=true
fi

# ============================================================================
# Path Configuration (environment-dependent)
# ============================================================================
if [ "$CODESPACES_MODE" = "true" ]; then
    echo "Running in Codespaces mode"
    # Codespaces: project is under /workspaces/research-agent-workspace/workspaces/projects/
    WORKSPACE_ROOT="/workspaces/research-agent-workspace/workspaces/projects"
    GREEN_SOLVER_ROOT="$WORKSPACE_ROOT/green-solver"

    # Java 17 via SDKMAN in Codespaces
    if [ -d "/usr/local/sdkman/candidates/java/current" ]; then
        export JAVA_HOME="/usr/local/sdkman/candidates/java/current"
    elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    fi
    export PATH="$JAVA_HOME/bin:$PATH"

    # Java 21 for GreenServer — install if needed
    GREEN_SERVER_JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    if [ ! -d "$GREEN_SERVER_JAVA_HOME" ]; then
        echo "Installing Java 21 for GreenServer (one-time setup)..."
        sudo apt-get update -qq && sudo apt-get install -y -qq openjdk-21-jdk-headless > /dev/null 2>&1
        echo "   Java 21 installed"
    fi
else
    # Local (Anne's machine): original paths
    WORKSPACE_ROOT="/home/anne/CocoPath"
    GREEN_SOLVER_ROOT="$WORKSPACE_ROOT/green-solver"
    export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    export PATH="$JAVA_HOME/bin:$PATH"
    GREEN_SERVER_JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi

echo "GreenServer + Galette Knarr Runtime Example"
echo "============================================"
echo "Java Configuration:"
echo "   JAVA_HOME: $JAVA_HOME"
echo "   Java version: $(java -version 2>&1 | head -1)"
if [ "$CODESPACES_MODE" = "true" ]; then
    echo "   Environment: GitHub Codespaces"
    echo "   Workspace root: $WORKSPACE_ROOT"
fi
echo ""

# ============================================================================
# Build Configuration Constants
# ============================================================================
FORCE_CLEAN_BUILD=true        # Force complete clean rebuild (overrides everything)
FORCE_REBUILD_AGENT=false      # Force rebuild galette-agent JAR only
FORCE_REBUILD_CLASSES=false    # Force rebuild knarr-runtime Java classes only (intelligent detection by default)
FORCE_REBUILD_JAVA=false       # Force rebuild instrumented Java installation only (intelligent detection by default)

# ============================================================================
# Runtime Configuration
# ============================================================================
USE_INSTRUMENTED_JAVA=true     # Use Galette-instrumented Java for constraint collection via comparison interception
                               # When true: Uses target/galette/java (generates if missing), enables -javaagent
                               # When false: Uses regular JAVA_HOME, no bytecode instrumentation
USE_GREEN_SOLVER=true          # Enable Green solver integration
USE_EXTERNAL_GREEN_SERVER=true # Use external GreenServer process

# ============================================================================
# GreenServer Configuration
# ============================================================================
GREEN_SERVER_PORT=9408
GREEN_SERVER_DIR="$GREEN_SOLVER_ROOT/greenserver"
GREEN_SERVER_GREEN_LIB="$GREEN_SOLVER_ROOT/green/lib"
GREEN_SERVER_PID=""
# Z3-turnkey from Maven (includes native libraries for Linux)
Z3_TURNKEY_JAR="$HOME/.m2/repository/io/github/tudo-aqua/z3-turnkey/4.8.14/z3-turnkey-4.8.14.jar"

# Auto-download z3-turnkey if missing (needed in Codespaces / fresh environments)
if [ ! -f "$Z3_TURNKEY_JAR" ]; then
    echo "Z3-turnkey JAR not found — resolving via Maven..."
    if [ -f "$GREEN_SOLVER_ROOT/green/pom.xml" ]; then
        (cd "$GREEN_SOLVER_ROOT/green" && mvn dependency:resolve -DincludeArtifactIds=z3-turnkey -q 2>&1) || true
    fi
    if [ ! -f "$Z3_TURNKEY_JAR" ]; then
        # Direct download fallback
        echo "   Maven resolve failed — downloading z3-turnkey directly..."
        mkdir -p "$(dirname "$Z3_TURNKEY_JAR")"
        curl -sL "https://repo1.maven.org/maven2/io/github/tudo-aqua/z3-turnkey/4.8.14/z3-turnkey-4.8.14.jar" \
            -o "$Z3_TURNKEY_JAR" 2>/dev/null || true
    fi
    if [ -f "$Z3_TURNKEY_JAR" ]; then
        echo "   Z3-turnkey JAR resolved successfully"
    else
        echo "   Warning: Could not obtain z3-turnkey JAR — GreenServer may fail to start"
    fi
fi

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

    # Find green.jar: prefer Maven repo, fall back to greenserver/lib, then build from source
    local GREEN_JAR=""
    local MAVEN_GREEN_JAR="$HOME/.m2/repository/za/ac/sun/cs/green/green/1.0-SNAPSHOT/green-1.0-SNAPSHOT.jar"
    local MAVEN_GREEN_JAR_ALT="$HOME/.m2/repository/edu/gmu/swe/greensolver/green/1.0-SNAPSHOT/green-1.0-SNAPSHOT.jar"
    local LOCAL_GREEN_JAR="$GREEN_SERVER_DIR/lib/green.jar"

    if [ -f "$MAVEN_GREEN_JAR" ]; then
        GREEN_JAR="$MAVEN_GREEN_JAR"
    elif [ -f "$MAVEN_GREEN_JAR_ALT" ]; then
        GREEN_JAR="$MAVEN_GREEN_JAR_ALT"
    elif [ -f "$LOCAL_GREEN_JAR" ]; then
        GREEN_JAR="$LOCAL_GREEN_JAR"
        echo "   Using local green.jar from $LOCAL_GREEN_JAR"
    else
        # Try to build green from source and install to Maven repo
        echo "   green.jar not found in Maven repo — building from source..."
        if [ -f "$GREEN_SOLVER_ROOT/green/pom.xml" ]; then
            (cd "$GREEN_SOLVER_ROOT/green" && mvn install -DskipTests -q 2>&1) || {
                echo "   Error: Failed to build green from source"
                cd "$original_dir"
                return 1
            }
            if [ -f "$MAVEN_GREEN_JAR_ALT" ]; then
                GREEN_JAR="$MAVEN_GREEN_JAR_ALT"
            elif [ -f "$MAVEN_GREEN_JAR" ]; then
                GREEN_JAR="$MAVEN_GREEN_JAR"
            fi
        fi
    fi

    if [ -z "$GREEN_JAR" ]; then
        echo "   Error: Cannot find or build green.jar"
        cd "$original_dir"
        return 1
    fi

    echo "   Using green.jar: $GREEN_JAR"
    mkdir -p bin
    "$GREEN_SERVER_JAVA_HOME/bin/javac" -cp "$GREEN_JAR" -d bin \
        src/za/ac/sun/cs/green/server/GreenServer.java 2>&1
    local result=$?
    cd "$original_dir"

    if [ $result -eq 0 ]; then
        echo "   GreenServer built successfully"
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

    # Check if GreenServer is built (look for actual .class files, not just directories)
    if ! find "$GREEN_SERVER_DIR/bin" -name "*.class" 2>/dev/null | grep -q .; then
        build_green_server || return 1
    fi

    # Start GreenServer with NON-INSTRUMENTED Java 21 (critical!)
    # GreenServer requires Java 21 due to green.jar class version
    local original_dir=$(pwd)
    cd "$GREEN_SERVER_DIR"

    # Build classpath with Z3-turnkey (includes native libs) and other dependencies
    # Find green.jar (same logic as build_green_server)
    local GREEN_JAR=""
    local MAVEN_GREEN_JAR="$HOME/.m2/repository/za/ac/sun/cs/green/green/1.0-SNAPSHOT/green-1.0-SNAPSHOT.jar"
    local MAVEN_GREEN_JAR_ALT="$HOME/.m2/repository/edu/gmu/swe/greensolver/green/1.0-SNAPSHOT/green-1.0-SNAPSHOT.jar"
    local LOCAL_GREEN_JAR="$GREEN_SERVER_DIR/lib/green.jar"
    if [ -f "$MAVEN_GREEN_JAR" ]; then GREEN_JAR="$MAVEN_GREEN_JAR";
    elif [ -f "$MAVEN_GREEN_JAR_ALT" ]; then GREEN_JAR="$MAVEN_GREEN_JAR_ALT";
    elif [ -f "$LOCAL_GREEN_JAR" ]; then GREEN_JAR="$LOCAL_GREEN_JAR";
    fi
    local GREEN_CP="bin:$GREEN_JAR:$Z3_TURNKEY_JAR"

    # Find SLF4J jars — check greenserver lib, then green lib, then Maven repo
    local SLF4J_API="" SLF4J_SIMPLE=""
    for dir in "$GREEN_SERVER_GREEN_LIB" "$GREEN_SOLVER_ROOT/green/lib"; do
        [ -z "$SLF4J_API" ] && [ -f "$dir/slf4j-api-1.7.12.jar" ] && SLF4J_API="$dir/slf4j-api-1.7.12.jar"
        [ -z "$SLF4J_SIMPLE" ] && [ -f "$dir/slf4j-simple-1.7.12.jar" ] && SLF4J_SIMPLE="$dir/slf4j-simple-1.7.12.jar"
    done
    # Fall back to any SLF4J in Maven repo
    [ -z "$SLF4J_API" ] && SLF4J_API=$(find "$HOME/.m2/repository/org/slf4j/slf4j-api" -name "slf4j-api-*.jar" 2>/dev/null | head -1)
    [ -z "$SLF4J_SIMPLE" ] && SLF4J_SIMPLE=$(find "$HOME/.m2/repository/org/slf4j/slf4j-simple" -name "slf4j-simple-*.jar" 2>/dev/null | head -1)
    [ -n "$SLF4J_API" ] && GREEN_CP="$GREEN_CP:$SLF4J_API"
    [ -n "$SLF4J_SIMPLE" ] && GREEN_CP="$GREEN_CP:$SLF4J_SIMPLE"

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

    if [ "$FORCE_REBUILD_JAVA" = "true" ] && [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
        echo "FORCE_REBUILD_JAVA enabled - rebuilding instrumented Java"
        return 0
    fi

    local target_dir="target/classes"
    local galette_java="target/galette/java"
    local main_class="$target_dir/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.class"

    # Check if main class exists
    if [ ! -d "$target_dir" ] || [ ! -f "$main_class" ]; then
        echo "Target directory or main class not found - build needed"
        return 0
    fi

    # Only check for instrumented Java if USE_INSTRUMENTED_JAVA is enabled
    if [ "$USE_INSTRUMENTED_JAVA" = "true" ] && [ ! -d "$galette_java" ]; then
        echo "Instrumented Java not found - build needed"
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

    if [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
        echo "Build is up-to-date - using existing compiled classes and instrumented Java"
    else
        echo "Build is up-to-date - using existing compiled classes (no instrumentation)"
    fi
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

# Rebuild galette modules if requested (both galette-agent AND galette-instrument)
if [ "$FORCE_CLEAN_BUILD" = "true" ] || [ "$FORCE_REBUILD_AGENT" = "true" ]; then
    echo "Rebuilding all galette modules from parent project..."
    if [ "$CODESPACES_MODE" = "true" ]; then
        # In Codespaces: skip source plugin (duplicate execution issue) and use install
        (cd .. && mvn clean install -pl galette-agent,galette-instrument -DskipTests -Dmaven.source.skip=true -q)
    else
        (cd .. && mvn clean install -pl galette-agent,galette-instrument -DskipTests -q)
    fi
    if [ $? -ne 0 ]; then
        echo "Error: Failed to rebuild galette modules!"
        exit 1
    fi
    echo "galette-agent and galette-instrument rebuilt successfully"
    echo ""
fi

# Build project (with or without instrumentation)
if needs_build; then
    if [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
        echo "Building project with Galette instrumentation..."
    else
        echo "Building project (no instrumentation)..."
    fi

    if [ "$FORCE_CLEAN_BUILD" = "true" ]; then
        echo "FORCE_CLEAN_BUILD enabled - removing all build artifacts"
        clean_instrumented_java "target/galette/java" "$CLEANUP_STRATEGY"
        echo "Cleaning Galette cache directory..."
        rm -rf target/galette/cache
        echo "Cleaning Maven target directory..."
        rm -rf target
    elif [ "$FORCE_REBUILD_JAVA" = "true" ] && [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
        echo "FORCE_REBUILD_JAVA enabled - removing only instrumented Java"
        clean_instrumented_java "target/galette/java" "$CLEANUP_STRATEGY"
    else
        echo "Cleaning Maven target directory..."
        rm -rf target
    fi

    if [ "$FORCE_CLEAN_BUILD" = "true" ] || [ "$FORCE_REBUILD_CLASSES" = "true" ] || [ ! -f "target/classes/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.class" ]; then
        echo "Compiling Java classes..."
        if [ "$CODESPACES_MODE" = "true" ]; then
            # In Codespaces: use compiler:compile to skip the galette instrument phase
            # (jlink instrumentation requires a specific JDK setup)
            mvn compiler:compile -q
        else
            mvn compile -q
        fi
    fi

    # Only create instrumented Java if USE_INSTRUMENTED_JAVA is enabled
    if [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
        # Clean any leftover partial instrumented Java (jlink fails if dir exists)
        rm -rf target/galette/java
        echo "Creating instrumented Java installation via process-resources phase..."
        if mvn process-resources -q; then
            echo "Build completed successfully with instrumentation"
        else
            echo "Warning: Instrumented Java creation failed."
            echo "   Falling back to non-instrumented execution (Galette agent still active)."
            USE_INSTRUMENTED_JAVA=false
        fi
    else
        echo "Build completed (no instrumentation)"
    fi
else
    if [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
        echo "Using existing build and instrumentation"
    else
        echo "Using existing build (no instrumentation)"
    fi
fi

# Determine which Java to use
INSTRUMENTED_JAVA="target/galette/java"
GALETTE_AGENT=""
JAVA_EXECUTABLE=""

if [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
    # Verify instrumented Java exists
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
    JAVA_EXECUTABLE="$INSTRUMENTED_JAVA/bin/java"

    # Find Galette agent JAR
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
else
    # Use regular (non-instrumented) Java
    JAVA_EXECUTABLE="$JAVA_HOME/bin/java"
fi

echo ""
echo "Configuration:"
echo "   Use Instrumented Java: $USE_INSTRUMENTED_JAVA"
if [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
    echo "   Java Executable: $JAVA_EXECUTABLE (instrumented)"
    echo "   Galette Agent: $GALETTE_AGENT"
else
    echo "   Java Executable: $JAVA_EXECUTABLE (regular)"
fi
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

# Display run mode
if [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
    echo "Running ModelTransformationExample with Galette instrumentation + External GreenServer..."
    echo "   Expected: Constraints collected via comparison interception and sent to GreenServer"
else
    echo "Running ModelTransformationExample WITHOUT instrumentation + External GreenServer..."
    echo "   Expected: No constraint collection (bytecode not instrumented)"
fi
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
if [ "$USE_INSTRUMENTED_JAVA" = "true" ]; then
    # Run with instrumented Java and Galette agent for constraint collection
    echo -e "2\n3" | "$JAVA_EXECUTABLE" \
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
else
    # Run with regular Java (no instrumentation)
    echo -e "2\n3" | "$JAVA_EXECUTABLE" \
      -cp "$CP" \
      -Dgalette.cache=target/galette/cache \
      -Dgalette.coverage=true \
      -Dsymbolic.execution.debug=true \
      -Dgalette.debug=true \
      $OPTIONAL_JVM_ARGS \
      edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"
fi

echo ""
echo "Execution completed"
echo "   GreenServer log available at: /tmp/greenserver.log"
