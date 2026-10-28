
"use strict";

let adminToken = null;
let allBookings = [];
let scheduledTransport = [];
let matchingVehicles = [];

const $ = (id) => document.getElementById(id);

function notify(message, type = "success") {
    const element = $("admin-notification");
    element.textContent = message;
    element.className = `admin-notification ${type}`;
}

function setAuthenticated(authenticated, identity = "") {
    $("admin-login-section").hidden = authenticated;
    $("admin-dashboard").hidden = !authenticated;
    $("admin-identity").textContent = identity;
}

async function request(path, options = {}) {
    const headers = {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {})
    };

    if (adminToken) {
        headers.Authorization = `Bearer ${adminToken}`;
    }

    let response;
    try {
        response = await fetch(path, { ...options, headers });
    } catch {
        throw new Error("Unable to connect to the server.");
    }

    if (!response.ok) {
        let message = `Request failed: HTTP ${response.status}`;

        try {
            const errorBody = await response.json();
            message = errorBody.detail || errorBody.message ||
                errorBody.error || message;
        } catch {
            // Keep the default HTTP message.
        }

        if (response.status === 401) {
            adminToken = null;
            setAuthenticated(false);
            throw new Error(
                "Session expired or invalid credentials. Please sign in again."
            );
        }

        if (response.status === 403) {
            throw new Error("Administrator access required.");
        }

        throw new Error(message);
    }

    if (response.status === 204) return null;

    const text = await response.text();
    return text ? JSON.parse(text) : null;
}

function formatDateTime(value) {
    if (!value) return "N/A";

    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? String(value)
        : date.toLocaleString();
}

function cell(row, value) {
    const td = document.createElement("td");
    td.textContent = String(value ?? "N/A");
    row.appendChild(td);
    return td;
}

function resetScheduleForm() {
    $("admin-schedule-form").reset();
    $("schedule-capacity").value = "40";
    matchingVehicles = [];
    $("schedule-vehicle").replaceChildren(
        new Option("Find vehicles first", "")
    );
    $("schedule-vehicle").disabled = true;
    $("schedule-transport-button").disabled = true;
    $("vehicle-selection-help").textContent =
        "Choose the route, departure time, type, and capacity first.";
}

function logout(message = "Signed out successfully") {
    adminToken = null;
    allBookings = [];
    scheduledTransport = [];
    matchingVehicles = [];

    $("admin-login-form").reset();
    $("admin-bookings-body").replaceChildren();
    $("admin-schedules-body").replaceChildren();

    updateStatistics();
    resetScheduleForm();
    setAuthenticated(false);
    notify(message);
}

$("admin-login-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const email = $("admin-email").value.trim();
    const password = $("admin-password").value;

    try {
        const result = await request("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        });

        if (result.role !== "ADMIN") {
            throw new Error("This account does not have administrator access.");
        }
        if (!result.token) {
            throw new Error("Authentication token missing.");
        }

        adminToken = result.token;
        $("admin-password").value = "";

        setAuthenticated(true, `Signed in as ${result.name} (${result.email})`);
        notify("Administrator login successful.");

        await Promise.all([
            loadBookings(),
            loadScheduledTransport()
        ]);
    } catch (error) {
        notify(error.message, "error");
    }
});

$("admin-logout").addEventListener("click", () => logout());

async function loadBookings() {
    try {
        const result = await request("/api/admin/bookings");
        allBookings = Array.isArray(result) ? result : [];
        updateStatistics();
        renderBookings();
    } catch (error) {
        notify(error.message, "error");
    }
}

function updateStatistics() {
    $("total-count").textContent = allBookings.length;

    for (const status of ["PENDING", "CONFIRMED", "REJECTED"]) {
        const count = allBookings.filter(
            booking => booking.status === status
        ).length;

        $(`${status.toLowerCase()}-count`).textContent = count;
    }
}

function renderBookings() {
    const body = $("admin-bookings-body");
    body.replaceChildren();

    const filter = $("booking-filter").value;
    const bookings = filter === "ALL"
        ? allBookings
        : allBookings.filter(booking => booking.status === filter);

    if (bookings.length === 0) {
        const row = document.createElement("tr");
        const td = cell(row, "No bookings found");
        td.colSpan = 6;
        body.appendChild(row);
        return;
    }

    for (const booking of bookings) {
        const row = document.createElement("tr");
        cell(row, booking.bookingId);
        cell(row, `${booking.origin ?? ""} → ${booking.destination ?? ""}`);
        cell(row, formatDateTime(booking.departureTime));
        cell(row, booking.seatsRequested);

        const status = String(booking.status ?? "UNKNOWN");
        const statusCell = cell(row, status);
        statusCell.className = `booking-status status-${status.toLowerCase()}`;

        const actions = document.createElement("td");

        if (status === "PENDING") {
            const confirm = document.createElement("button");
            confirm.type = "button";
            confirm.textContent = "Confirm";
            confirm.className = "confirm-button";
            confirm.addEventListener("click", () =>
                updateBooking(booking.bookingId, "confirm")
            );

            const reject = document.createElement("button");
            reject.type = "button";
            reject.textContent = "Reject";
            reject.className = "reject-button";
            reject.addEventListener("click", () =>
                updateBooking(booking.bookingId, "reject")
            );

            actions.append(confirm, reject);
        } else {
            actions.textContent = "—";
        }

        row.appendChild(actions);
        body.appendChild(row);
    }
}

async function updateBooking(bookingId, action) {
    const verb = action === "confirm" ? "confirm" : "reject";

    if (!window.confirm(
        `Are you sure you want to ${verb} booking #${bookingId}?`
    )) return;

    try {
        await request(
            `/api/admin/bookings/${encodeURIComponent(bookingId)}/${action}`,
            { method: "PATCH" }
        );

        await Promise.all([loadBookings(), loadScheduledTransport()]);
        notify(`Booking #${bookingId} ${verb}ed successfully.`);
    } catch (error) {
        notify(error.message, "error");
    }
}

/* ---------- Transport scheduling ---------- */

function getScheduleCriteria() {
    const origin = $("schedule-origin").value.trim();
    const destination = $("schedule-destination").value.trim();
    const departureValue = $("schedule-departure").value;
    const vehicleType = $("schedule-vehicle-type").value;
    const minimumCapacity = Number($("schedule-capacity").value);

    if (!origin || !destination) {
        throw new Error("Enter both the origin and destination.");
    }
    if (origin.toLowerCase() === destination.toLowerCase()) {
        throw new Error("Origin and destination must be different.");
    }
    if (!departureValue) {
        throw new Error("Select a departure date and time.");
    }
    if (new Date(departureValue).getTime() <= Date.now()) {
        throw new Error("Departure time must be in the future.");
    }
    if (!vehicleType) {
        throw new Error("Select a vehicle type.");
    }
    if (!Number.isInteger(minimumCapacity) || minimumCapacity < 1) {
        throw new Error("Minimum capacity must be a whole number of at least 1.");
    }

    return {
        origin,
        destination,
        departureTime: departureValue,
        vehicleType,
        minimumCapacity
    };
}

function invalidateVehicleSelection() {
    matchingVehicles = [];
    $("schedule-vehicle").replaceChildren(
        new Option("Find matching vehicles first", "")
    );
    $("schedule-vehicle").disabled = true;
    $("schedule-transport-button").disabled = true;
    $("vehicle-selection-help").textContent =
        "Criteria changed. Find matching vehicles again.";
}

for (const id of [
    "schedule-origin",
    "schedule-destination",
    "schedule-departure",
    "schedule-vehicle-type",
    "schedule-capacity"
]) {
    $(id).addEventListener("input", invalidateVehicleSelection);
    $(id).addEventListener("change", invalidateVehicleSelection);
}

$("find-vehicles-button").addEventListener("click", async () => {
    let criteria;

    try {
        criteria = getScheduleCriteria();
    } catch (error) {
        notify(error.message, "error");
        return;
    }

    const button = $("find-vehicles-button");
    button.disabled = true;
    button.textContent = "Searching...";
    invalidateVehicleSelection();

    try {
        const params = new URLSearchParams({
            vehicleType: criteria.vehicleType,
            minimumCapacity: String(criteria.minimumCapacity),
            departureTime: criteria.departureTime
        });

        const result = await request(
            `/api/admin/vehicles/available?${params.toString()}`
        );

        matchingVehicles = Array.isArray(result) ? result : [];
        const select = $("schedule-vehicle");
        select.replaceChildren();

        if (matchingVehicles.length === 0) {
            select.add(new Option("No matching vehicles available", ""));
            select.disabled = true;
            $("schedule-transport-button").disabled = true;
            $("vehicle-selection-help").textContent =
                "Try a different vehicle type, capacity, or departure time.";
            notify("No matching vehicles found.", "error");
            return;
        }

        select.add(new Option("Select a vehicle", ""));

        for (const vehicle of matchingVehicles) {
            const label =
                `${vehicle.registrationNumber} — ` +
                `${vehicle.vehicleType} — ${vehicle.capacity} seats`;

            select.add(new Option(label, String(vehicle.vehicleId)));
        }

        select.disabled = false;
        $("vehicle-selection-help").textContent =
            `${matchingVehicles.length} matching vehicle(s) found. Select one to continue.`;
        notify(`${matchingVehicles.length} matching vehicle(s) found.`);
    } catch (error) {
        notify(error.message, "error");
    } finally {
        button.disabled = false;
        button.textContent = "Find matching vehicles";
    }
});

$("schedule-vehicle").addEventListener("change", () => {
    const selectedId = Number($("schedule-vehicle").value);
    const vehicle = matchingVehicles.find(
        item => Number(item.vehicleId) === selectedId
    );

    $("schedule-transport-button").disabled = !vehicle;

    $("vehicle-selection-help").textContent = vehicle
        ? `Selected ${vehicle.registrationNumber}: ${vehicle.capacity} seats.`
        : "Select one of the matching vehicles.";
});

$("admin-schedule-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    let criteria;
    try {
        criteria = getScheduleCriteria();
    } catch (error) {
        notify(error.message, "error");
        return;
    }

    const vehicleId = Number($("schedule-vehicle").value);
    const vehicle = matchingVehicles.find(
        item => Number(item.vehicleId) === vehicleId
    );

    if (!vehicle || !Number.isInteger(vehicleId)) {
        notify("Find and select an available vehicle first.", "error");
        return;
    }

    if (vehicle.vehicleType !== criteria.vehicleType ||
        Number(vehicle.capacity) < criteria.minimumCapacity) {
        notify("The selected vehicle does not meet the current criteria.", "error");
        return;
    }

    const submitButton = $("schedule-transport-button");
    submitButton.disabled = true;
    submitButton.textContent = "Scheduling...";

    try {
        const slot = await request("/api/admin/transport-slots", {
            method: "POST",
            body: JSON.stringify({
                ...criteria,
                vehicleId
            })
        });

        resetScheduleForm();

        await loadScheduledTransport();

        notify(
            `Transport scheduled successfully. Slot ID: ${slot.slotId}.`
        );
    } catch (error) {
        notify(error.message, "error");
        // Refresh the candidate list if the vehicle became unavailable.
        invalidateVehicleSelection();
    } finally {
        submitButton.textContent = "Schedule transport →";
        submitButton.disabled = !$("schedule-vehicle").value;
    }
});

async function loadScheduledTransport() {
    try {
        const result = await request("/api/admin/transport-slots");
        scheduledTransport = Array.isArray(result) ? result : [];
        renderScheduledTransport();
    } catch (error) {
        notify(error.message, "error");
    }
}

function renderScheduledTransport() {
    const body = $("admin-schedules-body");
    body.replaceChildren();

    if (scheduledTransport.length === 0) {
        const row = document.createElement("tr");
        const td = cell(row, "No scheduled transport found");
        td.colSpan = 7;
        body.appendChild(row);
        return;
    }

    for (const slot of scheduledTransport) {
        const row = document.createElement("tr");
        cell(row, slot.slotId);
        cell(row, slot.registrationNumber);
        cell(row, slot.vehicleType);
        cell(row, slot.vehicleCapacity);
        cell(row, `${slot.origin ?? ""} → ${slot.destination ?? ""}`);
        cell(row, formatDateTime(slot.departureTime));
        cell(row, slot.availableSeats);
        body.appendChild(row);
    }
}

$("booking-filter").addEventListener("change", renderBookings);

$("refresh-bookings").addEventListener("click", async () => {
    await loadBookings();
    notify("Booking data refreshed.");
});

$("refresh-schedules").addEventListener("click", async () => {
    await loadScheduledTransport();
    notify("Scheduled transport refreshed.");
});

setAuthenticated(false);
