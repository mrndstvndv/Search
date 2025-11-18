# Performance Considerations

- Move the SharedPreferences reads in `ProviderSettingsRepository` and `AliasRepository` onto `Dispatchers.IO` and emit their values to `MutableStateFlow` afterward. This keeps cold launches from blocking the UI thread while flows initialize.
- Consider caching the initial state for those repositories via `DataStore` or a precomputed snapshot so the Compose tree can render immediately while background work catches up.
- Evaluate whether the assistant-role detection needs to run on every `onResume`. If not, debounce it or gate on actual user triggers to avoid redundant work.

# File/Folder Search

- Introduce a local file/folder search provider indexing on-device storage (scoped to user-approved roots) to accelerate document discovery; reuse the existing provider pattern with throttled indexing and permission prompts.
- Implement a provider-based indexer for on-device storage with incremental indexing and background updates; surface results in search with metadata.
- Restrict indexing to user-approved roots; enforce permission prompts before scanning.
- Cache index results and support incremental invalidation on storage changes.

# Animation-Only Enhancements

- Replace the ad-hoc animation toggle plumbing with a MotionPreferences model exposed from `ProviderSettingsRepository`, then surface it via a `CompositionLocal` or ambient parameter. This lets composables call helpers like `rememberMotionAwareFloat` instead of threading `animationsEnabled` everywhere and recreating specs manually.
- Create utility wrappers (`motionAwareTween`, `motionAwareVisibility`) so disabling animations automatically swaps specs for zero-duration ones, and collect motion settings centrally in `SearchTheme` rather than each screen.

# Calendar Provider

- Add a Calendar provider that surfaces upcoming events/reminders alongside other search providers; source events via `CalendarContract` and consider caching results to avoid UI jank.

