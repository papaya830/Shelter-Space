const STATIC_CACHE = "ss-static-v17";
const API_CACHE = "ss-api-v2";

self.addEventListener("install", () => self.skipWaiting());

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys().then((keys) =>
            Promise.all(
                keys
                    .filter((k) => k !== STATIC_CACHE && k !== API_CACHE)
                    .map((k) => caches.delete(k))
            )
        ).then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", (event) => {
    const { request } = event;
    const url = new URL(request.url);

    // Stale-while-revalidate for shelter API reads
    if (url.pathname.startsWith("/api/shelters") && request.method === "GET") {
        event.respondWith(swrResponse(API_CACHE, request));
        return;
    }

    // Keep HTML network-first so new shell versions are not masked by an old
    // cached index during iterative UI work.
    if (url.pathname === "/" || url.pathname === "/index.html") {
        event.respondWith(networkFirstResponse(STATIC_CACHE, request));
        return;
    }

    // Cache-first for versioned static assets so the shell still loads offline.
    if (url.pathname.startsWith("/assets/")) {
        event.respondWith(cacheFirstResponse(STATIC_CACHE, request));
        return;
    }
});

async function swrResponse(cacheName, request) {
    const cache = await caches.open(cacheName);
    const cached = await cache.match(request);

    const networkFetch = fetch(request)
        .then((response) => {
            if (response.ok) {
                cache.put(request, response.clone());
            }
            return response;
        })
        .catch(() => null);

    // Return cached immediately and revalidate in background; fall through to
    // the network promise only when there is no cached entry yet.
    return cached ?? (await networkFetch) ?? new Response(
        JSON.stringify([]),
        { status: 503, headers: { "Content-Type": "application/json" } }
    );
}

async function cacheFirstResponse(cacheName, request) {
    const cache = await caches.open(cacheName);
    const cached = await cache.match(request);
    if (cached) return cached;

    try {
        const response = await fetch(request);
        if (response.ok) {
            cache.put(request, response.clone());
        }
        return response;
    } catch {
        return new Response("Offline — cached version unavailable.", {
            status: 503,
            headers: { "Content-Type": "text/plain" }
        });
    }
}

async function networkFirstResponse(cacheName, request) {
    const cache = await caches.open(cacheName);

    try {
        const response = await fetch(request);
        if (response.ok) {
            cache.put(request, response.clone());
        }
        return response;
    } catch {
        const cached = await cache.match(request);
        return cached ?? new Response("Offline — cached version unavailable.", {
            status: 503,
            headers: { "Content-Type": "text/plain" }
        });
    }
}
