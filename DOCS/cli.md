# Vireo CLI Reference Manual

The `vireo` command-line interface parses, analyzes, and renders Vireo `.dac` (Design as a Code) files.
Starting in **Phase 5**, the CLI follows `kubectl`-inspired design principles: intuitive command hierarchy, first-class per-command `--help`, standard output formatting flags (`-o <format>`), convenience format shorthands, and project scaffolding.

---

## Installation & Setup

### One-Line Installer (macOS & Linux)
```bash
curl -fsSL https://raw.githubusercontent.com/DorMiwww/Vireo/main/install.sh | bash
```

### From Repository / Source
```bash
# Build and install to ~/.local/bin/vireo
./gradlew installCli

# Or run the repository wrapper directly from the project root
./vireo <command> [options]
```

---

## Global Usage

```bash
vireo <command> [arguments] [flags]
```

### Global Flags

| Flag | Shorthand | Description |
| :--- | :--- | :--- |
| `--help` | `-h` | Display usage and help information for `vireo` or any subcommand |
| `--version` | `-v` | Display Vireo version and pure JVM runtime details |

You can also run `vireo help <command>` to inspect any command.

---

## Commands

| Command | Purpose |
| :--- | :--- |
| [`vireo render`](#vireo-render) | Compile and render a `.dac` file to JSON, standard HTML, or Figma |
| [`vireo check`](#vireo-check) | Validate syntax and resolve semantic references in a `.dac` file |
| [`vireo init`](#vireo-init) | Scaffold a new Vireo DaaC project with templates and tokens (Phase 5) |
| [`vireo version`](#vireo-version) | Print version and supported renderer targets |

---

## `vireo render`

Compiles a `.dac` design file and renders it to the target format.

### Synopsis

```bash
vireo render <file.dac> [format-options] [output-options] [renderer-params]
```

### Target Format Selection (`kubectl`-style)

Vireo supports standard `kubectl`-style `-o <format>` flags, as well as dedicated format flags:

| Flag | Description | Default |
| :--- | :--- | :--- |
| `-o, --output <format>` | Set output format: `json`, `html`, `figma`, `standard-html` | `json` |
| `--to <format>` | Alias for output format (`json`, `html`, `figma`) | — |
| `--html` | Convenience shorthand for `-o html` | — |
| `--figma` | Convenience shorthand for `-o figma` | — |
| `--json` | Convenience shorthand for `-o json` | — |

### Output Destination

| Flag | Description |
| :--- | :--- |
| `--out, --output-file <file>` | File path to write output to (defaults to `stdout`) |
| `-o <file>` | File path to write output to (when argument contains `/`, `\`, or file extension) |

> [!NOTE]
> When using `-o <val>`, if `<val>` matches a format name (`json`, `html`, `figma`), it sets the output format. If it is a file path or extension (e.g. `login.html`), it sets the output destination file. For unambiguous scripts, use `--html --out <file>`.

### HTML Output Parameters

When rendering to HTML (`-o html` or `--html`):

| Flag | Description | Default |
| :--- | :--- | :--- |
| `--standard-html`, `--full-page` | Render a complete, standalone W3C standard HTML5 document (`<!DOCTYPE html>`, `<head>`, `<meta charset>`, viewport, `<title>`, and centered preview container) | `true` |
| `--snippet`, `--fragment` | Render only the component HTML tree without DOCTYPE, head, or body wrappers (ideal for embedding in React, Vue, or existing web pages) | `false` |
| `--title <title>` | Specify a custom HTML `<title>` for the document | Source file path |
| `--theme <light\|dark>` | Specify preview canvas theme: `light` (`#F3F4F6`) or `dark` (`#111827`) | `light` |

### Figma Output Parameters

When rendering to Figma AST JSON (`-o figma` or `--figma`):

| Flag | Description | Default |
| :--- | :--- | :--- |
| `--token <token>` | Figma Personal Access Token for direct Figma REST API post (falls back to `FIGMA_TOKEN` environment variable) | `null` |
| `--pretty` | Indent and format Figma JSON document for human inspection | `true` |
| `--compact` | Emit minified, compact JSON without extra whitespace | `false` |

### `vireo render` Examples

```bash
# 1. Render design to JSON (kubectl style output flag to stdout)
vireo render designs/card.dac -o json

# 2. Render standard HTML document to stdout
vireo render designs/card.dac --html

# 3. Render standard HTML with custom page title and output to file
vireo render designs/card.dac --html --title "SaaS Dashboard" --out dashboard.html

# 4. Render dark-themed HTML preview
vireo render designs/card.dac --html --theme dark --out card-dark.html

# 5. Render embeddable HTML snippet/fragment (no DOCTYPE wrapper)
vireo render designs/card.dac --html --snippet

# 6. Render to Figma AST JSON for Figma Development Plugin
vireo render designs/card.dac --figma -o designs/card.figma.json

# 7. Render compact Figma JSON
vireo render designs/card.dac --figma --compact --out card.figma.json

# 8. View render command options
vireo render --help
```

---

## `vireo check`

Parses and validates syntax and semantic references in a `.dac` file without rendering.
Checks include:
- Lexical and syntactic correctness
- Import path resolution
- Cross-file component reference validation (`ref: file.block.component`)
- Circular import detection
- Layout constraint validation

### Synopsis

```bash
vireo check <file.dac>
```

### Output & Exit Codes

- **Success**: Prints `OK: <file.dac> passed analysis.` and exits with code `0`.
- **Failure**: Prints errors in standard compiler format (`<file>:<line>:<column>: error: <message>`) and exits with code `1`.

### `vireo check` Examples

```bash
# Validate a design file
vireo check designs/login.dac

# View check help
vireo check --help
```

---

## `vireo init` (Phase 5 Scaffolding Wizard)

Scaffolds a new Vireo DaaC project with recommended folder layout, design tokens, reusable components, and an example card.

### Synopsis

```bash
vireo init [project-name] [options]
```

### Options

| Option | Shorthand | Description | Default |
| :--- | :--- | :--- | :--- |
| `--template <name>` | — | Template preset (`card`, `minimal`, `dashboard`) | `card` |
| `--dir <path>` | `-d` | Target directory to scaffold files into | `./[project-name]` |
| `--force` | — | Overwrite existing files if directory already exists | `false` |
| `--help` | `-h` | Display init help | — |

### Generated Project Structure

```
<project-name>/
├── vireo.config.json           # Project manifest and entrypoint definition
├── designs/
│   ├── tokens.dac              # Design tokens: colors, typography, spacing, radiuses
│   ├── components/
│   │   └── button.dac          # Primary button component library
│   └── card.dac                # Screen composing tokens and components
└── README.md                   # Quickstart and command cheat sheet
```

### `vireo init` Examples

```bash
# Initialize a new project in directory ./my-app
vireo init my-app

# Initialize into the current working directory
vireo init --dir .

# Initialize with force overwrite
vireo init my-app --force
```

---

## `vireo version`

Displays Vireo CLI version and JVM runtime information.

```bash
vireo version
# or
vireo --version
```

**Output:**
```
vireo version 0.1.0-SNAPSHOT (pure JVM 21)
Supported target formats: json, html (standard & snippet), figma
```

---

## Exit Codes

| Code | Meaning |
| :--- | :--- |
| `0` | Success |
| `1` | Parse error, semantic error, file not found, or invalid option |
