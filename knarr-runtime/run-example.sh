#!/bin/bash

# Script to run the ModelTransformationExample with proper classpath

# Set JAVA_HOME automatically if not set
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME not set, detecting automatically..."
    JAVA_HOME=$(readlink -f $(which java) | sed "s:bin/java::")
    export JAVA_HOME
    echo "Set JAVA_HOME to: $JAVA_HOME"
fi

echo "Building project..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Running ModelTransformationExample..."
echo ""

# Generate classpath using Maven
echo "Generating classpath..."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q

if [ ! -f cp.txt ]; then
    echo "Failed to generate classpath file!"
    exit 1
fi

# Create classpath with compiled classes and all dependencies
CP="target/classes:target/test-classes:$(cat cp.txt)"

echo "Running with classpath containing $(echo $CP | tr ':' '\n' | wc -l) entries..."

# Run the example
java -cp "$CP" edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"