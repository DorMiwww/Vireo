# Layout System & Constraints

Vireo features a hybrid layout engine that unites **Auto Layout** (inspired by modern flexbox and Figma auto layout), **Explicit Geometry**, and **Relational Constraints**.

---

## 1. Auto Layout

Auto Layout arranges children sequentially along a primary axis and calculates positions automatically.

### Direction (`layout`)
- `layout: vertical`: Stacks child components top-to-bottom (CSS `flex-direction: column`).
- `layout: horizontal`: Lines up child components left-to-right (CSS `flex-direction: row`).

```dac
component Toolbar {
    layout: horizontal
    gap: 12
    alignItems: center
}
```

### Spacing & Padding
- `gap: <number>`: Distance in pixels between consecutive children.
- `padding: <uniform>` or `padding: <top-bottom> <left-right>`: Internal space around children.

```dac
component Sidebar {
    layout: vertical
    gap: 16
    padding: 24 16
}
```

### Alignment & Justification
- `alignItems: "center" | "flex-start" | "flex-end" | "stretch"`: Alignment along the cross-axis.
- `justifyContent: "center" | "flex-start" | "flex-end" | "space-between"`: Distribution along the main-axis.

---

## 2. Dynamic Sizing Modes

Children inside an Auto Layout container can declare dynamic sizing:

| Sizing Value | Behavior | Equivalent CSS | Figma Mapping |
| :--- | :--- | :--- | :--- |
| `fill` | Stretches to occupy available parent space | `flex-grow: 1; width/height: 100%` | Fill container |
| `hug` | Sizes strictly to wrap inner children | `width/height: fit-content` | Hug contents |
| `<number>` | Fixed dimension in pixels | `width/height: <n>px` | Fixed dimension |

```dac
component HeaderRow {
    layout: horizontal
    width: fill
    gap: 16

    component Logo {
        width: 140   // Fixed width
    }

    component SearchBar {
        width: fill  // Expands to take all remaining row width
    }

    component ProfilePill {
        width: hug   // Hugs text inside
    }
}
```

---

## 3. Explicit Positioning

When precise coordinates are needed (e.g. badges floating over an avatar or absolute overlays), specify explicit coordinates:

```dac
component AvatarContainer {
    width: 64
    height: 64

    component AvatarImage {
        width: 64
        height: 64
        radius: 32
        color: #3B82F6
    }

    component OnlineIndicator {
        width: 14
        height: 14
        radius: 7
        color: #10B981
        border: 2 #FFFFFF
        x: 48
        y: 48
    }
}
```
Components with `x` and `y` properties render as `position: absolute; left: 48px; top: 48px;`.

---

## 4. Relational Constraints

Relational constraints allow defining dimensions relative to parent or sibling nodes:

```dac
component SplitView {
    component LeftPane {
        width: 50%parent
    }

    component RightPane {
        width: 50%parent
    }
}
```

Relational offsets can also compute dynamic relative coordinates:
```dac
component FloatingCallout {
    x: parent.x + 24
    width: 80%parent
}
```
