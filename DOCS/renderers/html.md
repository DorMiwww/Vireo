# HTML Renderer

The HTML Renderer transforms Vireo AST files into clean, readable HTML and inline CSS with zero external web framework dependencies.

---

## Basic Usage

Render a `.dac` file to HTML using `-o html` or `--html`:

```bash
vireo render button.dac -o html
# Or using shorthand:
vireo render button.dac --html
```

By default, this writes `button.html` in the current directory.

---

## Formatting Options

### Standalone HTML Document vs Snippet

- **`--standard-html` (default):** Emits a complete HTML5 document including `<!DOCTYPE html>`, `<head>`, viewport meta tags, global typography resets, and a centered canvas.
- **`--snippet`:** Emits only the inner component markup and styles. Ideal for embedding into documentation sites, component previews, or web applications.

```bash
vireo render card.dac -o html --snippet
```

### Theming: Light & Dark Mode

Use `--theme` to control the canvas background environment:

```bash
# Light background (#F3F4F6)
vireo render dashboard.dac -o html --theme light

# Dark background (#111827)
vireo render dashboard.dac -o html --theme dark
```

### Custom Page Title

Set the browser tab title:

```bash
vireo render pricing.dac -o html --title "Vireo Pricing Plans"
```

---

## CSS & Layout Mapping

The HTML renderer translates Vireo concepts into modern standard CSS:
- `layout: horizontal` ➔ `display: flex; flex-direction: row;`
- `layout: vertical` ➔ `display: flex; flex-direction: column;`
- `layout: stack` ➔ `position: relative; display: block;` (with layered children `position: absolute; z-index: ...;`)
- `gap: 16` ➔ `gap: 16px;`
- `width: fill` ➔ `flex: 1; width: 100%;`
- `width: hug` ➔ `width: fit-content;`
- `shadow: true` ➔ `box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);`
- `href: "..."` ➔ renders an `<a>` element with hover states and external link attributes.

---

## Rich Media & Asset Rendering (Phase 8)

The HTML renderer natively outputs semantic HTML5 tags for media:

| Vireo Property | HTML Output | Attributes / Styles |
| :--- | :--- | :--- |
| `image: "photo.jpg"` | `<img>` | `src="photo.jpg"`, `object-fit: cover` |
| `backgroundImage: "bg.jpg"` | `<div>` (container) | `background-image: url('bg.jpg'); background-size: cover;` |
| `video: "clip.mp4"` | `<video>` | `controls`, `poster`, `autoplay`, `loop`, `muted` |
| `audio: "song.mp3"` | `<audio>` | `controls`, `src="song.mp3"` |
| `iframe: "..."` | `<iframe>` | `frameborder="0"`, `allowfullscreen`, YouTube auto-embed URL |

### Embedding Local Assets (`--embed-assets`)

By default, the HTML renderer keeps relative paths to local assets intact (`src="assets/logo.png"`).

Pass `--embed-assets` to bundle local assets directly into self-contained `data:...;base64,...` data URIs:

```bash
vireo render landing.dac --html --embed-assets
```

