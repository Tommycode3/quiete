package info.quiete.launcher

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import info.quiete.launcher.data.AppEntry
import info.quiete.launcher.data.AppRepository
import info.quiete.launcher.data.Prefs
import info.quiete.launcher.data.Search
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppRepository(app)
    private val prefs = Prefs(app)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Bumped whenever the pinned list changes, to re-derive [pinned]. */
    private val pinnedRevision = MutableStateFlow(0)

    val apps: StateFlow<List<AppEntry>> = repository.apps

    /** Home screen list, in the user's chosen order — not alphabetical. */
    val pinned: StateFlow<List<AppEntry>> =
        combine(repository.apps, pinnedRevision) { all, _ ->
            if (all.isEmpty()) return@combine emptyList()
            val byComponent = all.associateBy { it.component }

            val stored = prefs.pinnedOrNull()
            if (stored != null) {
                return@combine stored.mapNotNull(byComponent::get)
            }

            // First run: seed from the device's own dialer/messages/browser/camera,
            // matched by package to each one's actual launcher entry.
            val seeded = prefs.defaultPackages().mapNotNull { pkg ->
                all.firstOrNull { it.packageName == pkg }
            }
            prefs.save(seeded.map { it.component })
            seeded
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Drawer contents: alphabetical when idle, ranked while searching. */
    val results: StateFlow<List<AppEntry>> =
        combine(repository.apps, _query) { all, q ->
            if (q.isBlank()) all else Search.rank(all, q)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        repository.start(viewModelScope)
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    private val _homeSignal = MutableStateFlow(0)

    /** Bumped by the Activity on every HOME press that lands while we are foreground. */
    val homeSignal: StateFlow<Int> = _homeSignal.asStateFlow()

    fun onHomePressed() {
        _homeSignal.value++
    }

    fun launch(entry: AppEntry, bounds: Rect?) {
        prefs.recordLaunch(entry.component)
        repository.launch(entry, bounds)
    }

    fun openAppInfo(entry: AppEntry, bounds: Rect?) = repository.openAppInfo(entry, bounds)

    fun togglePin(entry: AppEntry) {
        prefs.toggle(entry.component)
        pinnedRevision.value++
    }

    fun isPinned(entry: AppEntry): Boolean = prefs.pinned().contains(entry.component)

    override fun onCleared() {
        repository.stop()
    }
}
