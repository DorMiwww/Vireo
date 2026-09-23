# Component Model

In Vireo, every visual element in an interface is a **Component**. Components are declared within named **Blocks** and can be nested to form arbitrary component trees.

---

## Anatomy of a Component

A component declaration consists of:
1. **Name:** A PascalCase identifier (e.g. `Button`, `UserProfile`, `NavigationRow`).
2. **Properties:** Key-value pairs configuring sizing, colors, typography, and borders.
3. **Constraints:** Rules defining how the component sizes and positions itself.
4. **Children:** Nested components that live inside this component.

```dac
block Navigation {
    component Navbar {
        // Properties & Constraints
        width: fill
        height: 64
        padding: 0 24
        color: #FFFFFF
        border: 1 #E5E7EB
        layout: horizontal
        alignItems: center
        justifyContent: "space-between"

        // Child components
        component BrandLogo {
            width: 120
            height: 32
            color: #2563EB
        }

        component Actions {
            layout: horizontal
            gap: 12
        }
    }
}
```

---

## Component Nesting

Nesting components creates a hierarchical DOM tree in HTML and frame groups in Figma:

```dac
component Card {
    width: 320
    layout: vertical
    gap: 12
    padding: 16
    color: #FFFFFF
    radius: 8

    component Header {
        layout: horizontal
        alignItems: center

        component Title {
            text: "Payment details"
            fontSize: 16
            fontWeight: bold
            color: #111827
        }
    }

    component Body {
        text: "Please review your transaction before confirming."
        fontSize: 14
        color: #6B7280
    }
}
```

---

## Built-In Component Types & Patterns

Vireo provides primitives and patterns for building production UI systems:

| Category | Reference | Description |
| :--- | :--- | :--- |
| **Properties** | [Properties Catalogue](./properties.md) | All valid properties, types, and values |
| **Buttons** | [Buttons & Actions](./buttons.md) | Interactive buttons with sizes and variants |
| **Badges** | [Badges & Indicators](./badges.md) | Status chips, notification pills, and tags |
| **Containers** | [Containers & Cards](./containers.md) | Surfaces, cards, headers, and flex containers |
| **Forms** | [Forms & Inputs](./forms.md) | Text inputs, placeholders, and form layouts |

---

## Component Lifecycle in Renderers

- **In HTML:** Non-text containers render as `<div>` elements (or `<a>` if `href:` is set). Components with `text:` or typography names render text content with appropriate styling.
- **In Figma:** Components map to native Figma `FRAME` layers with Auto Layout or explicit sizing, and nested typography maps to Figma `TEXT` layers with font loading.
- **In JSON:** Components serialize to structured AST nodes with all resolved properties and child arrays.
