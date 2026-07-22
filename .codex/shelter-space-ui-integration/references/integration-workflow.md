# Integration workflow

Follow this workflow when applying the Shelter-Space visual system to an existing repository.

## Phase 1: inspect before changing

1. List the repository root and identify the frontend entry point.
2. Read the root `README`, relevant `AGENTS.md`, package manifest, and build/test configuration.
3. Map routes/pages corresponding to:
   - public directory;
   - shelter detail;
   - booking;
   - confirmation;
   - staff queue;
   - staff availability;
   - staff settings.
4. Identify existing shared layout, button, form, card, badge, table, and navigation styles/components.
5. Trace where shelter, availability, booking, and staff data enter the UI.
6. Identify live behavior that screenshots cannot express: loading, saving, error, authentication, permissions, pagination, filtering, and navigation.
7. Run the app before changing it and capture baseline screenshots when browser tooling is available.

Output a brief implementation plan before broad edits. The plan should name files and explain which changes are global versus page-specific.

## Phase 2: normalize foundations

1. Add IBM Plex font loading in the project’s established font-loading mechanism.
2. Define or map the design tokens.
3. Normalize box sizing, body background, text rendering, link styles, focus styles, and form font inheritance.
4. Establish container and section utilities or equivalent layout primitives.
5. Ensure existing dark-mode logic is not silently broken. Do not add dark mode unless requested.

## Phase 3: shared components

Refactor one representative instance of each repeated pattern before applying it everywhere:

- primary and secondary button;
- status pill;
- input/select/textarea;
- card/section shell;
- header/navigation;
- filter chip;
- metric card;
- tabs;
- data row/card;
- utilization bar;
- number stepper.

Verify the representative instance visually and functionally before multiplying it across routes.

## Phase 4: public routes

Implement the public directory first because it establishes the main container, header, filters, cards, tags, and availability states.

Then implement shelter detail, booking, and confirmation. Reuse the same availability, button, form, and section patterns.

Preserve query parameters, route parameters, form submissions, and server-rendered behavior.

## Phase 5: staff routes

Implement the staff shell and active navigation state, then queue metrics/tabs/rows. Implement availability management after queue so shared staff layout and buttons are stable.

Keep staff screens denser than public screens, but never below accessible target sizes.

## Phase 6: responsive conversion

Do not treat responsiveness as a final patch. For each page:

1. Validate desktop composition.
2. Reduce to 1024px and fix container/gap issues.
3. Reduce to 768px and intentionally change grid structure.
4. Reduce to 390px and intentionally reorder content.
5. Check 320px for overflow.

Use content-driven breakpoints when possible rather than device labels alone.

## Phase 7: state coverage

For every data-driven area, inspect or implement:

- initial loading;
- empty data;
- partial data;
- long names and long notes;
- maximum counts;
- zero capacity/open beds;
- stale/fresh update indicator;
- network or server failure;
- disabled and submitting states;
- successful update;
- permission-denied state for staff features when applicable.

Do not fabricate backend states merely for display. Use fixtures, mocks, or existing test utilities in development only.

## Phase 8: browser QA

Use the repository’s browser/visual test tooling. When interactive Playwright is available:

1. Open each route.
2. Compare to the reference screenshot at a similar viewport.
3. Inspect computed styles and layout when mismatches are hard to diagnose.
4. Test keyboard-only navigation.
5. Test form errors and success.
6. Test staff row disclosures and actions.
7. Test steppers at zero and capacity boundaries.
8. Recheck mobile after every major desktop adjustment.

Capture final screenshots when practical.

## Phase 9: automated checks

Run the project’s existing:

- formatter;
- CSS linter;
- JavaScript/TypeScript linter;
- type checker;
- unit tests;
- integration tests;
- build.

Add targeted tests for behavior you changed. Do not introduce broad snapshot tests that merely lock in unstable markup.

## Phase 10: review the diff

Before finishing:

1. Review `git diff` for unrelated changes.
2. Remove duplicate or dead CSS.
3. Check for hardcoded values that belong in tokens.
4. Check for placeholder copy, dead links, and nonfunctional controls.
5. Check that no secrets, local paths, generated screenshots, or test credentials were committed accidentally.
6. Summarize remaining visual differences honestly.
