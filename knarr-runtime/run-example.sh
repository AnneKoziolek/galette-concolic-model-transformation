#!/bin/bash

# Script to run the ModelTransformationExample with proper classpath
# Optimized to avoid unnecessary recompilation

# Set JAVA_HOME automatically if not set
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME not set, detecting automatically..."
    JAVA_HOME=$(readlink -f $(which java) | sed "s:bin/java::")
    export JAVA_HOME
    echo "Set JAVA_HOME to: $JAVA_HOME"
fi

# Function to check if compilation is needed
needs_compilation() {
    local target_dir="target/classes"
    local main_class="$target_dir/edu/neu/ccs/prl/galette/examples/ModelTransformationExample.class"
    
    # If target directory doesn't exist or main class doesn't exist, need compilation
    if [ ! -d "$target_dir" ] || [ ! -f "$main_class" ]; then
        echo "Target directory or main class not found - compilation needed"
        return 0  # true - needs compilation
    fi
    
    # Check if main class was compiled within the last 5 minutes (300 seconds)
    local current_time=$(date +%s)
    local file_time=$(stat -c %Y "$main_class" 2>/dev/null || echo 0)
    local time_diff=$((current_time - file_time))
    
    if [ $time_diff -lt 300 ]; then
        echo "Main class compiled $time_diff seconds ago (< 5 minutes) - skipping compilation"
        echo "manual override... compile anyway"
        return 0  # true - no compilation needed
        #return 1  # false - no compilation needed
    fi
    
    # Check if any source files are newer than the compiled class
    local src_dir="src/main/java"
    if [ -d "$src_dir" ]; then
        local newest_src=$(find "$src_dir" -name "*.java" -newer "$main_class" | head -1)
        if [ -n "$newest_src" ]; then
            echo "Source file $newest_src is newer than compiled class - compilation needed"
            return 0  # true - needs compilation
        fi
    fi
    
    echo "Compiled classes are up-to-date (< 5 minutes old) - skipping compilation"
    echo "manual override... compile anyway"
    return 0  # false - no compilation needed
}

# Check if compilation is needed
if needs_compilation; then
    echo "Building project..."
    mvn compile -q
    
    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi
    echo "Build completed successfully"
else
    echo "Using existing compiled classes"
fi

echo ""
echo "Running ModelTransformationExample..."
echo ""

# Generate classpath using Maven (only if needed)
if [ ! -f cp.txt ] || [ $(find cp.txt -mmin +60 2>/dev/null | wc -l) -eq 1 ]; then
    echo "Generating classpath..."
    mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q
    
    if [ ! -f cp.txt ]; then
        echo "Failed to generate classpath file!"
        exit 1
    fi
else
    echo "Using cached classpath (cp.txt)"
fi

# Create classpath with compiled classes and all dependencies
CP="target/classes:target/test-classes:$(cat cp.txt)"

echo "Running with classpath containing $(echo $CP | tr ':' '\n' | wc -l) entries..."

# Run the example
java -cp "$CP" edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"