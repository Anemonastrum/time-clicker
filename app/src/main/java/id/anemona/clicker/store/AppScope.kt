package id.anemona.clicker.store

import android.content.Context

/** App-wide singletons reachable from activity, service and overlay alike. */
object AppScope {
    @Volatile private var store: Store? = null

    fun store(context: Context): Store {
        val existing = store
        if (existing != null) return existing
        synchronized(this) {
            val again = store
            if (again != null) return again
            val created = Store(context)
            store = created
            return created
        }
    }
}
