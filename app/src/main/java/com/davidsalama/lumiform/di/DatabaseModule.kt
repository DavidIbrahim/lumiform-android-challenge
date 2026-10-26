package com.davidsalama.lumiform.di

import android.content.Context
import androidx.room.Room
import com.davidsalama.lumiform.data.local.FormDatabase
import com.davidsalama.lumiform.data.local.dao.FormDao
import com.davidsalama.lumiform.data.local.dao.SelectionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FormDatabase =
        Room.databaseBuilder(context, FormDatabase::class.java, FormDatabase.NAME).build()

    @Provides
    fun provideFormDao(database: FormDatabase): FormDao = database.formDao()

    @Provides
    fun provideSelectionDao(database: FormDatabase): SelectionDao = database.selectionDao()
}
