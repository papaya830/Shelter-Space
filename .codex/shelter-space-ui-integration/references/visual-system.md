# Shelter-Space visual system

Use this document as the implementation baseline. Exact values may move slightly to satisfy contrast, align with an existing brand, or fit an established codebase, but maintain the same visual character.

## 1. Visual character

The interface should resemble a well-maintained municipal or nonprofit service:

- warm paper-like canvas;
- crisp white information surfaces;
- deep civic teal for primary actions and active navigation;
- dark charcoal type;
- cool gray borders and secondary text;
- subtle green, amber, and red status treatments;
- moderate density, generous section spacing, and restrained component padding;
- almost no decorative shadow.

The screenshots are intentionally quiet. Their polish comes from alignment, hierarchy, whitespace, and consistent borders rather than visual effects.

## 2. Suggested CSS tokens

```css
:root {
  color-scheme: light;

  --font-body: "IBM Plex Sans", ui-sans-serif, system-ui, -apple-system,
    BlinkMacSystemFont, "Segoe UI", sans-serif;
  --font-display: "IBM Plex Serif", ui-serif, Georgia, serif;

  --color-canvas: #f9f8f5;
  --color-surface: #ffffff;
  --color-surface-muted: #f4f3ef;
  --color-surface-hover: #f7f8f8;

  --color-text: #18212b;
  --color-text-muted: #5f6b78;
  --color-text-subtle: #7b858f;

  --color-primary: #005d72;
  --color-primary-hover: #004b5c;
  --color-primary-active: #003f4e;
  --color-primary-soft: #e9f3f5;

  --color-border: #d6dfe4;
  --color-border-strong: #bcc8cf;
  --color-divider: #e4e9ec;

  --color-success: #228f5a;
  --color-success-bg: #dcf6e6;
  --color-success-border: #a7dfba;

  --color-warning: #d58c13;
  --color-warning-bg: #fff2d8;
  --color-warning-border: #efcf8e;

  --color-danger: #c84a45;
  --color-danger-bg: #fde7e5;
  --color-danger-border: #f0b4af;

  --color-neutral-status: #aeb7bd;
  --color-neutral-status-bg: #eef1f3;

  --shadow-subtle: 0 1px 2px rgb(20 31 43 / 0.05);

  --radius-sm: 0.5rem;
  --radius-md: 0.75rem;
  --radius-lg: 1rem;
  --radius-pill: 999px;

  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-5: 1.25rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-10: 2.5rem;
  --space-12: 3rem;
  --space-16: 4rem;
  --space-20: 5rem;
  --space-24: 6rem;

  --text-xs: 0.75rem;
  --text-sm: 0.875rem;
  --text-base: 1rem;
  --text-lg: 1.125rem;
  --text-xl: 1.375rem;
  --text-2xl: clamp(1.75rem, 1.45rem + 1vw, 2.25rem);
  --text-display: clamp(2.1rem, 1.65rem + 1.7vw, 3rem);

  --leading-tight: 1.15;
  --leading-heading: 1.25;
  --leading-body: 1.55;

  --control-sm: 2.25rem;
  --control-md: 2.75rem;
  --control-lg: 3rem;

  --container-wide: 72rem;
  --container-staff: 80rem;
  --container-reading: 48rem;

  --focus-ring: 0 0 0 3px rgb(0 93 114 / 0.24);

  --transition-fast: 140ms ease;
  --transition-base: 180ms ease;
}
```

Do not blindly duplicate these values if the project already has a coherent token layer. Map the existing tokens to this direction instead.

## 3. Typography

### Body and interface

Use IBM Plex Sans for:

- navigation;
- body copy;
- forms;
- filter controls;
- table data;
- status pills;
- operational headings;
- labels and metadata.

Default body size should be around 16px with a 1.5–1.6 line height. Secondary metadata may use 13–14px but should remain readable.

### Display headings

Use IBM Plex Serif sparingly for major public-facing headings such as:

- “Find a bed near you.”
- shelter detail page title blocks when the hierarchy remains clear;
- booking and confirmation page H1s.

The staff console should remain predominantly IBM Plex Sans to preserve the compact operational character visible in the screenshots.

### Weight

- Body: 400.
- Emphasized metadata and controls: 500.
- Card titles and operational headings: 600.
- Avoid using 700 everywhere.

### Labels

Eyebrows and small section labels may be uppercase with slight tracking, approximately 0.06em, but never use long uppercase sentences.

## 4. Layout and containers

### Global shell

- Header height: approximately 64px.
- Header background: canvas or white, with a subtle bottom border.
- Desktop horizontal page gutters: 24–32px.
- Mobile gutters: 16px.
- Public directory and staff content: centered, max width approximately 1152–1280px depending on route.
- Detail and form routes: centered, max width approximately 720–800px.

### Section rhythm

- Major desktop sections: 48–72px vertical separation.
- Related card groups: 16–24px gaps.
- Card padding: generally 20–24px desktop and 16px mobile.
- Use more whitespace around page-level groups than inside individual controls.

## 5. Borders, radius, and elevation

- Standard cards: 1px cool-gray border, white background, 12–16px radius.
- Inputs and controls: 1px border, 8–12px radius.
- Pills: fully rounded only for statuses, compact filters, and small tags.
- Use no shadow or only `--shadow-subtle`.
- Hover may strengthen the border or introduce the subtle shadow; avoid floating-card movement.

## 6. Buttons

### Primary

- Deep teal fill, white text.
- 44–48px height for major calls to action.
- 8–10px radius, not a giant pill unless compact.
- Hover darkens slightly; active darkens again.

### Secondary

- White or canvas background.
- Visible border and dark text.
- Hover uses a faint teal or gray tint.

### Danger

- Use a subtle red background and border for ordinary destructive actions.
- Reserve solid red for exceptional high-risk confirmation actions.

### Icon buttons

- Minimum 44px target.
- Visible border when the action would otherwise be hard to discover.
- Accessible name required.

## 7. Inputs and filters

- Input height: 44–48px.
- Search field should have a leading search icon and a useful visible label or accessible label.
- Filter chips should be compact and quiet, with a clear selected state using teal fill or soft teal background.
- Select chevrons should be visually consistent.
- Focus state must be stronger than hover state.
- Error text should sit directly beneath the associated control.

## 8. Status tokens

Availability states:

- Available/open: green.
- Limited/low capacity: amber.
- Full/unavailable: red.
- Held/internal workflow: teal.
- Neutral/inactive: gray.

Every status must include text and, where useful, a small dot or icon. A green pill should say “14 open / 60,” not merely show a green circle.

## 9. Tags

Accessibility and amenity tags should be small outlined chips with:

- 28–32px minimum height;
- subtle border;
- neutral text;
- no heavy filled background by default;
- 8–12px horizontal padding.

Do not use tags as decoration. Each tag should communicate useful information.

## 10. Tabs

- Use an underline or low-emphasis segmented active state.
- Include counts in compact badges where useful.
- Keep the active state teal.
- Use proper tab semantics only when content switches in place; use navigation links when they change routes.

## 11. Progress and utilization bars

- Track: pale gray.
- Occupied: amber.
- Held: muted teal.
- Open: represented in remaining track and/or explicit numeric label.
- Include text like “85% utilized” and “3 open / 20.”
- Height around 8px.
- Do not animate on page load.

## 12. Empty, loading, and error states

- Loading: restrained skeletons or inline progress; preserve layout to prevent shifting.
- Empty: explain what the state means and give one relevant next action.
- Error: explain what failed and how to retry; preserve existing data when possible.
- Do not use cute illustrations or humorous copy in urgent service flows.

## 13. Motion

- Hover/focus transitions: 140–180ms.
- Disclosure expansion: short and optional.
- No large parallax, scroll reveals, bouncing icons, shimmer-heavy surfaces, or animated gradients.
