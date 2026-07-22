# Page and component patterns

This document translates the supplied screenshots into implementable patterns.

## 1. Public directory

Reference: `assets/reference-screenshots/public-directory.png`

### Desktop composition

1. Public header with brand left and “Staff sign in” right.
2. Warm paper hero region containing:
   - uppercase eyebrow;
   - task-oriented H1;
   - two-line explanation;
   - live summary row with available-bed count and freshness timestamp.
3. White filter strip separated by borders:
   - wide search input;
   - selected “All” chip;
   - boolean chips such as “Has space” and “Walk-in”;
   - select-style chips for guest type and requirements.
4. Directory list with one wide shelter card per row.

### Shelter-card anatomy

- Header row: shelter name, neighborhood beneath, availability pill aligned right.
- Four-column metadata row on wide screens:
  - serves;
  - requirements;
  - intake;
  - hours.
- Tag row for accessibility and amenities.
- Footer row: freshness indicator left and “View details” link right.

### Responsive conversion

At tablet:

- metadata becomes two columns;
- search may occupy a full first row;
- filters wrap below.

At mobile:

- availability pill moves beneath or beside the name without compressing it;
- metadata becomes a single stack or two compact rows;
- filter controls are horizontally scrollable or wrap with full-size targets;
- “View details” can become a full-width secondary action only when it improves usability.

## 2. Public shelter detail

Reference: `assets/reference-screenshots/public-shelter-detail.png`

### Composition

- Back link above the content column.
- Primary shelter card contains:
  - eyebrow neighborhood;
  - large shelter name;
  - availability pill;
  - short descriptive paragraph;
  - 2x2 facts grid for population, requirements, intake, and hours;
  - phone secondary action;
  - full-width or dominant request action;
  - reassurance/supporting copy.
- Below the primary card:
  - rules/requirements section;
  - amenities section with tags;
  - accessibility/location/contact sections as needed.

### Rules

- Keep the first card dense but not cramped.
- On mobile, facts stack to one column and actions stack with the request action first.
- Avoid burying intake hours or phone information below long descriptive content.

## 3. Booking form

### Recommended structure

1. Back link and page title.
2. Compact shelter summary with name, availability, intake hours, and contact.
3. Form section: guest details.
4. Form section: arrival and party.
5. Form section: accessibility and support needs.
6. Optional notes.
7. Review/privacy helper text based only on actual product behavior.
8. Primary submit and secondary cancel/back action.

### Field behavior

- Guest name: text input.
- Party size: select or stepper with sensible limits.
- Contact: select contact method when multiple types are supported; otherwise a clearly labeled field.
- Accessibility: checkboxes plus optional detail textarea when necessary.
- Arrival: constrained window choices when supplied by the shelter.
- Notes: optional textarea with guidance, not a demand for sensitive history.

### Validation

- Validate after blur and on submit, not on every keystroke unless necessary.
- Place an error summary at the top after failed submission and inline errors at fields.
- Focus the error summary after submit while preserving all entered values.

## 4. Confirmation

### Composition

- Success icon or status mark.
- Clear heading such as “Your request was sent.”
- Confirmation ID in a distinct but restrained block.
- Shelter name, contact, arrival window, and next steps.
- Primary action to view details or return to the directory.
- Secondary contact action.

Do not imply a bed is guaranteed unless the system state truly confirms it.

## 5. Staff queue

Reference: `assets/reference-screenshots/staff-queue.png`

### Desktop composition

1. Staff header:
   - Shelter-Space Staff brand;
   - nav items for Queue, Availability, and Shelter settings;
   - shelter/operator identity at right.
2. Four metric cards in one row:
   - open beds;
   - pending requests;
   - admitted tonight;
   - confirmed arrivals.
3. Booking queue title and date.
4. Refresh action aligned right.
5. Workflow tabs with counts.
6. Bordered table/list.

### Queue row anatomy

- Guest avatar/initials, name, and request ID.
- Requested time.
- Arrival window.
- Party size.
- Status pill.
- Inline primary and destructive actions.
- Disclosure icon.
- Expanded details row for contact, accessibility, and notes.

### Responsive conversion

At mobile, render each request as a card with this order:

1. name and status;
2. arrival and party;
3. requested time and ID;
4. expandable contact/accessibility/notes;
5. actions in a two-column or stacked layout.

Do not retain a six-column table at phone width.

## 6. Availability management

Reference: `assets/reference-screenshots/staff-availability.png`

### Composition

- Page title and concise explanation.
- Summary strip showing open, held, occupied, and total capacity.
- One room/dorm card per section.

### Room card anatomy

- Room icon, room name, and audience/type description.
- Open count aligned right.
- Utilization bar.
- Two control blocks:
  - occupied with decrement/value/increment;
  - held for arrival with decrement/value/increment.
- Utilization percentage beneath.

### Behavioral constraints

- Disable decrement at zero.
- Disable increment when occupied + held reaches capacity.
- Use optimistic updates only if rollback/error feedback is implemented.
- Provide explicit saving and failure state.
- Announce successful count updates to assistive technology without excessive chatter.

## 7. Shelter settings

### Suggested sections

1. Public identity: name, short description, neighborhood, address.
2. Contact: phone, email, public contact preferences.
3. Population served.
4. Intake method and hours.
5. Barrier/requirements level.
6. Accessibility features.
7. Amenities.
8. Rules and notes.
9. Rooms and capacity.
10. Publication/visibility state.

Use checkboxes for finite feature lists, text inputs for short values, textareas for descriptive content, and repeatable rows only where the data model already supports them.

## 8. Shared component contracts

### Availability pill

Inputs:

- open count;
- total capacity;
- optional state override;
- freshness when relevant.

State thresholds should come from product rules, not arbitrary visual assumptions. When no rules exist, present the raw count and use neutral styling until the user approves thresholds.

### Status pill

Inputs:

- machine status;
- human-readable label;
- icon/dot optional.

Do not expose raw enum values directly to users.

### Metric card

- Small uppercase or sentence-case label.
- Optional colored dot.
- Large numeric value.
- Optional denominator or explanation.
- No decorative chart unless it communicates a real trend.

### Number stepper

- Visible label.
- Decrement button.
- Current numeric value.
- Increment button.
- Accessible names that include the room and quantity, e.g. “Decrease occupied beds in Dorm A.”
- Disabled boundary states.

### Crisis-support footer

- Keep visually quiet but readable.
- Preserve product-provided emergency wording exactly unless the user asks for content changes.
- Do not hardcode region-specific resources without validated requirements.
