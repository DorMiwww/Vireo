# Addressing & Component Composition

In large design systems, components must be shared and reused across multiple files. Vireo uses a Helm-inspired dot-notation addressing model:

```
<import-alias>.<block-name>.<component-name>
```

---

## Importing External Files

To use components from another `.dac` file, declare an `import` at the top of your file:

```dac
import buttons from "./components/buttons.dac"
import badges  from "./components/badges.dac"
```

- `buttons`: The local alias assigned to the imported file.
- `"./components/buttons.dac"`: Relative path to the `.dac` file on disk.

---

## Referencing Components with `ref:`

Inside any component, use the `ref:` property to inherit properties, constraints, and children from another component:

```dac
import buttons from "./components/buttons.dac"

block Checkout {
    component PayButton {
        ref: buttons.Primary.Default
        width: fill
        color: #10B981

        component Label {
            text: "Pay $49.00"
        }
    }
}
```

### How `ref:` Resolution Works

1. **Resolution:** The Vireo Analyzer resolves `buttons.Primary.Default` by locating the `buttons` import, opening the referenced file, finding block `Primary`, and extracting component `Default`.
2. **Inheritance & Merging:** All properties from `Default` are copied into `PayButton`.
3. **Property Overrides:** Any property declared directly inside `PayButton` (such as `color: #10B981` or `width: fill`) overrides the inherited value.
4. **Child Matching & Replacement:** Nested components with matching names (e.g. `Label`) override the base child component.

---

## Nested Addressing

Addressing works through nested components as well:

```dac
import cards from "./cards.dac"

block View {
    component HeaderPreview {
        ref: cards.Dashboard.OverviewCard.Header
    }
}
```

This deep addressing pattern enables design systems to expose atomic primitives and complex compound components from a single source of truth.

---

## Circular Dependency Prevention

The Vireo Analyzer statically scans import graphs before compilation. If file `A` imports `B` and `B` imports `A`, the compiler immediately halts with a descriptive `VireoError` indicating the circular dependency chain.

---

## Next Steps
- Learn how to define dynamic tokens in [Expressions & Variables](./expressions.md).
- Browse the [Component Model](../components/index.md).
