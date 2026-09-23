# Expressions, Variables & Conditionals

Vireo supports dynamic design token expressions, variables, and conditional logic. This eliminates hardcoded repetition and empowers design systems with theme tokens.

---

## Variables (`var`)

Variables are declared at the file level using `var`:

```dac
var primaryColor = #2563EB
var baseSpacing  = 16
var cardRadius   = 12
var defaultFont  = "Inter"
```

### Referencing Variables

Prefix variable names with `$` when assigning to properties:

```dac
component Card {
    radius: $cardRadius
    padding: $baseSpacing
    color: #FFFFFF

    component Title {
        color: $primaryColor
    }
}
```

---

## Conditionals (`if ... then ... else`)

Properties can be assigned dynamically based on boolean conditions or variable comparisons:

```dac
var isDarkMode = true

component ThemeSurface {
    color: if $isDarkMode then #111827 else #FFFFFF
}
```

Conditionals can also be nested for multi-variant styles:

```dac
var status = "warning"

component StatusBadge {
    color: if $status == "success" then #10B981 else if $status == "warning" then #F59E0B else #EF4444
}
```

---

## Functions (`fun`)

Functions allow computing values such as mathematical scales (e.g. 8pt grid multipliers):

```dac
fun spacing(multiplier: Int): Int {
    return 8 * multiplier
}

component Container {
    padding: spacing(3) // resolves to 24
    gap: spacing(2)     // resolves to 16
}
```

---

## Next Steps
- Explore the [Component Model](../components/index.md).
- Learn about the [Properties Catalogue](../components/properties.md).
