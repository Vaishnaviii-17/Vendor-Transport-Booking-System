
"use strict";

let authToken = null;
let currentUser = null;
let transportResults = [];

const $ = (id) => document.getElementById(id);

function notify(message, type = "success") {
    const element = $("notification");
    element.textContent = message;
    element.className = type;
}

function createTextElement(tag, text, className = "") {
    const element = document.createElement(tag);
    element.textContent = String(text ?? "");
    if (className) element.className = className;
    return element;
}

function addResult(container, fields) {
    const card = document.createElement("div");
    card.className = "result-item";

    for (const [label, value] of Object.entries(fields)) {
        const p = document.createElement("p");
        const strong = createTextElement("strong", `${label}: `);
        p.append(strong, document.createTextNode(String(value ?? "N/A")));
        card.appendChild(p);
    }

    container.appendChild(card);
}

function clearAuthentication() {
    authToken = null;
    currentUser = null;

    $("login-status").textContent = "Not signed in";
    $("logout-button").hidden = true;
    $("login-password").value = "";
    $("booking-history").replaceChildren(
        createTextElement("p", "Sign in and load your booking history to see your trips here.", "muted")
    );
}

function requireCustomerLogin() {
    if (!authToken || !currentUser) {
        notify("Please sign in as a customer before booking.", "error");
        $("login-section").scrollIntoView({ behavior: "smooth", block: "center" });
        return false;
    }

    if (currentUser.role !== "CUSTOMER") {
        notify("This feature is available only to customers.", "error");
        return false;
    }

    return true;
}

async function apiRequest(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    if (authToken) headers.Authorization = `Bearer ${authToken}`;

    let response;
    try {
        response = await fetch(path, { ...options, headers });
    } catch {
        throw new Error("Unable to connect to the server. Check that the application is running.");
    }

    if (!response.ok) {
        const messages = {
            400: "Invalid request. Please check your details.",
            401: "Invalid credentials or session expired.",
            403: "You do not have permission to perform this action.",
            404: "The requested resource was not found.",
            409: "The request conflicts with current booking availability."
        };

        if (response.status === 401 && authToken) clearAuthentication();
        throw new Error(messages[response.status] ||
            (response.status >= 500
                ? "Server error. Please try again later."
                : `Request failed: HTTP ${response.status}`));
    }

    const text = await response.text();
    if (!text) return null;

    try {
        return JSON.parse(text);
    } catch {
        throw new Error("The server returned an unexpected response.");
    }
}

// CUSTOMER REGISTRATION

$("register-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const payload = {
        name: $("register-name").value.trim(),
        email: $("register-email").value.trim(),
        password: $("register-password").value
    };

    const button = $("register-button");
    button.disabled = true;

    try {
        const result = await apiRequest("/api/auth/register", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        notify(result?.message || "Registration successful. Please sign in.");
        $("login-email").value = payload.email;
        $("register-form").reset();
    } catch (error) {
        notify(error.message, "error");
    } finally {
        button.disabled = false;
    }
});

// CUSTOMER LOGIN

$("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const payload = {
        email: $("login-email").value.trim(),
        password: $("login-password").value
    };

    const button = $("login-button");
    button.disabled = true;
    clearAuthentication();

    try {
        const response = await apiRequest("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        if (!response?.token) throw new Error("Login response did not contain a token.");

        if (response.role === "ADMIN") {
            notify("Administrator account detected. Opening the Admin Portal.");
            window.location.assign("/admin");
            return;
        }

        if (response.role !== "CUSTOMER") {
            throw new Error("This account is not authorized for customer access.");
        }

        authToken = response.token;
        currentUser = {
            userId: response.userId,
            name: response.name,
            email: response.email,
            role: response.role
        };

        $("login-status").textContent = `Signed in as ${response.name}`;
        $("logout-button").hidden = false;
        $("login-password").value = "";
        notify(`Welcome, ${response.name}. You can now book a journey.`);
    } catch (error) {
        clearAuthentication();
        notify(error.message, "error");
    } finally {
        button.disabled = false;
    }
});

$("logout-button").addEventListener("click", () => {
    clearAuthentication();
    notify("Signed out successfully.");
});

// TRANSPORT SEARCH
// GET /api/transport-slots?origin=&destination=&vehicleType=&seats=

$("transport-search-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const origin = $("search-origin").value.trim();
    const destination = $("search-destination").value.trim();
    const vehicleType = $("search-vehicle-type").value;
    const seats = Number($("search-seats").value);
    const travelDate = $("search-date").value;

    if (!Number.isSafeInteger(seats) || seats < 1) {
        notify("Enter a valid number of seats.", "error");
        return;
    }

    const params = new URLSearchParams();
    if (origin) params.set("origin", origin);
    if (destination) params.set("destination", destination);
    if (vehicleType) params.set("vehicleType", vehicleType);
    params.set("seats", String(seats));

    const button = $("search-button");
    button.disabled = true;
    button.textContent = "Searching…";

    $("transport-results").replaceChildren(
        createTextElement("p", "Finding available transport…", "muted")
    );

    try {
        const data = await apiRequest(`/api/transport-slots?${params.toString()}`);

        if (!Array.isArray(data)) {
            throw new Error("Unexpected transport search response.");
        }

        // Apply the selected date to the returned departures. The server-side
        // endpoint is still responsible for its future-trip and seat filters.
        transportResults = data.filter(slot => {
            if (!travelDate) return true;
            const departure = slot.departureTime;
            return typeof departure === "string" && departure.slice(0, 10) === travelDate;
        });

        renderTransportResults(seats);

        $("results-summary").textContent = travelDate
            ? `Showing matching journeys for ${travelDate}.`
            : "Showing matching upcoming journeys.";

        $("results-section").scrollIntoView({ behavior: "smooth", block: "start" });

        notify(`${transportResults.length} matching journey(s) found.`);
    } catch (error) {
        transportResults = [];
        $("transport-results").replaceChildren();
        $("transport-results").appendChild(
            createTextElement("p", error.message, "empty-message")
        );
        $("results-count").textContent = "0 journeys";
        $("results-summary").textContent = "Search could not be completed.";
        notify(error.message, "error");
    } finally {
        button.disabled = false;
        button.innerHTML = 'Search Available Transport <span>→</span>';
    }
});

function formatDeparture(value) {
    if (!value) return "Departure time unavailable";

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);

    return new Intl.DateTimeFormat("en-IN", {
        weekday: "short",
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date);
}

function renderTransportResults(requestedSeats) {
    const container = $("transport-results");
    container.replaceChildren();
    $("results-count").textContent =
        `${transportResults.length} ${transportResults.length === 1 ? "journey" : "journeys"}`;

    if (transportResults.length === 0) {
        container.appendChild(createTextElement("div", "", "empty-state"));
        const empty = container.firstElementChild;
        empty.append(
            createTextElement("span", "⌕", "empty-icon"),
            createTextElement("h3", "No matching journeys"),
            createTextElement("p", "Try another route, date, vehicle type or passenger count.")
        );
        return;
    }

    for (const slot of transportResults) {
        const card = document.createElement("article");
        card.className = "transport-card";

        const top = document.createElement("div");
        top.className = "transport-card-top";
        top.append(
            createTextElement("span", slot.vehicleType || "TRANSPORT", "vehicle-badge"),
            createTextElement("span", `${slot.availableSeats ?? 0} seats available`, "seats-badge")
        );

        const route = document.createElement("div");
        route.className = "route-display";
        route.append(
            createTextElement("h3", slot.origin || "Origin not provided"),
            createTextElement("span", "→", "route-arrow"),
            createTextElement("h3", slot.destination || "Destination not provided")
        );

        const details = document.createElement("div");
        details.className = "transport-details";
        const vehicle = slot.registrationNumber || "Vehicle details unavailable";
        const capacity = slot.capacity != null ? `${slot.capacity} seats capacity` : "Capacity not provided";
        details.append(
            createTextElement("p", `▣  ${vehicle}`),
            createTextElement("p", `◷  ${formatDeparture(slot.departureTime)}`),
            createTextElement("p", `♙  ${capacity}`)
        );

        const button = createTextElement("button", "Book this journey →", "primary-button book-button");
        button.type = "button";
        button.addEventListener("click", () => bookTransport(slot, requestedSeats));

        card.append(top, route, details, button);
        container.appendChild(card);
    }
}

async function bookTransport(slot, requestedSeats) {
    if (!requireCustomerLogin()) return;

    const availableSeats = Number(slot.availableSeats ?? 0);
    if (availableSeats < requestedSeats) {
        notify("This journey no longer has enough seats. Please search again.", "error");
        return;
    }

    const slotId = Number(slot.slotId ?? slot.id);
    if (!Number.isSafeInteger(slotId) || slotId < 1) {
        notify("This result does not contain a valid transport slot ID.", "error");
        return;
    }

    const confirmed = window.confirm(
        `Book ${requestedSeats} seat(s)?\n${slot.origin} → ${slot.destination}\nDeparture: ${formatDeparture(slot.departureTime)}`
    );
    if (!confirmed) return;

    const button = [...document.querySelectorAll(".book-button")]
        .find(candidate => candidate.closest(".transport-card")?.querySelector(".route-display h3")?.textContent === slot.origin);

    if (button) {
        button.disabled = true;
        button.textContent = "Submitting booking…";
    }

    try {
        const booking = await apiRequest("/api/bookings", {
            method: "POST",
            body: JSON.stringify({
                slotId,
                seatsRequested: requestedSeats
            })
        });

        notify(`Booking submitted successfully. Booking ID: ${booking.bookingId}`);
        await loadBookingHistory();
        // Refresh the results to reflect seats consumed by the booking.
        $("transport-search-form").requestSubmit();
    } catch (error) {
        notify(error.message, "error");
        if (button) {
            button.disabled = false;
            button.textContent = "Book this journey →";
        }
    }
}

$("clear-search").addEventListener("click", () => {
    $("transport-search-form").reset();
    $("search-seats").value = "1";
    transportResults = [];
    $("transport-results").replaceChildren(
        createTextElement("div", "", "empty-state")
    );

    const empty = $("transport-results").firstElementChild;
    empty.append(
        createTextElement("span", "⌕", "empty-icon"),
        createTextElement("h3", "Where would you like to go?"),
        createTextElement("p", "Enter your route and preferences above to find available transport.")
    );

    $("results-count").textContent = "0 journeys";
    $("results-summary").textContent = "Search to see available transport.";
    notify("Search filters cleared.");
});

// CUSTOMER BOOKING HISTORY
// GET /api/bookings/my

async function loadBookingHistory() {
    if (!requireCustomerLogin()) return;

    const container = $("booking-history");
    const button = $("history-button");
    button.disabled = true;
    container.replaceChildren(createTextElement("p", "Loading bookings…", "muted"));

    try {
        const bookings = await apiRequest("/api/bookings/my");
        container.replaceChildren();

        if (!Array.isArray(bookings) || bookings.length === 0) {
            container.appendChild(createTextElement("p", "No bookings found yet.", "muted"));
            return;
        }

        for (const booking of bookings) {
            addResult(container, {
                "Booking ID": booking.bookingId,
                "Route": `${booking.origin ?? "N/A"} → ${booking.destination ?? "N/A"}`,
                "Departure": formatDeparture(booking.departureTime),
                "Seats": booking.seatsRequested,
                "Status": booking.status
            });
        }

        notify("Booking history loaded successfully.");
    } catch (error) {
        container.replaceChildren(createTextElement("p", error.message, "empty-message"));
        notify(error.message, "error");
    } finally {
        button.disabled = false;
    }
}

$("history-button").addEventListener("click", loadBookingHistory);

clearAuthentication();
