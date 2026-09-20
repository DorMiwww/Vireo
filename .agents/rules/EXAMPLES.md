# Vireo — `.dac` Examples

Concrete examples of valid `.dac` files at various levels of complexity.
These examples drive syntax decisions — if something is hard to write as an example, the syntax is probably wrong.

> **Note:** Syntax is still being designed. Examples marked `[proposal]` use proposed syntax that is not yet finalized.

---

## 1. Hello Rectangle `[proposal]`

The simplest possible `.dac` file — one block, one component, explicit sizing.

```dac
block Main {
    component Box {
        width: 200
        height: 100
        color: #3B82F6
        radius: 8
    }
}
```

**What this renders to (JSON):**
```json
{
  "blocks": [{
    "name": "Main",
    "components": [{
      "name": "Box",
      "properties": {
        "width": 200,
        "height": 100,
        "color": "#3B82F6",
        "radius": 8
      }
    }]
  }]
}
```

---

## 2. Button Component `[proposal]`

A reusable button with text and styling.

```dac
block Primary {
    component Default {
        width: 120
        height: 40
        color: #3B82F6
        radius: 8

        component Label {
            text: "Button"
            fontSize: 14
            fontWeight: bold
            color: #FFFFFF
        }
    }
}
```

**File:** `buttons.dac`
**Address:** `buttons.Primary.Default`

---

## 3. Auto Layout — Card `[proposal]`

A card that stacks children vertically using the constraint system.

```dac
block Cards {
    component Card {
        layout: vertical
        mainAxis: hug
        crossAxis: fill
        gap: 12
        padding: 16
        color: #FFFFFF
        radius: 12
        shadow: true

        component Title {
            text: "Card Title"
            fontSize: 18
            fontWeight: bold
            color: #111827
        }

        component Description {
            text: "Card description goes here."
            fontSize: 14
            color: #6B7280
        }
    }
}
```

---

## 4. Cross-file Reference `[proposal]`

A login form that reuses components from other files.

**`buttons.dac`:**
```dac
block Primary {
    component Default {
        width: 200
        height: 44
        color: #3B82F6
        radius: 8

        component Label {
            text: "Button"
            fontSize: 14
            fontWeight: bold
            color: #FFFFFF
        }
    }
}
```

**`login.dac`:**
```dac
import buttons from "./buttons.dac"

block LoginForm {
    component Container {
        layout: vertical
        gap: 16
        padding: 24
        width: 360

        component Title {
            text: "Sign In"
            fontSize: 24
            fontWeight: bold
            color: #111827
        }

        component EmailField {
            width: fill
            height: 44
            radius: 8
            border: 1 #D1D5DB
            placeholder: "Email"
        }

        component PasswordField {
            width: fill
            height: 44
            radius: 8
            border: 1 #D1D5DB
            placeholder: "Password"
        }

        component SubmitButton {
            ref: buttons.Primary.Default
            label: "Sign In"
            width: fill
        }
    }
}
```

**Address of the submit button from outside:** `login.LoginForm.SubmitButton`

---

## 5. Variables `[proposal]`

Shared tokens extracted as variables.

```dac
var primaryColor = #3B82F6
var textPrimary  = #111827
var textMuted    = #6B7280
var radiusBase   = 8
var spacingUnit  = 8

block Tokens {
    component Badge {
        color: $primaryColor
        radius: $radiusBase
        padding: $spacingUnit
        
        component Text {
            text: "New"
            fontSize: 12
            color: #FFFFFF
        }
    }
}
```

---

## 6. Relational Constraints `[proposal]`

Components that size relative to their parent.

```dac
block Layouts {
    component SplitPanel {
        layout: horizontal
        width: 800
        height: 600

        component Left {
            width: 50%parent
            height: fill
            color: #F9FAFB
        }

        component Right {
            width: 50%parent
            height: fill
            color: #FFFFFF
        }
    }
}
```

---

## Roadmap

| Phase | Example Goals |
|-------|--------------|
| **MVP** | Examples 1–2 render correctly to JSON and HTML |
| **v0.2** | Examples 3–4 work (layout + cross-file refs) |
| **v0.3** | Examples 5–6 work (variables + relational constraints) |
| **v1.0** | All examples render to Figma |