#!/bin/bash

# Simple test to verify variable name preservation without instrumentation

echo "Testing variable name preservation..."

# Kill any existing GreenServer
pkill -f GreenServer 2>/dev/null

# Start GreenServer in background
echo "Starting GreenServer..."
java -cp target/classes:$(find ~/.m2/repository -name "green-1.0-SNAPSHOT.jar" | head -1) \
  edu.neu.ccs.prl.galette.concolic.knarr.runtime.GreenServer &
GREEN_PID=$!

sleep 2

# Run a simple test without instrumentation
java -cp target/classes \
  -Dgalette.useGreenServer=true \
  -DDEBUG=true \
  edu.neu.ccs.prl.galette.examples.ModelTransformationExample <<< "2"

# Kill GreenServer
kill $GREEN_PID 2>/dev/null

echo "Test complete"