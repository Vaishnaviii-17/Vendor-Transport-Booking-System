
"use strict";

let authToken = null;

const $ = (id) => document.getElementById(id);

function notify(message, type = "success") {
    const element = $("notification");
    element.textContent = message;
    element.className = type;
}

function createTextElement(tag, text) {
    const element = document.createElement(tag);
    element.textContent = String(text ?? "");
    return element;
}

function addResult(container, fields) {
    const card = document.createElement("div");
    card.className = "result-item";

    for (const [label, value] of Object.entries(fields)) {
        const paragraph = document.createElement("p");
        const strong = document.createElement("strong");

        strong.textContent = `${label}: `;
        paragraph.appendChild(strong);
        paragraph.appendChild(
            document.createTextNode(String(value ?? "N/A"))
        );

        card.appendChild(paragraph);
    }

    container.appendChild(card);
}

async function apiRequest(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    if (authToken) {
        headers.Authorization = `Bearer ${authToken}`;
    }

    let response;

    try {
        response = await fetch(path, {
            ...options,
            headers
        });
    } catch (error) {
        throw new Error("Unable to connect to the server");
    }

    if (!response.ok) {
        if (response.status === 401) {
            throw new Error("Authentication required or session expired");
        }

        if (response.status === 403) {
            throw new Error("You do not have permission");
        }

        if (response.status === 409) {
            throw new Error("Request conflicts with existing data");
        }

        throw new Error(`Request failed: HTTP ${response.status}`);
    }

    const text = await response.text();

    return text ? JSON.parse(text) : null;
}

$("register-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const payload = {
        name: $("register-name").value.trim(),
        email: $("register-email").value.trim(),
        password: $("register-password").value
    };

    try {
        const response = await apiRequest("/api/auth/register", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        notify(response.message || "Registration successful");

        $("login-email").value = payload.email;
        $("register-form").reset();
    } catch (error) {
        notify(error.message, "error");
    }
});

$("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const payload = {
        email: $("login-email").value.trim(),
        password: $("login-password").value
    };

    try {
        const response = await apiRequest("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        if (!response.token) {
            throw new Error("Login response did not contain a token");
        }

        authToken = response.token;

        $("login-status").textContent =
            `Signed in as ${response.name} (${response.role})`;

        $("logout-button").hidden = false;

        notify("Login successful");
    } catch (error) {
        notify(error.message, "error");
    }
});

$("logout-button").addEventListener("click", () => {
    authToken = null;

    $("login-status").textContent = "Not signed in";
    $("logout-button").hidden = true;

    $("availability-results").replaceChildren();
    $("booking-result").replaceChildren();
    $("booking-history").replaceChildren();

    notify("Signed out successfully");
});

$("availability-button").addEventListener("click", async () => {
    const container = $("availability-results");
    container.replaceChildren();

    const date = $("availability-date").value;

    const params = new URLSearchParams();

    if (date) {
        params.set("date", date);
    }

    const query = params.toString();

    try {
        const slots = await apiRequest(
            `/api/availability${query ? `?${query}` : ""}`
        );

        if (!slots.length) {
            container.appendChild(
                createTextElement("p", "No availability found")
            );
            return;
        }

        for (const slot of slots) {
            addResult(container, {
                "Availability Slot ID": slot.slotId,
                "Vehicle": slot.registrationNumber,
                "Type": slot.vehicleType,
                "Date": slot.availableDate,
                "Start": slot.startTime,
                "End": slot.endTime,
                "Status": slot.status
            });
        }

        notify("Availability loaded successfully");
    } catch (error) {
        notify(error.message, "error");
    }
});

$("booking-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const container = $("booking-result");
    container.replaceChildren();

    const payload = {
        slotId: Number($("booking-slot-id").value),
        seatsRequested: Number($("booking-seats").value)
    };

    try {
        const booking = await apiRequest("/api/bookings", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        addResult(container, {
            "Booking ID": booking.bookingId,
            "Transport Slot ID": booking.slotId,
            "Seats": booking.seatsRequested,
            "Status": booking.status
        });

        notify("Booking submitted successfully");
    } catch (error) {
        notify(error.message, "error");
    }
});

$("history-button").addEventListener("click", async () => {
    const container = $("booking-history");
    container.replaceChildren();

    try {
        const bookings = await apiRequest("/api/bookings/my");

        if (!bookings.length) {
            container.appendChild(
                createTextElement("p", "No bookings found")
            );
            return;
        }

        for (const booking of bookings) {
            addResult(container, {
                "Booking ID": booking.bookingId,
                "Route": `${booking.origin} → ${booking.destination}`,
                "Departure": booking.departureTime,
                "Seats": booking.seatsRequested,
                "Status": booking.status
            });
        }

        notify("Booking history loaded successfully");
    } catch (error) {
        notify(error.message, "error");
    }
});
