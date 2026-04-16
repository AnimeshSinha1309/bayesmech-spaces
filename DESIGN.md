# Third Space Design System

## Brand Direction

Third Space should feel warm, social, and human rather than glossy or futuristic.

The default visual foundation is a light yellow "old paper" surface across most screens. This gives the product a calm, tactile feel and keeps the interface distinct from generic white SaaS layouts.

The brand accents are:

- McLaren-style papaya orange for energy, action, and emphasis
- Black for structure, contrast, and typography

The overall effect should feel editorial, intentional, and slightly analog.

---

## Core Palette

### Primary Surfaces

- `Paper Base`: `#F6EFCF`
  Main background for most screens. This is the default "old paper" color.

- `Paper Warm`: `#EFE4BF`
  Alternate surface for cards, side panels, or grouped sections.

- `Paper Deep`: `#E3D4A2`
  Borders, dividers, subtle hovers, and low-contrast fills.

### Brand Colors

- `Papaya Orange`: `#FF8700`
  Primary brand accent. Use for CTAs, key icons, active states, highlights, and important chips.

- `Black`: `#111111`
  Primary text, strong contrast elements, navigation, and high-emphasis UI.

### Supporting Neutrals

- `Ink`: `#2A241C`
  Softer body text on paper backgrounds when pure black feels too harsh.

- `Muted Brown`: `#6C6253`
  Secondary text, metadata, helper labels, and disabled icon strokes.

- `Line`: `#CDBE8C`
  Default border and divider color on paper surfaces.

---

## Usage Rules

### 1. Backgrounds

- Most screens should use `Paper Base`
- Use `Paper Warm` for nested surfaces, cards, sheets, or chat bubbles that need separation
- Avoid plain white as the main background unless there is a very specific product need

### 2. Accent Color

- `Papaya Orange` should be used intentionally, not everywhere
- Reserve it for primary actions, selected states, links that need emphasis, and important counts or badges
- Do not use orange for large text blocks or full-screen fills

### 3. Text and Contrast

- Use `Black` for headings, navigation, buttons, and critical UI text
- Use `Ink` for default body copy on paper surfaces
- Use `Muted Brown` for timestamps, captions, placeholders, and lower-priority metadata

### 4. Borders and Depth

- Prefer thin borders and tonal contrast over heavy shadows
- Use `Line` or `Paper Deep` for separators
- If shadows are used, they should be soft and warm, not cold gray

---

## Suggested UI Mapping

- `App background`: `Paper Base`
- `Cards / panels`: `Paper Warm`
- `Primary button`: `Papaya Orange` background with `Black` text
- `Secondary button`: transparent or `Paper Warm` with `Black` border/text
- `Top nav / tab bar`: `Paper Base` with `Black` text and orange active indicators
- `Chat input`: `Paper Warm` with `Black` text and `Line` border
- `Links / active chips / key icons`: `Papaya Orange`
- `Primary headings`: `Black`
- `Body text`: `Ink`
- `Metadata / helper text`: `Muted Brown`

---

## Design Tokens

```css
:root {
  --color-paper-base: #F6EFCF;
  --color-paper-warm: #EFE4BF;
  --color-paper-deep: #E3D4A2;

  --color-brand-orange: #FF8700;
  --color-brand-black: #111111;

  --color-text-primary: #111111;
  --color-text-body: #2A241C;
  --color-text-muted: #6C6253;

  --color-border: #CDBE8C;
}
```

---

## Visual Notes

- The product should feel like a modern service layered on top of printed paper tones
- Orange should provide sharp moments of energy against the quieter yellow base
- Black should keep the interface grounded, legible, and confident
- The palette should stay warm overall; avoid cool grays and blue-tinted neutrals where possible
