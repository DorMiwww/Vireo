# Media & Assets

Vireo supports a rich media pipeline for designing real-world visual applications. You can place raster images, vector SVGs, audio streams, HTML5 videos, and embedded iframes (such as YouTube videos) directly into your layouts, or stack visual layers under other elements (ConstraintLayout-style).

---

## 1. Images & Visual Assets

Vireo supports all standard image formats: `.png`, `.jpg`, `.jpeg`, `.webp`, `.gif`, `.avif`, and `.svg`.

Images can be specified with `image` or `src`:

```dac
component Avatar {
    image: "assets/avatar.png"
    width: 64
    height: 64
    radius: 32
    fit: cover
}
```

### Image Fit Modes

The `fit` property controls how an image scales to fit its container:

| Value | HTML Behavior | Figma Mapping | Description |
| :--- | :--- | :--- | :--- |
| `cover` | `object-fit: cover` | `FILL` | Scales image to cover container while maintaining aspect ratio (default) |
| `contain` | `object-fit: contain` | `FIT` | Scales image so entire image is visible within bounds |
| `fill` | `object-fit: fill` | `STRETCH` | Stretches image to fill exact width and height |
| `none` | `object-fit: none` | `CROP` | Preserves original image dimensions without resizing |

---

## 2. Background Layers & Stack Layout

To place visual elements (like photos or overlays) directly under other components, use `layout: stack` (or `layout: layer`, `layout: constraint`):

```dac
component HeroBanner {
    layout: stack
    width: 720
    height: 280
    radius: 16

    // Photo under other elements
    component BgPhoto {
        image: "hero.jpg"
        fit: cover
        width: 720
        height: 280
        zIndex: 1
    }

    // Dark tint layer
    component Tint {
        width: 720
        height: 280
        backgroundColor: #000000
        zIndex: 2
    }

    // Content placed on top
    component OverlayCard {
        x: 32
        y: 60
        width: 400
        zIndex: 3

        component Title {
            text: "Welcome to Vireo"
            color: #FFFFFF
            fontSize: 24
            fontWeight: bold
        }
    }
}
```

### Container Background Images

Containers can also have a background image applied directly using `backgroundImage`:

```dac
component Card {
    width: 360
    height: 200
    radius: 12
    backgroundImage: "card-bg.webp"
    fit: cover
    padding: 24

    component Title {
        text: "Featured Card"
        color: #FFFFFF
    }
}
```

---

## 3. Video Players

Vireo renders HTML5 `<video>` tags with customizable playback parameters:

```dac
component PromoVideo {
    video: "promo.mp4"
    poster: "preview.jpg"
    width: 480
    height: 270
    radius: 8
    controls: true
    autoplay: false
    loop: false
    muted: true
}
```

| Property | Type | Description |
| :--- | :--- | :--- |
| `video` | `String` (path or URL) | Path to `.mp4` or `.webm` video file |
| `poster` | `String` (path or URL) | Preview poster image displayed before playback |
| `controls` | `Boolean` | Shows default player controls (play/pause/volume) |
| `autoplay` | `Boolean` | Automatically begins playing on load |
| `loop` | `Boolean` | Automatically loops playback |
| `muted` | `Boolean` | Mutes audio by default |

---

## 4. Audio Streams

Embed sound clips and music streams using the `audio` property:

```dac
component PodcastPlayer {
    audio: "episode-1.mp3"
    width: fill
    controls: true
}
```

Supported formats include `.mp3`, `.wav`, and `.ogg`.

---

## 5. Embeds & YouTube iFrames

Embed interactive external content like YouTube videos, Vimeo streams, or interactive sandboxes using `iframe` or `embed`:

```dac
component YoutubeVideo {
    iframe: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    width: 560
    height: 315
    radius: 8
}
```

> [!NOTE] YouTube URL Auto-Conversion
> When you provide a YouTube URL in format `https://www.youtube.com/watch?v=ID` or `https://youtu.be/ID`, Vireo automatically converts it into a compliant responsive `https://www.youtube.com/embed/ID` iframe.

---

## 6. Asset Embedding (`--embed-assets`)

When rendering designs with local assets, pass the `--embed-assets` CLI flag to pack all local raster images, SVGs, and audio files into self-contained base64 data URIs:

```bash
# Embed all local assets into a self-contained HTML file
vireo render designs/card.dac --html --embed-assets

# Embed assets into the Figma AST JSON file
vireo render designs/card.dac --figma --embed-assets
```
