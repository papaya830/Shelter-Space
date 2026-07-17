const STATUS_TONES = {
    REQUESTED: "neutral",
    WAITLISTED: "warn",
    ADMITTED: "success",
    CHECKED_IN: "success",
    REJECTED: "error",
    CANCELLED: "neutral",
    CHECKED_OUT: "neutral",
    OPEN: "success",
    LIMITED: "warn",
    TEMPORARILY_CLOSED: "error",
    SEASONAL: "neutral",
    FULL: "error",
    AVAILABLE: "success"
};

const STAFF_FILTERS = [
    { key: "all", label: "All bookings" },
    { key: "actionable", label: "Needs action" },
    { key: "active", label: "Active stay" },
    { key: "closed", label: "Closed" }
];

const BOOKING_ACTIONS = {
    admit: {
        label: "Admit booking",
        endpoint: "admit",
        helper: "Reserve a bed and mark this request as admitted."
    },
    reject: {
        label: "Reject booking",
        endpoint: "reject",
        helper: "Record that this request cannot move forward tonight."
    },
    "check-in": {
        label: "Check in guest",
        endpoint: "check-in",
        helper: "Confirm that the admitted guest arrived on site."
    },
    "check-out": {
        label: "Check out guest",
        endpoint: "check-out",
        helper: "Release the bed back into live availability."
    }
};

const ENUM_OPTIONS = {
    operationalStatus: ["OPEN", "LIMITED", "TEMPORARILY_CLOSED", "SEASONAL"],
    barrierLevel: ["LOW_BARRIER", "HIGH_BARRIER"],
    populationType: ["ANY_GENDER", "MEN_ONLY", "WOMEN_ONLY", "FAMILY_ONLY", "WOMEN_WITH_CHILDREN", "YOUTH_ONLY"],
    intakeType: ["CALL_AHEAD", "FIRST_COME_FIRST_SERVED", "LINE_UP", "REFERRAL"]
};

const state = {
    route: parseRoute(),
    shelters: [],
    bookings: [],
    loadingShelters: false,
    loadingBookings: false,
    publicSearch: "",
    publicAvailableOnly: false,
    publicWheelchairOnly: false,
    publicPetsOnly: false,
    publicRequestForm: buildEmptyPublicRequestForm(),
    publicRequestErrors: {},
    publicRequestSuccess: null,
    staffBookingFilter: "all",
    staffSelectedBookingId: null,
    staffSelectedShelterId: null,
    staffShelterSearch: "",
    staffShelterForm: null,
    staffShelterErrors: {},
    flash: null,
    connection: { tone: "neutral", label: "Ready" },
    dialogAction: null,
    dialogBookingId: null
};

const elements = {
    root: document.querySelector("#root"),
    dialog: document.querySelector("#booking-action-dialog"),
    dialogForm: document.querySelector("#booking-action-form"),
    dialogTitle: document.querySelector("#dialog-title"),
    dialogActionLabel: document.querySelector("#dialog-action-label"),
    dialogSummary: document.querySelector("#dialog-booking-summary"),
    dialogStaffName: document.querySelector("#dialog-staff-name"),
    dialogNotes: document.querySelector("#dialog-notes"),
    dialogError: document.querySelector("#dialog-error"),
    dialogSubmit: document.querySelector("#dialog-submit")
};

async function init() {
    bindGlobalEvents();
    await ensureDataForRoute({ silent: false });
    render();
}

function bindGlobalEvents() {
    window.addEventListener("hashchange", async () => {
        state.route = parseRoute();
        if (state.route.mode === "public" && state.route.view === "request") {
            hydratePublicRequestForm();
        }
        await ensureDataForRoute({ silent: true });
        render();
    });

    document.querySelector("#dialog-close").addEventListener("click", closeDialog);
    document.querySelector("#dialog-cancel").addEventListener("click", closeDialog);
    elements.dialogForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        await submitStaffBookingAction();
    });
}

function parseRoute() {
    const hash = window.location.hash.replace(/^#\/?/, "");
    const parts = hash.split("/").filter(Boolean);

    if (parts[0] === "staff") {
        const view = ["dashboard", "availability", "settings"].includes(parts[1]) ? parts[1] : "dashboard";
        return { mode: "staff", view };
    }

    if (parts[0] === "shelters" && parts[1]) {
        const shelterId = Number(parts[1]);
        if (parts[2] === "request") {
            return { mode: "public", view: "request", shelterId };
        }
        return { mode: "public", view: "detail", shelterId };
    }

    return { mode: "public", view: "list" };
}

async function ensureDataForRoute({ silent }) {
    const needsShelters = state.shelters.length === 0 || state.route.mode === "public" || state.route.view !== "settings";
    const needsBookings = state.route.mode === "staff";

    const tasks = [];
    if (needsShelters) {
        tasks.push(loadShelters({ silent }));
    }
    if (needsBookings) {
        tasks.push(loadBookings({ silent }));
    }
    if (tasks.length) {
        await Promise.all(tasks);
    }
}

async function loadShelters({ silent }) {
    state.loadingShelters = true;
    updateConnection("neutral", "Loading shelters");
    if (!silent) {
        showFlash("Loading current shelter information.", "success", 1400);
    }
    render();

    try {
        state.shelters = await apiFetch("/api/shelters");
        if (!state.staffSelectedShelterId && state.shelters.length > 0) {
            state.staffSelectedShelterId = state.shelters[0].id;
        }
        if (state.staffSelectedShelterId && !state.shelters.some((shelter) => shelter.id === state.staffSelectedShelterId)) {
            state.staffSelectedShelterId = state.shelters[0]?.id ?? null;
        }
        hydrateStaffShelterForm();
        if (state.route.mode === "public" && state.route.view === "request") {
            hydratePublicRequestForm();
        }
        updateConnection("success", state.route.mode === "staff" ? "Staff data ready" : "Public data ready");
    } catch (error) {
        updateConnection("error", "Shelter load failed");
        showFlash(error.message || "Could not load shelters.", "error");
    } finally {
        state.loadingShelters = false;
    }
}

async function loadBookings({ silent }) {
    state.loadingBookings = true;
    updateConnection("neutral", "Loading bookings");
    render();

    try {
        state.bookings = await apiFetch("/api/bookings");
        if (!state.staffSelectedBookingId && state.bookings.length > 0) {
            state.staffSelectedBookingId = state.bookings[0].id;
        }
        if (state.staffSelectedBookingId && !state.bookings.some((booking) => booking.id === state.staffSelectedBookingId)) {
            state.staffSelectedBookingId = state.bookings[0]?.id ?? null;
        }
        if (!silent) {
            updateConnection("success", "Staff data ready");
        }
    } catch (error) {
        updateConnection("error", "Booking load failed");
        showFlash(error.message || "Could not load bookings.", "error");
    } finally {
        state.loadingBookings = false;
    }
}

function render() {
    elements.root.innerHTML = state.route.mode === "staff" ? renderStaffApp() : renderPublicApp();
    bindViewEvents();
}

function bindViewEvents() {
    const refreshButton = document.querySelector("[data-action='refresh']");
    if (refreshButton) {
        refreshButton.addEventListener("click", async () => {
            if (state.route.mode === "staff") {
                await Promise.all([loadShelters({ silent: false }), loadBookings({ silent: false })]);
            } else {
                await loadShelters({ silent: false });
            }
            render();
        });
    }

    const publicSearch = document.querySelector("#public-search");
    if (publicSearch) {
        publicSearch.addEventListener("input", (event) => {
            state.publicSearch = event.target.value;
            render();
        });
    }

    document.querySelectorAll("[data-public-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const key = button.dataset.publicToggle;
            state[key] = !state[key];
            render();
        });
    });

    document.querySelectorAll("[data-staff-filter]").forEach((button) => {
        button.addEventListener("click", () => {
            state.staffBookingFilter = button.dataset.staffFilter;
            render();
        });
    });

    document.querySelectorAll("[data-staff-booking]").forEach((row) => {
        row.addEventListener("click", () => {
            state.staffSelectedBookingId = Number(row.dataset.staffBooking);
            render();
        });
    });

    document.querySelectorAll("[data-staff-action]").forEach((button) => {
        button.addEventListener("click", () => {
            openDialog(button.dataset.staffAction, Number(button.dataset.bookingId));
        });
    });

    const publicForm = document.querySelector("#public-booking-form");
    if (publicForm) {
        publicForm.addEventListener("input", handlePublicRequestInput);
        publicForm.addEventListener("change", handlePublicRequestInput);
        publicForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await submitPublicBooking();
        });
    }

    const shelterSearch = document.querySelector("#staff-shelter-search");
    if (shelterSearch) {
        shelterSearch.addEventListener("input", (event) => {
            state.staffShelterSearch = event.target.value;
            render();
        });
    }

    document.querySelectorAll("[data-staff-shelter-select]").forEach((button) => {
        button.addEventListener("click", () => {
            state.staffSelectedShelterId = Number(button.dataset.staffShelterSelect);
            hydrateStaffShelterForm();
            render();
        });
    });

    const shelterForm = document.querySelector("#staff-shelter-form");
    if (shelterForm) {
        shelterForm.addEventListener("input", handleStaffShelterInput);
        shelterForm.addEventListener("change", handleStaffShelterInput);
        shelterForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await submitStaffShelterUpdate();
        });
    }
}

function renderPublicApp() {
    return `
        <div class="public-shell">
            <header class="public-header">
                <div class="brand-block">
                    <p class="eyebrow">Shelter-Space</p>
                    <h1>Find shelter options tonight</h1>
                    <p class="lede">Browse shelter details, check intake requirements, and send a basic booking request from a phone-friendly public interface.</p>
                </div>
                <div class="header-actions">
                    <a class="button ghost" href="#/staff/dashboard">Staff tools</a>
                    <button class="button secondary" data-action="refresh">Refresh shelters</button>
                </div>
            </header>

            ${renderFlash()}

            <main class="public-main">
                ${renderPublicContent()}
            </main>
        </div>
    `;
}

function renderPublicContent() {
    if (state.loadingShelters && state.shelters.length === 0) {
        return renderEmptyState("Loading shelters", "Fetching the current shelter list and intake information.");
    }

    if (state.route.view === "detail") {
        return renderPublicShelterDetail();
    }
    if (state.route.view === "request") {
        return renderPublicRequestView();
    }
    return renderPublicShelterList();
}

function renderPublicShelterList() {
    const shelters = getFilteredPublicShelters();

    return `
        <section class="public-tools panel">
            <div>
                <p class="eyebrow">Public shelter list</p>
                <h2>Browse shelters</h2>
                <p class="helper-text">Look for availability, intake rules, accessibility, and who each shelter serves.</p>
            </div>
            <label class="field">
                <span>Search by shelter name or city</span>
                <input id="public-search" value="${escapeHtml(state.publicSearch)}" placeholder="Try Vancouver, youth, family">
            </label>
            <div class="toggle-row">
                ${renderPublicFilterChip("publicAvailableOnly", "Only show shelters with space", state.publicAvailableOnly)}
                ${renderPublicFilterChip("publicWheelchairOnly", "Wheelchair accessible", state.publicWheelchairOnly)}
                ${renderPublicFilterChip("publicPetsOnly", "Pets allowed", state.publicPetsOnly)}
            </div>
        </section>

        <section class="public-card-grid">
            ${shelters.length === 0
                ? renderEmptyState("No shelters match these filters.", "Clear a filter or search for a different city.")
                : shelters.map(renderPublicShelterCard).join("")}
        </section>
    `;
}

function renderPublicShelterCard(shelter) {
    const availability = getAvailabilityLabel(shelter);
    return `
        <article class="shelter-card panel">
            <div class="card-top">
                <div>
                    <p class="eyebrow">${escapeHtml(shelter.city)}</p>
                    <h3>${escapeHtml(shelter.name)}</h3>
                    <p class="helper-text">${escapeHtml(shelter.confidentialAddress ? "Address shared after intake" : shelter.address)}</p>
                </div>
                ${renderChip(availability.label, availability.tone)}
            </div>

            <div class="chip-row">
                ${renderChip(formatLabel(shelter.operationalStatus), STATUS_TONES[shelter.operationalStatus])}
                ${renderChip(formatLabel(shelter.populationType), "neutral")}
                ${renderChip(formatLabel(shelter.barrierLevel), "neutral")}
            </div>

            <div class="fact-grid">
                <div class="fact-box"><span>Open beds</span><strong>${shelter.availableBeds}</strong></div>
                <div class="fact-box"><span>Capacity</span><strong>${shelter.currentOccupancy}/${shelter.totalCapacity}</strong></div>
            </div>

            <div class="chip-row">
                ${renderFlagChip("Wheelchair accessible", shelter.wheelchairAccessible)}
                ${renderFlagChip("Pets allowed", shelter.petsAllowed)}
                ${renderFlagChip("Call ahead required", shelter.callAheadRequired)}
                ${renderFlagChip("Open 24 hours", shelter.open24Hours)}
            </div>

            <p class="helper-text">${escapeHtml(shelter.intakeInstructions || "See detail view for intake directions and shelter rules.")}</p>

            <div class="card-actions">
                <a class="button secondary" href="#/shelters/${shelter.id}">View details</a>
                <a class="button" href="#/shelters/${shelter.id}/request">Request a bed</a>
            </div>
        </article>
    `;
}

function renderPublicShelterDetail() {
    const shelter = getRouteShelter();
    if (!shelter) {
        return renderEmptyState("Shelter not found", "The shelter you selected is not available in the current list.");
    }

    const availability = getAvailabilityLabel(shelter);
    return `
        <section class="detail-layout">
            <article class="panel detail-hero">
                <div class="detail-head">
                    <div>
                        <p class="eyebrow">${escapeHtml(shelter.city)}</p>
                        <h2>${escapeHtml(shelter.name)}</h2>
                        <p class="helper-text">${escapeHtml(shelter.confidentialAddress ? "Address shared after screening or intake" : shelter.address)}</p>
                    </div>
                    ${renderChip(availability.label, availability.tone)}
                </div>

                <div class="chip-row">
                    ${renderChip(formatLabel(shelter.operationalStatus), STATUS_TONES[shelter.operationalStatus])}
                    ${renderChip(formatLabel(shelter.populationType), "neutral")}
                    ${renderChip(formatLabel(shelter.barrierLevel), "neutral")}
                    ${renderChip(formatLabel(shelter.intakeType), "neutral")}
                </div>

                <div class="fact-grid fact-grid-wide">
                    <div class="fact-box"><span>Open beds</span><strong>${shelter.availableBeds}</strong></div>
                    <div class="fact-box"><span>Current occupancy</span><strong>${shelter.currentOccupancy}/${shelter.totalCapacity}</strong></div>
                    <div class="fact-box"><span>Phone</span><strong>${escapeHtml(shelter.phoneNumber || "Not listed")}</strong></div>
                    <div class="fact-box"><span>Intake time</span><strong>${formatIntakeWindow(shelter)}</strong></div>
                </div>

                <div class="card-actions">
                    <a class="button ghost" href="#/shelters">Back to list</a>
                    <a class="button" href="#/shelters/${shelter.id}/request">Request a bed</a>
                </div>
            </article>

            <aside class="panel detail-panel">
                <h3>Important flags</h3>
                <div class="chip-row">
                    ${renderFlagChip("Wheelchair accessible", shelter.wheelchairAccessible)}
                    ${renderFlagChip("Pets allowed", shelter.petsAllowed)}
                    ${renderFlagChip("Call ahead required", shelter.callAheadRequired)}
                    ${renderFlagChip("Open 24 hours", shelter.open24Hours)}
                    ${renderFlagChip("Legal name required", shelter.legalNameRequired)}
                    ${renderFlagChip("Large items accepted", shelter.acceptsLargeItems)}
                </div>
            </aside>

            <article class="panel detail-section">
                <h3>Intake instructions</h3>
                <p>${escapeHtml(shelter.intakeInstructions || "No intake instructions listed yet.")}</p>
            </article>

            <article class="panel detail-section">
                <h3>Rules and stay details</h3>
                <div class="detail-copy">
                    <p><strong>Rules:</strong> ${escapeHtml(shelter.rules || "No rules listed.")}</p>
                    <p><strong>Programs:</strong> ${escapeHtml(shelter.programs || "No programs listed.")}</p>
                    <p><strong>Perks:</strong> ${escapeHtml(shelter.perks || "No perks listed.")}</p>
                    <p><strong>Notes:</strong> ${escapeHtml(shelter.notes || "No extra notes listed.")}</p>
                </div>
            </article>

            <article class="panel detail-section">
                <h3>Eligibility</h3>
                <div class="detail-copy">
                    <p><strong>Population served:</strong> ${escapeHtml(formatLabel(shelter.populationType))}</p>
                    <p><strong>Barrier level:</strong> ${escapeHtml(formatLabel(shelter.barrierLevel))}</p>
                    <p><strong>Max stay:</strong> ${escapeHtml(shelter.maxStayDays ? `${shelter.maxStayDays} days` : "Not specified")}</p>
                    <p><strong>Age range:</strong> ${escapeHtml(formatAgeRange(shelter))}</p>
                </div>
            </article>
        </section>
    `;
}

function renderPublicRequestView() {
    const shelter = getRouteShelter();
    if (!shelter) {
        return renderEmptyState("Shelter not found", "Go back to the shelter list and choose a shelter before sending a request.");
    }

    return `
        <section class="request-layout">
            <article class="panel request-panel">
                <p class="eyebrow">Request a bed</p>
                <h2>${escapeHtml(shelter.name)}</h2>
                <p class="helper-text">${escapeHtml(shelter.intakeInstructions || "Staff will review your request using the shelter details already on file.")}</p>
                <div class="chip-row">
                    ${renderChip(getAvailabilityLabel(shelter).label, getAvailabilityLabel(shelter).tone)}
                    ${renderChip(formatLabel(shelter.populationType), "neutral")}
                    ${renderChip(formatLabel(shelter.barrierLevel), "neutral")}
                </div>
                ${state.publicRequestSuccess ? `
                    <div class="flash success">
                        Request sent. Booking #${state.publicRequestSuccess.id} is now ${formatLabel(state.publicRequestSuccess.status)}.
                    </div>
                ` : ""}
            </article>

            <form id="public-booking-form" class="panel request-form">
                <div class="form-grid">
                    ${renderInputField("displayName", "Display name or alias", state.publicRequestForm.displayName, true, state.publicRequestErrors.displayName)}
                    ${renderInputField("requestedBedDate", "Requested bed date", state.publicRequestForm.requestedBedDate, true, state.publicRequestErrors.requestedBedDate, "date")}
                    ${renderInputField("phoneNumber", "Phone number (optional)", state.publicRequestForm.phoneNumber, false, state.publicRequestErrors.phoneNumber, "tel")}
                    ${renderInputField("birthDate", "Birth date (optional)", state.publicRequestForm.birthDate, false, state.publicRequestErrors.birthDate, "date")}
                    ${renderInputField("legalName", "Legal name (optional)", state.publicRequestForm.legalName, false, state.publicRequestErrors.legalName)}
                    ${renderInputField("requestedBy", "Helper or agency name (optional)", state.publicRequestForm.requestedBy, false, state.publicRequestErrors.requestedBy)}
                </div>
                ${renderTextAreaField("intakeNotes", "Anything staff should know? (optional)", state.publicRequestForm.intakeNotes, state.publicRequestErrors.intakeNotes)}
                ${state.publicRequestErrors.message ? `<div class="form-error">${escapeHtml(state.publicRequestErrors.message)}</div>` : ""}
                <div class="card-actions">
                    <a class="button ghost" href="#/shelters/${shelter.id}">Back to shelter details</a>
                    <button class="button" type="submit">Send booking request</button>
                </div>
                <p class="helper-text">This MVP request uses the live booking API and sends a minimal guest profile to staff for review.</p>
            </form>
        </section>
    `;
}

function renderStaffApp() {
    return `
        <div class="staff-shell">
            <aside class="sidebar">
                <div class="brand">
                    <p class="eyebrow">Shelter-Space</p>
                    <h1>Staff Console</h1>
                    <p class="brand-copy">Manage booking decisions, check-ins, shelter capacity, and intake settings while public users browse the live shelter list.</p>
                </div>

                <nav class="nav">
                    <a href="#/staff/dashboard" class="${state.route.view === "dashboard" ? "active" : ""}">Booking Queue</a>
                    <a href="#/staff/availability" class="${state.route.view === "availability" ? "active" : ""}">Shelter Availability</a>
                    <a href="#/staff/settings" class="${state.route.view === "settings" ? "active" : ""}">Shelter Config</a>
                    <a href="#/shelters">Public UI</a>
                </nav>

                <section class="sidebar-panel">
                    <p class="panel-label">Operational Snapshot</p>
                    <div class="metrics-stack">
                        <div class="metric-row"><span>Actionable bookings</span><strong>${state.bookings.filter((booking) => ["REQUESTED", "WAITLISTED", "ADMITTED"].includes(booking.status)).length}</strong></div>
                        <div class="metric-row"><span>Open beds</span><strong>${sumAvailableBeds()}</strong></div>
                        <div class="metric-row"><span>Total occupancy</span><strong>${sumCurrentOccupancy()}</strong></div>
                    </div>
                </section>
            </aside>

            <main class="content">
                <header class="topbar">
                    <div>
                        <p class="eyebrow">Staff operations</p>
                        <h2>${state.route.view === "dashboard" ? "Booking Queue" : state.route.view === "availability" ? "Shelter Availability" : "Shelter Config"}</h2>
                    </div>
                    <div class="topbar-actions">
                        <button class="button secondary" data-action="refresh">Refresh data</button>
                        <div class="status-pill ${state.connection.tone}">${escapeHtml(state.connection.label)}</div>
                    </div>
                </header>

                ${renderFlash()}
                ${renderStaffContent()}
            </main>
        </div>
    `;
}

function renderStaffContent() {
    if (state.route.view === "availability") {
        return renderStaffAvailability();
    }
    if (state.route.view === "settings") {
        return renderStaffSettings();
    }
    return renderStaffDashboard();
}

function renderStaffDashboard() {
    const filteredBookings = getFilteredStaffBookings();
    const selectedBooking = getSelectedStaffBooking();

    return `
        <section class="stats-grid">
            ${renderStatCard("Pending review", countBookings(["REQUESTED", "WAITLISTED"]))}
            ${renderStatCard("Admitted", countBookings(["ADMITTED"]))}
            ${renderStatCard("Checked in", countBookings(["CHECKED_IN"]))}
            ${renderStatCard("Closed outcomes", countBookings(["REJECTED", "CANCELLED", "CHECKED_OUT"]))}
        </section>

        <section class="split-layout">
            <div class="table-shell">
                <div class="table-tools">
                    <div>
                        <p class="eyebrow">Booking workflow</p>
                        <h3>Live intake queue</h3>
                    </div>
                    <div class="filter-group">
                        ${STAFF_FILTERS.map((filter) => `
                            <button class="filter-chip ${filter.key === state.staffBookingFilter ? "active" : ""}" data-staff-filter="${filter.key}">
                                ${filter.label}
                            </button>
                        `).join("")}
                    </div>
                </div>

                ${state.loadingBookings && state.bookings.length === 0 ? renderEmptyState("Loading bookings", "Fetching staff review data.") : filteredBookings.length === 0
                    ? renderEmptyState("No bookings match this filter.", "Try a different booking state.")
                    : `
                        <table>
                            <thead>
                                <tr>
                                    <th>Guest</th>
                                    <th>Status</th>
                                    <th>Shelter</th>
                                    <th>Channel</th>
                                    <th>Requested bed</th>
                                    <th>Requested at</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${filteredBookings.map(renderStaffBookingRow).join("")}
                            </tbody>
                        </table>
                    `}
            </div>

            <aside class="panel-card">
                ${selectedBooking ? renderStaffBookingDetail(selectedBooking) : renderEmptyState("Select a booking", "Choose a booking row to review notes and next steps.")}
            </aside>
        </section>
    `;
}

function renderStaffAvailability() {
    return `
        <section class="section-header">
            <div>
                <p class="eyebrow">Capacity overview</p>
                <h3>Shelter availability</h3>
            </div>
            <div class="helper-text">Live values come from shelter capacity plus booking lifecycle actions.</div>
        </section>
        <section class="availability-grid">
            ${state.shelters.map(renderStaffAvailabilityCard).join("")}
        </section>
    `;
}

function renderStaffSettings() {
    const shelter = getSelectedStaffShelter();
    const visibleShelters = getVisibleStaffShelters();

    return `
        <section class="split-layout">
            <aside class="panel-card">
                <div class="panel-header">
                    <div>
                        <p class="eyebrow">Shelter records</p>
                        <h3>Edit an existing shelter</h3>
                    </div>
                </div>

                <label class="field">
                    <span>Find shelter</span>
                    <input id="staff-shelter-search" value="${escapeHtml(state.staffShelterSearch)}" placeholder="Search by name or city">
                </label>

                <div class="detail-grid">
                    ${visibleShelters.map((entry) => `
                        <button class="button secondary" data-staff-shelter-select="${entry.id}">
                            ${escapeHtml(entry.name)} · ${escapeHtml(entry.city)}
                        </button>
                    `).join("") || `<div class="helper-text">No shelters match this search.</div>`}
                </div>
            </aside>

            <section class="form-shell">
                ${shelter ? renderStaffShelterForm(shelter) : renderEmptyState("No shelter selected", "Choose a shelter to update its staff-facing configuration.")}
            </section>
        </section>
    `;
}

function renderStaffBookingRow(booking) {
    return `
        <tr data-staff-booking="${booking.id}" class="${booking.id === state.staffSelectedBookingId ? "selected" : ""}">
            <td data-label="Guest">
                <div class="booking-main">
                    <strong>${escapeHtml(booking.guest.displayName)}</strong>
                    <span class="booking-meta">${escapeHtml(booking.requestedBy || "No requester noted")}</span>
                </div>
            </td>
            <td data-label="Status">${renderChip(formatLabel(booking.status), STATUS_TONES[booking.status])}</td>
            <td data-label="Shelter">
                <div class="booking-main">
                    <strong>${escapeHtml(booking.shelter.name)}</strong>
                    <span class="booking-meta">${escapeHtml(booking.shelter.city)}</span>
                </div>
            </td>
            <td data-label="Channel">${escapeHtml(formatLabel(booking.requestChannel))}</td>
            <td data-label="Requested bed">${escapeHtml(formatDate(booking.requestedBedDate))}</td>
            <td data-label="Requested at">${escapeHtml(formatDateTime(booking.requestedAt))}</td>
        </tr>
    `;
}

function renderStaffBookingDetail(booking) {
    const actions = getAllowedStaffActions(booking);
    return `
        <div>
            <p class="eyebrow">Selected booking</p>
            <h3>${escapeHtml(booking.guest.displayName)}</h3>
            <p class="helper-text">${escapeHtml(booking.shelter.name)} · ${escapeHtml(booking.shelter.city)}</p>
        </div>

        <div class="detail-grid">
            <div><strong>Status</strong>${renderChip(formatLabel(booking.status), STATUS_TONES[booking.status])}</div>
            <div><strong>Request channel</strong>${escapeHtml(formatLabel(booking.requestChannel))}</div>
            <div><strong>Requested bed date</strong>${escapeHtml(formatDate(booking.requestedBedDate))}</div>
            <div><strong>Requested at</strong>${escapeHtml(formatDateTime(booking.requestedAt))}</div>
            <div><strong>Decision notes</strong>${escapeHtml(booking.decisionNotes || "None recorded")}</div>
            <div><strong>Intake notes</strong>${escapeHtml(booking.intakeNotes || "None recorded")}</div>
            <div><strong>Bed availability</strong>${escapeHtml(`${booking.shelter.availableBeds} beds open now`)}</div>
            <div><strong>Guest phone</strong>${escapeHtml(booking.guest.phoneNumber || "Not provided")}</div>
        </div>

        <div>
            <p class="panel-label">Next actions</p>
            <div class="actions-row">
                ${actions.map((action) => `
                    <button class="button inline" data-staff-action="${action}" data-booking-id="${booking.id}">
                        ${BOOKING_ACTIONS[action].label}
                    </button>
                `).join("") || `<div class="helper-text">No staff action available for this booking state.</div>`}
            </div>
        </div>
    `;
}

function renderStaffAvailabilityCard(shelter) {
    const occupancyRate = shelter.totalCapacity > 0 ? Math.round((shelter.currentOccupancy / shelter.totalCapacity) * 100) : 0;
    return `
        <article class="availability-card panel">
            <div class="card-top">
                <div>
                    <p class="eyebrow">${escapeHtml(shelter.city)}</p>
                    <h3>${escapeHtml(shelter.name)}</h3>
                    <p class="helper-text">${escapeHtml(shelter.address)}</p>
                </div>
                ${renderChip(`${shelter.availableBeds} beds open`, occupancyRate >= 100 ? "error" : occupancyRate >= 85 ? "warn" : "success")}
            </div>

            <div class="occupancy-row">
                <strong>${shelter.currentOccupancy}/${shelter.totalCapacity}</strong>
                <span>${occupancyRate}% occupied</span>
            </div>
            <div class="occupancy-track">
                <div class="occupancy-fill ${occupancyRate >= 95 ? "critical" : ""}" style="width: ${Math.min(occupancyRate, 100)}%"></div>
            </div>

            <div class="fact-grid">
                <div class="fact-box"><span>Status</span>${escapeHtml(formatLabel(shelter.operationalStatus))}</div>
                <div class="fact-box"><span>Intake</span>${escapeHtml(formatLabel(shelter.intakeType))}</div>
                <div class="fact-box"><span>Barrier</span>${escapeHtml(formatLabel(shelter.barrierLevel))}</div>
                <div class="fact-box"><span>Population</span>${escapeHtml(formatLabel(shelter.populationType))}</div>
            </div>
        </article>
    `;
}

function renderStaffShelterForm(shelter) {
    const form = state.staffShelterForm;
    return `
        <div class="section-header">
            <div>
                <p class="eyebrow">Editing shelter</p>
                <h3>${escapeHtml(shelter.name)}</h3>
            </div>
            <div class="helper-text">Occupancy stays read-only because it is controlled through booking actions.</div>
        </div>

        <form id="staff-shelter-form" class="form-grid">
            ${renderInputField("name", "Shelter name", form.name, true, state.staffShelterErrors.name)}
            ${renderInputField("organizationName", "Organization", form.organizationName, false, state.staffShelterErrors.organizationName)}
            ${renderInputField("city", "City", form.city, true, state.staffShelterErrors.city)}
            ${renderInputField("address", "Address", form.address, true, state.staffShelterErrors.address)}
            ${renderInputField("phoneNumber", "Phone number", form.phoneNumber, false, state.staffShelterErrors.phoneNumber)}
            ${renderInputField("totalCapacity", "Total capacity", form.totalCapacity, true, state.staffShelterErrors.totalCapacity, "number")}
            ${renderSelectField("operationalStatus", "Operational status", form.operationalStatus, ENUM_OPTIONS.operationalStatus, state.staffShelterErrors.operationalStatus)}
            ${renderSelectField("barrierLevel", "Barrier level", form.barrierLevel, ENUM_OPTIONS.barrierLevel, state.staffShelterErrors.barrierLevel)}
            ${renderSelectField("populationType", "Population type", form.populationType, ENUM_OPTIONS.populationType, state.staffShelterErrors.populationType)}
            ${renderSelectField("intakeType", "Intake type", form.intakeType, ENUM_OPTIONS.intakeType, state.staffShelterErrors.intakeType)}
            ${renderInputField("intakeStartTime", "Intake start time", form.intakeStartTime, false, state.staffShelterErrors.intakeStartTime, "time")}
            ${renderInputField("intakeCutoffTime", "Intake cutoff time", form.intakeCutoffTime, false, state.staffShelterErrors.intakeCutoffTime, "time")}
            ${renderInputField("maxStayDays", "Max stay days", form.maxStayDays, false, state.staffShelterErrors.maxStayDays, "number")}
            ${renderInputField("minimumAge", "Minimum age", form.minimumAge, false, state.staffShelterErrors.minimumAge, "number")}
            ${renderInputField("maximumAge", "Maximum age", form.maximumAge, false, state.staffShelterErrors.maximumAge, "number")}
            ${renderTextAreaField("intakeInstructions", "Intake instructions", form.intakeInstructions, state.staffShelterErrors.intakeInstructions)}
            ${renderTextAreaField("programs", "Programs", form.programs, state.staffShelterErrors.programs)}
            ${renderTextAreaField("rules", "Rules", form.rules, state.staffShelterErrors.rules)}
            ${renderTextAreaField("perks", "Perks", form.perks, state.staffShelterErrors.perks)}
            ${renderTextAreaField("notes", "Notes", form.notes, state.staffShelterErrors.notes)}

            <label class="field readonly">
                <span>Current occupancy</span>
                <input value="${escapeHtml(String(shelter.currentOccupancy))}" readonly>
            </label>
            <label class="field readonly">
                <span>Available beds</span>
                <input value="${escapeHtml(String(shelter.availableBeds))}" readonly>
            </label>

            <div class="toggles-grid">
                ${renderToggleField("confidentialAddress", "Confidential address", form.confidentialAddress)}
                ${renderToggleField("open24Hours", "Open 24 hours", form.open24Hours)}
                ${renderToggleField("callAheadRequired", "Call ahead required", form.callAheadRequired)}
                ${renderToggleField("petsAllowed", "Pets allowed", form.petsAllowed)}
                ${renderToggleField("wheelchairAccessible", "Wheelchair accessible", form.wheelchairAccessible)}
                ${renderToggleField("acceptsLargeItems", "Accepts large items", form.acceptsLargeItems)}
                ${renderToggleField("legalNameRequired", "Legal name required", form.legalNameRequired)}
            </div>

            <div class="panel form-summary">
                ${state.staffShelterErrors.message ? `<div class="form-error">${escapeHtml(state.staffShelterErrors.message)}</div>` : ""}
                <div class="card-actions">
                    <button class="button" type="submit">Save shelter</button>
                </div>
            </div>
        </form>
    `;
}

function renderPublicFilterChip(key, label, active) {
    return `<button class="filter-chip ${active ? "active" : ""}" data-public-toggle="${key}">${escapeHtml(label)}</button>`;
}

function renderStatCard(label, value) {
    return `
        <article class="stat-card panel">
            <p class="eyebrow">${escapeHtml(label)}</p>
            <strong>${escapeHtml(String(value))}</strong>
        </article>
    `;
}

function renderInputField(name, label, value, required = false, error = "", type = "text") {
    return `
        <label class="field">
            <span>${escapeHtml(label)}</span>
            <input name="${name}" type="${type}" value="${escapeHtml(value ?? "")}" ${required ? "required" : ""}>
            ${error ? `<div class="field-error">${escapeHtml(error)}</div>` : ""}
        </label>
    `;
}

function renderSelectField(name, label, value, options, error = "") {
    return `
        <label class="field">
            <span>${escapeHtml(label)}</span>
            <select name="${name}">
                ${options.map((option) => `<option value="${option}" ${option === value ? "selected" : ""}>${escapeHtml(formatLabel(option))}</option>`).join("")}
            </select>
            ${error ? `<div class="field-error">${escapeHtml(error)}</div>` : ""}
        </label>
    `;
}

function renderTextAreaField(name, label, value, error = "") {
    return `
        <label class="field field-full">
            <span>${escapeHtml(label)}</span>
            <textarea name="${name}" rows="4">${escapeHtml(value ?? "")}</textarea>
            ${error ? `<div class="field-error">${escapeHtml(error)}</div>` : ""}
        </label>
    `;
}

function renderToggleField(name, label, checked) {
    return `
        <label class="toggle">
            <input type="checkbox" name="${name}" ${checked ? "checked" : ""}>
            <span>${escapeHtml(label)}</span>
        </label>
    `;
}

function renderFlash() {
    if (!state.flash) {
        return "";
    }
    return `<div class="flash ${state.flash.tone}">${escapeHtml(state.flash.message)}</div>`;
}

function renderEmptyState(title, message) {
    return `
        <div class="empty-state panel">
            <h3>${escapeHtml(title)}</h3>
            <p class="helper-text">${escapeHtml(message)}</p>
        </div>
    `;
}

function getFilteredPublicShelters() {
    const query = state.publicSearch.trim().toLowerCase();

    return state.shelters.filter((shelter) => {
        if (query && !`${shelter.name} ${shelter.city} ${shelter.populationType}`.toLowerCase().includes(query)) {
            return false;
        }
        if (state.publicAvailableOnly && shelter.availableBeds <= 0) {
            return false;
        }
        if (state.publicWheelchairOnly && !shelter.wheelchairAccessible) {
            return false;
        }
        if (state.publicPetsOnly && !shelter.petsAllowed) {
            return false;
        }
        return true;
    });
}

function getRouteShelter() {
    return state.shelters.find((shelter) => shelter.id === state.route.shelterId) || null;
}

function getFilteredStaffBookings() {
    return state.bookings.filter((booking) => {
        if (state.staffBookingFilter === "actionable") {
            return ["REQUESTED", "WAITLISTED"].includes(booking.status);
        }
        if (state.staffBookingFilter === "active") {
            return ["ADMITTED", "CHECKED_IN"].includes(booking.status);
        }
        if (state.staffBookingFilter === "closed") {
            return ["REJECTED", "CANCELLED", "CHECKED_OUT"].includes(booking.status);
        }
        return true;
    });
}

function getSelectedStaffBooking() {
    return state.bookings.find((booking) => booking.id === state.staffSelectedBookingId) || null;
}

function getSelectedStaffShelter() {
    return state.shelters.find((shelter) => shelter.id === state.staffSelectedShelterId) || null;
}

function getVisibleStaffShelters() {
    const needle = state.staffShelterSearch.trim().toLowerCase();
    if (!needle) {
        return state.shelters;
    }
    return state.shelters.filter((shelter) => `${shelter.name} ${shelter.city}`.toLowerCase().includes(needle));
}

function getAllowedStaffActions(booking) {
    if (["REQUESTED", "WAITLISTED"].includes(booking.status)) {
        return ["admit", "reject"];
    }
    if (booking.status === "ADMITTED") {
        return ["check-in", "check-out"];
    }
    if (booking.status === "CHECKED_IN") {
        return ["check-out"];
    }
    return [];
}

function getAvailabilityLabel(shelter) {
    if (shelter.operationalStatus === "TEMPORARILY_CLOSED") {
        return { label: "Temporarily closed", tone: "error" };
    }
    if (shelter.availableBeds <= 0) {
        return { label: "No beds showing", tone: "warn" };
    }
    return { label: `${shelter.availableBeds} beds available`, tone: "success" };
}

function renderChip(label, tone = "neutral") {
    return `<span class="chip ${tone}">${escapeHtml(label)}</span>`;
}

function renderFlagChip(label, enabled) {
    return renderChip(label, enabled ? "success" : "neutral");
}

function handlePublicRequestInput(event) {
    if (!event.target.name) {
        return;
    }
    state.publicRequestErrors = {};
    state.publicRequestSuccess = null;
    state.publicRequestForm[event.target.name] = event.target.value;
}

async function submitPublicBooking() {
    const shelter = getRouteShelter();
    if (!shelter) {
        return;
    }

    state.publicRequestErrors = {};
    const payload = {
        shelterId: shelter.id,
        displayName: state.publicRequestForm.displayName.trim(),
        legalName: normalizeBlank(state.publicRequestForm.legalName),
        phoneNumber: normalizeBlank(state.publicRequestForm.phoneNumber),
        birthDate: normalizeBlank(state.publicRequestForm.birthDate),
        requestedBedDate: state.publicRequestForm.requestedBedDate,
        requestedBy: normalizeBlank(state.publicRequestForm.requestedBy),
        intakeNotes: normalizeBlank(state.publicRequestForm.intakeNotes)
    };

    try {
        const response = await apiFetch("/api/bookings/public", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        state.publicRequestSuccess = response;
        state.publicRequestErrors = {};
        showFlash("Booking request sent to staff for review.", "success");
        state.publicRequestForm = buildEmptyPublicRequestForm();
        hydratePublicRequestForm();
        await Promise.all([loadShelters({ silent: true }), loadBookings({ silent: true })]);
    } catch (error) {
        state.publicRequestErrors = extractFieldErrors(error);
        if (!Object.keys(state.publicRequestErrors).length) {
            state.publicRequestErrors = { message: error.message };
        }
        showFlash(error.message || "Could not send booking request.", "error");
    } finally {
        render();
    }
}

function hydratePublicRequestForm() {
    const currentShelter = getRouteShelter();
    state.publicRequestForm = {
        ...buildEmptyPublicRequestForm(),
        shelterId: currentShelter?.id ?? null
    };
    state.publicRequestErrors = {};
}

function handleStaffShelterInput(event) {
    if (!event.target.name || !state.staffShelterForm) {
        return;
    }
    state.staffShelterErrors = {};
    state.staffShelterForm[event.target.name] = event.target.type === "checkbox"
        ? event.target.checked
        : event.target.value;
}

function hydrateStaffShelterForm() {
    const shelter = getSelectedStaffShelter();
    state.staffShelterErrors = {};
    state.staffShelterForm = shelter
        ? {
            name: shelter.name ?? "",
            organizationName: shelter.organizationName ?? "",
            city: shelter.city ?? "",
            address: shelter.address ?? "",
            confidentialAddress: Boolean(shelter.confidentialAddress),
            phoneNumber: shelter.phoneNumber ?? "",
            operationalStatus: shelter.operationalStatus ?? "OPEN",
            barrierLevel: shelter.barrierLevel ?? "LOW_BARRIER",
            populationType: shelter.populationType ?? "ANY_GENDER",
            intakeType: shelter.intakeType ?? "CALL_AHEAD",
            totalCapacity: shelter.totalCapacity ?? 0,
            open24Hours: Boolean(shelter.open24Hours),
            callAheadRequired: Boolean(shelter.callAheadRequired),
            petsAllowed: Boolean(shelter.petsAllowed),
            wheelchairAccessible: Boolean(shelter.wheelchairAccessible),
            acceptsLargeItems: Boolean(shelter.acceptsLargeItems),
            legalNameRequired: Boolean(shelter.legalNameRequired),
            intakeStartTime: shelter.intakeStartTime ?? "",
            intakeCutoffTime: shelter.intakeCutoffTime ?? "",
            maxStayDays: shelter.maxStayDays ?? "",
            minimumAge: shelter.minimumAge ?? "",
            maximumAge: shelter.maximumAge ?? "",
            programs: shelter.programs ?? "",
            rules: shelter.rules ?? "",
            intakeInstructions: shelter.intakeInstructions ?? "",
            notes: shelter.notes ?? "",
            perks: shelter.perks ?? ""
        }
        : null;
}

async function submitStaffShelterUpdate() {
    const shelter = getSelectedStaffShelter();
    if (!shelter || !state.staffShelterForm) {
        return;
    }

    const payload = {
        ...state.staffShelterForm,
        name: state.staffShelterForm.name.trim(),
        organizationName: normalizeBlank(state.staffShelterForm.organizationName),
        city: state.staffShelterForm.city.trim(),
        address: state.staffShelterForm.address.trim(),
        phoneNumber: normalizeBlank(state.staffShelterForm.phoneNumber),
        totalCapacity: normalizeInteger(state.staffShelterForm.totalCapacity, true),
        intakeStartTime: normalizeBlank(state.staffShelterForm.intakeStartTime),
        intakeCutoffTime: normalizeBlank(state.staffShelterForm.intakeCutoffTime),
        maxStayDays: normalizeInteger(state.staffShelterForm.maxStayDays),
        minimumAge: normalizeInteger(state.staffShelterForm.minimumAge),
        maximumAge: normalizeInteger(state.staffShelterForm.maximumAge),
        programs: normalizeBlank(state.staffShelterForm.programs),
        rules: normalizeBlank(state.staffShelterForm.rules),
        intakeInstructions: normalizeBlank(state.staffShelterForm.intakeInstructions),
        notes: normalizeBlank(state.staffShelterForm.notes),
        perks: normalizeBlank(state.staffShelterForm.perks)
    };

    try {
        await apiFetch(`/api/shelters/${shelter.id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
        showFlash("Shelter details saved.", "success");
        await loadShelters({ silent: true });
    } catch (error) {
        state.staffShelterErrors = extractFieldErrors(error);
        if (!Object.keys(state.staffShelterErrors).length) {
            state.staffShelterErrors = { message: error.message };
        }
        showFlash(error.message || "Could not update shelter.", "error");
    } finally {
        render();
    }
}

function openDialog(action, bookingId) {
    const booking = state.bookings.find((entry) => entry.id === bookingId);
    if (!booking || !BOOKING_ACTIONS[action]) {
        return;
    }

    state.dialogAction = action;
    state.dialogBookingId = bookingId;
    elements.dialogTitle.textContent = BOOKING_ACTIONS[action].label;
    elements.dialogActionLabel.textContent = BOOKING_ACTIONS[action].helper;
    elements.dialogSummary.innerHTML = `
        <strong>${escapeHtml(booking.guest.displayName)}</strong>
        <div>${escapeHtml(booking.shelter.name)} · ${escapeHtml(formatDate(booking.requestedBedDate))}</div>
    `;
    elements.dialogStaffName.value = "";
    elements.dialogNotes.value = "";
    elements.dialogError.textContent = "";
    elements.dialogError.classList.add("hidden");
    elements.dialogSubmit.textContent = BOOKING_ACTIONS[action].label;
    elements.dialog.showModal();
    elements.dialogStaffName.focus();
}

function closeDialog() {
    elements.dialog.close();
    state.dialogAction = null;
    state.dialogBookingId = null;
}

async function submitStaffBookingAction() {
    const booking = getSelectedStaffBooking();
    if (!booking || !state.dialogAction) {
        return;
    }

    elements.dialogSubmit.disabled = true;
    elements.dialogError.classList.add("hidden");

    try {
        await apiFetch(`/api/bookings/${booking.id}/${BOOKING_ACTIONS[state.dialogAction].endpoint}`, {
            method: "POST",
            body: JSON.stringify({
                staffName: elements.dialogStaffName.value.trim(),
                notes: normalizeBlank(elements.dialogNotes.value)
            })
        });
        closeDialog();
        showFlash(`${BOOKING_ACTIONS[state.dialogAction].label} completed.`, "success");
        await Promise.all([loadBookings({ silent: true }), loadShelters({ silent: true })]);
        render();
    } catch (error) {
        elements.dialogError.textContent = error.message || "Could not update booking.";
        elements.dialogError.classList.remove("hidden");
    } finally {
        elements.dialogSubmit.disabled = false;
    }
}

async function apiFetch(path, options = {}) {
    const response = await fetch(path, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    if (!response.ok) {
        let payload;
        try {
            payload = await response.json();
        } catch (error) {
            payload = { message: `Request failed with status ${response.status}` };
        }
        const requestError = new Error(payload.message || "Request failed");
        requestError.status = response.status;
        requestError.payload = payload;
        throw requestError;
    }

    if (response.status === 204) {
        return null;
    }
    return response.json();
}

function updateConnection(tone, label) {
    state.connection = { tone, label };
}

function showFlash(message, tone, timeout = 3200) {
    state.flash = { message, tone };
    window.clearTimeout(showFlash.timer);
    showFlash.timer = window.setTimeout(() => {
        state.flash = null;
        render();
    }, timeout);
}

function buildEmptyPublicRequestForm() {
    return {
        shelterId: null,
        displayName: "",
        legalName: "",
        phoneNumber: "",
        birthDate: "",
        requestedBedDate: new Date().toISOString().slice(0, 10),
        requestedBy: "",
        intakeNotes: ""
    };
}

function countBookings(statuses) {
    return state.bookings.filter((booking) => statuses.includes(booking.status)).length;
}

function sumAvailableBeds() {
    return state.shelters.reduce((sum, shelter) => sum + (shelter.availableBeds ?? 0), 0);
}

function sumCurrentOccupancy() {
    return state.shelters.reduce((sum, shelter) => sum + (shelter.currentOccupancy ?? 0), 0);
}

function formatLabel(value) {
    return String(value)
        .toLowerCase()
        .split("_")
        .map((chunk) => chunk.charAt(0).toUpperCase() + chunk.slice(1))
        .join(" ");
}

function formatDate(value) {
    if (!value) {
        return "Not set";
    }
    return new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function formatDateTime(value) {
    if (!value) {
        return "Not set";
    }
    return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function formatIntakeWindow(shelter) {
    if (shelter.open24Hours) {
        return "24 hours";
    }
    if (shelter.intakeStartTime && shelter.intakeCutoffTime) {
        return `${formatSimpleTime(shelter.intakeStartTime)} to ${formatSimpleTime(shelter.intakeCutoffTime)}`;
    }
    return "See shelter instructions";
}

function formatSimpleTime(value) {
    const [hour, minute] = value.split(":");
    return new Intl.DateTimeFormat(undefined, { timeStyle: "short" }).format(new Date(`2026-07-17T${hour}:${minute}:00`));
}

function formatAgeRange(shelter) {
    if (shelter.minimumAge && shelter.maximumAge) {
        return `${shelter.minimumAge} to ${shelter.maximumAge}`;
    }
    if (shelter.minimumAge) {
        return `${shelter.minimumAge}+`;
    }
    if (shelter.maximumAge) {
        return `Up to ${shelter.maximumAge}`;
    }
    return "Not specified";
}

function normalizeBlank(value) {
    if (value == null) {
        return null;
    }
    const trimmed = String(value).trim();
    return trimmed ? trimmed : null;
}

function normalizeInteger(value, required = false) {
    if (value === "" || value == null) {
        return required ? 0 : null;
    }
    return Number(value);
}

function extractFieldErrors(error) {
    return error?.payload?.fields || {};
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

init();
