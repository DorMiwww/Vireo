# Getting Started with Vireo

This guide walks you through installing the Vireo CLI, scaffolding a new project, writing your first `.dac` file, and compiling it to HTML and Figma.

---

## 1. Installation

### Quick Install (macOS & Linux)

Install the pre-compiled `vireo` binary directly into `~/.local/bin`:

```bash
curl -fsSL https://raw.githubusercontent.com/DorMiwww/Vireo/main/install.sh | bash
```

Verify your installation:

```bash
vireo --version
# Output: vireo 0.2.0
```

### Build from Source

Ensure you have Java 17+ installed, clone the repository, and install:

```bash
git clone https://github.com/DorMiwww/Vireo.git
cd Vireo
./gradlew installCli
```

---

## 2. Initialize a Project

Use the interactive initialization wizard to create a recommended project layout:

```bash
vireo init my-design-system
cd my-design-system
```

The wizard scaffolds:
```
my-design-system/
├── components/
│   ├── buttons.dac
│   └── badges.dac
├── screens/
│   └── dashboard.dac
└── tokens.dac
```

---

## 3. Your First `.dac` File

Create a file named `card.dac`:

```dac
block Cards {
    component UserProfile {
        width: 320
        padding: 24
        radius: 12
        color: #FFFFFF
        border: 1 #E5E7EB
        shadow: true
        layout: vertical
        gap: 16

        component Avatar {
            width: 48
            height: 48
            radius: 24
            color: #3B82F6
        }

        component Info {
            layout: vertical
            gap: 4

            component Name {
                text: "Alex Rivers"
                fontSize: 18
                fontWeight: bold
                color: #111827
            }

            component Role {
                text: "Principal Product Designer"
                fontSize: 14
                color: #6B7280
            }
        }
    }
}
```

---

## 4. Validating and Rendering

### Validate Syntax & References
Run `vireo check` to verify your design without rendering:

```bash
vireo check card.dac
```

### Render to HTML
Compile your design into a standalone HTML preview:

```bash
vireo render card.dac -o html --theme light
```
This generates `card.html`. Open it in any web browser to see pixel-accurate rendering.

### Render to JSON AST
Inspect the underlying abstract syntax tree:

```bash
vireo render card.dac -o json --pretty
```

### Render for Figma
Compile into a Figma-compatible tree for the [Vireo Figma Plugin](./figma-plugin.md):

```bash
vireo render card.dac -o figma
```

---

## Next Steps

- Learn about [Core Concepts](./concepts/index.md) and file organization.
- Master [Addressing & Composition](./concepts/addressing.md) to reuse components across files.
- Explore the [Components Catalogue](./components/index.md).
- Dive into the [Auto Layout & Constraints Guide](./layout.md).
