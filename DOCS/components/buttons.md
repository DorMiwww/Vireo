# Buttons & Actions

Buttons trigger actions or navigation in an application. Vireo's showcase design system defines standard button styles with consistent height, padding, font weights, and border radii.

---

## Button Anatomy

A standard Vireo button consists of a container and an inner `Label` (and optional icon/indicator):

```dac
component PrimaryButton {
    height: 40
    padding: 0 16
    color: #2563EB
    radius: 8
    layout: horizontal
    alignItems: center
    justifyContent: center

    component Label {
        text: "Get Started"
        fontSize: 14
        fontWeight: bold
        color: #FFFFFF
    }
}
```

---

## Standard Variants

From `showcase/components/buttons.dac`:

### 1. Primary Button
Used for the principal call to action on a screen:
```dac
block Primary {
    component Default {
        height: 40
        padding: 0 16
        color: #2563EB
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

### 2. Secondary Button
Used for secondary or alternative actions:
```dac
block Secondary {
    component Default {
        height: 40
        padding: 0 16
        color: #F3F4F6
        border: 1 #E5E7EB
        radius: 8

        component Label {
            text: "Button"
            fontSize: 14
            fontWeight: bold
            color: #374151
        }
    }
}
```

### 3. Success / Confirm Button
Used for positive affirmations, purchases, or confirmations:
```dac
block Success {
    component Default {
        height: 40
        padding: 0 16
        color: #10B981
        radius: 8

        component Label {
            text: "Confirm"
            fontSize: 14
            fontWeight: bold
            color: #FFFFFF
        }
    }
}
```

### 4. Outline & Ghost Buttons
Used for subtle actions, toolbars, or tertiary options:
```dac
block Ghost {
    component Default {
        height: 40
        padding: 0 16
        radius: 8

        component Label {
            text: "Learn More"
            fontSize: 14
            fontWeight: medium
            color: #2563EB
        }
    }
}
```

---

## Sizes & Scaling

Buttons generally provide consistent sizes across variants:
- **Small (`Small`):** `height: 32`, `padding: 0 12`, `fontSize: 13`, `radius: 6`
- **Default (`Default`):** `height: 40`, `padding: 0 16`, `fontSize: 14`, `radius: 8`
- **Large (`Large`):** `height: 48`, `padding: 0 20`, `fontSize: 16`, `radius: 10`

---

## Using Buttons via `ref:`

```dac
import buttons from "./components/buttons.dac"

block Checkout {
    component SubmitButton {
        ref: buttons.Primary.Default
        width: fill

        component Label {
            text: "Complete Purchase"
        }
    }
}
```
