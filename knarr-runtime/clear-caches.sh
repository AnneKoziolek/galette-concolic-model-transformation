#!/bin/bash

# Script to clear all Galette caches and force clean rebuild

echo "🧹 Clearing all Galette caches and build artifacts..."

# Clear transformation cache
if [ -d "target/galette/cache" ]; then
    echo "🗑️ Removing transformation cache: target/galette/cache"
    rm -rf target/galette/cache
fi

# Clear instrumented Java
if [ -d "target/galette/java" ]; then
    echo "🗑️ Removing instrumented Java: target/galette/java"
    rm -rf target/galette/java
fi

# Clear Maven target in knarr-runtime
if [ -d "target" ]; then
    echo "🗑️ Cleaning knarr-runtime target directory"
    rm -rf target
fi

# Clear Maven target in galette-agent
if [ -d "../galette-agent/target" ]; then
    echo "🗑️ Cleaning galette-agent target directory"
    rm -rf ../galette-agent/target
fi

# Clear classpath cache
if [ -f "cp.txt" ]; then
    echo "🗑️ Removing cached classpath file"
    rm -f cp.txt
fi

echo "✅ All caches cleared!"
echo ""
echo "Next steps:"
echo "1. Run ./run-example.sh with FORCE_CLEAN_BUILD=true"
echo "2. This will rebuild everything from scratch"