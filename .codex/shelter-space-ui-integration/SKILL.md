---
name: shelter-space-ui-integration
description: Integrate, redesign, or polish the Shelter-Space HTML/CSS frontend to match the supplied civic shelter-directory reference screens. Use for requests such as “match the Lovable design,” “improve the Shelter-Space UI,” “build the public directory,” “polish the staff dashboard,” “make this responsive,” or “apply the Shelter-Space design system.” Do not use for backend-only, database-only, or infrastructure-only work.
---

# Shelter-Space UI Integration

Implement a calm, trustworthy, civic-service interface for Shelter-Space while preserving the existing application’s behavior, routes, data flow, and working integrations.

The reference screenshots in `assets/reference-screenshots/` are the primary visual source of truth for composition, density, spacing, hierarchy, and component treatment. The written rules below are the source of truth for design intent, accessibility, responsive behavior, and implementation quality.

Read these files before making substantial UI changes:

- `references/visual-system.md`
- `references/page-patterns.md`
- `references/integration-workflow.md`
- `references/quality-checklist.md`

## Core outcome

The result should feel:

- calm, practical, and safe;
- easy to scan under time pressure;
- appropriate for a public service rather than a commercial startup;
- visually consistent across public and staff surfaces;
- polished without being decorative;
- responsive and usable from small phones to large desktops.

The result should not feel like a generic AI dashboard.

## Non-negotiable visual direction

1. Use a warm off-white paper background, white surfaces, dark charcoal text, and deep civic teal as the primary action color.
2. Use IBM Plex Sans for interface and body copy. Use IBM Plex Serif selectively for major public-facing display headings. Keep dense operational headings, tables, labels, and staff navigation in IBM Plex Sans for clarity.
3. Use green, amber, and red only for meaningful availability or workflow status.
4. Prefer thin borders and tonal separation over visible drop shadows.
5. Use moderate corner radii. Do not turn every element into an oversized rounded card.
6. Avoid purple or indigo gradients, glassmorphism, neon colors, excessive shadows, floating blobs, decorative background graphics, and unnecessary animation.
7. Keep content widths controlled. The screenshots use wide but not edge-to-edge desktop layouts and a narrower reading column for detail and form pages.
8. Make primary actions obvious, but do not overwhelm the page with several competing filled buttons.

## Implementation rules

### Preserve the application

Before editing:

1. Inspect the repository structure, routes, current HTML/templates/components, CSS organization, JavaScript behavior, package manager, test scripts, and build command.
2. Identify which files own layout, typography, tokens, reusable components, page-specific styles, and interactive behavior.
3. Record the existing behavior that must remain intact.
4. Do not replace a working framework, router, data layer, component library, or form system merely to reproduce the visuals.
5. Do not rewrite the whole frontend when a token-and-component refactor is sufficient.
6. Keep unrelated files untouched.
7. Do not install a new UI framework unless the user explicitly asks for one or the repository already uses it.

### Prefer native, maintainable frontend code

- Use semantic HTML elements.
- Use CSS custom properties for all repeated design decisions.
- Use Grid for page and card layouts, Flexbox for one-dimensional alignment, and normal document flow wherever possible.
- Avoid absolute positioning for primary layout.
- Use `clamp()` for responsive typography and spacing where it improves fluidity.
- Use container queries only when the project’s browser support allows them and they materially simplify component responsiveness.
- Reuse existing icon infrastructure. If none exists and adding one is acceptable, prefer a single consistent icon set rather than hand-mixing icon styles.
- Keep selectors shallow and predictable.
- Do not use `!important` unless overriding an external style is unavoidable and documented.
- Do not hardcode repeated colors, radii, spacing, or font values outside the token layer.

### Create or normalize the design-token layer

Create one authoritative token section or file. Use the reference values in `references/visual-system.md` as the starting point, then adapt only when required by existing branding or contrast.

At minimum, define tokens for:

- page and surface colors;
- text and muted text;
- primary teal and interaction states;
- green, amber, red, and neutral status colors plus subtle backgrounds/borders;
- borders and dividers;
- typography families, weights, line heights, and sizes;
- spacing scale;
- content widths;
- radii;
- control heights;
- focus ring;
- subtle shadow, if any;
- transitions and reduced-motion behavior.

### Build reusable primitives before page-level duplication

Create or consolidate reusable patterns for:

- app shell and page container;
- public and staff headers;
- page title/header block;
- buttons and icon buttons;
- inputs, selects, textareas, labels, helper text, and errors;
- filter chips/toggles;
- status pills and count badges;
- information cards and section cards;
- shelter directory cards;
- metric/stat cards;
- tabs;
- data rows or responsive table patterns;
- progress/utilization bars;
- number steppers;
- empty, loading, error, and success states;
- footer and crisis-support notice.

Do not create a component abstraction for a pattern used only once unless it meaningfully improves readability or testing.

## Page implementation order

Unless the user specifies otherwise, integrate in this order:

1. Global tokens, fonts, page background, reset, focus styles, and container utilities.
2. Shared public header and staff header/navigation.
3. Public shelter directory.
4. Public shelter detail.
5. Booking form.
6. Confirmation page.
7. Staff queue.
8. Staff availability management.
9. Shelter settings/configuration.
10. Loading, empty, error, and responsive states across all pages.

This order establishes the shared system before repeating patterns.

## Public experience requirements

### Directory

- Use a calm introductory header with a small eyebrow, a clear task-focused H1, a brief explanation, and a compact live summary.
- Place search and filters in a separate horizontal utility band on desktop.
- Each shelter card must expose the essential decision-making information without requiring expansion: name, neighborhood, population served, barrier or requirements level, intake method, hours, accessibility/amenity tags, freshness timestamp, and availability.
- Availability must be visible at a glance and expressed as text, not color alone.
- Keep “View details” visually secondary to the information but clearly interactive.

### Shelter detail

- Use a narrower centered reading column.
- Put the shelter name, neighborhood, availability, short description, essential intake facts, phone action, and booking action above the fold where feasible.
- Separate rules, accessibility, amenities, location, and additional notes into restrained sections below.
- Keep the primary request action prominent and the phone action adjacent.
- Add reassurance copy near the request action without making unsupported privacy or service guarantees.

### Booking form

- Make the form low-friction and emotionally neutral.
- Group fields into clear sections rather than presenting one undifferentiated column.
- Include guest name, party size, contact method/details, accessibility needs, arrival window, and optional notes when those fields exist in the product requirements.
- Mark optional fields explicitly.
- Explain why sensitive information is requested when needed.
- Use inline validation that is specific, respectful, and actionable.
- Preserve user-entered data after validation errors.
- Do not require unnecessary identity information.

### Confirmation

- Use a reassuring success state without celebratory confetti or playful animation.
- Show the confirmation identifier, shelter name, contact method, arrival window, what to bring or expect, and next steps.
- Provide clear actions to return to the directory, view the request, or contact the shelter when supported.

## Staff experience requirements

### Queue

- Use a compact staff navigation header with clear active state.
- Place summary metrics above the queue.
- Provide tabs for relevant workflow states: pending, confirmed, checked-in, checked-out, and no-show.
- Keep the primary row information scannable: guest, request time, arrival window, party size, status, and actions.
- Expand secondary details such as contact, accessibility needs, and notes beneath the selected row or in an accessible disclosure.
- Distinguish constructive actions from destructive actions. Admit/check-in uses primary teal; decline/no-show uses a restrained danger treatment.
- Require confirmation for destructive or difficult-to-reverse actions.

### Availability management

- Show summary counts for open, held, occupied, and total capacity.
- Present each room or dorm as a restrained bordered section.
- Use utilization bars with labels and numeric counts; never rely on bar color alone.
- Number steppers must have explicit accessible labels, disabled boundary states, and minimum 44px touch targets.
- Prevent impossible states such as negative occupancy or totals exceeding room capacity.
- Clearly communicate when changes are saving, saved, or failed.

### Shelter settings

- Organize the form into public information, eligibility/population served, intake method and hours, accessibility, amenities, policies/rules, contact, and capacity configuration.
- Use section headings and helper copy.
- Keep destructive controls separate from ordinary save actions.
- Show a persistent or repeated save affordance for long forms when appropriate.
- Warn about unsaved changes if the existing application supports navigation interception.

## Responsive requirements

Validate at minimum at 390px, 768px, 1024px, and 1440px.

- No horizontal page overflow at 320px or wider.
- Desktop containers should not stretch indefinitely; maintain readable line lengths.
- On mobile, stack shelter-card metadata into a clear vertical order.
- Let filter chips scroll horizontally or wrap cleanly without tiny targets.
- Convert staff tables into stacked records or a deliberate horizontally scrollable region with preserved headers; do not simply squeeze columns until text becomes unreadable.
- Stack primary and secondary actions on narrow screens when needed.
- Keep status and primary task information near the top of each mobile record.
- Navigation must remain usable by keyboard and touch.

## Accessibility requirements

Target WCAG 2.2 AA quality.

- Use semantic landmarks and a logical heading hierarchy.
- Every input must have a programmatic label.
- Maintain visible keyboard focus with a consistent focus ring.
- Ensure interactive targets are at least 44px by 44px where practical.
- Never communicate availability, urgency, validation, or workflow status through color alone.
- Ensure text and meaningful UI boundaries have sufficient contrast.
- Use real buttons and links rather than clickable generic containers.
- Give icon-only controls accessible names.
- Use `aria-expanded` and `aria-controls` for disclosures.
- Announce asynchronous save results, availability changes, and important form errors through an appropriate live region.
- Respect `prefers-reduced-motion`.
- Do not auto-focus unexpectedly or move focus without a user-triggered reason.

## Interaction and motion

- Keep transitions short and functional, generally 120–200ms.
- Use motion for hover/focus feedback, disclosure expansion, and saving feedback only.
- Do not animate counters, progress bars, or page sections merely for decoration.
- Disable or reduce nonessential motion for users who prefer reduced motion.

## Content and tone

- Use plain, respectful language.
- Prefer task-based labels such as “Request a bed,” “Call shelter,” “Admit,” and “Check in.”
- Avoid marketing language, jokes, gamification, or language that judges the guest.
- Do not invent policy, legal, medical, privacy, safety, or service guarantees.
- Preserve crisis-support content supplied by the product, but do not add unsupported hotline information.

## Browser-based validation loop

After the first implementation pass:

1. Start the application using the repository’s documented command.
2. Open each changed route in an interactive browser.
3. Compare against the corresponding screenshot at a similar desktop viewport.
4. Inspect the page at 390px, 768px, 1024px, and 1440px.
5. Test keyboard navigation, focus order, disclosures, filters, tabs, forms, and staff actions.
6. Check loading, empty, error, validation, disabled, success, and long-content states.
7. Fix the three most visually or functionally important issues first.
8. Repeat until the acceptance criteria in `references/quality-checklist.md` are met.

Do not claim the UI is complete after only running a build or linter. Visual and interaction inspection are required whenever browser tools are available.

## Package policy

- Prefer the project’s current stack and existing packages.
- Do not add a CSS framework solely for this redesign.
- Add a package only when it clearly reduces implementation risk or duplication and is compatible with the repository.
- Before adding any package, state why native HTML/CSS/JavaScript or existing dependencies are insufficient.
- If an icon package already exists, reuse it.
- If testing infrastructure exists, extend it rather than introducing a parallel system.

## Completion report

At the end, report:

1. What visual system and shared components were added or changed.
2. Which routes/pages were updated.
3. Which existing behavior was preserved.
4. What was tested and at which viewport sizes.
5. Any remaining mismatch, limitation, or product decision requiring user review.
6. Any package added, with a one-sentence justification.

Keep the report factual. Do not describe unverified behavior as tested.
