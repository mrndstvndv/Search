# File Search Auto-Sync Implementation Spec

## Overview

This document specifies the implementation of automatic file/folder index synchronization for the Search app. The goal is to keep search results up-to-date when files are created, deleted, or renamed in indexed folders.

**Related TODO.md item**: *"Persist the index in Room (path, display data, mime-type, lastModified) with change observers so incremental updates and invalidations are cheap."*

---

## Problem Statement

Currently, the file search index is only updated when:
1. A folder is first added (full index via `FileSearchIndexWorker`)
2. User manually taps "Rescan" button

This means new/deleted/renamed files don't appear in search results until manual intervention.

---

## Solution Architecture

### Sync Triggers

| Trigger | Condition | Worker Type |
|---------|-----------|-------------|
| **App open** | `syncOnAppOpen == true` AND last sync > 1 min ago | OneTimeWork (expedited) |
| **Periodic** | `syncIntervalMinutes > 0` | PeriodicWork |
| **Manual rescan** | User taps "Rescan" | OneTimeWork (existing) |
| **Folder added** | New folder indexed | OneTimeWork (existing full index) |

### Sync Strategy: Incremental Diffing

Instead of re-indexing everything, the sync worker will:

1. Load all indexed entries for each enabled root from Room
2. Traverse the SAF document tree
3. Compare `lastModified` timestamps to detect changes
4. Apply incremental updates:
   - **New files**: Insert into database
   - **Modified files**: Update in database
   - **Deleted files**: Remove from database
5. **Update scan metadata**: After syncing each root, update `FileSearchScanMetadata` with the new item count and timestamp. This refreshes the "Updated xx ago" subtitle shown in the settings UI.

This is much faster than a full re-index for large folders with few changes.

---

## Settings Changes

### New Fields in `FileSearchSettings`

```kotlin
data class FileSearchSettings(
    // ... existing fields ...
    val syncIntervalMinutes: Int,    // 0, 15, 30, 60, 120 (0 = disabled)
    val syncOnAppOpen: Boolean       // default: true
)
```

### Sync Interval Options

| Value | UI Label |
|-------|----------|
| 0 | Disabled |
| 15 | Every 15 minutes |
| 30 | Every 30 minutes |
| 60 | Every hour |
| 120 | Every 2 hours |

### Default Values

- `syncIntervalMinutes`: 30
- `syncOnAppOpen`: true

---

## File Changes Required

### 1. `ProviderSettingsRepository.kt`

Add new methods:

```kotlin
fun setFileSearchSyncInterval(minutes: Int)
fun setFileSearchSyncOnAppOpen(enabled: Boolean)
```

Update `FileSearchSettings`:
- Add `syncIntervalMinutes: Int = 30`
- Add `syncOnAppOpen: Boolean = true`
- Update `toJsonString()` to include new fields
- Update `fromJson()` to parse new fields

### 2. `IndexedDocumentDao.kt`

Add new methods for incremental sync:

```kotlin
@Upsert
suspend fun upsertAll(entities: List<IndexedDocumentEntity>)

@Query("SELECT * FROM indexed_documents WHERE rootId = :rootId")
suspend fun getAllForRoot(rootId: String): List<IndexedDocumentEntity>

@Query("DELETE FROM indexed_documents WHERE documentUri IN (:uris)")
suspend fun deleteByUris(uris: List<String>)
```

### 3. New File: `IncrementalFileSyncWorker.kt`

Location: `app/src/main/java/com/mrndstvndv/search/provider/files/indexing/IncrementalFileSyncWorker.kt`

```kotlin
class IncrementalFileSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dao = FileSearchDatabase.get(applicationContext).indexedDocumentDao()
        val settingsRepository = ProviderSettingsRepository(applicationContext)
        
        // Get all enabled roots (including Downloads if enabled)
        val roots = getEnabledRoots(settingsRepository)
        
        roots.forEach { root ->
            val itemCount = syncRoot(root, dao)
            
            // Update scan metadata to refresh "Updated xx ago" in settings UI
            settingsRepository.updateFileSearchScanState(
                rootId = root.id,
                state = FileSearchScanState.SUCCESS,
                itemCount = itemCount,
                errorMessage = null
            )
        }
        
        // Update last sync timestamp
        settingsRepository.updateLastSyncTimestamp(System.currentTimeMillis())
        
        Result.success()
    }
    
    private suspend fun syncRoot(root: SyncableRoot, dao: IndexedDocumentDao): Int {
        val docFile = resolveDocumentFile(root) ?: return 0
        
        // Get current indexed state
        val indexed = dao.getAllForRoot(root.id).associateBy { it.documentUri }
        val seenUris = mutableSetOf<String>()
        val batch = mutableListOf<IndexedDocumentEntity>()
        
        // Scan and diff
        scanAndDiff(docFile, root, "", indexed, seenUris, batch, dao)
        
        // Flush remaining batch
        if (batch.isNotEmpty()) {
            dao.upsertAll(batch)
        }
        
        // Delete removed files
        val deleted = indexed.keys - seenUris
        if (deleted.isNotEmpty()) {
            dao.deleteByUris(deleted.toList())
        }
        
        // Return total item count after sync
        return seenUris.size
    }
    
    private suspend fun scanAndDiff(
        doc: DocumentFile,
        root: SyncableRoot,
        currentPath: String,
        indexed: Map<String, IndexedDocumentEntity>,
        seenUris: MutableSet<String>,
        batch: MutableList<IndexedDocumentEntity>,
        dao: IndexedDocumentDao
    ) {
        doc.listFiles().forEach { child ->
            val uri = child.uri.toString()
            seenUris += uri
            
            val existing = indexed[uri]
            val name = child.name ?: "(unnamed)"
            val relativePath = if (currentPath.isEmpty()) name else "$currentPath/$name"
            
            // Check if new or modified
            if (existing == null || existing.lastModified != child.lastModified()) {
                batch += IndexedDocumentEntity(
                    id = existing?.id ?: 0, // Use existing ID for updates
                    rootId = root.id,
                    rootDisplayName = root.displayName,
                    documentUri = uri,
                    relativePath = relativePath,
                    displayName = name,
                    mimeType = child.type,
                    sizeBytes = child.length(),
                    lastModified = child.lastModified(),
                    isDirectory = child.isDirectory
                )
                
                if (batch.size >= BATCH_SIZE) {
                    dao.upsertAll(batch.toList())
                    batch.clear()
                }
            }
            
            if (child.isDirectory) {
                scanAndDiff(child, root, relativePath, indexed, seenUris, batch, dao)
            }
        }
    }
    
    companion object {
        private const val BATCH_SIZE = 100
    }
}
```

### 4. `FileSearchRepository.kt`

Add methods for scheduling periodic sync:

```kotlin
fun schedulePeriodicSync(intervalMinutes: Int) {
    if (intervalMinutes <= 0) {
        cancelPeriodicSync()
        return
    }
    
    val request = PeriodicWorkRequestBuilder<IncrementalFileSyncWorker>(
        intervalMinutes.toLong(), TimeUnit.MINUTES
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()
    
    workManager.enqueueUniquePeriodicWork(
        PERIODIC_SYNC_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun cancelPeriodicSync() {
    workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
}

fun triggerImmediateSync() {
    val request = OneTimeWorkRequestBuilder<IncrementalFileSyncWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()
    workManager.enqueue(request)
}

companion object {
    private const val PERIODIC_SYNC_WORK_NAME = "file-search-periodic-sync"
}
```

### 5. `FileSearchSettingsScreen.kt`

Add UI controls for sync settings:

```kotlin
// After the sort order section, add:

SettingsDivider()

// Sync interval picker
SyncIntervalRow(
    selectedInterval = fileSearchSettings.syncIntervalMinutes,
    onIntervalSelected = { settingsRepository.setFileSearchSyncInterval(it) }
)

SettingsDivider()

// Sync on app open toggle
SettingsToggleRow(
    title = "Sync on app open",
    subtitle = "Check for file changes when Search opens.",
    checked = fileSearchSettings.syncOnAppOpen,
    onCheckedChange = { settingsRepository.setFileSearchSyncOnAppOpen(it) }
)
```

New composable for interval picker:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncIntervalRow(
    selectedInterval: Int,
    onIntervalSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Background sync",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Automatically check for file changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Dropdown or SegmentedButton for interval options
        // Options: Disabled, 15m, 30m, 1h, 2h
    }
}
```

### 6. `MainActivity.kt`

Add sync-on-open logic:

```kotlin
// In onCreate, after provider initialization:

LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
        val settings = settingsRepository.fileSearchSettings.value
        if (settings.syncOnAppOpen && settings.hasEnabledRoots()) {
            val lastSync = settings.lastSyncTimestamp
            val minGap = 60_000L // 1 minute
            if (System.currentTimeMillis() - lastSync > minGap) {
                fileSearchRepository.triggerImmediateSync()
            }
        }
    }
}
```

### 7. Update `FileSearchSettings` JSON Schema

Add to `toJsonString()`:
```kotlin
json.put("syncIntervalMinutes", syncIntervalMinutes)
json.put("syncOnAppOpen", syncOnAppOpen)
json.put("lastSyncTimestamp", lastSyncTimestamp)
```

Add to `fromJson()`:
```kotlin
val syncIntervalMinutes = json.optInt("syncIntervalMinutes", 30)
val syncOnAppOpen = json.optBoolean("syncOnAppOpen", true)
val lastSyncTimestamp = json.optLong("lastSyncTimestamp", 0L)
```

---

## Database Considerations

### Room Version

The current database version is 1. Adding `@Upsert` does not require a schema migration since it's a DAO method, not a schema change. However, if we add `lastSyncTimestamp` to `FileSearchSettings` (stored in SharedPreferences, not Room), no migration is needed.

### Batch Size

Use batch size of 100 for upsert operations to balance memory usage and database transaction overhead.

---

## UI Mock

### File Search Settings Screen (updated)

```
┌─────────────────────────────────────────┐
│ Files & folders                    [←]  │
│ Pick which directories are indexed.     │
├─────────────────────────────────────────┤
│ Load thumbnails                    [ON] │
│ Show previews for images, videos...     │
├─────────────────────────────────────────┤
│ Thumbnail crop                          │
│ [ Fit ] [ Center crop ]                 │
├─────────────────────────────────────────┤
│ Sort order                              │
│ [ Name ] [ Date modified ]              │
│ Ascending order                    [ON] │
├─────────────────────────────────────────┤
│ Background sync                         │  ← NEW
│ Automatically check for file changes.   │
│ [ Off | 15m | 30m | 1h | 2h ]          │
├─────────────────────────────────────────┤
│ Sync on app open                   [ON] │  ← NEW
│ Check for file changes when Search...   │
├─────────────────────────────────────────┤
│ Downloads folder                   [ON] │
│ Indexed 1,234 items                     │
│ Updated 5 minutes ago                   │
│ [Rescan]                                │
├─────────────────────────────────────────┤
│ Documents                          [ON] │
│ Indexed 567 items                       │
│ [Rescan] [Remove]                       │
├─────────────────────────────────────────┤
│        [ Add folder ]                   │
└─────────────────────────────────────────┘
```

---

## Testing Checklist

### Unit Tests
- [ ] `FileSearchSettings.fromJson()` correctly parses new fields
- [ ] `FileSearchSettings.toJsonString()` correctly serializes new fields
- [ ] Default values are correct when fields are missing

### Integration Tests
- [ ] Periodic sync schedules correctly with given interval
- [ ] Periodic sync cancels when interval set to 0
- [ ] Sync-on-open respects 1-minute minimum gap
- [ ] Incremental sync correctly detects new files
- [ ] Incremental sync correctly detects deleted files
- [ ] Incremental sync correctly detects modified files (lastModified change)
- [ ] Incremental sync updates scan metadata (item count and "Updated xx ago" timestamp)

### Manual Tests
- [ ] Add a file to indexed folder → appears in search after sync
- [ ] Delete a file from indexed folder → disappears from search after sync
- [ ] Rename a file → old name gone, new name appears after sync
- [ ] Change sync interval in settings → periodic work updates
- [ ] Toggle sync-on-open → behavior changes accordingly

---

## Implementation Order

1. **Update `FileSearchSettings`** - Add new fields, JSON serialization
2. **Update `ProviderSettingsRepository`** - Add setter methods
3. **Update `IndexedDocumentDao`** - Add `upsertAll`, `getAllForRoot`, `deleteByUris`
4. **Create `IncrementalFileSyncWorker`** - Core sync logic
5. **Update `FileSearchRepository`** - Add scheduling methods
6. **Update `FileSearchSettingsScreen`** - Add UI controls
7. **Update `MainActivity`** - Add sync-on-open trigger
8. **Test and verify**

---

## Performance Notes

- SAF tree traversal involves IPC overhead; keep scans efficient
- Use `lastModified` comparison before updating to minimize DB writes
- Batch DB operations (100 items per batch)
- Expedited work for sync-on-open to prioritize user-facing freshness
- Add `setRequiresBatteryNotLow(true)` constraint for periodic sync

---

## Future Enhancements (Out of Scope)

- ContentObserver for MediaStore (faster Downloads updates)
- Foreground service for real-time monitoring (battery trade-off)
- Selective sync (only sync roots that changed)
- Sync progress indicator in main UI
