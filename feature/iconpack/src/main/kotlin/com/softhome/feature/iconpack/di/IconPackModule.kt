package com.softhome.feature.iconpack.di

import com.softhome.feature.iconpack.data.AppFilterParser
import com.softhome.feature.iconpack.data.IconPackRepository
import com.softhome.feature.iconpack.data.IconPackRepositoryImpl
import com.softhome.feature.iconpack.domain.IconResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IconPackModule {

    @Provides
    @Singleton
    fun provideAppFilterParser(): AppFilterParser = AppFilterParser()

    @Provides
    @Singleton
    fun provideIconResolver(): IconResolver = IconResolver()

    @Provides
    @Singleton
    fun provideIconPackRepository(impl: IconPackRepositoryImpl): IconPackRepository = impl
}
