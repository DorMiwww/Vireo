# Containers & Cards

Containers provide visual structure, elevation, borders, and layout flow for surrounding content.

---

## 1. Cards

Cards are elevated surface containers that group related actions and information:

```dac
component FeatureCard {
    width: 340
    padding: 24
    radius: 16
    color: #FFFFFF
    border: 1 #E5E7EB
    shadow: true
    layout: vertical
    gap: 16

    component IconWrapper {
        width: 44
        height: 44
        radius: 10
        color: #EFF6FF
        layout: horizontal
        alignItems: center
        justifyContent: center
    }

    component Content {
        layout: vertical
        gap: 8

        component Heading {
            text: "Instant Compilation"
            fontSize: 18
            fontWeight: bold
            color: #111827
        }

        component Description {
            text: "Compile .dac design specifications directly into clean HTML and Figma components."
            fontSize: 14
            color: #6B7280
        }
    }
}
```

---

## 2. Sections & Rows

Sections create responsive full-width bands or bounded grids:

```dac
component PricingSection {
    width: 1024
    padding: 48 24
    layout: vertical
    gap: 32
    alignItems: center

    component SectionHeader {
        layout: vertical
        gap: 8
        alignItems: center

        component Title {
            text: "Plans that scale with your team"
            fontSize: 32
            fontWeight: bold
            color: #111827
        }
    }

    component CardGrid {
        layout: horizontal
        gap: 24
        width: fill
    }
}
```

---

## 3. Flex Stretching with `width: fill`

When a container has `layout: horizontal` or `layout: vertical`, its children can participate in dynamic distribution:

- `width: fill`: Fills all remaining cross-axis or main-axis width (`flex: 1` / `width: 100%`).
- `height: fill`: Fills all remaining height.
- `width: hug`: Shrinks boundaries to precisely wrap nested children.

---

## 4. Stack Containers & Background Images

Containers can layer children on top of each other using `layout: stack`, or display background images with `backgroundImage`:

```dac
component MediaCard {
    width: 360
    height: 220
    radius: 16
    backgroundImage: "mountain-cover.jpg"
    fit: cover
    padding: 24
    layout: vertical
    justifyContent: "flex-end"

    component CardTitle {
        text: "Alpine Retreat"
        fontSize: 20
        fontWeight: bold
        color: #FFFFFF
    }
}
```

See [Media & Assets](media.md) and [Layout System](../layout.md) for full details on stack layers and z-index ordering.

