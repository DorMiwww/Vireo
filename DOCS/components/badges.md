# Badges & Status Indicators

Badges are compact components used to highlight status, counts, or categories in cards, tables, and headers.

---

## Badge Structure

A badge typically comprises a background surface with small padding, high border radius (pill shape), and an optional dot indicator:

```dac
component StatusBadge {
    padding: 4 10
    radius: 9999
    color: #ECFDF5
    border: 1 #A7F3D0
    layout: horizontal
    gap: 6
    alignItems: center

    component Dot {
        width: 6
        height: 6
        radius: 3
        color: #10B981
    }

    component Text {
        text: "Active"
        fontSize: 12
        fontWeight: medium
        color: #065F46
    }
}
```

---

## Standard Variants

From `showcase/components/badges.dac`:

### 1. Success (Green)
```dac
block Success {
    component Pill {
        padding: 4 12
        radius: 9999
        color: #ECFDF5
        border: 1 #A7F3D0

        component Label {
            text: "Published"
            fontSize: 12
            fontWeight: bold
            color: #065F46
        }
    }
}
```

### 2. Warning (Amber)
```dac
block Warning {
    component Pill {
        padding: 4 12
        radius: 9999
        color: #FFFBEB
        border: 1 #FDE68A

        component Label {
            text: "Pending Review"
            fontSize: 12
            fontWeight: bold
            color: #92400E
        }
    }
}
```

### 3. Danger (Red)
```dac
block Danger {
    component Pill {
        padding: 4 12
        radius: 9999
        color: #FEF2F2
        border: 1 #FECACA

        component Label {
            text: "Failed"
            fontSize: 12
            fontWeight: bold
            color: #991B1B
        }
    }
}
```

### 4. Info (Blue) / Neutral (Gray)
```dac
block Info {
    component Pill {
        padding: 4 12
        radius: 9999
        color: #EFF6FF
        border: 1 #BFDBFE

        component Label {
            text: "New"
            fontSize: 12
            fontWeight: bold
            color: #1E40AF
        }
    }
}
```

---

## Composition Example

```dac
import badges from "./components/badges.dac"

component PlanCard {
    layout: horizontal
    justifyContent: "space-between"

    component Title {
        text: "Enterprise Plan"
    }

    component PopularBadge {
        ref: badges.Info.Pill

        component Label {
            text: "Most Popular"
        }
    }
}
```
