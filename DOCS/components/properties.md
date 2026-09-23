# Properties Catalogue

This document is the comprehensive reference for all properties recognized by the Vireo compiler and renderers.

---

## 1. Sizing & Geometry

| Property | Type / Values | Description | Example |
| :--- | :--- | :--- | :--- |
| `width` | `Number`, `"fill"`, `"hug"` | Width in pixels, flex-grow (`fill`), or content hugging (`hug`) | `width: 320`<br>`width: fill` |
| `height` | `Number`, `"fill"`, `"hug"` | Height in pixels, flex-grow (`fill`), or content hugging (`hug`) | `height: 48`<br>`height: hug` |
| `x` | `Number` | Explicit horizontal offset (renders `position: absolute`) | `x: 100` |
| `y` | `Number` | Explicit vertical offset (renders `position: absolute`) | `y: 200` |

---

## 2. Typography

| Property | Type / Values | Description | Example |
| :--- | :--- | :--- | :--- |
| `text` | `String` | Text content rendered inside the element | `text: "Submit"` |
| `fontSize` | `Number` | Font size in pixels | `fontSize: 16` |
| `fontWeight` | `"normal"`, `"medium"`, `"semibold"`, `"bold"`, etc. | CSS font weight | `fontWeight: bold` |
| `fontFamily` | `String` | Font family name (default: Inter) | `fontFamily: "Inter"` |

---

## 3. Colors & Fills

| Property | Type / Values | Description | Example |
| :--- | :--- | :--- | :--- |
| `color` | Hex (`#RRGGBB`), Token | Text color if element contains text; background color otherwise | `color: #2563EB` |
| `backgroundColor` | Hex (`#RRGGBB`), Token | Explicit background surface color | `backgroundColor: #F3F4F6` |

---

## 4. Spacing, Borders & Shapes

| Property | Type / Values | Description | Example |
| :--- | :--- | :--- | :--- |
| `padding` | `Number` or Space-separated | Uniform padding in px or `vertical horizontal` | `padding: 16`<br>`padding: 12 24` |
| `radius` | `Number` | Corner radius (`border-radius`) in pixels | `radius: 8` |
| `border` | `Width HexColor` or CSS string | Border stroke width and color | `border: 1 #E5E7EB` |

---

## 5. Visual Effects & Elevation

| Property | Type / Values | Description | Example |
| :--- | :--- | :--- | :--- |
| `shadow` | `Boolean` (`true` / `false`) | Applies a subtle elevation drop shadow | `shadow: true` |

---

## 6. Layout & Alignment

| Property | Type / Values | Description | Example |
| :--- | :--- | :--- | :--- |
| `layout` | `"horizontal"`, `"vertical"`, `"stack"` | Direction of children (flexbox, or layered stack) | `layout: vertical`<br>`layout: stack` |
| `gap` | `Number` | Gap in pixels between child components | `gap: 16` |
| `alignItems` | `"flex-start"`, `"center"`, `"flex-end"`, `"stretch"` | Cross-axis alignment of children | `alignItems: center` |
| `justifyContent` | `"flex-start"`, `"center"`, `"space-between"`, etc. | Main-axis distribution of children | `justifyContent: "space-between"` |

---

## 7. Interactive & Form Controls

| Property | Type / Values | Description | Example |
| :--- | :--- | :--- | :--- |
| `placeholder` | `String` | Placeholder text for input components | `placeholder: "name@company.com"` |
| `label` | `String` | Shorthand text label for button or input elements | `label: "Sign In"` |
| `href` / `url` / `link` | `String` | Web hyperlink (renders an `<a>` anchor element) | `href: "https://vireo.dev"` |
| `ref` | Dot-notation identifier | Inherit definition from another component | `ref: buttons.Primary.Default` |

---

## 8. Rich Media & Layer Stacking (Phase 8)

| Property | Type / Values | Description | Example |
| :--- | :--- | :--- | :--- |
| `image` / `src` | `String` (path or URL) | Path or URL to raster image (`.png`, `.jpg`, `.webp`, `.gif`, `.avif`) or vector (`.svg`) | `image: "avatar.png"` |
| `backgroundImage` | `String` (path or URL) | Background image applied to a container frame | `backgroundImage: "hero-bg.jpg"` |
| `fit` / `objectFit` | `"cover"`, `"contain"`, `"fill"`, `"none"` | Scale mode for image assets | `fit: cover` |
| `video` | `String` (path or URL) | HTML5 video player stream (`.mp4`, `.webm`) | `video: "demo.mp4"` |
| `poster` | `String` (path or URL) | Video preview poster thumbnail | `poster: "thumb.jpg"` |
| `controls` | `Boolean` | Show audio/video native browser player controls | `controls: true` |
| `autoplay` | `Boolean` | Autoplay video on load | `autoplay: true` |
| `loop` | `Boolean` | Loop media playback | `loop: true` |
| `muted` | `Boolean` | Mute media audio | `muted: true` |
| `audio` | `String` (path or URL) | HTML5 audio player stream (`.mp3`, `.wav`, `.ogg`) | `audio: "song.mp3"` |
| `iframe` / `embed` | `String` (URL or path) | Embedded webpage or YouTube iframe | `iframe: "https://www.youtube.com/watch?v=..."` |
| `zIndex` / `z` | `Number` (integer) | Stacking layer order inside stack layouts | `zIndex: 2` |

