# Gates: Refactor Phase 1 — vireo-core AST file split

OWNS: vireo-core/src/main/kotlin/com/vireo/core/**, .agents/rules/REFACTOR_PLAN.md, .agents/rules/CONTEXT.md

Scope: Split `vireo-core/src/main/kotlin/com/vireo/core/Ast.kt` (131 lines, 5 unrelated concerns in one file) into canonical per-concern files under `vireo-core/src/main/kotlin/com/vireo/core/ast/`, keeping every type in `package com.vireo.core` unchanged so no consumer module needs an import edit. Zero behavior change: same types, same fields, same package. Phase 1 of `.agents/rules/REFACTOR_PLAN.md`.

- [ ] G0: this ledger states outcomes that can fail
  CHECK: node .agents/skills/unlazy/scripts/gate-lint.mjs GATES.md
  EXPECT: LINT OK
  EVIDENCE: pending

- [ ] G1: vireo-core module compiles and its own test suite passes after the split
  CHECK: ./gradlew :vireo-core:test
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: pending

- [ ] G2: full repository test suite passes after the split (every downstream module still compiles against the moved types)
  CHECK: ./gradlew test
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: pending

- [ ] G3: canonical ast/ layout exists with the five split files
  CHECK: bash -c 'test -f vireo-core/src/main/kotlin/com/vireo/core/ast/VireoFile.kt && test -f vireo-core/src/main/kotlin/com/vireo/core/ast/Reference.kt && test -f vireo-core/src/main/kotlin/com/vireo/core/ast/Property.kt && test -f vireo-core/src/main/kotlin/com/vireo/core/ast/Constraint.kt && test -f vireo-core/src/main/kotlin/com/vireo/core/ast/Declarations.kt && echo LAYOUT_OK'
  EXPECT: LAYOUT_OK
  EVIDENCE: pending

- [ ] G4: original monolithic Ast.kt is gone (fully decomposed, not left as a duplicate)
  CHECK: bash -c 'test -f vireo-core/src/main/kotlin/com/vireo/core/Ast.kt && echo STILL_PRESENT || echo REMOVED'
  EXPECT: REMOVED
  EVIDENCE: pending

- [ ] G5: no java.io / java.net introduced into vireo-core (BLOCK.md: core stays platform-neutral)
  CHECK: bash -c 'grep -rE "java\.io|java\.net" vireo-core/src/main/kotlin/com/vireo/core || echo NO_JVM_IO'
  EXPECT: NO_JVM_IO
  EVIDENCE: pending

- [ ] G6: Phase 1 marked complete in the refactor plan
  CHECK: bash -c 'grep -q "Phase 1 — \`vireo-core\` \`\[complete\]\`" .agents/rules/REFACTOR_PLAN.md && echo PHASE1_MARKED'
  EXPECT: PHASE1_MARKED
  EVIDENCE: pending

- [ ] G7: work is committed, nothing left dangling in the working tree
  CHECK: bash -c '[ -z "$(git status --porcelain)" ] && echo CLEAN_TREE'
  EXPECT: CLEAN_TREE
  EVIDENCE: pending
