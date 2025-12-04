#!/bin/bash

# Script to rebuild instrumented Java with updated GaletteTransformer
# This ensures our modified GaletteTransformer is embedded in the instrumented Java

set -e  # Exit on any error

echo "🔧 Rebuilding Instrumented Java with Updated GaletteTransformer"
echo "=============================================================="
echo ""
echo "This script will:"
echo "1. Clean and rebuild galette-agent (contains our modified GaletteTransformer)"
echo "2. Clean and rebuild galette-instrument (uses galette-agent classes)"
echo "3. Delete existing instrumented Java"
echo "4. Create new instrumented Java with embedded updated classes"
echo ""

# Step 1: Clean and rebuild galette-agent
echo "📦 Step 1: Rebuilding galette-agent with our changes..."
cd ../galette-agent
mvn clean install -DskipTests -q
if [ $? -ne 0 ]; then
    echo "❌ Failed to build galette-agent!"
    exit 1
fi
echo "✅ galette-agent built and installed to local Maven repository"

# Step 2: Clean and rebuild galette-instrument
echo ""
echo "📦 Step 2: Rebuilding galette-instrument (depends on galette-agent)..."
cd ../galette-instrument
mvn clean install -DskipTests -q
if [ $? -ne 0 ]; then
    echo "❌ Failed to build galette-instrument!"
    exit 1
fi
echo "✅ galette-instrument built and installed to local Maven repository"

# Step 3: Delete existing instrumented Java
echo ""
echo "🗑️ Step 3: Deleting existing instrumented Java..."
cd ../knarr-runtime
if [ -d "target/galette/java" ]; then
    rm -rf target/galette/java
    echo "✅ Deleted existing instrumented Java"
else
    echo "⚠️ No existing instrumented Java found"
fi

# Also delete the cache to ensure fresh transformation
if [ -d "target/galette/cache" ]; then
    rm -rf target/galette/cache
    echo "✅ Deleted transformation cache"
fi

# Step 4: Rebuild instrumented Java
echo ""
echo "🔨 Step 4: Creating new instrumented Java with embedded updated classes..."
mvn process-test-resources -q
if [ $? -ne 0 ]; then
    echo "❌ Failed to create instrumented Java!"
    exit 1
fi

# Step 5: Build the knarr-runtime classes
echo ""
echo "📦 Step 5: Building knarr-runtime classes..."
mvn compile -q
if [ $? -ne 0 ]; then
    echo "❌ Failed to build knarr-runtime classes!"
    exit 1
fi
echo "✅ knarr-runtime classes compiled successfully"

echo ""
echo "✅ SUCCESS! Instrumented Java rebuilt with updated GaletteTransformer"
echo ""
echo "The instrumented Java at target/galette/java now contains:"
echo "- Our modified GaletteTransformer with debug output"
echo "- Hardcoded enabled ComparisonInterceptorVisitor"
echo "- Hardcoded enabled PathUtils with ThreadLocal initialization"
echo ""
echo "Next step: Run ./run-example.sh to test path constraint collection"