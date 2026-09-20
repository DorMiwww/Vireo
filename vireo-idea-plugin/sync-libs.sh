#!/usr/bin/env bash
# Rebuilds vireo-core/vireo-lexer/vireo-parser/vireo-analysis and copies their
# jars into vireo-idea-plugin/libs/. Run this after changing any of those four
# modules, before building or running the plugin. See BLOCK.md for why this
# project depends on plain jars instead of live project(...) dependencies.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLUGIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$ROOT_DIR"
./gradlew :vireo-core:jar :vireo-lexer:jar :vireo-parser:jar :vireo-analysis:jar

mkdir -p "$PLUGIN_DIR/libs"
rm -f "$PLUGIN_DIR"/libs/*.jar
cp vireo-core/build/libs/*.jar "$PLUGIN_DIR/libs/"
cp vireo-lexer/build/libs/*.jar "$PLUGIN_DIR/libs/"
cp vireo-parser/build/libs/*.jar "$PLUGIN_DIR/libs/"
cp vireo-analysis/build/libs/*.jar "$PLUGIN_DIR/libs/"

echo "Synced jars into $PLUGIN_DIR/libs:"
ls -1 "$PLUGIN_DIR/libs"
