package com.softhome.core.data.di

import com.softhome.core.common.DefaultDispatcherProvider
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.data.repository.AppActionsRepository
import com.softhome.core.data.repository.AppActionsRepositoryImpl
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.AppRepositoryImpl
import com.softhome.core.data.repository.DeviceStatusRepository
import com.softhome.core.data.repository.DeviceStatusRepositoryImpl
import com.softhome.core.data.repository.FolderRepository
import com.softhome.core.data.repository.FolderRepositoryImpl
import com.softhome.core.data.repository.NotesRepository
import com.softhome.core.data.repository.NotesRepositoryImpl
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.data.repository.PrefsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @Singleton
    fun provideDispatchers(): DispatcherProvider = DefaultDispatcherProvider()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    abstract fun bindAppActionsRepository(impl: AppActionsRepositoryImpl): AppActionsRepository

    @Binds
    abstract fun bindPrefsRepository(impl: PrefsRepositoryImpl): PrefsRepository

    @Binds
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    @Binds
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds
    abstract fun bindDeviceStatusRepository(impl: DeviceStatusRepositoryImpl): DeviceStatusRepository
}
