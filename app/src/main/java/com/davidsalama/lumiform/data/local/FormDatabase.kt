package com.davidsalama.lumiform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.davidsalama.lumiform.data.local.dao.FormDao
import com.davidsalama.lumiform.data.local.dao.SelectionDao
import com.davidsalama.lumiform.data.local.entity.ChoiceResponseEntity
import com.davidsalama.lumiform.data.local.entity.FormNodeEntity
import com.davidsalama.lumiform.data.local.entity.SelectedResponseEntity
import com.davidsalama.lumiform.data.local.entity.SyncMetadataEntity

@Database(
    entities = [
        FormNodeEntity::class,
        ChoiceResponseEntity::class,
        SelectedResponseEntity::class,
        SyncMetadataEntity::class,
    ],
    version = 1,
)
abstract class FormDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao
    abstract fun selectionDao(): SelectionDao

    companion object {
        const val NAME = "lumiform.db"
    }
}
