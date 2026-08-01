# Vireo CLI Reference

The `vireo` command-line tool parses, analyzes, and renders `.dac` design files.

---

## Usage

```bash
vireo <command> [options]
```

## Commands

### `vireo render`

Compiles and renders a `.dac` file to the specified target format.

```bash
vireo render <file.dac> [--to json] [-o <output-file>]
```

**Options:**
- `--to <format>`: Target format (currently supports `json`). Default: `json`.
- `-o, --output <file>`: File path to write output to. If omitted, prints to `stdout`.

---

### `vireo check`

Parses and performs semantic analysis on a `.dac` file (loading imports, verifying reference resolution, detecting circular imports).

```bash
vireo check <file.dac>
```

**Output:**
- Outputs `OK: <file.dac> passed analysis.` with exit code `0` on success.
- Prints all formatted errors (`<file>:<line>:<column>: error: <message>`) and returns exit code `1` on failure.
