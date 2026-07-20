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

const CHAT_REPLY_LABELS = {
    BED: "Find a bed",
    STATUS: "Check my request",
    HELP: "See options",
    CANCEL: "Cancel request",
    DIR: "Get directions",
    YES: "Yes",
    NO: "No",
    MORE: "Show more shelters"
};

const CHAT_REPLY_HINTS = {
    BED: "Start a new bed request.",
    STATUS: "Check your latest request status.",
    HELP: "See what this chat can do.",
    CANCEL: "Cancel a pending request.",
    DIR: "Get shelter address and phone details.",
    YES: "Confirm and send this step.",
    NO: "Decline or go back for this step.",
    MORE: "See the next list of shelter options."
};

const APP_NOW = new Date("2026-07-18T12:00:00-07:00");

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
    waitlist: {
        label: "Waitlist booking",
        endpoint: "waitlist",
        helper: "Keep this request active while staff wait for a bed to open."
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
    intakeType: ["CALL_AHEAD", "FIRST_COME_FIRST_SERVED", "LINE_UP", "REFERRAL"],
    turnAwayReason: ["NO_BEDS_AVAILABLE", "INTAKE_CLOSED", "INELIGIBLE", "BEHAVIOUR_RESTRICTION", "REFERRED_ELSEWHERE", "OTHER"]
};

const SHELTER_CACHE_KEY = "ss_shelters_v2";
const FILTER_CACHE_KEY = "ss_filters_v1";
const DEVICE_ID_KEY = "ss_device_id";
const INTEREST_CACHE_PREFIX = "ss_interest_";
const LATENCY_WARN_MS = 4000;

const state = {
    route: parseRoute(),
    shelters: [],
    bookings: [],
    turnAwayLogs: [],
    loadingShelters: false,
    loadingBookings: false,
    loadingTurnAwayLogs: false,
    networkOffline: !navigator.onLine,
    lastFetchLatencyMs: null,
    sheltersFetchFailed: false,
    sheltersCachedAt: null,
    publicSearch: "",
    publicAvailableOnly: false,
    publicOpenNowOnly: false,
    publicCallAheadOnly: false,
    publicWheelchairOnly: false,
    publicPetsOnly: false,
    publicBarrierLevel: "",
    publicPopulationType: "",
    publicRequestForm: buildEmptyPublicRequestForm(),
    publicRequestErrors: {},
    publicRequestSuccess: null,
    staffBookingFilter: "all",
    staffSelectedBookingId: null,
    staffSelectedShelterId: null,
    staffShelterSearch: "",
    staffShelterForm: null,
    staffShelterErrors: {},
    staffTurnAwayForm: buildEmptyTurnAwayForm(),
    staffTurnAwayErrors: {},
    flash: null,
    connection: { tone: "neutral", label: "Ready" },
    dialogAction: null,
    dialogBookingId: null,
    chatClientSessionId: null,
    chatOpen: false,
    chatAlias: "",
    chatInput: "",
    chatSending: false,
    chatMessages: [],
    chatNextInputs: ["BED", "HELP"],
    userLat: null,
    userLng: null,
    nearbyMode: false,
    locationLoading: false,
    locationError: null,
    demandSummary: [],
    demandRecords: [],
    loadingDemand: false
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
    bootstrapChatState();
    const savedFilters = readFilterState();
    if (savedFilters) {
        FILTER_KEYS.forEach((key) => {
            if (savedFilters[key] !== undefined) {
                state[key] = savedFilters[key];
            }
        });
    }
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

    window.addEventListener("online", () => {
        state.networkOffline = false;
        render();
        loadShelters({ silent: true });
    });

    window.addEventListener("offline", () => {
        state.networkOffline = true;
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
        const view = ["dashboard", "availability", "turnaways", "settings", "demand"].includes(parts[1]) ? parts[1] : "dashboard";
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
    const needsTurnAwayLogs = state.route.mode === "staff" && state.route.view === "turnaways";
    const needsDemand = state.route.mode === "staff" && (state.route.view === "dashboard" || state.route.view === "demand") && state.demandSummary.length === 0;

    const tasks = [];
    if (needsShelters) {
        tasks.push(loadShelters({ silent }));
    }
    if (needsBookings) {
        tasks.push(loadBookings({ silent }));
    }
    if (needsTurnAwayLogs) {
        tasks.push(loadTurnAwayLogs({ silent }));
    }
    if (needsDemand) {
        tasks.push(loadDemandSummary());
    }
    if (tasks.length) {
        await Promise.all(tasks);
    }
}

function readShelterCache() {
    try {
        const raw = localStorage.getItem(SHELTER_CACHE_KEY);
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        const currentDeviceId = ensureDeviceId();
        if (currentDeviceId && parsed.deviceId && parsed.deviceId !== currentDeviceId) {
            return null;
        }
        return parsed;
    } catch {
        return null;
    }
}

function writeShelterCache(data) {
    try {
        localStorage.setItem(SHELTER_CACHE_KEY, JSON.stringify({ data, ts: Date.now(), deviceId: ensureDeviceId() }));
    } catch {}
}

function ensureDeviceId() {
    try {
        let id = localStorage.getItem(DEVICE_ID_KEY);
        if (!id) {
            id = crypto.randomUUID();
            localStorage.setItem(DEVICE_ID_KEY, id);
        }
        return id;
    } catch {
        return null;
    }
}

async function recordGuestTypeInterest(populationType) {
    const deviceId = ensureDeviceId();
    if (!deviceId) return;

    const cacheKey = INTEREST_CACHE_PREFIX + populationType;
    if (localStorage.getItem(cacheKey)) return;

    try {
        await fetch("/api/analytics/interest", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ deviceId, populationType })
        });
        localStorage.setItem(cacheKey, "1");
    } catch {
        // silent fail — analytics must never disrupt UX
    }
}

async function loadDemandSummary() {
    state.loadingDemand = true;
    try {
        const [summaryRes, recordsRes] = await Promise.all([
            fetch("/api/analytics/interest/summary"),
            fetch("/api/analytics/interest/records")
        ]);
        if (summaryRes.ok) state.demandSummary = await summaryRes.json();
        if (recordsRes.ok) state.demandRecords = await recordsRes.json();
    } catch {
        // non-critical
    } finally {
        state.loadingDemand = false;
    }
}

const FILTER_KEYS = [
    "publicAvailableOnly",
    "publicOpenNowOnly",
    "publicCallAheadOnly",
    "publicWheelchairOnly",
    "publicPetsOnly",
    "publicBarrierLevel",
    "publicPopulationType"
];

function readFilterState() {
    try {
        const raw = localStorage.getItem(FILTER_CACHE_KEY);
        if (!raw) return null;
        return JSON.parse(raw);
    } catch {
        return null;
    }
}

function writeFilterState() {
    try {
        const snapshot = {};
        FILTER_KEYS.forEach((key) => { snapshot[key] = state[key]; });
        localStorage.setItem(FILTER_CACHE_KEY, JSON.stringify(snapshot));
    } catch {}
}

async function loadShelters({ silent }) {
    // SWR step 1: hydrate from cache immediately so the UI is never blank
    const cached = readShelterCache();
    if (cached && state.shelters.length === 0) {
        state.shelters = cached.data;
        state.sheltersCachedAt = cached.ts;
        if (!state.staffSelectedShelterId && state.shelters.length > 0) {
            state.staffSelectedShelterId = state.shelters[0].id;
        }
        if (state.staffSelectedShelterId && !state.shelters.some((s) => s.id === state.staffSelectedShelterId)) {
            state.staffSelectedShelterId = state.shelters[0]?.id ?? null;
        }
        hydrateStaffShelterForm();
        hydrateStaffTurnAwayForm();
        if (state.route.mode === "public" && state.route.view === "request") {
            hydratePublicRequestForm();
        }
        render();
    }

    state.loadingShelters = true;
    updateConnection("neutral", "Loading shelters");
    if (!silent) {
        showFlash("Loading current shelter information.", "success", 1400);
    }
    render();

    try {
        // SWR step 2: fetch fresh data in background
        const fresh = await apiFetch("/api/shelters");
        state.shelters = fresh;
        state.sheltersFetchFailed = false;
        writeShelterCache(fresh);
        state.sheltersCachedAt = Date.now();
        if (!state.staffSelectedShelterId && state.shelters.length > 0) {
            state.staffSelectedShelterId = state.shelters[0].id;
        }
        if (state.staffSelectedShelterId && !state.shelters.some((shelter) => shelter.id === state.staffSelectedShelterId)) {
            state.staffSelectedShelterId = state.shelters[0]?.id ?? null;
        }
        hydrateStaffShelterForm();
        hydrateStaffTurnAwayForm();
        if (state.route.mode === "public" && state.route.view === "request") {
            hydratePublicRequestForm();
        }
        updateConnection("success", state.route.mode === "staff" ? "Staff data ready" : "Public data ready");
    } catch (error) {
        state.sheltersFetchFailed = true;
        updateConnection("error", "Shelter load failed");
        if (!cached) {
            showFlash(error.message || "Could not load shelters.", "error");
        } else if (!silent) {
            showFlash("Could not refresh. Showing saved shelter data.", "warn");
        }
    } finally {
        state.loadingShelters = false;
    }
}

async function loadNearbyShelters({ silent }) {
    state.loadingShelters = true;
    if (!silent) {
        showFlash("Finding shelters near you.", "success", 1400);
    }
    render();

    try {
        state.shelters = await apiFetch(`/api/shelters/nearby?lat=${state.userLat}&lng=${state.userLng}&radius=100`);
        updateConnection("success", "Nearby shelters loaded");
    } catch (error) {
        updateConnection("error", "Nearby search failed");
        showFlash(error.message || "Could not find nearby shelters.", "error");
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

async function loadTurnAwayLogs({ silent }) {
    state.loadingTurnAwayLogs = true;
    if (!silent) {
        updateConnection("neutral", "Loading turn-away logs");
        render();
    }

    try {
        state.turnAwayLogs = await apiFetch("/api/turn-away-logs");
        if (!silent) {
            updateConnection("success", "Turn-away logs ready");
        }
    } catch (error) {
        updateConnection("error", "Turn-away log load failed");
        showFlash(error.message || "Could not load turn-away logs.", "error");
    } finally {
        state.loadingTurnAwayLogs = false;
    }
}

function render() {
    elements.root.innerHTML = state.route.mode === "staff" ? renderStaffApp() : renderPublicApp();
    bindViewEvents();
    queueChatAutoScroll();
}

function queueChatAutoScroll() {
    window.requestAnimationFrame(() => {
        const transcript = document.querySelector(".chat-transcript");
        if (!transcript) {
            return;
        }
        transcript.scrollTop = transcript.scrollHeight;
    });
}

function bindViewEvents() {
    const refreshButton = document.querySelector("[data-action='refresh']");
    if (refreshButton) {
        refreshButton.addEventListener("click", async () => {
            if (state.route.mode === "staff") {
                const tasks = [loadShelters({ silent: false }), loadBookings({ silent: false })];
                if (state.route.view === "turnaways") {
                    tasks.push(loadTurnAwayLogs({ silent: false }));
                }
                if (state.route.view === "dashboard" || state.route.view === "demand") {
                    tasks.push(loadDemandSummary());
                }
                await Promise.all(tasks);
            } else if (state.nearbyMode) {
                await loadNearbyShelters({ silent: false });
            } else {
                await loadShelters({ silent: false });
            }
            render();
        });
    }

    const useLocationButton = document.querySelector("[data-action='use-location']");
    if (useLocationButton) {
        useLocationButton.addEventListener("click", () => {
            if (!navigator.geolocation) {
                state.locationError = "Your browser does not support location sharing.";
                render();
                return;
            }
            state.locationLoading = true;
            state.locationError = null;
            render();
            navigator.geolocation.getCurrentPosition(
                async (position) => {
                    state.userLat = position.coords.latitude;
                    state.userLng = position.coords.longitude;
                    state.nearbyMode = true;
                    state.locationLoading = false;
                    await loadNearbyShelters({ silent: false });
                    render();
                },
                (err) => {
                    state.locationLoading = false;
                    state.locationError = err.code === 1
                        ? "Location access was denied. Please allow location in your browser settings."
                        : "Could not determine your location. Please try again.";
                    render();
                },
                { timeout: 10000 }
            );
        });
    }

    const clearLocationButton = document.querySelector("[data-action='clear-location']");
    if (clearLocationButton) {
        clearLocationButton.addEventListener("click", async () => {
            state.nearbyMode = false;
            state.userLat = null;
            state.userLng = null;
            state.locationError = null;
            await loadShelters({ silent: true });
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

    document.querySelectorAll("[data-public-clear]").forEach((button) => {
        button.addEventListener("click", () => {
            state.publicAvailableOnly = false;
            state.publicOpenNowOnly = false;
            state.publicCallAheadOnly = false;
            state.publicWheelchairOnly = false;
            state.publicPetsOnly = false;
            state.publicBarrierLevel = "";
            state.publicPopulationType = "";
            writeFilterState();
            render();
        });
    });

    document.querySelectorAll("[data-public-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const key = button.dataset.publicToggle;
            state[key] = !state[key];
            writeFilterState();
            render();
        });
    });

    document.querySelectorAll("[data-public-filter-select]").forEach((select) => {
        select.addEventListener("change", (event) => {
            const { name, value } = event.target;
            state[name] = value;
            writeFilterState();
            if (name === "publicPopulationType" && value && value !== "ANY_GENDER") {
                recordGuestTypeInterest(value);
            }
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

    const chatAliasInput = document.querySelector("#chat-alias");
    if (chatAliasInput) {
        chatAliasInput.addEventListener("input", (event) => {
            state.chatAlias = event.target.value;
            persistChatAlias();
        });
    }

    const chatForm = document.querySelector("#keyword-chat-form");
    if (chatForm) {
        chatForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await submitChatMessage(state.chatInput);
        });
    }

    const chatInput = document.querySelector("#chat-message");
    if (chatInput) {
        chatInput.addEventListener("input", (event) => {
            state.chatInput = event.target.value;
        });
    }

    document.querySelectorAll("[data-chat-reply]").forEach((button) => {
        button.addEventListener("click", async () => {
            await submitChatMessage(button.dataset.chatReply || "");
        });
    });

    const chatToggleButton = document.querySelector("[data-chat-toggle]");
    if (chatToggleButton) {
        chatToggleButton.addEventListener("click", () => {
            state.chatOpen = !state.chatOpen;
            render();
        });
    }

    const chatCloseButton = document.querySelector("[data-chat-close]");
    if (chatCloseButton) {
        chatCloseButton.addEventListener("click", () => {
            state.chatOpen = false;
            render();
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

    const turnAwayForm = document.querySelector("#staff-turn-away-form");
    if (turnAwayForm) {
        turnAwayForm.addEventListener("input", handleStaffTurnAwayInput);
        turnAwayForm.addEventListener("change", handleStaffTurnAwayInput);
        turnAwayForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await submitStaffTurnAwayLog();
        });
    }
}

function renderPublicApp() {
    return `
        <div class="public-shell">
            <header class="site-header">
                <div class="site-header-inner">
                    <a class="brand-link" href="#/shelters">
                        <span class="brand-mark">◎</span>
                        <span class="brand-name">Shelter-Space</span>
                    </a>
                    <a class="site-link" href="#/staff/dashboard">Staff sign in</a>
                </div>
            </header>

            <div class="public-hero-band">
                <section class="public-header">
                    <div class="brand-block">
                        <p class="eyebrow">Tonight's Shelter Directory</p>
                        <h1>Find a bed near you.</h1>
                        <p class="lede">Real-time openings, intake methods, and accessibility notes for shelters in your area. Availability updates every few minutes.</p>
                        <div class="hero-meta">
                            <span class="hero-stat"><span class="dot success"></span><strong>${sumAvailableBeds()}</strong> beds open across <strong>${state.shelters.filter((shelter) => shelter.availableBeds > 0).length}</strong> shelters</span>
                            <span class="hero-stat muted">◷ Updated just now</span>
                        </div>
                    </div>
                </section>
            </div>

            <main class="public-main">
                ${renderConnectionBanner()}
                ${renderFlash()}
                ${renderPublicContent()}
                ${renderPublicChatWidget()}
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

function renderPublicChatWidget() {
    const transcript = state.chatMessages.length
        ? state.chatMessages.map((entry) => `
            <div class="chat-line ${entry.role}">
                <span class="chat-role">${entry.role === "user" ? "You" : "Bot"}</span>
                <p>${escapeHtml(entry.text)}</p>
            </div>
        `).join("")
        : `<div class="chat-line bot"><span class="chat-role">Bot</span><p>I can help you find a bed or check your request.</p></div>`;

    return `
        <div class="chat-fab-shell">
            <button
                type="button"
                class="chat-fab"
                data-chat-toggle="true"
                aria-expanded="${state.chatOpen ? "true" : "false"}"
                aria-controls="public-chat-panel"
            >
                ${state.chatOpen ? "Close help" : "Get bed help"}
            </button>
            <section id="public-chat-panel" class="panel public-chat-panel ${state.chatOpen ? "open" : ""}">
                <div class="chat-header">
                    <div>
                        <p class="eyebrow chat-eyebrow">Shelter assistant</p>
                        <h3>Bed request chat</h3>
                    </div>
                    <button type="button" class="button ghost inline" data-chat-close="true">Close</button>
                </div>
                <label class="field chat-alias-field">
                    <span>Name to use (optional)</span>
                    <input id="chat-alias" maxlength="120" value="${escapeHtml(state.chatAlias)}" placeholder="e.g. Sam">
                </label>
                <div class="chat-transcript" aria-live="polite">
                    ${transcript}
                </div>
                <form id="keyword-chat-form" class="chat-form">
                    <input id="chat-message" maxlength="280" value="${escapeHtml(state.chatInput)}" placeholder="Type a message or tap an option..." ${state.chatSending ? "disabled" : ""}>
                    <button class="button" type="submit" ${state.chatSending ? "disabled" : ""}>Send</button>
                </form>
                <div class="chat-quick-replies">
                    ${(state.chatNextInputs || ["BED", "HELP"]).slice(0, 4).map((reply) => `
                        <button class="filter-chip" type="button" data-chat-reply="${escapeHtml(reply)}" title="${escapeHtml(chatReplyHint(reply))}" aria-label="${escapeHtml(chatReplyHint(reply))}" ${state.chatSending ? "disabled" : ""}>
                            ${escapeHtml(chatReplyLabel(reply))}
                        </button>
                    `).join("")}
                </div>
                <p class="helper-text chat-helper-text">Tap an option or type your message.</p>
            </section>
        </div>
    `;
}

function renderPublicShelterList() {
    const shelters = getFilteredPublicShelters();

    return `
        <section class="public-tools panel">
            <div class="public-filter-stack">
                <div class="public-search-row">
                    <label class="field public-search-field">
                        <span class="sr-only">Search by shelter name or neighborhood</span>
                        <input id="public-search" value="${escapeHtml(state.publicSearch)}" placeholder="Search by name or neighborhood">
                    </label>
                    <div class="public-location-actions">
                        ${state.nearbyMode
                            ? `<span class="location-active-label">Near you</span>
                               <button class="button ghost inline" data-action="clear-location">Clear</button>`
                            : `<button class="button secondary inline" data-action="use-location" ${state.locationLoading ? "disabled" : ""}>
                                   ${state.locationLoading ? "Locating…" : "Use my location"}
                               </button>`}
                    </div>
                </div>
                <div class="public-dropdown-row">
                    ${renderFilterSelect("publicBarrierLevel", "Any barrier level", state.publicBarrierLevel, ENUM_OPTIONS.barrierLevel)}
                    ${renderFilterSelect("publicPopulationType", "Any guest type", state.publicPopulationType, ENUM_OPTIONS.populationType)}
                    <button class="button secondary inline public-refresh" data-action="refresh">Refresh</button>
                </div>
                <div class="toggle-row public-filter-row">
                    ${renderPublicFilterChip("allFake", "All", !state.publicAvailableOnly && !state.publicOpenNowOnly && !state.publicCallAheadOnly && !state.publicWheelchairOnly && !state.publicPetsOnly && !state.publicBarrierLevel && !state.publicPopulationType)}
                    ${renderPublicFilterChip("publicAvailableOnly", "Has space", state.publicAvailableOnly)}
                    ${renderPublicFilterChip("publicOpenNowOnly", "Open now", state.publicOpenNowOnly)}
                    ${renderPublicFilterChip("publicCallAheadOnly", "Call ahead", state.publicCallAheadOnly)}
                    ${renderPublicFilterChip("publicWheelchairOnly", "Wheelchair accessible", state.publicWheelchairOnly)}
                    ${renderPublicFilterChip("publicPetsOnly", "Pets allowed", state.publicPetsOnly)}
                </div>
            </div>
        </section>

        ${state.locationError ? `<p class="helper-text error-text location-error">${escapeHtml(state.locationError)}</p>` : ""}

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
            <div class="card-top shelter-list-top">
                <div class="shelter-title-block">
                    <h3>${escapeHtml(shelter.name)}</h3>
                    <p class="location-line">⌖ ${escapeHtml(shelter.city)}${shelter.distanceKm != null ? ` · ${shelter.distanceKm < 1 ? (shelter.distanceKm * 1000).toFixed(0) + " m" : shelter.distanceKm.toFixed(1) + " km"} away` : ""}</p>
                </div>
                ${renderAvailabilityPill(shelter, availability)}
            </div>

            <div class="list-facts-row">
                <div class="list-fact">
                    <span>Serves</span>
                    <strong>${escapeHtml(formatLabel(shelter.populationType))}</strong>
                </div>
                <div class="list-fact">
                    <span>Requirements</span>
                    <strong>${escapeHtml(formatLabel(shelter.barrierLevel))}</strong>
                </div>
                <div class="list-fact">
                    <span>Intake</span>
                    <strong>${escapeHtml(formatLabel(shelter.intakeType))}</strong>
                </div>
                <div class="list-fact">
                    <span>Hours</span>
                    <strong>${escapeHtml(formatIntakeWindow(shelter))}</strong>
                </div>
            </div>

            <div class="chip-row compact">
                ${shelter.wheelchairAccessible ? renderOutlineChip("Wheelchair accessible") : ""}
                ${shelter.acceptsLargeItems ? renderOutlineChip("Ground-floor beds") : ""}
                ${shelter.petsAllowed ? renderOutlineChip("Service animals welcome") : ""}
                ${shelter.callAheadRequired ? renderOutlineChip("Call ahead") : ""}
                ${shelter.supportsWaitlist ? renderOutlineChip("Waitlist available") : ""}
            </div>

            <div class="list-card-footer">
                <span class="helper-text">⌚ Updated just now</span>
                <a class="details-link" href="#/shelters/${shelter.id}">View details →</a>
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
        <section class="detail-layout aligned-detail">
            <a class="back-link" href="#/shelters">← All shelters</a>

            <article class="panel detail-hero detail-primary-card">
                <div class="detail-head detail-hero-top">
                    <div>
                        <p class="eyebrow">${escapeHtml(shelter.city)}</p>
                        <h2>${escapeHtml(shelter.name)}</h2>
                        <p class="detail-description">${escapeHtml(shelter.notes || shelter.intakeInstructions || "Review the intake information below before heading to this shelter.")}</p>
                    </div>
                    ${renderAvailabilityPill(shelter, availability)}
                </div>

                <div class="detail-summary-grid">
                    <div class="summary-mini-card">
                        <span class="mini-icon">⋈</span>
                        <div>
                            <span>Who can stay</span>
                            <strong>${escapeHtml(formatLabel(shelter.populationType))}</strong>
                        </div>
                    </div>
                    <div class="summary-mini-card">
                        <span class="mini-icon">⛨</span>
                        <div>
                            <span>Requirements</span>
                            <strong>${escapeHtml(formatLabel(shelter.barrierLevel))}</strong>
                        </div>
                    </div>
                    <div class="summary-mini-card">
                        <span class="mini-icon">☰</span>
                        <div>
                            <span>How to get in</span>
                            <strong>${escapeHtml(formatLabel(shelter.intakeType))}</strong>
                        </div>
                    </div>
                    <div class="summary-mini-card">
                        <span class="mini-icon">◷</span>
                        <div>
                            <span>Hours</span>
                            <strong>${escapeHtml(formatIntakeWindow(shelter))}</strong>
                        </div>
                    </div>
                </div>

                <div class="detail-cta-row">
                    <a class="button secondary phone-button" href="${shelter.phoneNumber ? `tel:${escapeHtml(shelter.phoneNumber)}` : "#"}">${escapeHtml(shelter.phoneNumber || "Phone not listed")}</a>
                    <a class="button detail-request-button" href="#/shelters/${shelter.id}/request">Request a bed for tonight</a>
                </div>
                <p class="helper-text detail-footnote">Requesting a bed is free. Your info is only shared with this shelter's intake team.</p>
            </article>

            <article class="panel detail-section">
                <h3>Who can stay & requirements</h3>
                <div class="detail-copy">
                    <p>${escapeHtml(shelter.intakeInstructions || "No ID or sobriety requirement is listed for this shelter.")}</p>
                    <ul class="detail-list">
                        <li>${escapeHtml(shelter.rules || "Shelter rules are not listed yet.")}</li>
                        <li>${escapeHtml(shelter.callAheadRequired ? "Call ahead before arrival." : "Walk-up intake may be available.")}</li>
                        <li>${escapeHtml(`Age range: ${formatAgeRange(shelter)}`)}</li>
                    </ul>
                </div>
            </article>

            <article class="panel detail-section">
                <h3>What's provided</h3>
                <div class="chip-row compact">
                    ${renderOutlineChip(shelter.programs || "Programs vary")}
                    ${renderOutlineChip(shelter.perks || "Basic shelter services")}
                    ${shelter.wheelchairAccessible ? renderOutlineChip("Wheelchair accessible") : ""}
                    ${shelter.petsAllowed ? renderOutlineChip("Pets allowed") : ""}
                    ${shelter.acceptsLargeItems ? renderOutlineChip("Large items") : ""}
                </div>
            </article>

            <article class="panel detail-section">
                <h3>More details</h3>
                <div class="detail-copy">
                    <p><strong>Address:</strong> ${escapeHtml(shelter.confidentialAddress ? "Address shared after screening or intake." : shelter.address)}</p>
                    <p><strong>Operational status:</strong> ${escapeHtml(formatLabel(shelter.operationalStatus))}</p>
                    <p><strong>Max stay:</strong> ${escapeHtml(shelter.maxStayDays ? `${shelter.maxStayDays} days` : "Not specified")}</p>
                    <p><strong>Waitlist:</strong> ${shelter.supportsWaitlist ? "This shelter accepts a waitlist. Contact intake staff if beds are full." : "No formal waitlist at this shelter."}</p>
                    <p><strong>Notes:</strong> ${escapeHtml(shelter.notes || "No extra notes listed.")}</p>
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
                <a class="back-link subtle" href="#/shelters/${shelter.id}">← Back to shelter details</a>
                <p class="eyebrow">Request a bed</p>
                <h2>${escapeHtml(shelter.name)}</h2>
                <p class="helper-text">${escapeHtml(shelter.intakeInstructions || "Staff will review your request using the shelter details already on file.")}</p>
                <div class="chip-row">
                    ${renderAvailabilityPill(shelter, getAvailabilityLabel(shelter))}
                    ${renderOutlineChip(formatLabel(shelter.populationType))}
                    ${renderOutlineChip(formatLabel(shelter.barrierLevel))}
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
            <header class="site-header staff-topbar">
                <div class="site-header-inner staff-header-inner">
                    <div class="staff-header-left">
                        <a class="brand-link" href="#/staff/dashboard">
                            <span class="brand-mark">◎</span>
                            <span class="brand-name">Shelter-Space Staff</span>
                        </a>
                        <nav class="staff-nav">
                            <a href="#/staff/dashboard" class="${state.route.view === "dashboard" ? "active" : ""}">Queue</a>
                            <a href="#/staff/availability" class="${state.route.view === "availability" ? "active" : ""}">Availability</a>
                            <a href="#/staff/turnaways" class="${state.route.view === "turnaways" ? "active" : ""}">Turn-aways</a>
                            <a href="#/staff/demand" class="${state.route.view === "demand" ? "active" : ""}">Demand</a>
                            <a href="#/staff/settings" class="${state.route.view === "settings" ? "active" : ""}">Shelter settings</a>
                        </nav>
                    </div>
                    <div class="staff-header-meta">
                        <div>
                            <strong>${escapeHtml(getSelectedStaffShelter()?.name || "Shelter-Space")}</strong>
                            <span>Night intake · M. Alvarez</span>
                        </div>
                        <div class="staff-avatar">MA</div>
                    </div>
                </div>
            </header>

            <main class="content staff-content">
                <header class="topbar">
                    <div>
                        <h2>${state.route.view === "dashboard" ? "Booking queue" : state.route.view === "availability" ? "Availability" : state.route.view === "turnaways" ? "Turn-away logs" : state.route.view === "demand" ? "Guest type demand" : "Shelter settings"}</h2>
                        <p class="helper-text">${escapeHtml(getSelectedStaffShelter()?.name || "Shelter-Space")} · ${APP_NOW.toLocaleDateString(undefined, { weekday: "long", month: "short", day: "numeric" })}</p>
                    </div>
                    <div class="topbar-actions">
                        <button class="button secondary" data-action="refresh">Refresh data</button>
                    </div>
                </header>

                ${renderFlash()}
                ${renderConnectionBanner()}
                ${renderStaffSummaryCards()}
                ${renderStaffContent()}
                <footer class="staff-footer">
                    <span>If you or someone with you is in immediate danger, call <strong>911</strong>. For 24-hour crisis support, call <strong>988</strong>.</span>
                    <span>Shelter-Space · Staff console</span>
                </footer>
            </main>
        </div>
    `;
}

function renderStaffSummaryCards() {
    return `
        <section class="stats-grid staff-kpi-grid">
            ${renderKpiCard("Open beds", `${sumAvailableBeds()}`, `of ${state.shelters.reduce((sum, shelter) => sum + (shelter.totalCapacity ?? 0), 0)}`, "success")}
            ${renderKpiCard("Pending requests", `${countBookings(["REQUESTED", "WAITLISTED"])}`, "", "warn")}
            ${renderKpiCard("Admitted tonight", `${countBookings(["ADMITTED", "CHECKED_IN"])}`, "", "neutral-accent")}
            ${renderKpiCard("Confirmed arrivals", `${countBookings(["CHECKED_IN"])}`, "", "muted")}
        </section>
    `;
}

function renderStaffContent() {
    if (state.route.view === "availability") {
        return renderStaffAvailability();
    }
    if (state.route.view === "turnaways") {
        return renderStaffTurnAways();
    }
    if (state.route.view === "settings") {
        return renderStaffSettings();
    }
    if (state.route.view === "demand") {
        return renderStaffDemand();
    }
    return renderStaffDashboard();
}

function renderStaffDashboard() {
    const filteredBookings = getFilteredStaffBookings();

    return `
        <section class="staff-tabs">
            <div class="filter-group staff-filter-tabs">
                ${STAFF_FILTERS.map((filter) => `
                    <button class="filter-chip ${filter.key === state.staffBookingFilter ? "active" : ""}" data-staff-filter="${filter.key}">
                        ${filter.label}
                        <span class="tab-count">${filter.key === "all" ? state.bookings.length : filter.key === "actionable" ? countBookings(["REQUESTED", "WAITLISTED"]) : filter.key === "active" ? countBookings(["ADMITTED", "CHECKED_IN"]) : countBookings(["REJECTED", "CANCELLED", "CHECKED_OUT"])}</span>
                    </button>
                `).join("")}
            </div>
        </section>

        <section class="table-shell staff-table-shell">
            ${state.loadingBookings && state.bookings.length === 0 ? renderEmptyState("Loading bookings", "Fetching staff review data.") : filteredBookings.length === 0
                ? renderEmptyState("No bookings match this filter.", "Try a different booking state.")
                : `
                    <table>
                        <thead>
                            <tr>
                                <th>Guest</th>
                                <th>Requested</th>
                                <th>Arrival</th>
                                <th>Party</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${filteredBookings.map(renderStaffBookingRow).join("")}
                        </tbody>
                    </table>
                `}
        </section>

        ${renderDemandSummarySection()}
    `;
}

function renderStaffDemand() {
    const loading = state.loadingDemand;
    const summary = state.demandSummary;
    const records = state.demandRecords;

    return `
        <section class="section-header">
            <div>
                <p class="eyebrow">Public demand signal</p>
                <h3>Guest type interest</h3>
            </div>
            <div class="topbar-actions">
                <a class="button secondary" href="/api/analytics/interest/export.csv" download="guest-type-demand.csv">Download CSV</a>
            </div>
        </section>

        <section class="stats-grid staff-kpi-grid">
            ${summary.map((row) => renderKpiCard(
                formatLabel(row.populationType),
                String(row.uniqueDevices),
                row.uniqueDevices === 1 ? "unique device" : "unique devices",
                "neutral-accent"
            )).join("")}
        </section>

        <section class="section-header">
            <div>
                <p class="eyebrow">Raw records</p>
                <h3>All interest events</h3>
            </div>
            <div class="helper-text">One row per unique device + guest type combination. Device IDs are anonymised UUIDs.</div>
        </section>

        <section class="table-shell staff-table-shell">
            ${loading
                ? renderEmptyState("Loading records", "Fetching demand data.")
                : records.length === 0
                    ? renderEmptyState("No demand records yet", "Interest is recorded when public users filter by guest type.")
                    : `
                        <table>
                            <thead>
                                <tr>
                                    <th>#</th>
                                    <th>Guest type</th>
                                    <th>Device ID</th>
                                    <th>Recorded at</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${records.map((r, i) => `
                                    <tr>
                                        <td>${i + 1}</td>
                                        <td>${escapeHtml(formatLabel(r.populationType))}</td>
                                        <td class="helper-text">${escapeHtml(r.deviceId)}</td>
                                        <td>${escapeHtml(formatDateTime(r.recordedAt))}</td>
                                    </tr>
                                `).join("")}
                            </tbody>
                        </table>
                    `}
        </section>
    `;
}

function renderDemandSummarySection() {
    return `
        <section class="section-header">
            <div>
                <p class="eyebrow">Public demand signal</p>
                <h3>Guest type interest</h3>
            </div>
            <div class="helper-text">Unique devices that filtered by each guest type. Helps identify underserved populations.</div>
        </section>
        <section class="table-shell staff-table-shell">
            ${state.loadingDemand
                ? renderEmptyState("Loading demand data", "Fetching guest type interest summary.")
                : state.demandSummary.length === 0
                    ? renderEmptyState("No demand data yet", "Interest is recorded when public users filter by guest type.")
                    : `
                        <table>
                            <thead>
                                <tr>
                                    <th>Guest type</th>
                                    <th>Unique devices</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${state.demandSummary.map((row) => `
                                    <tr>
                                        <td>${escapeHtml(formatLabel(row.populationType))}</td>
                                        <td>${escapeHtml(String(row.uniqueDevices))}</td>
                                    </tr>
                                `).join("")}
                            </tbody>
                        </table>
                    `}
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

function renderStaffTurnAways() {
    const shelter = getSelectedStaffShelter();
    const form = state.staffTurnAwayForm;
    const visibleLogs = getVisibleTurnAwayLogs();

    return `
        <section class="turn-away-layout">
            <article class="panel-card">
                <div class="panel-header">
                    <div>
                        <p class="eyebrow">Quick intake logging</p>
                        <h3>Record a turn-away</h3>
                    </div>
                    <div class="helper-text">Capture structured reasons for demand, capacity, and intake barriers.</div>
                </div>

                <form id="staff-turn-away-form" class="form-grid">
                    ${renderSelectField("shelterId", "Shelter", String(form.shelterId ?? ""), state.shelters.map((entry) => String(entry.id)), state.staffTurnAwayErrors.shelterId, (value) => {
                        const match = state.shelters.find((entry) => String(entry.id) === value);
                        return match ? `${match.name} · ${match.city}` : value;
                    })}
                    ${renderGuestSelectField("guestId", "Guest (optional)", form.guestId ?? "", getTurnAwayGuestOptions(), state.staffTurnAwayErrors.guestId)}
                    ${renderSelectField("reason", "Reason", form.reason, ENUM_OPTIONS.turnAwayReason, state.staffTurnAwayErrors.reason)}
                    ${renderInputField("occurredAt", "Occurred at", form.occurredAt, false, state.staffTurnAwayErrors.occurredAt, "datetime-local")}
                    ${renderInputField("recordedBy", "Recorded by", form.recordedBy, true, state.staffTurnAwayErrors.recordedBy)}
                    ${renderTextAreaField("notes", "Notes", form.notes, state.staffTurnAwayErrors.notes)}

                    <div class="panel form-summary">
                        ${state.staffTurnAwayErrors.message ? `<div class="form-error">${escapeHtml(state.staffTurnAwayErrors.message)}</div>` : ""}
                        <div class="card-actions">
                            <button class="button" type="submit">Save turn-away log</button>
                        </div>
                    </div>
                </form>
            </article>

            <section class="panel-card">
                <div class="section-header">
                    <div>
                        <p class="eyebrow">Recent history</p>
                        <h3>${escapeHtml(shelter?.name || "Recent turn-aways")}</h3>
                    </div>
                    <div class="helper-text">${visibleLogs.length} logged event${visibleLogs.length === 1 ? "" : "s"}</div>
                </div>

                <div class="turn-away-history">
                    ${state.loadingTurnAwayLogs && state.turnAwayLogs.length === 0
                        ? renderEmptyState("Loading turn-away logs", "Fetching recent shelter demand and refusal records.")
                        : visibleLogs.length === 0
                            ? renderEmptyState("No turn-away logs yet.", "Use the form to record the next turn-away.")
                            : visibleLogs.map(renderTurnAwayLogCard).join("")}
                </div>
            </section>
        </section>
    `;
}

function renderStaffBookingRow(booking) {
    const detailOpen = booking.id === state.staffSelectedBookingId;
    return `
        <tr data-staff-booking="${booking.id}" class="${detailOpen ? "selected" : ""}">
            <td data-label="Guest">
                <div class="staff-guest-cell">
                    <span class="guest-avatar">${escapeHtml(getGuestInitials(booking.guest.displayName))}</span>
                    <div class="booking-main">
                        <strong>${escapeHtml(booking.guest.displayName)}</strong>
                        <span class="booking-meta">BKG-${booking.id}</span>
                    </div>
                </div>
            </td>
            <td data-label="Requested">${escapeHtml(formatRelativeTime(booking.requestedAt))}</td>
            <td data-label="Arrival">${escapeHtml(formatDate(booking.requestedBedDate))}</td>
            <td data-label="Party">1</td>
            <td data-label="Status">${renderStatusBadge(booking.status)}</td>
            <td data-label="Actions">
                <div class="staff-row-actions">
                    ${getAllowedStaffActions(booking).map((action) => `
                        <button class="button inline ${action === "reject" ? "danger-button" : ""}" data-staff-action="${action}" data-booking-id="${booking.id}">
                            ${action === "admit" ? "✓ Admit" : action === "waitlist" ? "◷ Waitlist" : action === "reject" ? "✕ Decline" : action === "check-in" ? "✓ Check in" : "↗ Check out"}
                        </button>
                    `).join("")}
                    <button class="icon-button chevron-button" type="button">${detailOpen ? "⌃" : "⌄"}</button>
                </div>
            </td>
        </tr>
        ${detailOpen ? `
            <tr class="detail-row">
                <td colspan="6">
                    <div class="staff-detail-grid">
                        <div><span>Contact</span><strong>${escapeHtml(booking.guest.phoneNumber || "Not provided")}</strong></div>
                        <div><span>Accessibility</span><strong>${escapeHtml(booking.intakeNotes || "—")}</strong></div>
                        <div><span>Notes</span><strong>${escapeHtml(booking.decisionNotes || booking.intakeNotes || "No extra notes.")}</strong></div>
                    </div>
                </td>
            </tr>
        ` : ""}
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
                ${renderToggleField("supportsWaitlist", "Accepts waitlist", form.supportsWaitlist)}
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
    if (key === "allFake") {
        return `<button class="filter-chip ${active ? "active" : ""}" data-public-clear="true">${escapeHtml(label)}</button>`;
    }
    return `<button class="filter-chip ${active ? "active" : ""}" data-public-toggle="${key}">${escapeHtml(label)}</button>`;
}

function renderFilterSelect(name, label, value, options) {
    return `
        <label class="field public-filter-select">
            <span class="sr-only">${escapeHtml(label)}</span>
            <select name="${name}" data-public-filter-select="true">
                <option value="">${escapeHtml(label)}</option>
                ${options.map((option) => `<option value="${option}" ${option === value ? "selected" : ""}>${escapeHtml(formatLabel(option))}</option>`).join("")}
            </select>
        </label>
    `;
}

function renderStatCard(label, value) {
    return `
        <article class="stat-card panel">
            <p class="eyebrow">${escapeHtml(label)}</p>
            <strong>${escapeHtml(String(value))}</strong>
        </article>
    `;
}

function renderKpiCard(label, value, suffix, tone) {
    return `
        <article class="stat-card panel staff-kpi ${tone}">
            <p class="eyebrow">${escapeHtml(label)}</p>
            <div class="kpi-value-row">
                <strong>${escapeHtml(String(value))}</strong>
                ${suffix ? `<span>${escapeHtml(suffix)}</span>` : ""}
            </div>
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

function renderSelectField(name, label, value, options, error = "", formatOption = (option) => formatLabel(option)) {
    return `
        <label class="field">
            <span>${escapeHtml(label)}</span>
            <select name="${name}">
                ${options.map((option) => `<option value="${option}" ${option === value ? "selected" : ""}>${escapeHtml(formatOption(option))}</option>`).join("")}
            </select>
            ${error ? `<div class="field-error">${escapeHtml(error)}</div>` : ""}
        </label>
    `;
}

function renderGuestSelectField(name, label, value, options, error = "") {
    return `
        <label class="field">
            <span>${escapeHtml(label)}</span>
            <select name="${name}">
                <option value="">No linked guest</option>
                ${options.map((option) => `<option value="${escapeHtml(String(option.id))}" ${String(option.id) === String(value) ? "selected" : ""}>${escapeHtml(option.label)}</option>`).join("")}
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

function renderConnectionBanner() {
    if (state.networkOffline) {
        const ageNote = state.sheltersCachedAt
            ? ` Showing saved shelter data from ${formatRelativeAge(state.sheltersCachedAt)}.`
            : " No saved data available.";
        return `
            <div class="connection-banner offline" role="alert" aria-live="assertive">
                <span class="banner-icon" aria-hidden="true">⚠</span>
                <span>You are offline.${ageNote}</span>
            </div>`;
    }
    if (state.sheltersFetchFailed && state.sheltersCachedAt) {
        return `
            <div class="connection-banner warn" role="alert" aria-live="polite">
                <span class="banner-icon" aria-hidden="true">⚠</span>
                <span>Could not reach the server. Showing cached shelter data from ${formatRelativeAge(state.sheltersCachedAt)}.</span>
            </div>`;
    }
    if (state.lastFetchLatencyMs != null && state.lastFetchLatencyMs > LATENCY_WARN_MS) {
        return `
            <div class="connection-banner warn" role="alert" aria-live="polite">
                <span class="banner-icon" aria-hidden="true">◷</span>
                <span>Slow connection detected (${state.lastFetchLatencyMs} ms). Data may be delayed.</span>
            </div>`;
    }
    return "";
}

function formatRelativeAge(ts) {
    const diffMs = Date.now() - ts;
    const minutes = Math.max(1, Math.round(diffMs / 60000));
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.round(minutes / 60);
    return `${hours} hr ago`;
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
        if (state.publicOpenNowOnly && !isShelterOpenNow(shelter)) {
            return false;
        }
        if (state.publicCallAheadOnly && !shelter.callAheadRequired) {
            return false;
        }
        if (state.publicWheelchairOnly && !shelter.wheelchairAccessible) {
            return false;
        }
        if (state.publicPetsOnly && !shelter.petsAllowed) {
            return false;
        }
        if (state.publicBarrierLevel && shelter.barrierLevel !== state.publicBarrierLevel) {
            return false;
        }
        if (state.publicPopulationType && shelter.populationType !== state.publicPopulationType) {
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
    if (booking.status === "REQUESTED") {
        return ["admit", "waitlist", "reject"];
    }
    if (booking.status === "WAITLISTED") {
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

function renderOutlineChip(label) {
    return `<span class="outline-chip">${escapeHtml(label)}</span>`;
}

function renderAvailabilityPill(shelter, availability) {
    const label = shelter.totalCapacity ? `${shelter.availableBeds} open / ${shelter.totalCapacity}` : availability.label;
    return `<span class="availability-pill ${availability.tone}"><span class="dot ${availability.tone === "warn" ? "warn" : availability.tone === "error" ? "error" : "success"}"></span>${escapeHtml(label)}</span>`;
}

function renderStatusBadge(status) {
    const tone = status === "REQUESTED"
        ? "neutral"
        : status === "WAITLISTED"
            ? "warn"
            : status === "REJECTED"
                ? "error"
                : ["CANCELLED", "CHECKED_OUT"].includes(status)
                    ? "neutral"
                    : "success";
    const label = status === "REQUESTED" ? "Requested" : status === "WAITLISTED" ? "Waitlisted" : formatLabel(status);
    return `<span class="status-badge ${tone}">${escapeHtml(label)}</span>`;
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

function handleStaffTurnAwayInput(event) {
    if (!event.target.name || !state.staffTurnAwayForm) {
        return;
    }
    state.staffTurnAwayErrors = {};
    state.staffTurnAwayForm[event.target.name] = event.target.value;
    if (event.target.name === "shelterId") {
        state.staffSelectedShelterId = Number(event.target.value) || null;
        render();
    }
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
            supportsWaitlist: Boolean(shelter.supportsWaitlist),
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

function hydrateStaffTurnAwayForm() {
    state.staffTurnAwayErrors = {};
    state.staffTurnAwayForm = {
        ...buildEmptyTurnAwayForm(),
        shelterId: state.staffSelectedShelterId ?? state.shelters[0]?.id ?? "",
        recordedBy: state.staffTurnAwayForm?.recordedBy || "M. Alvarez"
    };
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

async function submitStaffTurnAwayLog() {
    if (!state.staffTurnAwayForm) {
        return;
    }

    const payload = {
        shelterId: normalizeInteger(state.staffTurnAwayForm.shelterId, true),
        guestId: normalizeInteger(state.staffTurnAwayForm.guestId),
        reason: state.staffTurnAwayForm.reason,
        notes: normalizeBlank(state.staffTurnAwayForm.notes),
        occurredAt: normalizeBlank(state.staffTurnAwayForm.occurredAt),
        recordedBy: state.staffTurnAwayForm.recordedBy.trim()
    };

    try {
        await apiFetch("/api/turn-away-logs", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        showFlash("Turn-away log saved.", "success");
        hydrateStaffTurnAwayForm();
        await loadTurnAwayLogs({ silent: true });
    } catch (error) {
        state.staffTurnAwayErrors = extractFieldErrors(error);
        if (!Object.keys(state.staffTurnAwayErrors).length) {
            state.staffTurnAwayErrors = { message: error.message };
        }
        showFlash(error.message || "Could not save turn-away log.", "error");
    } finally {
        render();
    }
}

function openDialog(action, bookingId) {
    const booking = state.bookings.find((entry) => entry.id === bookingId);
    if (!booking || !BOOKING_ACTIONS[action]) {
        return;
    }

    state.staffSelectedBookingId = bookingId;
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
    const booking = state.bookings.find((entry) => entry.id === state.dialogBookingId);
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
    const t0 = performance.now();
    let response;
    try {
        response = await fetch(path, {
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
            },
            ...options
        });
    } catch (networkError) {
        state.networkOffline = !navigator.onLine;
        throw networkError;
    }

    state.lastFetchLatencyMs = Math.round(performance.now() - t0);
    state.networkOffline = false;

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

function bootstrapChatState() {
    const storedSessionId = window.localStorage.getItem("chatClientSessionId");
    const storedAlias = window.localStorage.getItem("chatAlias") || "";
    state.chatClientSessionId = storedSessionId || `chat-${crypto.randomUUID()}`;
    state.chatAlias = storedAlias;
    state.chatMessages = [{
        role: "bot",
        text: "I can help you find a bed or check your request."
    }];
    if (!storedSessionId) {
        window.localStorage.setItem("chatClientSessionId", state.chatClientSessionId);
    }
}

function persistChatAlias() {
    const alias = state.chatAlias.trim();
    if (alias) {
        window.localStorage.setItem("chatAlias", alias);
    }
}

function chatReplyLabel(value) {
    const token = String(value || "").toUpperCase();
    if (/^\d+$/.test(token)) {
        return `Choose #${token}`;
    }
    const rangeMatch = token.match(/^(\d+)-(\d+)$/);
    if (rangeMatch) {
        return `Choose ${rangeMatch[1]}-${rangeMatch[2]}`;
    }
    return CHAT_REPLY_LABELS[token] || value;
}

function chatReplyHint(value) {
    const token = String(value || "").toUpperCase();
    if (/^\d+$/.test(token)) {
        return `Choose shelter option ${token}.`;
    }
    const rangeMatch = token.match(/^(\d+)-(\d+)$/);
    if (rangeMatch) {
        return `Choose a shelter from option ${rangeMatch[1]} to ${rangeMatch[2]}.`;
    }
    return CHAT_REPLY_HINTS[token] || `Send ${token}`;
}

async function submitChatMessage(rawMessage) {
    const message = String(rawMessage || "").trim();
    if (!message || state.chatSending) {
        return;
    }

    state.chatSending = true;
    state.chatInput = "";
    state.chatMessages.push({ role: "user", text: message });
    render();

    try {
        const response = await apiFetch("/api/chatbot/messages", {
            method: "POST",
            body: JSON.stringify({
                clientSessionId: state.chatClientSessionId,
                message,
                alias: normalizeBlank(state.chatAlias)
            })
        });
        if (Array.isArray(response.messages)) {
            const mergedResponse = response.messages
                .map((entry) => String(entry || "").trim())
                .filter(Boolean)
                .join(" ");
            if (mergedResponse) {
                state.chatMessages.push({ role: "bot", text: mergedResponse });
            }
        }
        state.chatNextInputs = Array.isArray(response.nextInputs) && response.nextInputs.length
            ? response.nextInputs
            : ["BED", "HELP"];
    } catch (error) {
        state.chatMessages.push({
            role: "bot",
            text: error.message || "Chat request failed. Please try again or tap See options."
        });
        state.chatNextInputs = ["HELP", "BED"];
    } finally {
        state.chatSending = false;
        render();
    }
}

function buildEmptyPublicRequestForm() {
    return {
        shelterId: null,
        displayName: "",
        legalName: "",
        phoneNumber: "",
        birthDate: "",
        requestedBedDate: formatLocalDateInput(APP_NOW),
        requestedBy: "",
        intakeNotes: ""
    };
}

function buildEmptyTurnAwayForm() {
    return {
        shelterId: "",
        guestId: "",
        reason: "NO_BEDS_AVAILABLE",
        occurredAt: formatLocalDateTimeInput(APP_NOW),
        recordedBy: "M. Alvarez",
        notes: ""
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

function getVisibleTurnAwayLogs() {
    if (!state.staffSelectedShelterId) {
        return state.turnAwayLogs;
    }
    return state.turnAwayLogs.filter((log) => log.shelter.id === state.staffSelectedShelterId);
}

function getTurnAwayGuestOptions() {
    const seen = new Map();
    state.bookings.forEach((booking) => {
        if (booking.guest?.id && !seen.has(booking.guest.id)) {
            seen.set(booking.guest.id, {
                id: booking.guest.id,
                label: `${booking.guest.displayName} · ${booking.shelter.name}`
            });
        }
    });
    return Array.from(seen.values()).sort((left, right) => left.label.localeCompare(right.label));
}

function renderTurnAwayLogCard(log) {
    return `
        <article class="panel turn-away-card">
            <div class="card-top">
                <div>
                    <p class="eyebrow">${escapeHtml(formatLabel(log.reason))}</p>
                    <h3>${escapeHtml(log.guest?.displayName || "Unlinked guest")}</h3>
                    <p class="helper-text">${escapeHtml(log.shelter.name)} · ${escapeHtml(formatDateTime(log.occurredAt))}</p>
                </div>
                ${renderChip(formatLabel(log.reason), log.reason === "NO_BEDS_AVAILABLE" || log.reason === "INTAKE_CLOSED" ? "warn" : "neutral")}
            </div>
            <div class="detail-copy">
                <p><strong>Recorded by:</strong> ${escapeHtml(log.recordedBy)}</p>
                <p><strong>Notes:</strong> ${escapeHtml(log.notes || "No extra notes provided.")}</p>
            </div>
        </article>
    `;
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

function formatRelativeTime(value) {
    if (!value) {
        return "Just now";
    }
    const diffMs = APP_NOW.getTime() - new Date(value).getTime();
    const minutes = Math.max(1, Math.round(diffMs / 60000));
    if (minutes < 60) {
        return `${minutes} min ago`;
    }
    const hours = Math.round(minutes / 60);
    if (hours < 24) {
        return `${hours} hr ago`;
    }
    const days = Math.round(hours / 24);
    return `${days} day${days === 1 ? "" : "s"} ago`;
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

function isShelterOpenNow(shelter) {
    if (!["OPEN", "LIMITED"].includes(shelter.operationalStatus)) {
        return false;
    }
    if (shelter.open24Hours) {
        return true;
    }
    if (!shelter.intakeStartTime || !shelter.intakeCutoffTime) {
        return false;
    }

    const now = APP_NOW;
    const currentMinutes = now.getHours() * 60 + now.getMinutes();
    const startMinutes = timeStringToMinutes(shelter.intakeStartTime);
    const cutoffMinutes = timeStringToMinutes(shelter.intakeCutoffTime);

    if (startMinutes <= cutoffMinutes) {
        return currentMinutes >= startMinutes && currentMinutes <= cutoffMinutes;
    }
    return currentMinutes >= startMinutes || currentMinutes <= cutoffMinutes;
}

function formatSimpleTime(value) {
    const [hour, minute] = value.split(":");
    return new Intl.DateTimeFormat(undefined, { timeStyle: "short" }).format(new Date(`2026-07-18T${hour}:${minute}:00`));
}

function timeStringToMinutes(value) {
    const [hour, minute] = value.split(":").map(Number);
    return (hour * 60) + minute;
}

function formatLocalDateInput(value) {
    const offsetMs = value.getTimezoneOffset() * 60000;
    return new Date(value.getTime() - offsetMs).toISOString().slice(0, 10);
}

function formatLocalDateTimeInput(value) {
    const offsetMs = value.getTimezoneOffset() * 60000;
    return new Date(value.getTime() - offsetMs).toISOString().slice(0, 16);
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

function getGuestInitials(name) {
    return String(name)
        .split(/\s+/)
        .filter(Boolean)
        .map((part) => part[0])
        .join("")
        .slice(0, 2)
        .toUpperCase();
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
