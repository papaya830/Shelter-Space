# Quality and acceptance checklist

The work is not complete until applicable items below pass.

## Visual system

- [ ] Warm off-white page canvas is used consistently.
- [ ] White surfaces and cool-gray borders create the main structure.
- [ ] Deep teal is the dominant action/active color.
- [ ] Green, amber, red, and neutral statuses have consistent text, background, and border treatments.
- [ ] Typography uses IBM Plex consistently through the project’s font-loading approach.
- [ ] Public display headings use serif selectively; dense staff UI remains legible and mostly sans-serif.
- [ ] Spacing follows a small repeatable scale.
- [ ] Card radii are moderate and consistent.
- [ ] Shadows are absent or extremely subtle.
- [ ] There are no purple gradients, glass panels, neon accents, oversized hero cards, decorative blobs, or excessive pills.

## Public directory

- [ ] Search and filters are understandable without instructions.
- [ ] Each shelter card exposes name, neighborhood, served population, requirements/barrier level, intake, hours, availability, and freshness.
- [ ] Accessibility/amenity tags wrap correctly.
- [ ] Availability is expressed in text and color.
- [ ] Directory remains usable at 390px.
- [ ] Long shelter names and long hours do not break alignment.

## Shelter detail and booking

- [ ] Intake method, hours, phone, and primary request action are easy to find.
- [ ] Details use a readable narrow column.
- [ ] Fact blocks stack cleanly on mobile.
- [ ] Booking form labels, helper text, errors, optional indicators, and submit state are clear.
- [ ] User input survives validation errors.
- [ ] Confirmation language does not overpromise availability.

## Staff queue

- [ ] Active staff route is visually clear.
- [ ] Summary metrics are readable and not decorative.
- [ ] Tabs have keyboard and selected-state behavior appropriate to their implementation.
- [ ] Queue records expose the essential fields at a glance.
- [ ] Secondary details are accessible through a disclosure or responsive layout.
- [ ] Constructive and destructive actions are distinct.
- [ ] Destructive actions have confirmation when needed.
- [ ] Mobile queue is intentionally reformatted rather than squeezed.

## Availability management

- [ ] Open, held, occupied, and total counts are internally consistent.
- [ ] Utilization bars include numeric labels.
- [ ] Number stepper buttons have accessible names.
- [ ] Decrement cannot go below zero.
- [ ] Increment cannot exceed capacity.
- [ ] Saving, saved, and failed states are visible.
- [ ] Count changes are announced appropriately to assistive technology.

## Responsive behavior

- [ ] Checked at 390px, 768px, 1024px, and 1440px.
- [ ] No horizontal page overflow at 320px or wider.
- [ ] Header/navigation remains usable on phone widths.
- [ ] Filter controls remain readable and touch-friendly.
- [ ] Primary actions remain visible and do not become tiny.
- [ ] Data does not overlap, clip, or create unreadable columns.
- [ ] Content width remains controlled on large monitors.

## Accessibility

- [ ] Logical page title and heading order.
- [ ] Semantic header/nav/main/footer landmarks.
- [ ] Skip link present when appropriate.
- [ ] Full keyboard navigation works.
- [ ] Visible focus on every interactive element.
- [ ] Inputs have programmatic labels.
- [ ] Icon-only controls have accessible names.
- [ ] Errors are associated with fields and summarized after submit.
- [ ] Color is not the sole status indicator.
- [ ] Text and boundaries meet contrast expectations.
- [ ] Reduced motion preference is respected.
- [ ] Dynamic updates use live announcements only where useful.

## Engineering quality

- [ ] Existing routes, data calls, form submission, authentication, and state management still work.
- [ ] No unnecessary framework replacement.
- [ ] No unexplained new dependency.
- [ ] Repeated visual values use tokens.
- [ ] Repeated patterns use shared styles/components where appropriate.
- [ ] No dead CSS, duplicate component implementations, placeholder content, or console errors.
- [ ] Existing formatter, linter, tests, type checker, and build pass.
- [ ] Browser inspection was performed, not just static code review.
- [ ] Final report clearly distinguishes tested behavior from assumptions.

## Visual comparison priorities

When the implementation cannot match every screenshot detail immediately, prioritize in this order:

1. Information hierarchy and task flow.
2. Overall layout, content width, and section spacing.
3. Typography scale and weight.
4. Card, border, and control treatment.
5. Status styling and icons.
6. Fine spacing and pixel-level details.

Never sacrifice functionality, accessibility, or responsive usability for a pixel-perfect desktop-only match.
