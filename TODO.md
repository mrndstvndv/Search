# Performance Considerations

- Move the SharedPreferences reads in `ProviderSettingsRepository` and `AliasRepository` onto `Dispatchers.IO` and emit their values to `MutableStateFlow` afterward. This keeps cold launches from blocking the UI thread while flows initialize.
- Consider caching the initial state for those repositories via `DataStore` or a precomputed snapshot so the Compose tree can render immediately while background work catches up.
- Evaluate whether the assistant-role detection needs to run on every `onResume`. If not, debounce it or gate on actual user triggers to avoid redundant work.

## ┃ File/Folder Search Plan

- Define the provider contract, result schema, and ranking hooks so the file/folder provider plugs into the existing dispatcher without regressions.
- Build a scoped indexing service (likely WorkManager + coroutines) that requests storage permissions per root, throttles scans, and tracks progress for UX messaging.
- Persist the index in Room (path, display data, mime-type, lastModified) with change observers so incremental updates and invalidations are cheap.
- Integrate UI affordances: permission education screen, search result rendering with metadata chips, and analytics instrumentation covering query latency & accuracy.
- Add automated tests (unit + integration) that simulate storage mutations and verify the provider updates its index and emits deltas to the UI flows.

## ┃ Animation-Only Enhancement Plan

- Inventory every composable that toggles animation flags manually and document the timing specs they require so replacements can be verified.
- Introduce a centralized `MotionPreferences` surface via `ProviderSettingsRepository` + `CompositionLocal` and migrate the theme to read from it.
- Provide helper APIs (`motionAwareTween`, motion-aware visibility wrappers, etc.) and refactor feature screens to adopt them iteratively.
- Add UI tests that flip the animation setting at runtime to ensure transitions switch to zero-duration specs without recomposition glitches.

# Animation-Only Enhancements ✅

- ~~Replace the ad-hoc animation toggle plumbing with a MotionPreferences model exposed from `ProviderSettingsRepository`, then surface it via a `CompositionLocal` or ambient parameter. This lets composables call helpers like `rememberMotionAwareFloat` instead of threading `animationsEnabled` everywhere and recreating specs manually.~~
- ~~Create utility wrappers (`motionAwareTween`, `motionAwareVisibility`) so disabling animations automatically swaps specs for zero-duration ones, and collect motion settings centrally in `SearchTheme` rather than each screen.~~

## ┃ Calendar Provider Plan

- Clarify requirements for calendar sources (personal vs. work profiles), sync windows, and how event relevance scores feed into the global ranking pipeline.
- Implement a `CalendarProviderRepository` that queries `CalendarContract`, normalizes attendees/reminders, and caches responses with graceful permission handling.
- Expose settings toggles (per-calendar visibility, sync horizon) inside Provider Settings and persist them in DataStore for quick reads during cold starts.
- Render calendar entries in search results with start/end chips, quick actions (open, RSVP), and add tests that validate daylight-saving edge cases plus multi-day events.
