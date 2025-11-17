# Performance Considerations

- Move the SharedPreferences reads in `ProviderSettingsRepository` and `AliasRepository` onto `Dispatchers.IO` and emit their values to `MutableStateFlow` afterward. This keeps cold launches from blocking the UI thread while flows initialize.
- Consider caching the initial state for those repositories via `DataStore` or a precomputed snapshot so the Compose tree can render immediately while background work catches up.
- Evaluate whether the assistant-role detection needs to run on every `onResume`. If not, debounce it or gate on actual user triggers to avoid redundant work.

# Interaction & Features

- Replace the ad-hoc animation toggle plumbing with a `MotionPreferences` model exposed from `ProviderSettingsRepository`, then surface it via a `CompositionLocal` or ambient parameter. This lets composables call helpers like `rememberMotionAwareFloat` instead of threading `animationsEnabled` everywhere and recreating specs manually.
- Create utility wrappers (`motionAwareTween`, `motionAwareVisibility`) so disabling animations automatically swaps specs for zero-duration ones, and collect motion settings centrally in `SearchTheme` rather than each screen.
- Add a Calendar provider that surfaces upcoming events/reminders alongside other search providers; source events via `CalendarContract` and consider caching results to avoid UI jank.
- Add a local file/folder provider that indexes on-device storage (scoped to user-approved roots) so Search can launch documents quickly; reuse the provider pattern with throttled indexing and permission prompts.
