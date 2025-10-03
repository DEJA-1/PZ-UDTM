package com.km.pz_app.di

import com.km.pz_app.presentation.nav.Destination
import com.km.pz_app.presentation.nav.navigator.DefaultNavigator
import com.km.pz_app.presentation.nav.navigator.INavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesNavigator(): INavigator {
        return DefaultNavigator(startDestination = Destination.Home)
    }
}