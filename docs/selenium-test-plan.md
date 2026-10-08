# Selenium Test Plan - E-Waste Pickup Scheduler

**Target:** web UI at `http://localhost:8080` (override with `-DbaseUrl=...`; dev = 8082, staging = 8083)
**Tool:** Selenium WebDriver 4 + JUnit 5, Chrome (headless optional with `-Dheadless=true`)
**Locators:** fixed element ids in `index.html`
**Data:** every run uses a unique email and a unique area name, so tests never collide.
**Failure evidence:** a PNG is saved to `target/screenshots/` when any test fails.

| ID | Journey | Preconditions | Steps | Expected result |
|----|---------|---------------|-------|-----------------|
| UI-01 | Registration | App running, DB reachable | Open page; enter name, unique email, password; click Register | `#reg-message` shows "Registered successfully. User id: N" (class `ok`); `#current-user` shows "Signed in as <name>" |
| UI-02 | Duplicate registration | None | Register with an email; click Register again with the same email | Second attempt shows a message in `#reg-message` with class `err`; no "Registered successfully" text |
| UI-03 | Slot creation | None | Fill date 2026-12-01, 09:00, 11:00, unique area; click Create slot; click Refresh slots | `#slot-message` shows "Slot created in <area>"; the slot appears in the Available slots table |
| UI-04 | Booking | User registered; slot created | Click Book on the slot | `#slots-message` shows "Booking N CONFIRMED"; slot disappears from Available slots; booking appears in My bookings with status CONFIRMED |
| UI-05 | Cancellation | Booking exists | Click Cancel on the booking | Booking status becomes CANCELLED; Cancel button gone; slot reappears in Available slots |
