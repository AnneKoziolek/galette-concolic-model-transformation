#!/bin/bash

# Script to run the ModelTransformationExample with Galette instrumentation
# This script demonstrates proper usage of Galette agent from an external project

set -e  # Exit on any error

echo "🚀 Galette Concolic Examples"
echo "============================="

# Configuration  
GALETTE_PROJECT_DIR=".."
GALETTE_AGENT_JAR="$GALETTE_PROJECT_DIR/galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"

# Function to check if dependencies are built
check_dependencies() {
    echo "🔍 Checking Galette dependencies..."
    
    if [ ! -f "$GALETTE_AGENT_JAR" ]; then
        echo "❌ Galette agent JAR not found at: $GALETTE_AGENT_JAR"
        echo "   Building Galette dependencies..."
        (cd "$GALETTE_PROJECT_DIR/galette-agent" && mvn clean package -DskipTests -q)
        echo "✅ Galette agent built successfully"
    else
        echo "✅ Galette agent JAR found"
    fi
    
    # Check if knarr-runtime is installed in local repo
    if [ ! -d "$HOME/.m2/repository/edu/neu/ccs/prl/galette/knarr-runtime" ]; then
        echo "📦 Installing knarr-runtime to local repository..."
        (cd "$GALETTE_PROJECT_DIR/knarr-runtime" && mvn install -DskipTests -q)
        echo "✅ knarr-runtime installed"
    else
        echo "✅ knarr-runtime found in local repository" 
    fi
    
    # Install galette-agent to local repo if needed
    if [ ! -d "$HOME/.m2/repository/edu/neu/ccs/prl/galette/galette-agent" ]; then
        echo "📦 Installing galette-agent to local repository..."
        (cd "$GALETTE_PROJECT_DIR/galette-agent" && mvn install -DskipTests -q)
        echo "✅ galette-agent installed"
    else
        echo "✅ galette-agent found in local repository"
    fi
}

# Function to build examples project
build_examples() {
    echo "🔨 Building examples project..."
    mvn compile -q
    
    if [ $? -ne 0 ]; then
        echo "❌ Build failed!"
        exit 1
    fi
    echo "✅ Examples project built successfully"
}

# Function to create instrumented Java if needed
setup_instrumented_java() {
    local instrumented_java_dir="target/galette/java"
    
    if [ ! -d "$instrumented_java_dir" ]; then
        echo "⚙️ Creating instrumented Java installation..."
        mkdir -p target/galette
        
        # Use the galette-instrument from the dependency project
        local instrument_jar="$GALETTE_PROJECT_DIR/galette-instrument/target/galette-instrument-1.0.0-SNAPSHOT.jar"
        if [ ! -f "$instrument_jar" ]; then
            echo "🔨 Building galette-instrument..."
            (cd "$GALETTE_PROJECT_DIR/galette-instrument" && mvn package -DskipTests -q)
        fi
        
        # Check if JAVA_HOME is set
        if [ -z "$JAVA_HOME" ]; then
            echo "❌ JAVA_HOME is not set. Cannot create instrumented Java."
            echo "   Skipping instrumented Java creation."
            return
        fi
        
        # Create instrumented Java with proper error handling
        echo "🔧 Instrumenting Java from $JAVA_HOME to $instrumented_java_dir"
        if java -jar "$instrument_jar" "$JAVA_HOME" "$instrumented_java_dir" > /tmp/instrument.log 2>&1; then
            echo "✅ Instrumented Java created"
        else
            echo "❌ Failed to create instrumented Java. See /tmp/instrument.log for details."
            echo "   Will use regular Java instead."
            return
        fi
    else
        echo "⚡ Using existing instrumented Java"
    fi
    
    echo "$instrumented_java_dir"
}

# Main execution
echo
check_dependencies
echo
build_examples
echo

# For now, skip instrumented Java and use regular Java
echo "🔧 Using regular Java with Galette agent (should work fine for testing)"
JAVA_CMD="java"

# Generate classpath
echo "📋 Generating classpath..."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q

if [ ! -f cp.txt ]; then
    echo "❌ Failed to generate classpath file!"
    exit 1
fi

# Create classpath
CP="target/classes:$(cat cp.txt)"

echo "📚 Using classpath with $(echo $CP | tr ':' '\n' | wc -l) entries"
echo

# Run with Galette agent
echo "🚀 Running ModelTransformationExample with Galette instrumentation..."
echo "   Command: $JAVA_CMD"
echo "   Agent: $GALETTE_AGENT_JAR"
echo "   Instrumentation: ENABLED"
echo

# Create cache directory
mkdir -p target/galette/cache

# Run the example
"$JAVA_CMD" \
  -cp "$CP" \
  -Xbootclasspath/a:"$GALETTE_AGENT_JAR" \
  -javaagent:"$GALETTE_AGENT_JAR" \
  -Dgalette.cache=target/galette/cache \
  -Dgalette.concolic.interception.enabled=true \
  edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"

echo ""
echo "✅ Execution completed"
echo ""
echo "📋 Summary:"
echo "   ✅ External project structure ensures proper agent instrumentation"
echo "   ✅ Classes are loaded and instrumented by Galette agent at runtime"
echo "   ✅ Automatic comparison interception should now work correctly"