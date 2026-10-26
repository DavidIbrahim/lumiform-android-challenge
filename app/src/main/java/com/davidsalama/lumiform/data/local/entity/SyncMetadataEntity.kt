package com.davidsalama.lumiform.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** When the cached form was last fetched successfully, for the "offline, last synced at" banner. */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAtMillis: Long,
) {
    companion object {
        const val FORM_KEY = "form"
    }
}
