# Vireo IntelliJ IDEA Plugin

Editor support for `.dac` files in IntelliJ IDEA: file type + icon, syntax highlighting,
inline diagnostics, and keyword/property autocomplete.

Standalone Gradle project — **not** part of the root multi-module build. See
`../.agents/rules/BLOCK.md` ("Architecture Rules") for why: the IntelliJ Platform
Gradle Plugin 2.x requires Gradle 9.0+ and Kotlin 2.x, which conflicts with the
root build's Gradle 8.5 / Kotlin 1.9.22.

## What it reuses vs. what it reimplements

- Reuses `vireo-lexer` (tokenizing) and `vireo-analysis` (parsing + reference/type
  checking) directly, as plain jar dependencies — no logic is duplicated.
- The IntelliJ-side PSI is a deliberately flat tree (every token is a direct leaf
  of the file, no nested grammar). That's enough to back highlighting,
  diagnostics, and keyword/property completion — see `ROADMAP.md` Phase 7 for
  what's explicitly out of scope (go-to-definition on `file.block.component`
  refs, a live preview panel) and why.

## Building

```bash
# 1. From the repo root: rebuild vireo-core/lexer/parser/analysis and copy
#    their jars in (needed once, and again after changing any of those four):
./sync-libs.sh

# 2. Build the plugin (first run downloads the IntelliJ Platform SDK — large,
#    can take a while):
./gradlew build

# 3. Launch a sandbox IDE with the plugin installed:
./gradlew runIde
```

## Layout

```
vireo-idea-plugin/
  libs/                    vireo-core/lexer/parser/analysis jars (gitignored, see sync-libs.sh)
  src/main/kotlin/com/vireo/ideaplugin/
    DacLanguage.kt          Language definition
    DacFileType.kt          File type + icon registration
    DacIcons.kt             Icon loader
    DacTokenTypes.kt        vireo-lexer TokenType <-> IntelliJ IElementType bridge
    DacLexerAdapter.kt      Wraps Lexer.tokenize() as an IntelliJ Lexer
    DacSyntaxHighlighter.kt Token type -> editor color mapping
    DacParserDefinition.kt  Minimal (flat) PSI tree
    DacAnnotator.kt         Runs Parser + Analyzer in-process, reports VireoErrors
    DacCompletionContributor.kt / DacKeywords.kt   Keyword + property-key completion
  src/main/resources/
    META-INF/plugin.xml     Extension point registration
    icons/dac.svg           File icon
```
