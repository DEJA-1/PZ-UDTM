package com.km.pz_app.di

import com.km.pz_app.data.repository.SystemRepository
import com.km.pz_app.domain.repository.ISystemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSystemRepository(impl: SystemRepository): ISystemRepository
}
